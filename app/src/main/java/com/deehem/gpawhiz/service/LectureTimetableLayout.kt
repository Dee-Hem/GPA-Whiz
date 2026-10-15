package com.deehem.gpawhiz.service

import com.deehem.gpawhiz.data.TimetableSlot
import java.util.Locale

data class LectureHourRange(
    val startHour: Int,
    val endHour: Int
) {
    val startMinute: Int get() = startHour * 60
    val durationMinutes: Int get() = (endHour - startHour) * 60
}

data class LectureBlockPlacement(
    val slotId: Int,
    val topFraction: Float,
    val heightFraction: Float,
    val laneIndex: Int,
    val laneCount: Int,
    val hasClash: Boolean
)

enum class WeeklyBlockTextMode {
    STACKED,
    INLINE,
    CODE_ONLY
}

data class LectureClashGroup(
    val dayOfWeek: Int,
    val slots: List<TimetableSlot>
)

data class TimetableCourseColor(
    val fill: Int,
    val border: Int,
    val text: Int
)

internal data class TimedLecture(
    val slot: TimetableSlot,
    val startMinute: Int,
    val endMinute: Int
)

fun lectureHourRange(slots: List<TimetableSlot>): LectureHourRange? {
    if (slots.isEmpty()) return null
    val lectures = slots.map(::timedLecture)
    val earliestStart = lectures.minOf { it.startMinute }
    val latestEnd = lectures.maxOf { it.endMinute }
    return LectureHourRange(
        startHour = earliestStart / 60,
        endHour = (latestEnd + 59) / 60
    )
}

fun lectureBlockPlacements(
    slots: List<TimetableSlot>,
    hourRange: LectureHourRange
): List<LectureBlockPlacement> {
    if (slots.isEmpty()) return emptyList()
    require(hourRange.durationMinutes > 0) { "Lecture hour range must have a positive duration." }
    val placements = mutableListOf<LectureBlockPlacement>()

    for ((_, daySlots) in slots.groupBy { it.dayOfWeek }) {
        val sortedLectures = daySlots.map(::timedLecture).sortedWith(
            compareBy<TimedLecture> { it.startMinute }.thenBy { it.endMinute }.thenBy { it.slot.id }
        )
        val overlapGroups = mutableListOf<List<TimedLecture>>()
        var currentGroup = mutableListOf<TimedLecture>()
        var groupEnd = -1

        for (lecture in sortedLectures) {
            if (currentGroup.isNotEmpty() && lecture.startMinute >= groupEnd) {
                overlapGroups.add(currentGroup)
                currentGroup = mutableListOf()
                groupEnd = -1
            }
            currentGroup.add(lecture)
            groupEnd = maxOf(groupEnd, lecture.endMinute)
        }
        if (currentGroup.isNotEmpty()) overlapGroups.add(currentGroup)

        for (group in overlapGroups) {
            val laneEnds = mutableListOf<Int>()
            val assignments = group.map { lecture ->
                val lane = laneEnds.indexOfFirst { it <= lecture.startMinute }
                    .takeIf { it >= 0 }
                    ?: laneEnds.size.also { laneEnds.add(lecture.endMinute) }
                if (lane < laneEnds.size) laneEnds[lane] = lecture.endMinute
                lecture to lane
            }
            val laneCount = laneEnds.size

            for ((lecture, lane) in assignments) {
                val overlaps = group.any { other ->
                    other !== lecture &&
                        lecture.startMinute < other.endMinute &&
                        other.startMinute < lecture.endMinute
                }
                val duration = hourRange.durationMinutes.toFloat()
                placements.add(
                    LectureBlockPlacement(
                        slotId = lecture.slot.id,
                        topFraction = (lecture.startMinute - hourRange.startMinute) / duration,
                        heightFraction = (lecture.endMinute - lecture.startMinute) / duration,
                        laneIndex = lane,
                        laneCount = laneCount,
                        hasClash = overlaps
                    )
                )
            }
        }
    }

    return placements
}

fun lectureClashGroups(slots: List<TimetableSlot>): List<LectureClashGroup> {
    if (slots.isEmpty()) return emptyList()
    val clashes = mutableListOf<LectureClashGroup>()

    for ((day, daySlots) in slots.groupBy { it.dayOfWeek }) {
        val sortedLectures = daySlots.map(::timedLecture).sortedWith(
            compareBy<TimedLecture> { it.startMinute }.thenBy { it.endMinute }.thenBy { it.slot.id }
        )
        var group = mutableListOf<TimedLecture>()
        var groupEnd = -1

        fun saveClashGroup() {
            if (group.size > 1) {
                clashes.add(LectureClashGroup(day, group.map { it.slot }))
            }
            group = mutableListOf()
            groupEnd = -1
        }

        for (lecture in sortedLectures) {
            if (group.isNotEmpty() && lecture.startMinute >= groupEnd) saveClashGroup()
            group.add(lecture)
            groupEnd = maxOf(groupEnd, lecture.endMinute)
        }
        if (group.isNotEmpty()) saveClashGroup()
    }

    return clashes.sortedBy { it.dayOfWeek }
}

fun lectureVisibleDays(slots: List<TimetableSlot>): List<Int> =
    (1..5).toList() + (6..7).filter { weekendDay -> slots.any { it.dayOfWeek == weekendDay } }

fun weeklyDayColumnWidth(pageWidth: Float, dayCount: Int, margin: Float = 40f, axisWidth: Float = 39f): Float {
    require(dayCount > 0) { "A weekly grid must have at least one day column." }
    return (pageWidth - 2 * margin - axisWidth) / dayCount
}

fun hourAxisLabelPositions(hourRange: LectureHourRange, top: Float, bottom: Float): List<Float> {
    require(hourRange.durationMinutes > 0 && bottom > top) { "Hour axis requires a positive range and height." }
    return (hourRange.startHour..hourRange.endHour).map { hour ->
        top + (hour - hourRange.startHour).toFloat() / (hourRange.endHour - hourRange.startHour) * (bottom - top)
    }
}

fun weeklyBlockTextMode(
    textWidth: Float,
    blockHeight: Float,
    courseCodeWidth: Float,
    timeRangeWidth: Float,
    lineHeight: Float = 8.75f,
    horizontalGap: Float = 4f
): WeeklyBlockTextMode = when {
    blockHeight >= lineHeight * 2f + 2f -> WeeklyBlockTextMode.STACKED
    courseCodeWidth + horizontalGap + timeRangeWidth <= textWidth -> WeeklyBlockTextMode.INLINE
    else -> WeeklyBlockTextMode.CODE_ONLY
}

fun timetableCourseColor(courseCode: String): TimetableCourseColor {
    return timetableCourseColors(listOf(courseCode)).getValue(normalizeCourseCode(courseCode))
}

fun timetableCourseColor(courseCode: String, allCourseCodes: Collection<String>): TimetableCourseColor =
    timetableCourseColors(allCourseCodes).getValue(normalizeCourseCode(courseCode))

fun timetableCourseColors(courseCodes: Collection<String>): Map<String, TimetableCourseColor> {
    val palette = listOf(
        TimetableCourseColor(0xFFE8F0FE.toInt(), 0xFF2B5896.toInt(), 0xFF17375E.toInt()),
        TimetableCourseColor(0xFFFFF3E0.toInt(), 0xFFEF6C00.toInt(), 0xFFE65100.toInt()),
        TimetableCourseColor(0xFFE8F5E9.toInt(), 0xFF2E7D32.toInt(), 0xFF1B5E20.toInt()),
        TimetableCourseColor(0xFFFFEBEE.toInt(), 0xFFC62828.toInt(), 0xFF7F0000.toInt()),
        TimetableCourseColor(0xFFF3E5F5.toInt(), 0xFF7B1FA2.toInt(), 0xFF4A148C.toInt()),
        TimetableCourseColor(0xFFE0F2F1.toInt(), 0xFF00796B.toInt(), 0xFF004D40.toInt()),
        TimetableCourseColor(0xFFFFFDE7.toInt(), 0xFF9E7A00.toInt(), 0xFF5D4800.toInt()),
        TimetableCourseColor(0xFFFCE4EC.toInt(), 0xFFC2185B.toInt(), 0xFF880E4F.toInt()),
        TimetableCourseColor(0xFFEFEBE9.toInt(), 0xFF6D4C41.toInt(), 0xFF3E2723.toInt()),
        TimetableCourseColor(0xFFF1F8E9.toInt(), 0xFF558B2F.toInt(), 0xFF33691E.toInt()),
        TimetableCourseColor(0xFFEDE7F6.toInt(), 0xFF512DA8.toInt(), 0xFF311B92.toInt()),
        TimetableCourseColor(0xFFE0F7FA.toInt(), 0xFF00838F.toInt(), 0xFF004D40.toInt())
    )
    val sortedCodes = courseCodes.map(::normalizeCourseCode).distinct().sorted()
    return sortedCodes.mapIndexed { index, code -> code to palette[index % palette.size] }.toMap()
}

private fun normalizeCourseCode(code: String): String = code.trim().uppercase(Locale.ROOT)

internal fun timedLecture(slot: TimetableSlot): TimedLecture {
    require(slot.dayOfWeek in 1..7) { "Lecture day must be between 1 and 7." }
    val startMinute = parseLectureTime(slot.startTime)
    val endMinute = parseLectureTime(slot.endTime)
    require(endMinute > startMinute) {
        "Lecture end time must be later than its start time for ${slot.courseCode}."
    }
    return TimedLecture(slot, startMinute, endMinute)
}

private fun parseLectureTime(time: String): Int {
    require(time.matches(Regex("\\d{1,2}:\\d{2}"))) { "Invalid lecture time: $time." }
    val parts = time.split(':')
    val hour = parts[0].toInt()
    val minute = parts[1].toInt()
    require(hour in 0..23 && minute in 0..59) { "Invalid lecture time: $time." }
    return hour * 60 + minute
}
