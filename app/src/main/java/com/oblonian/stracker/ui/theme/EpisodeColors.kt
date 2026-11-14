package com.oblonian.stracker.ui.theme

import androidx.compose.ui.graphics.Color
import com.oblonian.stracker.domain.EpisodeType
import com.oblonian.stracker.domain.EventType

/** One accent colour per episode type, matched to the approved mockups. Nothing else is coloured. */
val EpisodeType.color: Color
    get() = when (this) {
        EpisodeType.SLEEP -> Color(0xFF5B6CFF)
        EpisodeType.NAP -> Color(0xFF5B6CFF).copy(alpha = 0.6f)
        EpisodeType.WORK -> Color(0xFF2FB67C)
        EpisodeType.PHONE -> Color(0xFFFF8A3D)
        EpisodeType.EXERCISE -> Color(0xFFE84A8A)
        EpisodeType.TRAVEL -> Color(0xFF9AA0A6)
        EpisodeType.EATING -> Color(0xFFF2C94C)
        EpisodeType.SICK -> Color(0xFFB06CFF)
        EpisodeType.PERIOD -> Color(0xFFFF5C8A)
        EpisodeType.CUSTOM -> Color(0xFF7FCBD1)
    }

val EventType.color: Color
    get() = when (this) {
        EventType.HEADACHE -> Color(0xFFE84A8A)
        EventType.FEVER -> Color(0xFFFF5C5C)
        EventType.MEAL -> Color(0xFFF2C94C)
        EventType.COFFEE -> Color(0xFFB07A4B)
        EventType.WASHROOM -> Color(0xFF7FCBD1)
        EventType.WATER -> Color(0xFF4FB3FF)
        EventType.CUSTOM -> Color(0xFF9AA0A6)
    }

val EpisodeType.displayName: String
    get() = name.lowercase()

/** The order segments appear in a day bar, and the legend. */
val BAR_ORDER = listOf(
    EpisodeType.SLEEP, EpisodeType.NAP, EpisodeType.WORK, EpisodeType.PHONE,
    EpisodeType.TRAVEL, EpisodeType.EXERCISE, EpisodeType.EATING, EpisodeType.CUSTOM,
)

val LEGEND = listOf(
    EpisodeType.SLEEP, EpisodeType.WORK, EpisodeType.PHONE,
    EpisodeType.EXERCISE, EpisodeType.TRAVEL, EpisodeType.EATING,
)

val EventType.emoji: String
    get() = when (this) {
        EventType.HEADACHE -> "🤕"
        EventType.FEVER -> "🌡️"
        EventType.MEAL -> "🍽️"
        EventType.COFFEE -> "☕"
        EventType.WASHROOM -> "🚽"
        EventType.WATER -> "💧"
        EventType.CUSTOM -> "•"
    }
