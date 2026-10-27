package np.com.miti.patro.data.repository

import kotlinx.coroutines.flow.Flow
import np.com.miti.patro.data.local.EventDao
import np.com.miti.patro.data.local.EventEntity
import np.com.miti.patro.domain.engine.BsDateConverter
import np.com.miti.patro.domain.model.BsDate
import np.com.miti.patro.domain.model.MissingDayStrategy
import java.time.LocalDate

data class ResolvedEventInstance(
    val entity: EventEntity,
    val effectiveBsDate: BsDate,
    val effectiveAdDate: LocalDate,
    val isClampedOrRolled: Boolean,
    val yearsElapsed: Int
) {
    val event: EventEntity get() = entity
    val resolvedBsDate: BsDate get() = effectiveBsDate
    val wasAdjustedForShortMonth: Boolean get() = isClampedOrRolled
}

class EventRepository(
    private val eventDao: EventDao
) {
    val allEventsFlow: Flow<List<EventEntity>> = eventDao.observeAllEvents()
    val allEvents: Flow<List<EventEntity>> get() = allEventsFlow

    suspend fun getAllEventsOnce(): List<EventEntity> = eventDao.getAllEventsOnce()
    suspend fun getAllEventsSnapshot(): List<EventEntity> = eventDao.getAllEventsOnce()

    suspend fun getEventById(id: Long): EventEntity? = eventDao.getEventById(id)

    suspend fun saveEvent(event: EventEntity): Long {
        return if (event.id == 0L) {
            eventDao.insertEvent(event)
        } else {
            eventDao.updateEvent(event)
            event.id
        }
    }

    suspend fun deleteEvent(event: EventEntity) {
        eventDao.deleteEvent(event)
    }

    fun resolveEventsForBsMonth(
        allEvents: List<EventEntity>,
        bsYear: Int,
        bsMonth: Int,
        strategy: MissingDayStrategy = MissingDayStrategy.CLAMP_TO_LAST_DAY
    ): List<ResolvedEventInstance> {
        return resolveEventsForMonth(allEvents, bsYear, bsMonth, strategy).values.flatten()
    }

    fun resolveEventsForMonth(
        allEvents: List<EventEntity>,
        bsYear: Int,
        bsMonth: Int,
        strategy: MissingDayStrategy = MissingDayStrategy.CLAMP_TO_LAST_DAY
    ): Map<Int, List<ResolvedEventInstance>> {
        val map = mutableMapOf<Int, MutableList<ResolvedEventInstance>>()

        for (event in allEvents) {
            if (bsYear < event.startBsYear) continue

            val occ = BsDateConverter.resolveAnnualOccurrence(
                eventMonth = event.bsMonth,
                eventDay = event.bsDay,
                targetBsYear = bsYear,
                strategy = strategy
            )
            if (occ != null && occ.targetDate.year == bsYear && occ.targetDate.month == bsMonth) {
                val instance = ResolvedEventInstance(
                    entity = event,
                    effectiveBsDate = occ.targetDate,
                    effectiveAdDate = BsDateConverter.bsToAd(occ.targetDate),
                    isClampedOrRolled = occ.isClampedOrRolled,
                    yearsElapsed = (bsYear - event.startBsYear).coerceAtLeast(0)
                )
                map.getOrPut(occ.targetDate.day) { mutableListOf() }.add(instance)
            }

            if (bsMonth == 1 && event.bsMonth == 12 && (bsYear - 1) >= event.startBsYear) {
                val prevYearOcc = BsDateConverter.resolveAnnualOccurrence(
                    eventMonth = 12,
                    eventDay = event.bsDay,
                    targetBsYear = bsYear - 1,
                    strategy = strategy
                )
                if (prevYearOcc != null && prevYearOcc.targetDate.year == bsYear && prevYearOcc.targetDate.month == 1) {
                    val instance = ResolvedEventInstance(
                        entity = event,
                        effectiveBsDate = prevYearOcc.targetDate,
                        effectiveAdDate = BsDateConverter.bsToAd(prevYearOcc.targetDate),
                        isClampedOrRolled = prevYearOcc.isClampedOrRolled,
                        yearsElapsed = (bsYear - 1 - event.startBsYear).coerceAtLeast(0)
                    )
                    map.getOrPut(prevYearOcc.targetDate.day) { mutableListOf() }.add(instance)
                }
            }
        }
        return map
    }
}
