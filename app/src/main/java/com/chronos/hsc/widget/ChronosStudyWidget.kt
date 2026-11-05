package com.chronos.hsc.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.chronos.hsc.MainActivity
import com.chronos.hsc.data.local.SliceWithParentChain
import com.chronos.hsc.data.local.StudyDatabase
import com.chronos.hsc.data.repository.StudyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.max

private val SliceIdParamKey = ActionParameters.Key<String>("slice_id")

object ChronosWidgetUpdater {
    fun requestUpdate(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                ChronosStudyWidget().updateAll(context.applicationContext)
            }
        }
    }
}

class ChronosStudyWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dao = StudyDatabase.getInstance(context).studyDao()
        val todayEpoch = LocalDate.now().toEpochDay()
        val todaySlices = dao.getSlicesForDateOnce(todayEpoch)
        val topActiveSlice = todaySlices.firstOrNull { !it.slice.isCompleted }
            ?: todaySlices.firstOrNull()
        val stats = dao.getUserStatsOnce()

        provideContent {
            GlanceTheme {
                WidgetContent(
                    activeItem = topActiveSlice,
                    level = stats?.currentLevel ?: 1,
                    streakDays = stats?.streakDays ?: 1
                )
            }
        }
    }

    @Composable
    private fun WidgetContent(
        activeItem: SliceWithParentChain?,
        level: Int,
        streakDays: Int
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(ColorProvider(Color(0xFF0B0F1A)))
                .padding(14.dp)
        ) {
            if (activeItem == null) {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionStartActivity<MainActivity>()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚡ CHRONOS HSC ENGINE",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF00E5FF)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "All today's slices conquered! Tap to add an HSC topic chain.",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFA0AEC0)),
                            fontSize = 12.sp
                        )
                    )
                }
            } else {
                val slice = activeItem.slice
                val chain = activeItem.chain
                val bossHpFraction = (activeItem.currentBossHp.toFloat() / max(1, chain.bossMaxHp).toFloat())
                    .coerceIn(0f, 1f)

                Column(
                    modifier = GlanceModifier.fillMaxSize()
                ) {
                    // Header Row: Badge + Day X/N + Streak/Level
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionStartActivity<MainActivity>()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${slice.shortBadge} • Day ${slice.sliceNumber}/${slice.totalSlicesInChain}",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF00E5FF)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            text = "LVL $level • 🔥${streakDays}d",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFFFEA00)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    // Title
                    Text(
                        text = slice.displayTitle,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFF3F6FF)),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        maxLines = 1
                    )

                    Spacer(modifier = GlanceModifier.height(6.dp))

                    // Mini Boss HP Progress Bar
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚔️ Boss HP: ${activeItem.currentBossHp}/${chain.bossMaxHp}",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFFFF1744)),
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Text(
                            text = "${slice.problemsCompleted}/${slice.problemsTarget} Problems",
                            style = TextStyle(
                                color = ColorProvider(Color(0xFF00E676)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = bossHpFraction,
                        color = ColorProvider(Color(0xFFFF1744)),
                        backgroundColor = ColorProvider(Color(0xFF1A2236)),
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    // Interactive Glance Action Buttons: [ +1 Problem ] and [ Complete Slice ]
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            text = "+1 Problem (${slice.problemsCompleted}/${slice.problemsTarget})",
                            onClick = actionRunCallback<IncrementWidgetProblemAction>(
                                actionParametersOf(SliceIdParamKey to slice.sliceId)
                            ),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = ColorProvider(Color(0xFF17233B)),
                                contentColor = ColorProvider(Color(0xFF00E5FF))
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )

                        Spacer(modifier = GlanceModifier.width(8.dp))

                        Button(
                            text = if (slice.isCompleted) "Slashed ✅" else "Complete Slice",
                            onClick = actionRunCallback<CompleteWidgetSliceAction>(
                                actionParametersOf(SliceIdParamKey to slice.sliceId)
                            ),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = ColorProvider(Color(0xFF00E676)),
                                contentColor = ColorProvider(Color(0xFF00210E))
                            ),
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }
                }
            }
        }
    }
}

class IncrementWidgetProblemAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val sliceId = parameters[SliceIdParamKey] ?: return
        val dao = StudyDatabase.getInstance(context).studyDao()
        val repository = StudyRepository(dao, context.applicationContext)
        repository.incrementProblemCounter(sliceId)
        ChronosStudyWidget().update(context, glanceId)
    }
}

class CompleteWidgetSliceAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val sliceId = parameters[SliceIdParamKey] ?: return
        val dao = StudyDatabase.getInstance(context).studyDao()
        val repository = StudyRepository(dao, context.applicationContext)
        repository.completeTaskSlice(sliceId)
        ChronosStudyWidget().update(context, glanceId)
    }
}
