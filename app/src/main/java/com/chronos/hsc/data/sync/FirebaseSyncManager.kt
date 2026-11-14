package com.chronos.hsc.data.sync

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.chronos.hsc.data.local.DailyTaskSlice
import com.chronos.hsc.data.local.DistributionCurve
import com.chronos.hsc.data.local.EnergyLevel
import com.chronos.hsc.data.local.ExamCountdownEntity
import com.chronos.hsc.data.local.HscPaper
import com.chronos.hsc.data.local.HscSubject
import com.chronos.hsc.data.local.NeuroDrillType
import com.chronos.hsc.data.local.NeuroSessionEntity
import com.chronos.hsc.data.local.StudyDao
import com.chronos.hsc.data.local.StudyTopicChain
import com.chronos.hsc.data.local.UserStats
import com.chronos.hsc.data.store.BatchExamRecord
import com.chronos.hsc.data.store.BatchExamStore
import com.chronos.hsc.data.store.CustomSubject
import com.chronos.hsc.data.store.CustomSubjectAndAiStore
import com.chronos.hsc.data.store.CustomSubjectLink
import com.chronos.hsc.widget.ChronosWidgetUpdater
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestoreSettings
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class SyncStatus {
    OFFLINE_LOCAL_ONLY,
    SYNCING,
    SYNCED_LIVE,
    SYNC_ERROR
}

class FirebaseSyncManager(
    context: Context,
    private val dao: StudyDao
) {
    companion object {
        const val WEB_CLIENT_ID = "180920389048-jut40ci1nfc5orb0n8at0rlmjb5174gh.apps.googleusercontent.com"

        private const val PREFS_NAME = "chronos_firebase_sync_prefs"
        private const val KEY_DEVICE_ID = "key_local_device_id"
        private const val KEY_LAST_SYNCED_TS = "key_last_synced_timestamp"
        private const val KEY_TOMBSTONE_CHAINS = "key_tombstone_chains"
        private const val KEY_TOMBSTONE_SLICES = "key_tombstone_slices"
        private const val KEY_TOMBSTONE_EXAMS = "key_tombstone_exams"
        private const val KEY_TOMBSTONE_BATCH_EXAMS = "key_tombstone_batch_exams"
        private const val KEY_TOMBSTONE_CUSTOM_SUBJECTS = "key_tombstone_custom_subjects"
        private const val KEY_SLICE_MOD_TIMES = "key_slice_mod_times"
        private const val KEY_CHAIN_MOD_TIMES = "key_chain_mod_times"
        private const val MAX_TOMBSTONES = 1500
        private const val FIRESTORE_BATCH_CHUNK_SIZE = 400

        @Volatile
        private var instance: FirebaseSyncManager? = null

        fun getInstanceIfInitialized(): FirebaseSyncManager? = instance

        fun notifyLocalMutation() {
            instance?.scheduleCloudPush()
        }
    }

    private val appContext: Context = context.applicationContext
    private val syncPrefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val localDeviceId: String = syncPrefs.getString(KEY_DEVICE_ID, null) ?: run {
        val created = UUID.randomUUID().toString()
        syncPrefs.edit().putString(KEY_DEVICE_ID, created).apply()
        created
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _syncErrorMessage.value = throwable.localizedMessage ?: "Sync background error"
        if (auth?.currentUser != null) {
            _syncStatus.value = SyncStatus.SYNC_ERROR
        } else {
            _syncStatus.value = SyncStatus.OFFLINE_LOCAL_ONLY
        }
    }

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)
    private val syncMutex = Mutex()
    private val localPrefsLock = Any()

    private val _currentUserEmail = MutableStateFlow<String?>(null)
    val currentUserEmail: StateFlow<String?> = _currentUserEmail.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.OFFLINE_LOCAL_ONLY)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncedTimestamp = MutableStateFlow<Long?>(
        syncPrefs.getLong(KEY_LAST_SYNCED_TS, 0L).takeIf { it > 0L }
    )
    val lastSyncedTimestamp: StateFlow<Long?> = _lastSyncedTimestamp.asStateFlow()

    private val _syncErrorMessage = MutableStateFlow<String?>(null)
    val syncErrorMessage: StateFlow<String?> = _syncErrorMessage.asStateFlow()

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance().apply {
                try {
                    firestoreSettings = firestoreSettings {
                        isPersistenceEnabled = true
                    }
                } catch (_: Exception) {
                    // Firestore settings may already be frozen cleanly
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private val activeListeners = mutableListOf<ListenerRegistration>()
    private val pendingOrphanSlices = mutableMapOf<String, DailyTaskSlice>()
    private var scheduledPushJob: Job? = null

    @Volatile
    private var isApplyingRemoteUpdate = false

    init {
        instance = this
        // Initialize Firestore offline persistence eagerly as early as safe
        firestore
        // Hook store callbacks so BatchExam and CustomSubject edits sync live automatically
        BatchExamStore.onLocalMutation = { scheduleCloudPush() }
        BatchExamStore.onLocalExamDeleted = { examId ->
            recordDeletedBatchExamTombstone(examId)
            scheduleCloudPush()
        }
        CustomSubjectAndAiStore.onLocalMutation = { scheduleCloudPush() }
        CustomSubjectAndAiStore.onLocalCustomSubjectDeleted = { customSubId ->
            recordDeletedCustomSubjectTombstone(customSubId)
            scheduleCloudPush()
        }

        val currentUser = try {
            auth?.currentUser
        } catch (_: Exception) {
            null
        }
        _currentUserEmail.value = currentUser?.email
        if (currentUser != null) {
            _syncStatus.value = SyncStatus.SYNCING
        }
    }

    fun initializeOnAppStart() {
        val currentUser = try {
            auth?.currentUser
        } catch (_: Exception) {
            null
        }
        if (currentUser != null) {
            _currentUserEmail.value = currentUser.email
            syncScope.launch {
                performInitialZeroLossMergeAndStartRealtimeSync()
            }
        } else {
            _currentUserEmail.value = null
            _syncStatus.value = SyncStatus.OFFLINE_LOCAL_ONLY
        }
    }

    private fun resolveWebClientId(context: Context): String {
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                context.getString(resId).takeIf { it.isNotBlank() } ?: WEB_CLIENT_ID
            } else {
                WEB_CLIENT_ID
            }
        } catch (_: Exception) {
            WEB_CLIENT_ID
        }
    }

    fun getGoogleSignInIntent(context: Context): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(resolveWebClientId(context))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    fun handleSignInResult(
        intent: Intent?,
        onComplete: (Boolean, String) -> Unit
    ) {
        if (intent == null) {
            onComplete(false, "Google Sign-In was cancelled.")
            return
        }

        syncScope.launch {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        onComplete(false, "Google Sign-In failed: Missing ID Token.")
                    }
                    return@launch
                }

                val firebaseAuth = auth ?: throw IllegalStateException("FirebaseAuth unavailable.")
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                val email = authResult.user?.email ?: account.email ?: "Signed-In User"
                _currentUserEmail.value = email
                _syncErrorMessage.value = null

                val mergeSucceeded = performInitialZeroLossMergeAndStartRealtimeSync()
                withContext(Dispatchers.Main) {
                    if (mergeSucceeded) {
                        onComplete(true, "Signed in as $email & uploaded offline data!")
                    } else {
                        onComplete(true, "Signed in as $email (will finish cloud sync when online).")
                    }
                }
            } catch (e: ApiException) {
                _syncStatus.value = if (auth?.currentUser != null) SyncStatus.SYNC_ERROR else SyncStatus.OFFLINE_LOCAL_ONLY
                val msg = "Google Sign-In error (${e.statusCode}): ${e.localizedMessage ?: "Check Play Services / SHA-1"}"
                _syncErrorMessage.value = msg
                withContext(Dispatchers.Main) {
                    onComplete(false, msg)
                }
            } catch (e: Exception) {
                _syncStatus.value = if (auth?.currentUser != null) SyncStatus.SYNC_ERROR else SyncStatus.OFFLINE_LOCAL_ONLY
                val msg = e.localizedMessage ?: "Authentication failed"
                _syncErrorMessage.value = msg
                withContext(Dispatchers.Main) {
                    onComplete(false, msg)
                }
            }
        }
    }

    fun syncNow(onComplete: ((Boolean, String) -> Unit)? = null) {
        val currentUser = try {
            auth?.currentUser
        } catch (_: Exception) {
            null
        }
        if (currentUser == null) {
            _syncStatus.value = SyncStatus.OFFLINE_LOCAL_ONLY
            onComplete?.invoke(false, "Not signed in.")
            return
        }

        syncScope.launch {
            val ok = performInitialZeroLossMergeAndStartRealtimeSync()
            withContext(Dispatchers.Main) {
                if (ok) {
                    onComplete?.invoke(true, "Cloud sync completed cleanly!")
                } else {
                    onComplete?.invoke(false, _syncErrorMessage.value ?: "Cloud sync unavailable while offline.")
                }
            }
        }
    }

    fun signOutKeepLocalData(context: Context, onComplete: (() -> Unit)? = null) {
        scheduledPushJob?.cancel()
        detachListeners()
        syncScope.launch {
            try {
                auth?.signOut()
                try {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(resolveWebClientId(context))
                        .requestEmail()
                        .build()
                    GoogleSignIn.getClient(context, gso).signOut().await()
                } catch (_: Exception) {}
            } finally {
                _currentUserEmail.value = null
                _syncStatus.value = SyncStatus.OFFLINE_LOCAL_ONLY
                _syncErrorMessage.value = null
                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            }
        }
    }

    // =========================================================================
    // TOMBSTONE & LOCAL MODIFICATION TRACKING (ZERO-LOSS DELETION SYNC)
    // =========================================================================

    fun recordSliceModified(sliceId: String) {
        synchronized(localPrefsLock) {
            val map = loadTimestampMap(KEY_SLICE_MOD_TIMES)
            map[sliceId] = System.currentTimeMillis()
            saveTimestampMap(KEY_SLICE_MOD_TIMES, map)
        }
    }

    fun recordChainModified(chainId: String) {
        synchronized(localPrefsLock) {
            val map = loadTimestampMap(KEY_CHAIN_MOD_TIMES)
            map[chainId] = System.currentTimeMillis()
            saveTimestampMap(KEY_CHAIN_MOD_TIMES, map)
        }
    }

    fun recordDeletedChainTombstone(chainId: String, sliceIds: List<String> = emptyList()) {
        synchronized(localPrefsLock) {
            addIdsToSetPref(KEY_TOMBSTONE_CHAINS, listOf(chainId))
            if (sliceIds.isNotEmpty()) {
                addIdsToSetPref(KEY_TOMBSTONE_SLICES, sliceIds)
            }
        }
    }

    fun removeChainTombstonesForRestore(chainId: String, sliceIds: List<String>) {
        synchronized(localPrefsLock) {
            removeIdsFromSetPref(KEY_TOMBSTONE_CHAINS, listOf(chainId))
            if (sliceIds.isNotEmpty()) {
                removeIdsFromSetPref(KEY_TOMBSTONE_SLICES, sliceIds)
            }
        }
    }

    fun recordDeletedSliceTombstone(sliceId: String) {
        synchronized(localPrefsLock) {
            addIdsToSetPref(KEY_TOMBSTONE_SLICES, listOf(sliceId))
        }
    }

    fun recordDeletedExamCountdownTombstone(examId: String) {
        synchronized(localPrefsLock) {
            addIdsToSetPref(KEY_TOMBSTONE_EXAMS, listOf(examId))
        }
    }

    private fun recordDeletedBatchExamTombstone(examId: String) {
        synchronized(localPrefsLock) {
            addIdsToSetPref(KEY_TOMBSTONE_BATCH_EXAMS, listOf(examId))
        }
    }

    private fun recordDeletedCustomSubjectTombstone(subjectId: String) {
        synchronized(localPrefsLock) {
            addIdsToSetPref(KEY_TOMBSTONE_CUSTOM_SUBJECTS, listOf(subjectId))
        }
    }

    // =========================================================================
    // LIVE CLOUD PUSH SCHEDULING
    // =========================================================================

    fun scheduleCloudPush() {
        if (isApplyingRemoteUpdate) return
        val currentUser = try {
            auth?.currentUser
        } catch (_: Exception) {
            null
        } ?: return

        scheduledPushJob?.cancel()
        scheduledPushJob = syncScope.launch {
            delay(650L)
            pushLocalStateToCloud(currentUser.uid)
        }
    }

    private suspend fun pushLocalStateToCloud(uid: String) {
        val db = firestore ?: return
        syncMutex.withLock {
            try {
                if (auth?.currentUser?.uid != uid) return
                _syncStatus.value = SyncStatus.SYNCING

                val localChains = dao.getAllChainsSnapshot()
                val localSlices = dao.getAllSlicesSnapshot()
                val localStats = dao.getUserStatsSnapshot() ?: UserStats()
                val localExams = dao.getAllExamCountdownsSnapshot()
                val localNeuro = dao.getAllNeuroSessionsSnapshot()
                val localBatchExams = BatchExamStore.getAllExams()
                val localCustomSubjects = CustomSubjectAndAiStore.getCustomSubjects()
                val localChainLinks = CustomSubjectAndAiStore.getAllChainLinks()

                val deletedChains = getIdSetPref(KEY_TOMBSTONE_CHAINS)
                val deletedSlices = getIdSetPref(KEY_TOMBSTONE_SLICES)
                val deletedExams = getIdSetPref(KEY_TOMBSTONE_EXAMS)
                val deletedBatchExams = getIdSetPref(KEY_TOMBSTONE_BATCH_EXAMS)
                val deletedCustomSubjects = getIdSetPref(KEY_TOMBSTONE_CUSTOM_SUBJECTS)

                val sliceModTimes = synchronized(localPrefsLock) { loadTimestampMap(KEY_SLICE_MOD_TIMES) }
                val chainModTimes = synchronized(localPrefsLock) { loadTimestampMap(KEY_CHAIN_MOD_TIMES) }
                val now = System.currentTimeMillis()

                val userDoc = db.collection("users").document(uid)
                val ops = mutableListOf<BatchOp>()

                for (chain in localChains) {
                    val ts = chainModTimes[chain.chainId] ?: now
                    ops.add(
                        BatchOp.Set(
                            userDoc.collection("chains").document(chain.chainId),
                            chain.toFirestoreMap(localDeviceId, ts)
                        )
                    )
                }

                for (slice in localSlices) {
                    val ts = sliceModTimes[slice.sliceId] ?: slice.completedAtTimestamp ?: now
                    ops.add(
                        BatchOp.Set(
                            userDoc.collection("slices").document(slice.sliceId),
                            slice.toFirestoreMap(localDeviceId, ts)
                        )
                    )
                }

                for (exam in localExams) {
                    ops.add(
                        BatchOp.Set(
                            userDoc.collection("exam_countdowns").document(exam.examId),
                            exam.toFirestoreMap(localDeviceId, now)
                        )
                    )
                }

                for (neuro in localNeuro) {
                    ops.add(
                        BatchOp.Set(
                            userDoc.collection("neuro_sessions").document(neuro.sessionId),
                            neuro.toFirestoreMap(localDeviceId, neuro.timestamp)
                        )
                    )
                }

                ops.add(
                    BatchOp.Set(
                        userDoc.collection("meta").document("user_stats"),
                        localStats.toFirestoreMap(localDeviceId, now)
                    )
                )

                val allTombstones = (deletedChains + deletedSlices + deletedExams).toList().takeLast(MAX_TOMBSTONES)
                val sharedMeta = hashMapOf<String, Any?>(
                    "batchExamsJson" to BatchExamStore.exportToJsonString(localBatchExams),
                    "customSubjectsJson" to CustomSubjectAndAiStore.exportCustomSubjectsJson(localCustomSubjects),
                    "chainLinksJson" to CustomSubjectAndAiStore.exportChainLinksJson(localChainLinks),
                    "deletedTombstoneIds" to allTombstones,
                    "deletedChainIds" to deletedChains.toList().takeLast(MAX_TOMBSTONES),
                    "deletedSliceIds" to deletedSlices.toList().takeLast(MAX_TOMBSTONES),
                    "deletedExamCountdownIds" to deletedExams.toList().takeLast(MAX_TOMBSTONES),
                    "deletedBatchExamIds" to deletedBatchExams.toList().takeLast(MAX_TOMBSTONES),
                    "deletedCustomSubjectIds" to deletedCustomSubjects.toList().takeLast(MAX_TOMBSTONES),
                    "lastUpdatedByDeviceId" to localDeviceId,
                    "lastModifiedTimestamp" to now
                )
                ops.add(BatchOp.Set(userDoc.collection("meta").document("shared_stores"), sharedMeta))

                for (chainId in deletedChains) {
                    ops.add(BatchOp.Delete(userDoc.collection("chains").document(chainId)))
                }
                for (sliceId in deletedSlices) {
                    ops.add(BatchOp.Delete(userDoc.collection("slices").document(sliceId)))
                }
                for (examId in deletedExams) {
                    ops.add(BatchOp.Delete(userDoc.collection("exam_countdowns").document(examId)))
                }

                commitChunkedBatches(db, ops)

                val completedAt = System.currentTimeMillis()
                syncPrefs.edit().putLong(KEY_LAST_SYNCED_TS, completedAt).apply()
                _lastSyncedTimestamp.value = completedAt
                _syncErrorMessage.value = null
                _syncStatus.value = SyncStatus.SYNCED_LIVE
            } catch (e: Exception) {
                _syncErrorMessage.value = e.localizedMessage ?: "Cloud push failed"
                _syncStatus.value = SyncStatus.SYNC_ERROR
            }
        }
    }

    // =========================================================================
    // ZERO-LOSS INITIAL UNION + PROGRESS-PRESERVING MERGE & REALTIME LISTENERS
    // =========================================================================

    suspend fun performInitialZeroLossMergeAndStartRealtimeSync(): Boolean {
        val uid = try {
            auth?.currentUser?.uid
        } catch (_: Exception) {
            null
        } ?: run {
            _syncStatus.value = SyncStatus.OFFLINE_LOCAL_ONLY
            return false
        }
        val db = firestore ?: run {
            _syncStatus.value = SyncStatus.SYNC_ERROR
            return false
        }

        return syncMutex.withLock {
            try {
                _syncStatus.value = SyncStatus.SYNCING

                // 1. Fetch ALL existing local offline state
                val localChains = dao.getAllChainsSnapshot()
                val localSlices = dao.getAllSlicesSnapshot()
                val localStats = dao.getUserStatsSnapshot() ?: UserStats()
                val localExams = dao.getAllExamCountdownsSnapshot()
                val localNeuro = dao.getAllNeuroSessionsSnapshot()
                val localBatchExams = BatchExamStore.getAllExams()
                val localCustomSubjects = CustomSubjectAndAiStore.getCustomSubjects()
                val localChainLinks = CustomSubjectAndAiStore.getAllChainLinks()

                val localSliceModTimes = synchronized(localPrefsLock) { loadTimestampMap(KEY_SLICE_MOD_TIMES) }
                val localChainModTimes = synchronized(localPrefsLock) { loadTimestampMap(KEY_CHAIN_MOD_TIMES) }

                // 2. Fetch existing Cloud state under users/{uid}
                val userDoc = db.collection("users").document(uid)
                val cloudChainsSnap = userDoc.collection("chains").get().await()
                val cloudSlicesSnap = userDoc.collection("slices").get().await()
                val cloudExamsSnap = userDoc.collection("exam_countdowns").get().await()
                val cloudNeuroSnap = userDoc.collection("neuro_sessions").get().await()
                val cloudStatsSnap = userDoc.collection("meta").document("user_stats").get().await()
                val cloudSharedSnap = userDoc.collection("meta").document("shared_stores").get().await()

                // Parse tombstones cleanly (ONLY items user explicitly deleted)
                val cloudTombstonesGeneric = cloudSharedSnap.getStringList("deletedTombstoneIds").toSet()
                val cloudDeletedChains = cloudSharedSnap.getStringList("deletedChainIds").toSet()
                val cloudDeletedSlices = cloudSharedSnap.getStringList("deletedSliceIds").toSet()
                val cloudDeletedExams = cloudSharedSnap.getStringList("deletedExamCountdownIds").toSet()
                val cloudDeletedBatchExams = cloudSharedSnap.getStringList("deletedBatchExamIds").toSet()
                val cloudDeletedCustomSubs = cloudSharedSnap.getStringList("deletedCustomSubjectIds").toSet()

                val localDeletedChains = getIdSetPref(KEY_TOMBSTONE_CHAINS)
                val localDeletedSlices = getIdSetPref(KEY_TOMBSTONE_SLICES)
                val localDeletedExams = getIdSetPref(KEY_TOMBSTONE_EXAMS)
                val localDeletedBatchExams = getIdSetPref(KEY_TOMBSTONE_BATCH_EXAMS)
                val localDeletedCustomSubs = getIdSetPref(KEY_TOMBSTONE_CUSTOM_SUBJECTS)

                val allDeletedChains = (localDeletedChains + cloudDeletedChains + cloudTombstonesGeneric)
                val allDeletedSlices = (localDeletedSlices + cloudDeletedSlices + cloudTombstonesGeneric)
                val allDeletedExams = (localDeletedExams + cloudDeletedExams + cloudTombstonesGeneric)
                val allDeletedBatchExams = (localDeletedBatchExams + cloudDeletedBatchExams)
                val allDeletedCustomSubs = (localDeletedCustomSubs + cloudDeletedCustomSubs)

                synchronized(localPrefsLock) {
                    addIdsToSetPref(KEY_TOMBSTONE_CHAINS, allDeletedChains)
                    addIdsToSetPref(KEY_TOMBSTONE_SLICES, allDeletedSlices)
                    addIdsToSetPref(KEY_TOMBSTONE_EXAMS, allDeletedExams)
                    addIdsToSetPref(KEY_TOMBSTONE_BATCH_EXAMS, allDeletedBatchExams)
                    addIdsToSetPref(KEY_TOMBSTONE_CUSTOM_SUBJECTS, allDeletedCustomSubs)
                }

                // 3. Smart Non-Destructive Union + Highest Progress Merge

                // 3A. Chains Union
                val cloudChainsWithTs = cloudChainsSnap.documents.mapNotNull { doc ->
                    doc.toStudyTopicChain()?.let { it to (doc.getLong("lastModifiedTimestamp") ?: 0L) }
                }
                val localChainMap = localChains.associateBy { it.chainId }
                val cloudChainMap = cloudChainsWithTs.associate { it.first.chainId to it }
                val mergedChains = mutableListOf<StudyTopicChain>()
                val mergedChainTimestamps = mutableMapOf<String, Long>()
                val now = System.currentTimeMillis()

                for (chainId in (localChainMap.keys + cloudChainMap.keys)) {
                    if (chainId in allDeletedChains) continue
                    val localC = localChainMap[chainId]
                    val cloudPair = cloudChainMap[chainId]
                    val cloudC = cloudPair?.first
                    val cloudTs = cloudPair?.second ?: 0L
                    val localTs = localChainModTimes[chainId] ?: 0L

                    val merged = when {
                        localC != null && cloudC == null -> localC
                        localC == null && cloudC != null -> cloudC
                        localC != null && cloudC != null -> {
                            val base = if (cloudTs > localTs) cloudC else localC
                            base.copy(
                                isChainCompleted = localC.isChainCompleted || cloudC.isChainCompleted,
                                totalDays = maxOf(localC.totalDays, cloudC.totalDays),
                                bossMaxHp = maxOf(localC.bossMaxHp, cloudC.bossMaxHp)
                            )
                        }
                        else -> null
                    }
                    if (merged != null) {
                        mergedChains.add(merged)
                        mergedChainTimestamps[chainId] = maxOf(localTs, cloudTs).takeIf { it > 0L } ?: now
                    }
                }
                val validChainIds = mergedChains.map { it.chainId }.toHashSet()

                // 3B. Slices Progress-Preserving Merge
                val cloudSlicesWithTs = cloudSlicesSnap.documents.mapNotNull { doc ->
                    doc.toDailyTaskSlice()?.let { it to (doc.getLong("lastModifiedTimestamp") ?: 0L) }
                }
                val localSliceMap = localSlices.associateBy { it.sliceId }
                val cloudSliceMap = cloudSlicesWithTs.associate { it.first.sliceId to it }
                val mergedSlices = mutableListOf<DailyTaskSlice>()
                val mergedSliceTimestamps = mutableMapOf<String, Long>()

                for (sliceId in (localSliceMap.keys + cloudSliceMap.keys)) {
                    val localS = localSliceMap[sliceId]
                    val cloudPair = cloudSliceMap[sliceId]
                    val cloudS = cloudPair?.first
                    val chainId = (localS ?: cloudS)?.chainId ?: continue
                    if (sliceId in allDeletedSlices || chainId in allDeletedChains || chainId !in validChainIds) continue

                    val localTs = localSliceModTimes[sliceId] ?: localS?.completedAtTimestamp ?: 0L
                    val cloudTs = cloudPair?.second ?: cloudS?.completedAtTimestamp ?: 0L

                    val merged = when {
                        localS != null && cloudS == null -> localS
                        localS == null && cloudS != null -> cloudS
                        localS != null && cloudS != null -> mergeSliceConflict(localS, localTs, cloudS, cloudTs)
                        else -> null
                    }
                    if (merged != null) {
                        mergedSlices.add(merged)
                        mergedSliceTimestamps[sliceId] = maxOf(localTs, cloudTs).takeIf { it > 0L } ?: now
                    }
                }

                // Persist merged slice/chain timestamps cleanly
                synchronized(localPrefsLock) {
                    val updatedSliceTs = loadTimestampMap(KEY_SLICE_MOD_TIMES).apply { putAll(mergedSliceTimestamps) }
                    saveTimestampMap(KEY_SLICE_MOD_TIMES, updatedSliceTs)
                    val updatedChainTs = loadTimestampMap(KEY_CHAIN_MOD_TIMES).apply { putAll(mergedChainTimestamps) }
                    saveTimestampMap(KEY_CHAIN_MOD_TIMES, updatedChainTs)
                }

                // 3C. UserStats (XP, Level, Streak — Highest Progress Wins)
                val cloudStats = cloudStatsSnap.toUserStats()
                val mergedTotalXp = maxOf(localStats.totalXp, cloudStats?.totalXp ?: 0)
                val mergedStats = UserStats(
                    id = 1,
                    totalXp = mergedTotalXp,
                    currentLevel = maxOf(localStats.currentLevel, cloudStats?.currentLevel ?: 1, (mergedTotalXp / 500) + 1),
                    streakDays = maxOf(localStats.streakDays, cloudStats?.streakDays ?: 0),
                    lastStudyEpochDay = listOfNotNull(localStats.lastStudyEpochDay, cloudStats?.lastStudyEpochDay).maxOrNull()
                )

                // 3D. Exam Countdowns Union
                val cloudExams = cloudExamsSnap.documents.mapNotNull { it.toExamCountdownEntity() }
                val examMap = linkedMapOf<String, ExamCountdownEntity>()
                for (exam in localExams) {
                    if (exam.examId !in allDeletedExams) examMap[exam.examId] = exam
                }
                for (exam in cloudExams) {
                    if (exam.examId !in allDeletedExams && exam.examId !in examMap) {
                        examMap[exam.examId] = exam
                    }
                }
                val mergedExams = normalizePinnedExam(examMap.values.toList())

                // 3E. Neuro Sessions Union
                val cloudNeuro = cloudNeuroSnap.documents.mapNotNull { it.toNeuroSessionEntity() }
                val neuroMap = linkedMapOf<String, NeuroSessionEntity>()
                for (s in localNeuro) neuroMap[s.sessionId] = s
                for (s in cloudNeuro) if (s.sessionId !in neuroMap) neuroMap[s.sessionId] = s
                val mergedNeuro = neuroMap.values.toList()

                // 3F. SharedPreferences Stores (Custom Subjects, Chain Links & Batch Exams)
                val cloudCustomSubjects = CustomSubjectAndAiStore.parseCustomSubjectsJson(
                    cloudSharedSnap.getString("customSubjectsJson")
                )
                val mergedCustomSubjects = mergeCustomSubjects(
                    local = localCustomSubjects,
                    cloud = cloudCustomSubjects,
                    deletedIds = allDeletedCustomSubs
                )

                val cloudChainLinks = CustomSubjectAndAiStore.parseChainLinksJson(
                    cloudSharedSnap.getString("chainLinksJson")
                )
                val mergedChainLinks = mergeChainLinks(
                    local = localChainLinks,
                    cloud = cloudChainLinks,
                    deletedChainIds = allDeletedChains,
                    validChainIds = validChainIds
                )

                val cloudBatchExams = BatchExamStore.parseJsonArray(
                    cloudSharedSnap.getString("batchExamsJson")
                )
                val mergedBatchExams = mergeBatchExams(
                    local = localBatchExams,
                    cloud = cloudBatchExams,
                    deletedIds = allDeletedBatchExams
                )

                // 4. Write Merged State Locally First (Single Atomic Transaction, No Data Wiped!)
                isApplyingRemoteUpdate = true
                try {
                    dao.applyCloudMerge(
                        chainsToUpsert = mergedChains,
                        slicesToUpsert = mergedSlices,
                        statsToUpsert = mergedStats,
                        examsToUpsert = mergedExams,
                        neuroSessionsToUpsert = mergedNeuro,
                        tombstonedChainIds = allDeletedChains,
                        tombstonedSliceIds = allDeletedSlices,
                        tombstonedExamIds = allDeletedExams
                    )
                    CustomSubjectAndAiStore.replaceAllCustomSubjectsFromSync(mergedCustomSubjects)
                    CustomSubjectAndAiStore.replaceAllChainLinksFromSync(mergedChainLinks)
                    BatchExamStore.replaceAllFromSync(mergedBatchExams)
                    ChronosWidgetUpdater.requestUpdate(appContext)
                } finally {
                    isApplyingRemoteUpdate = false
                }

                // 5. Commit Merged State & Tombstones back to Firestore via Chunked WriteBatch (<= 400 ops)
                val ops = mutableListOf<BatchOp>()
                for (chain in mergedChains) {
                    val ts = mergedChainTimestamps[chain.chainId] ?: now
                    ops.add(
                        BatchOp.Set(
                            userDoc.collection("chains").document(chain.chainId),
                            chain.toFirestoreMap(localDeviceId, ts)
                        )
                    )
                }
                for (slice in mergedSlices) {
                    val ts = mergedSliceTimestamps[slice.sliceId] ?: slice.completedAtTimestamp ?: now
                    ops.add(
                        BatchOp.Set(
                            userDoc.collection("slices").document(slice.sliceId),
                            slice.toFirestoreMap(localDeviceId, ts)
                        )
                    )
                }
                for (exam in mergedExams) {
                    ops.add(
                        BatchOp.Set(
                            userDoc.collection("exam_countdowns").document(exam.examId),
                            exam.toFirestoreMap(localDeviceId, now)
                        )
                    )
                }
                for (neuro in mergedNeuro) {
                    ops.add(
                        BatchOp.Set(
                            userDoc.collection("neuro_sessions").document(neuro.sessionId),
                            neuro.toFirestoreMap(localDeviceId, neuro.timestamp)
                        )
                    )
                }

                ops.add(
                    BatchOp.Set(
                        userDoc.collection("meta").document("user_stats"),
                        mergedStats.toFirestoreMap(localDeviceId, now)
                    )
                )

                val allGenericTombstones = (allDeletedChains + allDeletedSlices + allDeletedExams).toList().takeLast(MAX_TOMBSTONES)
                val mergedSharedMap = hashMapOf<String, Any?>(
                    "batchExamsJson" to BatchExamStore.exportToJsonString(mergedBatchExams),
                    "customSubjectsJson" to CustomSubjectAndAiStore.exportCustomSubjectsJson(mergedCustomSubjects),
                    "chainLinksJson" to CustomSubjectAndAiStore.exportChainLinksJson(mergedChainLinks),
                    "deletedTombstoneIds" to allGenericTombstones,
                    "deletedChainIds" to allDeletedChains.toList().takeLast(MAX_TOMBSTONES),
                    "deletedSliceIds" to allDeletedSlices.toList().takeLast(MAX_TOMBSTONES),
                    "deletedExamCountdownIds" to allDeletedExams.toList().takeLast(MAX_TOMBSTONES),
                    "deletedBatchExamIds" to allDeletedBatchExams.toList().takeLast(MAX_TOMBSTONES),
                    "deletedCustomSubjectIds" to allDeletedCustomSubs.toList().takeLast(MAX_TOMBSTONES),
                    "lastUpdatedByDeviceId" to localDeviceId,
                    "lastModifiedTimestamp" to now
                )
                ops.add(BatchOp.Set(userDoc.collection("meta").document("shared_stores"), mergedSharedMap))

                for (chainId in allDeletedChains) ops.add(BatchOp.Delete(userDoc.collection("chains").document(chainId)))
                for (sliceId in allDeletedSlices) ops.add(BatchOp.Delete(userDoc.collection("slices").document(sliceId)))
                for (examId in allDeletedExams) ops.add(BatchOp.Delete(userDoc.collection("exam_countdowns").document(examId)))

                commitChunkedBatches(db, ops)

                val completedAt = System.currentTimeMillis()
                syncPrefs.edit().putLong(KEY_LAST_SYNCED_TS, completedAt).apply()
                _lastSyncedTimestamp.value = completedAt
                _syncErrorMessage.value = null
                _syncStatus.value = SyncStatus.SYNCED_LIVE

                // 6. Attach Live Multi-Device Realtime Listeners
                startRealtimeListenersLocked(db, uid)
                true
            } catch (e: Exception) {
                _syncErrorMessage.value = e.localizedMessage ?: "Cloud sync error"
                _syncStatus.value = SyncStatus.SYNC_ERROR
                startRealtimeListenersLocked(db, uid)
                false
            }
        }
    }

    private fun startRealtimeListenersLocked(db: FirebaseFirestore, uid: String) {
        detachListeners()
        val userDoc = db.collection("users").document(uid)

        // 1. Live Chains Listener
        activeListeners += userDoc.collection("chains").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val remoteDocs = snapshot.documentChanges.mapNotNull { change ->
                val doc = change.document
                if (doc.getString("lastUpdatedByDeviceId") == localDeviceId) return@mapNotNull null
                doc
            }
            if (remoteDocs.isEmpty()) return@addSnapshotListener

            syncScope.launch {
                syncMutex.withLock {
                    isApplyingRemoteUpdate = true
                    try {
                        val deletedChains = getIdSetPref(KEY_TOMBSTONE_CHAINS)
                        val existingChains = dao.getAllChainsSnapshot().associateBy { it.chainId }
                        val localChainModTimes = synchronized(localPrefsLock) { loadTimestampMap(KEY_CHAIN_MOD_TIMES) }
                        val toUpsert = mutableListOf<StudyTopicChain>()

                        for (doc in remoteDocs) {
                            val remoteChain = doc.toStudyTopicChain() ?: continue
                            if (remoteChain.chainId in deletedChains) continue
                            val remoteTs = doc.getLong("lastModifiedTimestamp") ?: 0L
                            val localChain = existingChains[remoteChain.chainId]
                            val localTs = localChainModTimes[remoteChain.chainId] ?: 0L

                            val resolved = if (localChain == null) {
                                remoteChain
                            } else {
                                val base = if (remoteTs >= localTs) remoteChain else localChain
                                base.copy(
                                    isChainCompleted = localChain.isChainCompleted || remoteChain.isChainCompleted,
                                    totalDays = maxOf(localChain.totalDays, remoteChain.totalDays),
                                    bossMaxHp = maxOf(localChain.bossMaxHp, remoteChain.bossMaxHp)
                                )
                            }
                            if (resolved != localChain) {
                                toUpsert.add(resolved)
                                synchronized(localPrefsLock) {
                                    val map = loadTimestampMap(KEY_CHAIN_MOD_TIMES)
                                    map[resolved.chainId] = maxOf(localTs, remoteTs)
                                    saveTimestampMap(KEY_CHAIN_MOD_TIMES, map)
                                }
                            }
                        }

                        if (toUpsert.isNotEmpty()) {
                            dao.upsertChains(toUpsert)
                            flushPendingOrphanSlicesLocked()
                            ChronosWidgetUpdater.requestUpdate(appContext)
                        }
                        markLiveSyncSuccess()
                    } finally {
                        isApplyingRemoteUpdate = false
                    }
                }
            }
        }

        // 2. Live Slices Listener
        activeListeners += userDoc.collection("slices").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val remoteDocs = snapshot.documentChanges.mapNotNull { change ->
                val doc = change.document
                if (doc.getString("lastUpdatedByDeviceId") == localDeviceId) return@mapNotNull null
                doc
            }
            if (remoteDocs.isEmpty()) return@addSnapshotListener

            syncScope.launch {
                syncMutex.withLock {
                    isApplyingRemoteUpdate = true
                    try {
                        val deletedChains = getIdSetPref(KEY_TOMBSTONE_CHAINS)
                        val deletedSlices = getIdSetPref(KEY_TOMBSTONE_SLICES)
                        val validChainIds = dao.getAllChainsSnapshot().map { it.chainId }.toHashSet()
                        val existingSlices = dao.getAllSlicesSnapshot().associateBy { it.sliceId }
                        val localModTimes = synchronized(localPrefsLock) { loadTimestampMap(KEY_SLICE_MOD_TIMES) }
                        val toUpsert = mutableListOf<DailyTaskSlice>()

                        for (doc in remoteDocs) {
                            val remoteSlice = doc.toDailyTaskSlice() ?: continue
                            if (remoteSlice.sliceId in deletedSlices || remoteSlice.chainId in deletedChains) continue
                            val remoteTs = doc.getLong("lastModifiedTimestamp") ?: remoteSlice.completedAtTimestamp ?: 0L
                            val localSlice = existingSlices[remoteSlice.sliceId]
                            val localTs = localModTimes[remoteSlice.sliceId] ?: localSlice?.completedAtTimestamp ?: 0L

                            val resolved = if (localSlice == null) {
                                remoteSlice
                            } else {
                                mergeSliceConflict(localSlice, localTs, remoteSlice, remoteTs)
                            }

                            if (resolved.chainId !in validChainIds) {
                                pendingOrphanSlices[resolved.sliceId] = resolved
                                continue
                            }

                            if (resolved != localSlice) {
                                toUpsert.add(resolved)
                                synchronized(localPrefsLock) {
                                    val map = loadTimestampMap(KEY_SLICE_MOD_TIMES)
                                    map[resolved.sliceId] = maxOf(localTs, remoteTs)
                                    saveTimestampMap(KEY_SLICE_MOD_TIMES, map)
                                }
                            }
                        }

                        if (toUpsert.isNotEmpty()) {
                            dao.upsertSlices(toUpsert)
                            ChronosWidgetUpdater.requestUpdate(appContext)
                        }
                        markLiveSyncSuccess()
                    } finally {
                        isApplyingRemoteUpdate = false
                    }
                }
            }
        }

        // 3. Live Exam Countdowns Listener
        activeListeners += userDoc.collection("exam_countdowns").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val remoteDocs = snapshot.documentChanges.mapNotNull { change ->
                val doc = change.document
                if (doc.getString("lastUpdatedByDeviceId") == localDeviceId) return@mapNotNull null
                doc
            }
            if (remoteDocs.isEmpty()) return@addSnapshotListener

            syncScope.launch {
                syncMutex.withLock {
                    isApplyingRemoteUpdate = true
                    try {
                        val deletedExams = getIdSetPref(KEY_TOMBSTONE_EXAMS)
                        val remoteExams = remoteDocs.mapNotNull { it.toExamCountdownEntity() }
                            .filter { it.examId !in deletedExams }
                        if (remoteExams.isNotEmpty()) {
                            val existing = dao.getAllExamCountdownsSnapshot().associateBy { it.examId }.toMutableMap()
                            for (re in remoteExams) existing[re.examId] = re
                            dao.upsertExamCountdowns(normalizePinnedExam(existing.values.toList()))
                        }
                        markLiveSyncSuccess()
                    } finally {
                        isApplyingRemoteUpdate = false
                    }
                }
            }
        }

        // 4. Live Neuro Sessions Listener
        activeListeners += userDoc.collection("neuro_sessions").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val remoteDocs = snapshot.documentChanges.mapNotNull { change ->
                val doc = change.document
                if (doc.getString("lastUpdatedByDeviceId") == localDeviceId) return@mapNotNull null
                doc
            }
            if (remoteDocs.isEmpty()) return@addSnapshotListener

            syncScope.launch {
                syncMutex.withLock {
                    isApplyingRemoteUpdate = true
                    try {
                        val sessions = remoteDocs.mapNotNull { it.toNeuroSessionEntity() }
                        if (sessions.isNotEmpty()) {
                            dao.upsertNeuroSessions(sessions)
                        }
                        markLiveSyncSuccess()
                    } finally {
                        isApplyingRemoteUpdate = false
                    }
                }
            }
        }

        // 5. Live Meta Listener (user_stats + shared_stores + tombstones)
        activeListeners += userDoc.collection("meta").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val remoteDocs = snapshot.documentChanges.mapNotNull { change ->
                val doc = change.document
                if (doc.getString("lastUpdatedByDeviceId") == localDeviceId) return@mapNotNull null
                doc
            }
            if (remoteDocs.isEmpty()) return@addSnapshotListener

            syncScope.launch {
                syncMutex.withLock {
                    isApplyingRemoteUpdate = true
                    try {
                        for (doc in remoteDocs) {
                            when (doc.id) {
                                "user_stats" -> {
                                    val cloudStats = doc.toUserStats() ?: continue
                                    val localStats = dao.getUserStatsSnapshot() ?: UserStats()
                                    val mergedXp = maxOf(localStats.totalXp, cloudStats.totalXp)
                                    val merged = UserStats(
                                        id = 1,
                                        totalXp = mergedXp,
                                        currentLevel = maxOf(localStats.currentLevel, cloudStats.currentLevel, (mergedXp / 500) + 1),
                                        streakDays = maxOf(localStats.streakDays, cloudStats.streakDays),
                                        lastStudyEpochDay = listOfNotNull(localStats.lastStudyEpochDay, cloudStats.lastStudyEpochDay).maxOrNull()
                                    )
                                    if (merged != localStats) {
                                        dao.upsertUserStats(merged)
                                    }
                                }

                                "shared_stores" -> {
                                    val cloudGeneric = doc.getStringList("deletedTombstoneIds").toSet()
                                    val cloudDelChains = (doc.getStringList("deletedChainIds") + cloudGeneric).toSet()
                                    val cloudDelSlices = (doc.getStringList("deletedSliceIds") + cloudGeneric).toSet()
                                    val cloudDelExams = (doc.getStringList("deletedExamCountdownIds") + cloudGeneric).toSet()
                                    val cloudDelBatch = doc.getStringList("deletedBatchExamIds").toSet()
                                    val cloudDelCustom = doc.getStringList("deletedCustomSubjectIds").toSet()

                                    synchronized(localPrefsLock) {
                                        addIdsToSetPref(KEY_TOMBSTONE_CHAINS, cloudDelChains)
                                        addIdsToSetPref(KEY_TOMBSTONE_SLICES, cloudDelSlices)
                                        addIdsToSetPref(KEY_TOMBSTONE_EXAMS, cloudDelExams)
                                        addIdsToSetPref(KEY_TOMBSTONE_BATCH_EXAMS, cloudDelBatch)
                                        addIdsToSetPref(KEY_TOMBSTONE_CUSTOM_SUBJECTS, cloudDelCustom)
                                    }

                                    // Propagate explicit deletes to local Room DB
                                    for (chainId in cloudDelChains) {
                                        dao.deleteTopicChainById(chainId)
                                        pendingOrphanSlices.entries.removeAll { it.value.chainId == chainId }
                                    }
                                    for (sliceId in cloudDelSlices) {
                                        dao.deleteSliceById(sliceId)
                                        pendingOrphanSlices.remove(sliceId)
                                    }
                                    for (examId in cloudDelExams) {
                                        dao.deleteExam(examId)
                                    }

                                    // Merge Custom Subjects & Chain Links
                                    val validChainIds = dao.getAllChainsSnapshot().map { it.chainId }.toHashSet()
                                    val mergedCustomSubs = mergeCustomSubjects(
                                        local = CustomSubjectAndAiStore.getCustomSubjects(),
                                        cloud = CustomSubjectAndAiStore.parseCustomSubjectsJson(doc.getString("customSubjectsJson")),
                                        deletedIds = getIdSetPref(KEY_TOMBSTONE_CUSTOM_SUBJECTS)
                                    )
                                    CustomSubjectAndAiStore.replaceAllCustomSubjectsFromSync(mergedCustomSubs)

                                    val mergedLinks = mergeChainLinks(
                                        local = CustomSubjectAndAiStore.getAllChainLinks(),
                                        cloud = CustomSubjectAndAiStore.parseChainLinksJson(doc.getString("chainLinksJson")),
                                        deletedChainIds = getIdSetPref(KEY_TOMBSTONE_CHAINS),
                                        validChainIds = validChainIds
                                    )
                                    CustomSubjectAndAiStore.replaceAllChainLinksFromSync(mergedLinks)

                                    // Merge Batch Exams
                                    val mergedBatch = mergeBatchExams(
                                        local = BatchExamStore.getAllExams(),
                                        cloud = BatchExamStore.parseJsonArray(doc.getString("batchExamsJson")),
                                        deletedIds = getIdSetPref(KEY_TOMBSTONE_BATCH_EXAMS)
                                    )
                                    BatchExamStore.replaceAllFromSync(mergedBatch)
                                }
                            }
                        }
                        ChronosWidgetUpdater.requestUpdate(appContext)
                        markLiveSyncSuccess()
                    } finally {
                        isApplyingRemoteUpdate = false
                    }
                }
            }
        }
    }

    private suspend fun flushPendingOrphanSlicesLocked() {
        if (pendingOrphanSlices.isEmpty()) return
        val validChainIds = dao.getAllChainsSnapshot().map { it.chainId }.toHashSet()
        val deletedSlices = getIdSetPref(KEY_TOMBSTONE_SLICES)
        val deletedChains = getIdSetPref(KEY_TOMBSTONE_CHAINS)
        val ready = pendingOrphanSlices.values.filter {
            it.chainId in validChainIds &&
                it.chainId !in deletedChains &&
                it.sliceId !in deletedSlices
        }
        if (ready.isNotEmpty()) {
            dao.upsertSlices(ready)
            ready.forEach { pendingOrphanSlices.remove(it.sliceId) }
        }
    }

    private fun markLiveSyncSuccess() {
        val now = System.currentTimeMillis()
        syncPrefs.edit().putLong(KEY_LAST_SYNCED_TS, now).apply()
        _lastSyncedTimestamp.value = now
        _syncErrorMessage.value = null
        _syncStatus.value = SyncStatus.SYNCED_LIVE
    }

    private fun detachListeners() {
        for (reg in activeListeners) {
            try {
                reg.remove()
            } catch (_: Exception) {}
        }
        activeListeners.clear()
    }

    // =========================================================================
    // MERGE CONFLICT RESOLUTION HELPERS
    // =========================================================================

    private fun mergeSliceConflict(
        local: DailyTaskSlice,
        localTs: Long,
        cloud: DailyTaskSlice,
        cloudTs: Long
    ): DailyTaskSlice {
        val preferCloudSchedule = when {
            cloudTs > localTs -> true
            localTs > cloudTs -> false
            cloud.shiftLagCount > local.shiftLagCount -> true
            else -> false
        }
        val scheduleOwner = if (preferCloudSchedule) cloud else local
        return scheduleOwner.copy(
            isCompleted = local.isCompleted || cloud.isCompleted,
            problemsCompleted = maxOf(local.problemsCompleted, cloud.problemsCompleted),
            completedAtTimestamp = local.completedAtTimestamp ?: cloud.completedAtTimestamp,
            xpMultiplier = maxOf(local.xpMultiplier, cloud.xpMultiplier),
            problemsTarget = maxOf(local.problemsTarget, cloud.problemsTarget)
        )
    }

    private fun normalizePinnedExam(exams: List<ExamCountdownEntity>): List<ExamCountdownEntity> {
        if (exams.isEmpty()) return emptyList()
        val pinnedCount = exams.count { it.isPrimaryPinned }
        if (pinnedCount <= 1) return exams
        var foundFirst = false
        return exams.sortedByDescending { it.createdAtEpochDay }.map { exam ->
            if (exam.isPrimaryPinned && !foundFirst) {
                foundFirst = true
                exam
            } else {
                exam.copy(isPrimaryPinned = false)
            }
        }
    }

    private fun mergeCustomSubjects(
        local: List<CustomSubject>,
        cloud: List<CustomSubject>,
        deletedIds: Set<String>
    ): List<CustomSubject> {
        val mergedById = linkedMapOf<String, CustomSubject>()
        for (sub in local) {
            if (sub.id !in deletedIds) mergedById[sub.id] = sub
        }
        for (sub in cloud) {
            if (sub.id !in deletedIds && sub.id !in mergedById) {
                mergedById[sub.id] = sub
            }
        }
        return mergedById.values.toList()
    }

    private fun mergeChainLinks(
        local: Map<String, CustomSubjectLink>,
        cloud: Map<String, CustomSubjectLink>,
        deletedChainIds: Set<String>,
        validChainIds: Set<String>
    ): Map<String, CustomSubjectLink> {
        val merged = linkedMapOf<String, CustomSubjectLink>()
        for ((k, v) in local) {
            if (k !in deletedChainIds && k in validChainIds) merged[k] = v
        }
        for ((k, v) in cloud) {
            if (k !in deletedChainIds && k in validChainIds && k !in merged) merged[k] = v
        }
        return merged
    }

    private fun mergeBatchExams(
        local: List<BatchExamRecord>,
        cloud: List<BatchExamRecord>,
        deletedIds: Set<String>
    ): List<BatchExamRecord> {
        val localMap = local.filter { it.examId !in deletedIds }.associateBy { it.examId }
        val cloudMap = cloud.filter { it.examId !in deletedIds }.associateBy { it.examId }
        val merged = mutableListOf<BatchExamRecord>()
        for (id in (localMap.keys + cloudMap.keys)) {
            val l = localMap[id]
            val c = cloudMap[id]
            val item = when {
                l != null && c == null -> l
                l == null && c != null -> c
                l != null && c != null -> l.copy(
                    studyHoursLogged = maxOf(l.studyHoursLogged, c.studyHoursLogged),
                    isCompleted = l.isCompleted || c.isCompleted
                )
                else -> null
            }
            if (item != null) merged.add(item)
        }
        return merged.sortedByDescending { it.createdAtEpochDay }
    }

    // =========================================================================
    // CHUNKED FIRESTORE BATCH WRITER (<= 400 OPS PER BATCH)
    // =========================================================================

    private sealed class BatchOp {
        data class Set(val ref: DocumentReference, val data: Map<String, Any?>) : BatchOp()
        data class Delete(val ref: DocumentReference) : BatchOp()
    }

    private suspend fun commitChunkedBatches(db: FirebaseFirestore, ops: List<BatchOp>) {
        if (ops.isEmpty()) return
        for (chunk in ops.chunked(FIRESTORE_BATCH_CHUNK_SIZE)) {
            val batch = db.batch()
            for (op in chunk) {
                when (op) {
                    is BatchOp.Set -> batch.set(op.ref, op.data, SetOptions.merge())
                    is BatchOp.Delete -> batch.delete(op.ref)
                }
            }
            batch.commit().await()
        }
    }

    // =========================================================================
    // SHARED PREFS PERSISTENCE HELPERS FOR TOMBSTONES & TIMESTAMPS
    // =========================================================================

    private fun getIdSetPref(key: String): Set<String> {
        synchronized(localPrefsLock) {
            return syncPrefs.getStringSet(key, emptySet())?.toSet() ?: emptySet()
        }
    }

    private fun addIdsToSetPref(key: String, ids: Collection<String>) {
        if (ids.isEmpty()) return
        val current = (syncPrefs.getStringSet(key, emptySet()) ?: emptySet()).toMutableSet()
        current.addAll(ids)
        val trimmed = if (current.size > MAX_TOMBSTONES) {
            current.toList().takeLast(MAX_TOMBSTONES).toSet()
        } else {
            current
        }
        syncPrefs.edit().putStringSet(key, trimmed).apply()
    }

    private fun removeIdsFromSetPref(key: String, ids: Collection<String>) {
        if (ids.isEmpty()) return
        val current = (syncPrefs.getStringSet(key, emptySet()) ?: emptySet()).toMutableSet()
        if (current.removeAll(ids.toSet())) {
            syncPrefs.edit().putStringSet(key, current).apply()
        }
    }

    private fun loadTimestampMap(key: String): MutableMap<String, Long> {
        val raw = syncPrefs.getString(key, null) ?: return mutableMapOf()
        val map = mutableMapOf<String, Long>()
        try {
            val obj = JSONObject(raw)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = obj.optLong(k, 0L)
            }
        } catch (_: Exception) {}
        return map
    }

    private fun saveTimestampMap(key: String, map: Map<String, Long>) {
        val obj = JSONObject()
        val entriesToKeep = if (map.size > 3000) {
            map.entries.sortedByDescending { it.value }.take(2500)
        } else {
            map.entries
        }
        for ((k, v) in entriesToKeep) {
            obj.put(k, v)
        }
        syncPrefs.edit().putString(key, obj.toString()).apply()
    }

    // =========================================================================
    // FIRESTORE MAPPING HELPERS
    // =========================================================================

    private fun StudyTopicChain.toFirestoreMap(deviceId: String, modTs: Long): Map<String, Any?> = hashMapOf(
        "chainId" to chainId,
        "subject" to subject.name,
        "paper" to paper.name,
        "chapterNumber" to chapterNumber,
        "chapterName" to chapterName,
        "totalDays" to totalDays,
        "distributionCurve" to distributionCurve.name,
        "energyLevel" to energyLevel.name,
        "dailyProblemTarget" to dailyProblemTarget,
        "createdAtEpochDay" to createdAtEpochDay,
        "bossMaxHp" to bossMaxHp,
        "bossBonusShieldHp" to bossBonusShieldHp,
        "isChainCompleted" to isChainCompleted,
        "linkedPreviousChainId" to linkedPreviousChainId,
        "lastUpdatedByDeviceId" to deviceId,
        "lastModifiedTimestamp" to modTs
    )

    private fun DocumentSnapshot.toStudyTopicChain(): StudyTopicChain? {
        if (!exists()) return null
        val id = getString("chainId") ?: id
        return StudyTopicChain(
            chainId = id,
            subject = runCatching { HscSubject.valueOf(getString("subject") ?: "") }.getOrDefault(HscSubject.PHYSICS),
            paper = runCatching { HscPaper.valueOf(getString("paper") ?: "") }.getOrDefault(HscPaper.FIRST_PAPER),
            chapterNumber = (getLong("chapterNumber") ?: 1L).toInt(),
            chapterName = getString("chapterName") ?: "",
            totalDays = (getLong("totalDays") ?: 1L).toInt(),
            distributionCurve = runCatching { DistributionCurve.valueOf(getString("distributionCurve") ?: "") }
                .getOrDefault(DistributionCurve.FLAT),
            energyLevel = runCatching { EnergyLevel.valueOf(getString("energyLevel") ?: "") }
                .getOrDefault(EnergyLevel.MEDIUM),
            dailyProblemTarget = (getLong("dailyProblemTarget") ?: 5L).toInt(),
            createdAtEpochDay = getLong("createdAtEpochDay") ?: 0L,
            bossMaxHp = (getLong("bossMaxHp") ?: 100L).toInt(),
            bossBonusShieldHp = (getLong("bossBonusShieldHp") ?: 0L).toInt(),
            isChainCompleted = getBoolean("isChainCompleted") ?: false,
            linkedPreviousChainId = getString("linkedPreviousChainId")
        )
    }

    private fun DailyTaskSlice.toFirestoreMap(deviceId: String, modTs: Long): Map<String, Any?> = hashMapOf(
        "sliceId" to sliceId,
        "chainId" to chainId,
        "sliceNumber" to sliceNumber,
        "totalSlicesInChain" to totalSlicesInChain,
        "displayTitle" to displayTitle,
        "shortBadge" to shortBadge,
        "phaseTag" to phaseTag,
        "scheduledDateEpochDay" to scheduledDateEpochDay,
        "problemsCompleted" to problemsCompleted,
        "problemsTarget" to problemsTarget,
        "isCompleted" to isCompleted,
        "completedAtTimestamp" to completedAtTimestamp,
        "isRetentionEcho" to isRetentionEcho,
        "echoIntervalDays" to echoIntervalDays,
        "sliceEnergyOverride" to sliceEnergyOverride?.name,
        "xpMultiplier" to xpMultiplier,
        "originalScheduledEpochDay" to originalScheduledEpochDay,
        "shiftLagCount" to shiftLagCount,
        "lastUpdatedByDeviceId" to deviceId,
        "lastModifiedTimestamp" to modTs
    )

    private fun DocumentSnapshot.toDailyTaskSlice(): DailyTaskSlice? {
        if (!exists()) return null
        val sId = getString("sliceId") ?: id
        val cId = getString("chainId") ?: return null
        val overrideStr = getString("sliceEnergyOverride")
        return DailyTaskSlice(
            sliceId = sId,
            chainId = cId,
            sliceNumber = (getLong("sliceNumber") ?: 1L).toInt(),
            totalSlicesInChain = (getLong("totalSlicesInChain") ?: 1L).toInt(),
            displayTitle = getString("displayTitle") ?: "",
            shortBadge = getString("shortBadge") ?: "",
            phaseTag = getString("phaseTag") ?: "",
            scheduledDateEpochDay = getLong("scheduledDateEpochDay") ?: 0L,
            problemsCompleted = (getLong("problemsCompleted") ?: 0L).toInt(),
            problemsTarget = (getLong("problemsTarget") ?: 5L).toInt(),
            isCompleted = getBoolean("isCompleted") ?: false,
            completedAtTimestamp = getLong("completedAtTimestamp"),
            isRetentionEcho = getBoolean("isRetentionEcho") ?: false,
            echoIntervalDays = getLong("echoIntervalDays")?.toInt(),
            sliceEnergyOverride = overrideStr?.let { str -> runCatching { EnergyLevel.valueOf(str) }.getOrNull() },
            xpMultiplier = (getLong("xpMultiplier") ?: 1L).toInt(),
            originalScheduledEpochDay = getLong("originalScheduledEpochDay") ?: (getLong("scheduledDateEpochDay") ?: 0L),
            shiftLagCount = (getLong("shiftLagCount") ?: 0L).toInt()
        )
    }

    private fun UserStats.toFirestoreMap(deviceId: String, modTs: Long): Map<String, Any?> = hashMapOf(
        "id" to id,
        "totalXp" to totalXp,
        "currentLevel" to currentLevel,
        "streakDays" to streakDays,
        "lastStudyEpochDay" to lastStudyEpochDay,
        "lastUpdatedByDeviceId" to deviceId,
        "lastModifiedTimestamp" to modTs
    )

    private fun DocumentSnapshot.toUserStats(): UserStats? {
        if (!exists()) return null
        return UserStats(
            id = 1,
            totalXp = (getLong("totalXp") ?: 0L).toInt(),
            currentLevel = (getLong("currentLevel") ?: 1L).toInt(),
            streakDays = (getLong("streakDays") ?: 0L).toInt(),
            lastStudyEpochDay = getLong("lastStudyEpochDay")
        )
    }

    private fun ExamCountdownEntity.toFirestoreMap(deviceId: String, modTs: Long): Map<String, Any?> = hashMapOf(
        "examId" to examId,
        "examTitle" to examTitle,
        "targetDateEpochDay" to targetDateEpochDay,
        "createdAtEpochDay" to createdAtEpochDay,
        "isPrimaryPinned" to isPrimaryPinned,
        "lastUpdatedByDeviceId" to deviceId,
        "lastModifiedTimestamp" to modTs
    )

    private fun DocumentSnapshot.toExamCountdownEntity(): ExamCountdownEntity? {
        if (!exists()) return null
        return ExamCountdownEntity(
            examId = getString("examId") ?: id,
            examTitle = getString("examTitle") ?: "HSC Exam",
            targetDateEpochDay = getLong("targetDateEpochDay") ?: 0L,
            createdAtEpochDay = getLong("createdAtEpochDay") ?: 0L,
            isPrimaryPinned = getBoolean("isPrimaryPinned") ?: false
        )
    }

    private fun NeuroSessionEntity.toFirestoreMap(deviceId: String, modTs: Long): Map<String, Any?> = hashMapOf(
        "sessionId" to sessionId,
        "drillType" to drillType.name,
        "scoreMetric" to scoreMetric,
        "durationSeconds" to durationSeconds,
        "timestamp" to timestamp,
        "lastUpdatedByDeviceId" to deviceId,
        "lastModifiedTimestamp" to modTs
    )

    private fun DocumentSnapshot.toNeuroSessionEntity(): NeuroSessionEntity? {
        if (!exists()) return null
        return NeuroSessionEntity(
            sessionId = getString("sessionId") ?: id,
            drillType = runCatching { NeuroDrillType.valueOf(getString("drillType") ?: "") }
                .getOrDefault(NeuroDrillType.FOVEAL_LOCK),
            scoreMetric = getDouble("scoreMetric") ?: 0.0,
            durationSeconds = (getLong("durationSeconds") ?: 0L).toInt(),
            timestamp = getLong("timestamp") ?: 0L
        )
    }

    private fun DocumentSnapshot.getStringList(field: String): List<String> {
        val raw = get(field) as? List<*> ?: return emptyList()
        return raw.mapNotNull { it as? String }
    }
}
