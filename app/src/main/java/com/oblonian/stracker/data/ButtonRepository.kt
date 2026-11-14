package com.oblonian.stracker.data

import com.oblonian.stracker.data.db.ButtonDao
import com.oblonian.stracker.domain.EpisodeType
import com.oblonian.stracker.domain.EventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ButtonRepository(private val dao: ButtonDao) {

    val buttons: Flow<List<QuickLogButton>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    /** Writes the spec's default button set the first time the app runs; a no-op afterwards. */
    suspend fun seedDefaultsIfEmpty() {
        if (dao.count() > 0) return
        dao.insertAll(DEFAULTS.mapIndexed { i, b -> b.copy(sortOrder = i).toEntity() })
    }

    suspend fun save(button: QuickLogButton): Long =
        if (button.id == 0L) dao.insert(button.toEntity()) else { dao.update(button.toEntity()); button.id }

    suspend fun setHidden(id: Long, hidden: Boolean) {
        val row = dao.all().firstOrNull { it.id == id } ?: return
        dao.update(row.copy(hidden = hidden))
    }

    suspend fun reorder(idsInOrder: List<Long>) {
        val rows = dao.all().associateBy { it.id }
        idsInOrder.forEachIndexed { i, id -> rows[id]?.let { dao.update(it.copy(sortOrder = i)) } }
    }

    suspend fun delete(id: Long) = dao.delete(id)

    companion object {
        private fun event(label: String, emoji: String, type: EventType) =
            QuickLogButton(label = label, emoji = emoji, kind = ButtonKind.EVENT, eventType = type, sortOrder = 0)

        private fun episode(label: String, emoji: String, type: EpisodeType, customName: String? = null) =
            QuickLogButton(label = label, emoji = emoji, kind = ButtonKind.EPISODE, episodeType = type, customName = customName, sortOrder = 0)

        /** Spec §6.5 screen 3, in display order. "Custom" is a UI affordance, not a row. */
        val DEFAULTS: List<QuickLogButton> = listOf(
            event("headache", "🤕", EventType.HEADACHE),
            event("fever", "🌡️", EventType.FEVER),
            episode("period", "🩸", EpisodeType.PERIOD),
            episode("sick", "🤒", EpisodeType.SICK),
            event("meal", "🍽️", EventType.MEAL),
            event("coffee", "☕", EventType.COFFEE),
            event("washroom", "🚽", EventType.WASHROOM),
            event("water", "💧", EventType.WATER),
            episode("exercise", "💪", EpisodeType.EXERCISE),
            episode("study", "📖", EpisodeType.CUSTOM, customName = "study"),
            episode("nap", "💤", EpisodeType.NAP),
        )
    }
}
