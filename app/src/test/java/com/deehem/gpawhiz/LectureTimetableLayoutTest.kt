package com.deehem.gpawhiz

import com.deehem.gpawhiz.data.TimetableSlot
import com.deehem.gpawhiz.service.LectureHourRange
import com.deehem.gpawhiz.service.lectureBlockPlacements
import com.deehem.gpawhiz.service.lectureClashGroups
import com.deehem.gpawhiz.service.lectureHourRange
import com.deehem.gpawhiz.service.lectureVisibleDays
import com.deehem.gpawhiz.service.timetableCourseColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LectureTimetableLayoutTest {
    @Test
    fun noLecturesHaveNoHourRangePlacementsOrClashes() {
        assertNull(lectureHourRange(emptyList()))
        assertTrue(lectureBlockPlacements(emptyList(), LectureHourRange(8, 9)).isEmpty())
        assertTrue(lectureClashGroups(emptyList()).isEmpty())
        assertEquals((1..5).toList(), lectureVisibleDays(emptyList()))
    }

    @Test
    fun oneLectureRoundsHourRangeAndCalculatesItsPosition() {
        val slot = slot(id = 1, day = 1, start = "08:20", end = "09:30")

        val range = lectureHourRange(listOf(slot))
        assertNotNull(range)
        assertEquals(8, range?.startHour)
        assertEquals(10, range?.endHour)

        val placement = lectureBlockPlacements(listOf(slot), requireNotNull(range)).single()
        assertEquals(1, placement.slotId)
        assertEquals(20f / 120f, placement.topFraction, 0.0001f)
        assertEquals(70f / 120f, placement.heightFraction, 0.0001f)
        assertEquals(0, placement.laneIndex)
        assertEquals(1, placement.laneCount)
        assertFalse(placement.hasClash)
    }

    @Test
    fun backToBackLecturesShareLaneWithoutClashing() {
        val first = slot(id = 1, day = 1, start = "08:00", end = "09:00")
        val second = slot(id = 2, day = 1, start = "09:00", end = "10:00")

        val placements = lectureBlockPlacements(listOf(first, second), requireNotNull(lectureHourRange(listOf(first, second))))

        assertEquals(2, placements.size)
        assertTrue(placements.all { it.laneIndex == 0 && it.laneCount == 1 && !it.hasClash })
        assertTrue(lectureClashGroups(listOf(first, second)).isEmpty())
    }

    @Test
    fun overlappingLecturesUseSideBySideLanesAndAreReportedAsClashes() {
        val first = slot(id = 1, day = 2, start = "08:00", end = "09:00")
        val second = slot(id = 2, day = 2, start = "08:30", end = "09:30")
        val slots = listOf(first, second)

        val placements = lectureBlockPlacements(slots, requireNotNull(lectureHourRange(slots)))
        val clash = lectureClashGroups(slots).single()

        assertEquals(setOf(0, 1), placements.map { it.laneIndex }.toSet())
        assertTrue(placements.all { it.laneCount == 2 && it.hasClash })
        assertEquals(2, clash.dayOfWeek)
        assertEquals(listOf(1, 2), clash.slots.map { it.id })
    }

    @Test
    fun weekendLecturesAddOnlyTheDaysThatContainLectures() {
        val saturday = slot(id = 1, day = 6, start = "10:00", end = "11:00")
        assertEquals((1..6).toList(), lectureVisibleDays(listOf(saturday)))

        val sunday = slot(id = 2, day = 7, start = "12:00", end = "13:00")
        assertEquals((1..7).toList(), lectureVisibleDays(listOf(saturday, sunday)))
    }

    @Test
    fun courseColorIsStableForTheSameCode() {
        assertEquals(timetableCourseColor("CSC 101"), timetableCourseColor(" csc 101 "))
    }

    private fun slot(id: Int, day: Int, start: String, end: String) = TimetableSlot(
        id = id,
        courseCode = "CSC $id",
        venue = "Room $id",
        dayOfWeek = day,
        startTime = start,
        endTime = end
    )
}
