package com.immersive.reader.ui.components

import java.util.concurrent.TimeUnit

fun formatReadingDuration(milliseconds: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds).coerceAtLeast(0L)
    if (totalSeconds == 0L) return "0 min"
    if (totalSeconds < 60L) return "${totalSeconds}s"
    val minutes = totalSeconds / 60L
    if (minutes < 60L) return "$minutes min"
    val hours = minutes / 60L
    val remain = minutes % 60L
    return if (remain == 0L) "${hours}h" else "${hours}h ${remain}m"
}

fun formatClock(milliseconds: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

fun formatProgressPercent(progression: Double): String = "${(progression * 100).toInt()}%"
