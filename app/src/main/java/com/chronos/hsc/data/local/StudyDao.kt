package com.chronos.hsc.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {

    @Transaction
    @Query("SELECT * FROM study_topic_chains ORDER BY isChainCompleted ASC, createdAtEpochDay DESC")
    fun observeAllChainsWithSlices(): Flow<List<TopicChainWithSlices>>

    @Transaction
    @Query("SELECT * FROM daily_task_slices ORDER BY scheduledDateEpochDay ASC, sliceNumber ASC")
    fun observeAllSlicesWithParent(): Flow<List<SliceWithParentChain>>

    @Transaction
    @Query(
        """
        SELECT * FROM daily_task_slices 
        WHERE scheduledDateEpochDay = :epochDay 
        ORDER BY isCompleted ASC, isRetentionEcho ASC, sliceNumber ASC
        """
    )
    fun observeSlicesForDate(epochDay: Long): Flow<List<SliceWithParentChain>>

    @Transaction
    @Query(
        """
        SELECT * FROM daily_task_slices 
        WHERE scheduledDateEpochDay < :todayEpochDay AND isCompleted = 0 
        ORDER BY scheduledDateEpochDay ASC, sliceNumber ASC
        """
    )
    fun observeOverdueSlices(todayEpochDay: Long): Flow<List<SliceWithParentChain>>

    @Transaction
    @Query(
        """
        SELECT * FROM daily_task_slices 
        WHERE isRetentionEcho = 1 AND isCompleted = 0 AND scheduledDateEpochDay >= :todayEpochDay
        ORDER BY scheduledDateEpochDay ASC
        """
    )
    fun observeUpcomingEchoes(todayEpochDay: Long): Flow<List<SliceWithParentChain>>

    @Query(
        """
        SELECT * FROM daily_task_slices 
        WHERE scheduledDateEpochDay BETWEEN :startEpochDay AND :endEpochDay
        """
    )
    fun observeSlicesInDateRange(startEpochDay: Long, endEpochDay: Long): Flow<List<DailyTaskSlice>>

    @Transaction
    @Query("SELECT * FROM study_topic_chains WHERE chainId = :chainId LIMIT 1")
    suspend fun getChainWithSlices(chainId: String): TopicChainWithSlices?

    @Transaction
    @Query("SELECT * FROM daily_task_slices WHERE sliceId = :sliceId LIMIT 1")
    suspend fun getSliceWithParent(sliceId: String): SliceWithParentChain?

    @Query("SELECT * FROM daily_task_slices WHERE sliceId = :sliceId LIMIT 1")
    suspend fun getSliceById(sliceId: String): DailyTaskSlice?

    @Query("SELECT * FROM study_topic_chains WHERE chainId = :chainId LIMIT 1")
    suspend fun getChainById(chainId: String): StudyTopicChain?

    @Query(
        """
        SELECT * FROM daily_task_slices 
        WHERE chainId = :chainId AND isCompleted = 0 
        ORDER BY sliceNumber ASC
        """
    )
    suspend fun getIncompleteSlicesForChain(chainId: String): List<DailyTaskSlice>

    @Query(
        """
        SELECT * FROM daily_task_slices 
        WHERE scheduledDateEpochDay < :todayEpochDay AND isCompleted = 0
        ORDER BY scheduledDateEpochDay ASC, sliceNumber ASC
        """
    )
    suspend fun getOverdueIncompleteSlicesOnce(todayEpochDay: Long): List<DailyTaskSlice>

    @Transaction
    @Query(
        """
        SELECT * FROM daily_task_slices 
        WHERE scheduledDateEpochDay = :todayEpochDay 
        ORDER BY isCompleted ASC, isRetentionEcho ASC, sliceNumber ASC
        """
    )
    suspend fun getSlicesForDateOnce(todayEpochDay: Long): List<SliceWithParentChain>

    @Query("SELECT * FROM study_topic_chains")
    suspend fun getAllChainsOnce(): List<StudyTopicChain>

    @Query("SELECT COUNT(*) FROM study_topic_chains")
    suspend fun getChainCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChain(chain: StudyTopicChain)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlices(slices: List<DailyTaskSlice>)

    @Transaction
    suspend fun insertChainWithSlices(chain: StudyTopicChain, slices: List<DailyTaskSlice>) {
        insertChain(chain)
        insertSlices(slices)
    }

    @Update
    suspend fun updateChain(chain: StudyTopicChain)

    @Update
    suspend fun updateSlice(slice: DailyTaskSlice)

    @Update
    suspend fun updateSlices(slices: List<DailyTaskSlice>)

    /**
     * ForeignKey.CASCADE on DailyTaskSlice(chainId) guarantees that deleting
     * the parent StudyTopicChain automatically wipes all past, today, and future
     * DailyTaskSlice rows as well as any Ebbinghaus retention echoes in one atomic operation.
     */
    @Query("DELETE FROM study_topic_chains WHERE chainId = :chainId")
    suspend fun deleteTopicChainById(chainId: String)

    @Query("DELETE FROM daily_task_slices WHERE sliceId = :sliceId")
    suspend fun deleteSliceById(sliceId: String)

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun observeUserStats(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    suspend fun getUserStatsOnce(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserStats(stats: UserStats)
}
