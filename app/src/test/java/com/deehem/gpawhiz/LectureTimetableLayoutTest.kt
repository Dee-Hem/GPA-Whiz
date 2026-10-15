package com.deehem.gpawhiz

import com.deehem.gpawhiz.data.TimetableSlot
import com.deehem.gpawhiz.service.LectureHourRange
import com.deehem.gpawhiz.service.lectureBlockPlacements
import com.deehem.gpawhiz.service.lectureClashGroups
import com.deehem.gpawhiz.service.lectureHourRange
import com.deehem.gpawhiz.service.lectureVisibleDays
import com.deehem.gpawhiz.service.hourAxisLabelPositions
import com.deehem.gpawhiz.service.timetableCourseColor
import com.deehem.gpawhiz.service.weeklyDayColumnWidth
import com.deehem.gpawhiz.service.weeklyBlockTextMode
import com.deehem.gpawhiz.service.WeeklyBlockTextMode
import com.deehem.gpawhiz.service.wrapPdfText
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
    fun weeklyGridSupportsDenseDaysFourWayClashesAndLongHourRanges() {
        val denseDay = (0 until 6).map { index ->
            slot(index + 1, day = 1, start = "%02d:00".format(8 + index * 2), end = "%02d:00".format(9 + index * 2))
        }
        val fourWayClash = (0 until 4).map { index ->
            slot(index + 20, day = 2, start = "10:00", end = "11:00")
        }
        val twelveLectures = (0 until 12).map { index ->
            slot(index + 40, day = index % 7 + 1, start = "09:00", end = "10:00")
        }
        val longRange = slot(100, day = 3, start = "07:00", end = "20:00")
        val allSlots = denseDay + fourWayClash + twelveLectures + longRange
        val placements = lectureBlockPlacements(allSlots, requireNotNull(lectureHourRange(allSlots)))
        val fourLanes = placements.filter { it.slotId in 20..23 }

        assertEquals(6, placements.count { it.slotId in 1..6 })
        assertEquals(12, placements.count { it.slotId in 40..51 })
        assertTrue(fourLanes.all { it.laneCount == 4 && it.hasClash })
        assertEquals(13, requireNotNull(lectureHourRange(listOf(longRange))).durationMinutes / 60)
        assertTrue(weeklyDayColumnWidth(842f, 7) > 0f)
        val labels = hourAxisLabelPositions(requireNotNull(lectureHourRange(listOf(longRange))), 100f, 400f)
        assertEquals(14, labels.size)
        assertTrue(labels.zipWithNext().all { (first, second) -> second > first })
    }

    @Test
    fun shortLectureGetsAValidPlacementEvenWithLongCourseCode() {
        val shortLecture = slot(
            id = 1,
            day = 5,
            start = "09:00",
            end = "09:30"
        ).copy(courseCode = "CSC1234567890123456789012345678901234567")

        val range = requireNotNull(lectureHourRange(listOf(shortLecture)))
        val placement = lectureBlockPlacements(listOf(shortLecture), range).single()

        assertEquals(1, placement.laneCount)
        assertEquals(0.5f, placement.heightFraction, 0.0001f)
        val longTitle = "A very long course title that wraps inside the course legend"
        val titleLines = wrapPdfText(longTitle, 60f, 7f) { text, size -> text.length * size * 0.5f }
        assertTrue(titleLines.size > 1)
        assertEquals(longTitle, titleLines.joinToString(" "))
        assertEquals(WeeklyBlockTextMode.INLINE, weeklyBlockTextMode(100f, 11f, 28f, 45f))
        assertEquals(WeeklyBlockTextMode.CODE_ONLY, weeklyBlockTextMode(28f, 11f, 28f, 45f))
        assertEquals(WeeklyBlockTextMode.STACKED, weeklyBlockTextMode(100f, 22f, 28f, 45f))
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
