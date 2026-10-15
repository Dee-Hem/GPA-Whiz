package com.deehem.gpawhiz

import com.deehem.gpawhiz.data.TimetableSlot
import com.deehem.gpawhiz.service.LectureHourRange
import com.deehem.gpawhiz.service.PdfTextRect
import com.deehem.gpawhiz.service.lectureBlockPlacements
import com.deehem.gpawhiz.service.lectureClashGroups
import com.deehem.gpawhiz.service.lectureHourRange
import com.deehem.gpawhiz.service.lectureVisibleDays
import com.deehem.gpawhiz.service.hourAxisLabelPositions
import com.deehem.gpawhiz.service.formatTimeRange
import com.deehem.gpawhiz.service.textRectsDoNotOverlap
import com.deehem.gpawhiz.service.textRectsStayInside
import com.deehem.gpawhiz.service.timetableCourseColor
import com.deehem.gpawhiz.service.weeklyDayColumnWidth
import com.deehem.gpawhiz.service.weeklyBlockNeedsClashDetails
import com.deehem.gpawhiz.service.weeklyBlockTextLayout
import com.deehem.gpawhiz.service.WeeklyBlockTextKind
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
        val text = weeklyBlockTextLayout(shortLecture, 45f, 100f) { value, size ->
            value.length * size * 0.5f
        }
        assertTrue(text.fits)
        assertTrue(text.lines.any { it.kind == WeeklyBlockTextKind.COURSE_CODE })
        assertTrue(text.lines.any { it.kind == WeeklyBlockTextKind.TIME })
        assertTrue(text.lines.any { it.kind == WeeklyBlockTextKind.VENUE })
        assertTrue(text.lines.filter { it.kind == WeeklyBlockTextKind.COURSE_CODE }.all { it.textSize >= 7.5f })
    }

    @Test
    fun weeklyGridTextFitsSingleHalfHourFourWayClashAndTwelveLectureCases() {
        val singleLecture = slot(1, day = 1, start = "08:00", end = "09:00")
        val halfHourLecture = slot(2, day = 3, start = "09:00", end = "09:30")
        val fourWayClash = (0 until 4).map { index ->
            slot(index + 10, day = 2, start = "10:00", end = "11:00")
        }
        val twelveLectures = (0 until 12).map { index ->
            slot(index + 30, day = index % 7 + 1, start = "09:00", end = "10:00")
        }

        listOf(singleLecture, halfHourLecture).forEach { assertWeeklyBlockText(listOf(it)) }
        assertWeeklyBlockText(fourWayClash)
        assertWeeklyBlockText(twelveLectures)
        assertTrue(weeklyBlockNeedsClashDetails(22f))
        assertFalse(weeklyBlockNeedsClashDetails(30f))
    }

    @Test
    fun weeklyTimeRangeWrapsAtDashWithoutSplittingTimeText() {
        val lecture = slot(1, day = 1, start = "11:00", end = "13:00")
        val layout = weeklyBlockTextLayout(lecture, 40f, 80f) { text, size ->
            text.length * size * 0.5f
        }
        val timeLines = layout.lines.filter { it.kind == WeeklyBlockTextKind.TIME }.map { it.text }

        assertEquals(listOf("11:00\u00A0AM -", "1:00\u00A0PM"), timeLines)
    }

    @Test
    fun weeklyBlockWithoutVenueStillIncludesCodeAndTime() {
        val lecture = slot(1, day = 1, start = "08:00", end = "09:00").copy(venue = "")
        val layout = weeklyBlockTextLayout(lecture, 70f, 50f) { text, size ->
            text.length * size * 0.5f
        }
        val kinds = layout.lines.map { it.kind }.toSet()

        assertTrue(layout.fits)
        assertTrue(WeeklyBlockTextKind.COURSE_CODE in kinds)
        assertTrue(WeeklyBlockTextKind.TIME in kinds)
        assertFalse(WeeklyBlockTextKind.VENUE in kinds)
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

    private fun assertWeeklyBlockText(slots: List<TimetableSlot>) {
        val measure: (String, Float) -> Float = { text, size -> text.length * size * 0.5f }
        val range = requireNotNull(lectureHourRange(slots))
        val days = lectureVisibleDays(slots)
        val margin = 40f
        val axisWidth = 39f
        val dayWidth = weeklyDayColumnWidth(842f, days.size, margin, axisWidth)
        val placements = lectureBlockPlacements(slots, range)
        val slotsById = slots.associateBy { it.id }
        val minimumGridHeight = placements.maxOf { placement ->
            val laneWidth = dayWidth / placement.laneCount
            val textWidth = laneWidth - 2.4f - 3f - (if (placement.hasClash) 8f else 3f)
            val layout = weeklyBlockTextLayout(slotsById.getValue(placement.slotId), textWidth, 0f, measure)
            (layout.requiredHeight + 3.6f) / placement.heightFraction
        }
        val gridHeight = maxOf(220f, minimumGridHeight)
        val gridLeft = margin + axisWidth
        val allTextRects = mutableListOf<PdfTextRect>()

        for (placement in placements) {
            val slot = slotsById.getValue(placement.slotId)
            val dayIndex = days.indexOf(slot.dayOfWeek)
            val laneWidth = dayWidth / placement.laneCount
            val blockLeft = gridLeft + dayWidth * dayIndex + laneWidth * placement.laneIndex + 1.2f
            val blockRight = gridLeft + dayWidth * dayIndex + laneWidth * (placement.laneIndex + 1) - 1.2f
            val blockTop = placement.topFraction * gridHeight + 0.8f
            val blockBottom = (placement.topFraction + placement.heightFraction) * gridHeight - 0.8f
            val x = blockLeft + 3f
            val textWidth = blockRight - (if (placement.hasClash) 8f else 3f) - x
            val layout = weeklyBlockTextLayout(
                slot,
                textWidth,
                blockBottom - blockTop - 2f,
                measure
            )

            assertTrue("text fits in block ${slot.courseCode}", layout.fits)
            assertTrue(layout.lines.any { it.kind == WeeklyBlockTextKind.COURSE_CODE })
            assertTrue(layout.lines.any { it.kind == WeeklyBlockTextKind.TIME })
            if (slot.venue.isNotBlank()) assertTrue(layout.lines.any { it.kind == WeeklyBlockTextKind.VENUE })
            val codeSize = layout.lines.filter { it.kind == WeeklyBlockTextKind.COURSE_CODE }.minOf { it.textSize }
            val detailSize = layout.lines.filter { it.kind != WeeklyBlockTextKind.COURSE_CODE }.minOf { it.textSize }
            assertTrue(codeSize >= detailSize)

            val joinedText = layout.lines.joinToString("") { it.text }.filterNot(Char::isWhitespace)
            assertTrue(joinedText.contains(slot.courseCode.filterNot(Char::isWhitespace)))
            assertTrue(joinedText.contains(formatTimeRange(slot.startTime, slot.endTime).filterNot(Char::isWhitespace)))
            if (slot.venue.isNotBlank()) {
                assertTrue(joinedText.contains(slot.venue.filterNot(Char::isWhitespace)))
            }

            val blockBounds = PdfTextRect(blockLeft, blockTop, blockRight, blockBottom)
            val lineRects = layout.lines.map { line ->
                PdfTextRect(
                    x + line.bounds.left,
                    blockTop + line.bounds.top,
                    x + line.bounds.right,
                    blockTop + line.bounds.bottom
                )
            }
            assertTrue(textRectsStayInside(lineRects, blockBounds))
            allTextRects += lineRects
        }
        assertTrue(textRectsDoNotOverlap(allTextRects))
    }
}
