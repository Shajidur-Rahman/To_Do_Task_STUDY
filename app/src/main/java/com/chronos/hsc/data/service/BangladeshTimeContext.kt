package com.chronos.hsc.data.service

import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class BangladeshTimeContext(
    val zonedDateTime: ZonedDateTime = ZonedDateTime.now(ZoneId.of("Asia/Dhaka")),
    val currentBdTimeFormatted: String = formatBdTime(zonedDateTime),
    val bdCircadianPhase: String = calculateBdCircadianPhase(zonedDateTime.toLocalTime())
) {
    companion object {
        private val FORMATTER = DateTimeFormatter.ofPattern("EEEE, hh:mm a 'BD Time (GMT+6)'", Locale.US)

        fun now(): BangladeshTimeContext {
            val zdt = ZonedDateTime.now(ZoneId.of("Asia/Dhaka"))
            return BangladeshTimeContext(
                zonedDateTime = zdt,
                currentBdTimeFormatted = zdt.format(FORMATTER),
                bdCircadianPhase = calculateBdCircadianPhase(zdt.toLocalTime())
            )
        }

        fun formatBdTime(zdt: ZonedDateTime): String {
            return zdt.format(FORMATTER)
        }

        fun calculateBdCircadianPhase(time: LocalTime): String {
            val minuteOfDay = time.hour * 60 + time.minute
            return when {
                minuteOfDay in (6 * 60)..<(11 * 60 + 30) ->
                    "🌅 BD Morning Peak Focus (06:00–11:30): High prefrontal alertness. Ideal for rescuing 1 heavy COMEBACK_LEFTOVER task from yesterday or tackling a tough TODAY_NORMAL math/physics derivation."
                minuteOfDay in (11 * 60 + 30)..<(15 * 60 + 30) ->
                    "☀️ BD Midday / Post-Lunch Transition (11:30–15:30): Moderate/dipping energy. Ideal for clearing TODAY_NORMAL conceptual/verbal tasks (Bangla, English, ICT, Biology) so daily slice counts don't fall behind."
                minuteOfDay in (15 * 60 + 30)..<(20 * 60 + 30) ->
                    "⚡ BD Late Afternoon / Evening Study Window (15:30–20:30): Second focus wave (post-tea window). Ideal for alternating between BATCH_EXAM MCQ/CQ drills and TODAY_NORMAL problem-solving."
                minuteOfDay in (20 * 60 + 30)..<(24 * 60) ->
                    "🌙 BD Night Comeback & Wrap-Up (20:30–23:59): Critical daily closing window. Focus on knocking out remaining TODAY_NORMAL tasks and quick COMEBACK_LEFTOVER slices so the student never goes to sleep with missed tasks!"
                else ->
                    "🦉 BD Late Night Overtime (00:00–05:59): Exhaustion zone. Light review only + warning to sleep soon for neural consolidation."
            }
        }
    }
}
