package com.xingheyuzhuan.shiguangschedule.ui.schedule.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.xingheyuzhuan.shiguangschedule.data.db.main.CourseWithWeeks
import com.xingheyuzhuan.shiguangschedule.data.db.main.TimeSlot
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.BorderTypeProto
import com.xingheyuzhuan.shiguangschedule.data.model.schedule_style.ScheduleModeProto
import com.xingheyuzhuan.shiguangschedule.ui.theme.LocalIsDarkTheme

internal data class CourseBlockDetailLayout(
    val locationMaxLines: Int,
    val teacherVisible: Boolean
)

internal const val NEXT_WEEK_CHIP_LINE_HEIGHT_SP = 9f
internal const val NEXT_WEEK_CHIP_VERTICAL_PADDING_DP = 1f
internal const val NEXT_WEEK_CHIP_BOTTOM_SPACING_DP = 2f

internal fun resolveCourseBlockBaseColor(
    normalCourseColor: Color,
    scheduleBackgroundColor: Color,
    showNextWeekPreview: Boolean
): Color = if (showNextWeekPreview) {
    Color(
        red = normalCourseColor.red * 0.6f + scheduleBackgroundColor.red * 0.4f,
        green = normalCourseColor.green * 0.6f + scheduleBackgroundColor.green * 0.4f,
        blue = normalCourseColor.blue * 0.6f + scheduleBackgroundColor.blue * 0.4f,
        alpha = normalCourseColor.alpha,
        colorSpace = normalCourseColor.colorSpace
    )
} else {
    normalCourseColor
}

internal fun nextWeekChipReservedHeightDp(
    isVisualDemoted: Boolean,
    isFloating: Boolean,
    fontScale: Float
): Float {
    return if (isVisualDemoted && !isFloating) {
        NEXT_WEEK_CHIP_LINE_HEIGHT_SP * fontScale +
                NEXT_WEEK_CHIP_VERTICAL_PADDING_DP * 2 +
                NEXT_WEEK_CHIP_BOTTOM_SPACING_DP
    } else {
        0f
    }
}

internal fun resolveCourseBlockDetailLayout(
    locationVisible: Boolean,
    teacherAvailable: Boolean,
    locationSingleLineWidthPx: Float,
    availableTextWidthPx: Float,
    contentHeightPx: Float,
    heightBeforeDetailsPx: Float,
    detailLineHeightPx: Float
): CourseBlockDetailLayout {
    val locationNeedsWrapping = locationVisible && locationSingleLineWidthPx > availableTextWidthPx
    val canFitTwoLocationLines = contentHeightPx >= heightBeforeDetailsPx + detailLineHeightPx * 2
    val locationMaxLines = if (locationNeedsWrapping && canFitTwoLocationLines) 2 else 1
    val locationLines = if (locationVisible) locationMaxLines else 0
    val teacherVisible = teacherAvailable &&
            !locationNeedsWrapping &&
            contentHeightPx >= heightBeforeDetailsPx + detailLineHeightPx * (locationLines + 1)

    return CourseBlockDetailLayout(
        locationMaxLines = locationMaxLines,
        teacherVisible = teacherVisible
    )
}

@Composable
fun CourseBlock(
    courseWrapper: CourseWithWeeks,
    isVisualDemoted: Boolean,
    style: ScheduleGridStyleComposed,
    timeSlots: List<TimeSlot>,
    modifier: Modifier = Modifier,
    isFloating: Boolean = false // 标记当前块是否处于长按选中/悬浮状态
) {
    val course = courseWrapper.course
    val isDarkTheme = LocalIsDarkTheme.current

    // 颜色适配
    val colorIndex = course.colorInt.takeIf { it in style.courseColorMaps.indices }
    val courseColorAdapted: Color? = colorIndex?.let { index ->
        val baseColorMap = style.courseColorMaps[index]
        if (isDarkTheme) baseColorMap.dark else baseColorMap.light
    }
    val fallbackColorAdapted: Color = if (isDarkTheme) style.courseColorMaps.first().dark else style.courseColorMaps.first().light

    val showNextWeekPreview = isVisualDemoted && !isFloating
    val normalCourseColor = courseColorAdapted ?: fallbackColorAdapted
    val scheduleBackgroundColor = MaterialTheme.colorScheme.surface
    val courseBaseColor = resolveCourseBlockBaseColor(
        normalCourseColor = normalCourseColor,
        scheduleBackgroundColor = scheduleBackgroundColor,
        showNextWeekPreview = showNextWeekPreview
    )
    val currentAlpha = if (isFloating) 0.95f else style.courseBlockAlpha
    val blockColor = courseBaseColor.copy(alpha = currentAlpha)
    val textColor = style.courseTextColor ?: MaterialTheme.colorScheme.onSurface

    // 字体大小
    val s13 = (13 * style.fontScale).sp
    val s10 = (10 * style.fontScale).sp

    // 核心分支逻辑：判断 24小时模式 与 节次模式 的时间文本渲染
    val customStartTime = course.customStartTime
    val customEndTime = course.customEndTime
    val customTimeString = if (customStartTime != null && customEndTime != null) "$customStartTime - $customEndTime" else null
    val isCustomTimeCourse = customTimeString != null

    val timeTextToShow = if (style.scheduleMode == ScheduleModeProto.TIME_24H_MODE) {
        // 24小时绝对时间轴模式：全部课程都显示起止时间
        if (isCustomTimeCourse) {
            customTimeString
        } else {
            val startSlot = timeSlots.find { it.number == course.startSection }
            val endSlot = timeSlots.find { it.number == course.endSection }
            if (startSlot != null && endSlot != null) "${startSlot.startTime} - ${endSlot.endTime}" else null
        }
    } else {
        // 传统节次模式：只有自定义课程显示起止时间；普通节次课程只有在开启展示开始时间时才显示开始时间
        if (isCustomTimeCourse) {
            customTimeString
        } else if (style.showStartTime) {
            timeSlots.find { it.number == course.startSection }?.startTime
        } else {
            null
        }
    }

    // 边框样式配置
    val borderColor = if (isFloating) Color(0xFF2196F3) else MaterialTheme.colorScheme.outline
    val borderWidth = if (isFloating) 2.dp else 1.dp
    val borderAlpha = if (isFloating) 1.0f else style.courseBlockAlpha
    val shape = RoundedCornerShape(style.courseBlockCornerRadius)

    val borderModifier = when (style.borderType) {
        BorderTypeProto.BORDER_TYPE_SOLID -> {
            Modifier.border(borderWidth, borderColor.copy(alpha = borderAlpha), shape)
        }
        BorderTypeProto.BORDER_TYPE_DASHED -> {
            Modifier.drawBehind {
                val strokeWidth = borderWidth.toPx()
                val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                drawOutline(
                    outline = shape.createOutline(size, layoutDirection, this),
                    color = borderColor.copy(alpha = borderAlpha),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, pathEffect = dashPathEffect)
                )
            }
        }
        else -> {
            if (isFloating) Modifier.border(borderWidth, borderColor, shape) else Modifier
        }
    }

    val horizontalAlignment = if (style.textAlignCenterHorizontal) Alignment.CenterHorizontally else Alignment.Start
    val verticalArrangement = if (style.textAlignCenterVertical) Arrangement.Center else Arrangement.Top
    val textAlign = if (style.textAlignCenterHorizontal) TextAlign.Center else TextAlign.Start

    // 选中捏起时，增加三维物理阴影
    val floatingShadowModifier = if (isFloating) {
        Modifier.shadow(elevation = 8.dp, shape = shape, clip = false)
    } else {
        Modifier
    }

    BoxWithConstraints(
        modifier = modifier
            .then(floatingShadowModifier)
            .fillMaxSize()
            .then(borderModifier)
            .clip(shape)
            .background(color = blockColor)
    ) {
        val density = LocalDensity.current
        val teacher = course.teacher
        val position = course.position
        val locationVisible = !style.hideLocation && position.isNotBlank()
        val locationText = if (locationVisible) {
            val prefix = if (style.removeLocationAt) "" else "@"
            "$prefix$position"
        } else {
            ""
        }
        val topPadding = if (style.textAlignCenterVertical) {
            style.courseBlockInnerPadding
        } else {
            style.courseTextTopPadding
        }
        val chipReservedHeight = nextWeekChipReservedHeightDp(
            isVisualDemoted = isVisualDemoted,
            isFloating = isFloating,
            fontScale = density.fontScale
        ).dp
        val contentHeight = (
                maxHeight - topPadding - style.courseBlockInnerPadding - chipReservedHeight
                ).coerceAtLeast(0.dp)
        val availableTextWidth = (maxWidth - style.courseBlockInnerPadding * 2).coerceAtLeast(0.dp)
        val detailTextStyle = TextStyle(fontSize = s10, lineHeight = 1.em)
        val textMeasurer = rememberTextMeasurer()
        val locationSingleLineWidthPx = if (locationVisible) {
            textMeasurer.measure(
                text = locationText,
                style = detailTextStyle,
                maxLines = 1,
                softWrap = false
            ).size.width.toFloat()
        } else {
            0f
        }
        val nameLineHeightPx = with(density) { s13.toPx() } * 1.2f
        val detailLineHeightPx = with(density) { s10.toPx() }
        val detailLayout = resolveCourseBlockDetailLayout(
            locationVisible = locationVisible,
            teacherAvailable = !style.hideTeacher && teacher.isNotBlank(),
            locationSingleLineWidthPx = locationSingleLineWidthPx,
            availableTextWidthPx = with(density) { availableTextWidth.toPx() },
            contentHeightPx = with(density) { contentHeight.toPx() },
            heightBeforeDetailsPx = nameLineHeightPx * 2 +
                    (if (timeTextToShow != null) detailLineHeightPx else 0f),
            detailLineHeightPx = detailLineHeightPx
        )

        // 课程文字内容容器
        Column(
            modifier = Modifier.fillMaxSize().padding(
                start = style.courseBlockInnerPadding,
                top = topPadding,
                end = style.courseBlockInnerPadding,
                bottom = style.courseBlockInnerPadding
            ),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement
        ) {
            if (showNextWeekPreview) {
                Text(
                    text = "下一周",
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                        .padding(
                            horizontal = 3.dp,
                            vertical = NEXT_WEEK_CHIP_VERTICAL_PADDING_DP.dp
                        ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(lineHeight = NEXT_WEEK_CHIP_LINE_HEIGHT_SP.sp)
                )
                Spacer(Modifier.height(NEXT_WEEK_CHIP_BOTTOM_SPACING_DP.dp))
            }

            if (timeTextToShow != null) {
                Text(
                    text = timeTextToShow,
                    fontSize = s10,
                    color = textColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = textAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(lineHeight = 1.em)
                )
            }

            Text(
                text = course.name,
                fontSize = s13,
                fontWeight = FontWeight.Bold,
                color = textColor,
                overflow = TextOverflow.Ellipsis,
                textAlign = textAlign,
                modifier = Modifier.weight(1f, fill = false),
                style = TextStyle(lineHeight = 1.2.em)
            )

            if (locationVisible) {
                Text(
                    text = locationText,
                    color = textColor,
                    textAlign = textAlign,
                    maxLines = detailLayout.locationMaxLines,
                    overflow = TextOverflow.Ellipsis,
                    style = detailTextStyle
                )
            }

            if (detailLayout.teacherVisible) {
                Text(
                    text = teacher,
                    fontSize = s10,
                    color = textColor,
                    textAlign = textAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(lineHeight = 1.em)
                )
            }
        }

    }
}
