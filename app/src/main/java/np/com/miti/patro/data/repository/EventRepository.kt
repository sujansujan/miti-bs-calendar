package np.com.miti.patro.data.repository

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.Flow
import np.com.miti.patro.data.local.EventDao
import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.MissingDayStrategy
import java.time.LocalDate

@Immutable
data class ResolvedEventInstance(
    val event: EventEntity,
    val resolvedBsDate: BsDate,
    val resolvedAdDate: LocalDate,
    val yearsElapsed: Int,
    val wasAdjustedForMissingDay: Boolean,
    val adjustmentNote: String?
)

class EventRepository(private val eventDao: EventDao) {

    val allEventsFlow: Flow<List<EventEntity>> = eventDao.observeAllEvents()

    suspend fun getAllEventsOnce(): List<EventEntity> = eventDao.getAllEventsOnce()

    suspend fun getAllReminderEnabledEvents(): List<EventEntity> =
        eventDao.getAllReminderEnabledEvents()

    suspend fun getEventById(id: Long): EventEntity? = eventDao.getEventById(id)

    suspend fun saveEvent(event: EventEntity): Long {
        return if (event.id == 0L) {
            eventDao.insertEvent(event)
        } else {
            eventDao.updateEvent(event)
            event.id
        }
    }

    suspend fun replaceAllEvents(events: List<EventEntity>) {
        eventDao.deleteAllEvents()
        if (events.isNotEmpty()) {
            eventDao.insertAllEvents(events)
        }
    }

    suspend fun deleteEvent(event: EventEntity) {
        eventDao.deleteEvent(event)
    }

    fun resolveEventsForMonth(
        allEvents: List<EventEntity>,
        bsYear: Int,
        bsMonth: Int,
        strategy: MissingDayStrategy
    ): Map<Int, List<ResolvedEventInstance>> {
        val map = mutableMapOf<Int, MutableList<ResolvedEventInstance>>()

        for (event in allEvents) {
            if (bsYear >= event.startBsYear) {
                val occurrences = BsDateConverter.resolveEventOccurrencesInBsYear(
                    event = event,
                    targetBsYear = bsYear,
                    strategy = strategy
                )
                for (occurrence in occurrences) {
                    if (occurrence.targetDate.year == bsYear && occurrence.targetDate.month == bsMonth) {
                        val adDate = BsDateConverter.bsToAd(occurrence.targetDate)
                        val instance = ResolvedEventInstance(
                            event = event,
                            resolvedBsDate = occurrence.targetDate,
                            resolvedAdDate = adDate,
                            yearsElapsed = (bsYear - event.startBsYear).coerceAtLeast(0),
                            wasAdjustedForMissingDay = occurrence.wasAdjusted,
                            adjustmentNote = occurrence.adjustmentNote
                        )
                        map.getOrPut(occurrence.targetDate.day) { mutableListOf() }.add(instance)
                    }
                }
            }

            if (bsMonth == 1 && !event.isShraddha && strategy == MissingDayStrategy.ROLL_TO_NEXT_MONTH && bsYear - 1 >= event.startBsYear) {
                val prevOccurrence = BsDateConverter.resolveEventOccurrence(
                    event = event,
                    targetBsYear = bsYear - 1,
                    strategy = strategy
                )
                if (prevOccurrence != null && prevOccurrence.targetDate.year == bsYear && prevOccurrence.targetDate.month == 1) {
                    val adDate = BsDateConverter.bsToAd(prevOccurrence.targetDate)
                    val instance = ResolvedEventInstance(
                        event = event,
                        resolvedBsDate = prevOccurrence.targetDate,
                        resolvedAdDate = adDate,
                        yearsElapsed = ((bsYear - 1) - event.startBsYear).coerceAtLeast(0),
                        wasAdjustedForMissingDay = prevOccurrence.wasAdjusted,
                        adjustmentNote = prevOccurrence.adjustmentNote
                    )
                    map.getOrPut(prevOccurrence.targetDate.day) { mutableListOf() }.add(instance)
                }
            }
        }
        return map
    }
}
