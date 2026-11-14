package com.oblonian.stracker.data

import com.oblonian.stracker.data.db.DayDao
import com.oblonian.stracker.domain.DayBoundaries
import com.oblonian.stracker.domain.DayData
import com.oblonian.stracker.domain.DayWindow
import com.oblonian.stracker.domain.Detail
import com.oblonian.stracker.domain.Episode
import com.oblonian.stracker.domain.EpisodeType
import com.oblonian.stracker.domain.Event
import com.oblonian.stracker.domain.EventType
import com.oblonian.stracker.domain.InferenceEngine
import com.oblonian.stracker.domain.InferenceSettings
import com.oblonian.stracker.domain.Override
import com.oblonian.stracker.domain.Signals
import com.oblonian.stracker.domain.SleepInference
import com.oblonian.stracker.domain.Source
import com.oblonian.stracker.domain.Span
import com.oblonian.stracker.domain.Steps
import com.oblonian.stracker.domain.key
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** What the calendar shows for one day. */
data class DaySummary(
    val millisByType: Map<EpisodeType, Long>,
    val eventTypes: List<EventType>,
    /** Multi-day episodes (PERIOD, SICK) covering this day. */
    val bands: Set<EpisodeType>,
)

/**
 * Runs inference for a day and stores it; serves the day view and the calendar; records the
 * owner's own logs and corrections.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DayRepository(
    private val signals: SignalRepository,
    private val dayDao: DayDao,
    private val places: PlaceRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    private val zone get() = clock.zone

    private suspend fun inferenceSettings(): InferenceSettings = settings.settings.first().toInference(zone)

    /** Signals in [from, to) plus the motion state the owner was already in at [from]. */
    private suspend fun signalsWithPriorMotion(from: Instant, to: Instant): Signals {
        val s = signals.signalsBetween(from, to)
        val prior = signals.motionBefore(from) ?: return s
        return s.copy(motion = listOf(prior) + s.motion)
    }

    private suspend fun mainSleep(day: LocalDate, s: InferenceSettings): Episode? {
        val (from, to) = DayBoundaries.sleepSearchRange(day, s)
        return SleepInference.mainSleep(day, signalsWithPriorMotion(from, to), s)
    }

    /** `rawSignals + manual + overrides → episodes` for [day]; safe to call any number of times. */
    suspend fun recompute(day: LocalDate) {
        val s = inferenceSettings()
        val sleep = mainSleep(day, s)
        val nextSleep = mainSleep(day.plusDays(1), s)
        val window = DayBoundaries.windows(listOf(day), listOfNotNull(sleep, nextSleep), zone).single()
        val sig = signalsWithPriorMotion(window.start, window.end)
        val manual = dayDao.manualEpisodesOverlapping(window.start.toEpochMilli(), window.end.toEpochMilli()).map { it.toDomain() }
        val overrides = dayDao.overridesForDay(day.toString()).map { it.toDomain() }
        val data = InferenceEngine(s).inferDay(day, window, sleep, sig, places.placesOnce(), manual, overrides, clock.instant())
        dayDao.replaceAutoEpisodes(day.toString(), data.episodes.filter { it.source != Source.MANUAL }.map { it.copy(day = day).toEntity() })
        dayDao.replaceAppUsage(day.toString(), data.appUsage.map { it.toEntity(day) })
    }

    private fun windowFor(day: LocalDate, stored: List<Episode>): DayWindow =
        DayBoundaries.windows(listOf(day), stored.filter { it.type == EpisodeType.SLEEP && it.source != Source.MANUAL }, zone).single()

    fun day(day: LocalDate): Flow<DayData> =
        dayDao.episodesBetweenDays(day.minusDays(1).toString(), day.plusDays(1).toString())
            .map { rows -> windowFor(day, rows.map { it.toDomain() }) }
            .flatMapLatest { window ->
                combine(
                    dayDao.episodesOverlapping(window.start.toEpochMilli(), window.end.toEpochMilli()),
                    dayDao.eventsBetween(window.start.toEpochMilli(), window.end.toEpochMilli()),
                    dayDao.appUsageForDay(day.toString()),
                ) { eps, evs, usage -> Triple(eps, evs, usage) }
                    .map { (eps, evs, usage) ->
                        val sig = signals.signalsBetween(window.start, window.end)
                        DayData(
                            window = window,
                            episodes = eps.map { it.toDomain() }.sortedBy { it.start },
                            events = evs.map { it.toDomain() },
                            appUsage = usage.map { it.toDomain() },
                            steps = Steps.forWindow(sig.steps, window),
                            path = sig.locations,
                        )
                    }
            }

    fun calendar(month: YearMonth): Flow<Map<LocalDate, DaySummary>> {
        val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
        val from = DayBoundaries.midnight(days.first().minusDays(2), zone)
        val to = DayBoundaries.midnight(days.last().plusDays(2), zone)
        return combine(
            dayDao.episodesOverlapping(from.toEpochMilli(), to.toEpochMilli()),
            dayDao.eventsBetween(from.toEpochMilli(), to.toEpochMilli()),
        ) { eps, evs ->
            val episodes = eps.map { it.toDomain() }
            val events = evs.map { it.toDomain() }
            val now = clock.instant()
            val windows = DayBoundaries.windows(days, episodes.filter { it.type == EpisodeType.SLEEP && it.source != Source.MANUAL }, zone)
            windows.associate { w ->
                val span = Span(w.start, w.end)
                val byType = HashMap<EpisodeType, Long>()
                val bands = HashSet<EpisodeType>()
                for (e in episodes) {
                    val eSpan = Span(e.start, e.end ?: now)
                    if (!eSpan.overlaps(span)) continue
                    if (e.type == EpisodeType.PERIOD || e.type == EpisodeType.SICK) { bands += e.type; continue }
                    eSpan.clip(span)?.let { byType.merge(e.type, it.millis, Long::plus) }
                }
                val eventTypes = events.filter { it.time >= w.start && it.time < w.end }.sortedBy { it.time }.map { it.type }.distinct()
                w.day to DaySummary(byType, eventTypes, bands)
            }
        }
    }

    // ---- the owner's own logs

    private fun dayOf(at: Instant): LocalDate = at.atZone(zone).toLocalDate()

    suspend fun logEvent(type: EventType, customName: String?, at: Instant): Long =
        dayDao.insertEvent(Event(type = type, customName = customName, time = at, day = dayOf(at)).toEntity())

    suspend fun startEpisode(type: EpisodeType, customName: String?, at: Instant): Long =
        dayDao.insertEpisode(Episode(type = type, customName = customName, start = at, end = null, day = dayOf(at), source = Source.MANUAL).toEntity())

    suspend fun endEpisode(id: Long, at: Instant) {
        val row = dayDao.episode(id) ?: return
        dayDao.updateEpisode(row.copy(end = at.toEpochMilli()))
        recompute(dayOf(at))
    }

    /** Reopens an episode that was just ended (undo). */
    suspend fun reopenEpisode(id: Long) {
        val row = dayDao.episode(id) ?: return
        dayDao.updateEpisode(row.copy(end = null))
    }

    fun ongoing(): Flow<List<Episode>> = dayDao.ongoingManualEpisodes().map { rows -> rows.map { it.toDomain() } }

    suspend fun ongoingOnce(): List<Episode> = dayDao.ongoingManualEpisodesOnce().map { it.toDomain() }

    /** Manual episodes are edited in place; automatic ones get an override and the day is recomputed. */
    suspend fun updateEpisode(episode: Episode) {
        if (episode.source == Source.MANUAL) {
            dayDao.updateEpisode(episode.toEntity())
            recompute(episode.day)
            return
        }
        dayDao.upsertOverride(
            Override(
                day = episode.day, episodeKey = episode.autoKey ?: episode.key(),
                newType = episode.type, newStart = episode.start, newEnd = episode.end,
            ).toEntity()
        )
        recompute(episode.day)
    }

    suspend fun retype(episode: Episode, newType: EpisodeType, customName: String? = null) =
        updateEpisode(episode.copy(type = newType, customName = customName))

    suspend fun setDetail(episode: Episode, detail: Detail?) {
        if (episode.source == Source.MANUAL) dayDao.updateEpisode(episode.copy(detail = detail).toEntity())
        else dayDao.episode(episode.id)?.let { dayDao.updateEpisode(it.copy(severity = detail?.severity, note = detail?.note)) }
    }

    suspend fun deleteEpisode(id: Long) {
        val row = dayDao.episode(id) ?: return
        val episode = row.toDomain()
        if (episode.source == Source.MANUAL) {
            dayDao.deleteEpisode(id)
        } else {
            dayDao.upsertOverride(Override(day = episode.day, episodeKey = episode.autoKey ?: episode.key(), deleted = true).toEntity())
        }
        recompute(episode.day)
    }

    suspend fun episode(id: Long): Episode? = dayDao.episode(id)?.toDomain()

    suspend fun event(id: Long): Event? = dayDao.event(id)?.toDomain()

    suspend fun updateEvent(event: Event) = dayDao.updateEvent(event.toEntity())

    suspend fun deleteEvent(id: Long) = dayDao.deleteEvent(id)
}
