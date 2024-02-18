package com.example.customslider

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import kotlinx.coroutines.coroutineScope
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun CustomSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    SliderContent(
        interactionSource = interactionSource,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
    )
}

@Composable
private fun SliderContent(
    interactionSource: MutableInteractionSource,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbWidth = remember { mutableStateOf(20.dp.value) }
    val totalWidth = remember { mutableStateOf(0f) }

    val rawOffset = remember { mutableStateOf(0f) }

    val draggableState = remember {
        SliderDraggableState {
            val maxPx = max(totalWidth.value - thumbWidth.value / 2, 0f)
            val minPx = min(thumbWidth.value / 2, maxPx)
            rawOffset.value = rawOffset.value + it
            onValueChange(
                norm(rawOffset.value, minPx, maxPx)
            )
        }
    }
    val dragModifier = Modifier.draggable(
        orientation = Orientation.Horizontal,
        interactionSource = interactionSource,
        startDragImmediately = true,
        onDragStopped = { _ -> },
        state = draggableState
    )
    Layout(
        content = {
            SliderTrack(
                sliderStartPosition = 0f,
                sliderEndPosition = value
            )
            SliderThumb(
                interactionSource = interactionSource,
            )
        },
        modifier = modifier
            .minimumInteractiveComponentSize()
            .requiredSizeIn(
                minWidth = 20.dp,
                minHeight = 20.dp,
            )
            .focusable(interactionSource = interactionSource, enabled = true)
            .then(dragModifier)
    ) { measurables, constraints ->
        val trackMeasurable = measurables[0]
        val thumbMeasurable = measurables[1]

        val thumbPlaceable = thumbMeasurable.measure(constraints)
        val trackPlaceable =
            trackMeasurable.measure(
                constraints.offset(
                    horizontal = -thumbPlaceable.width
                ).copy(minHeight = 0)
            )

        thumbWidth.value = thumbPlaceable.width.toFloat()
        totalWidth.value = constraints.maxWidth.toFloat()

        val sliderHeight = thumbPlaceable.height.coerceAtLeast(trackPlaceable.height)
        val trackOffsetX = thumbPlaceable.width / 2
        val thumbOffsetX = ((trackPlaceable.width) * value).roundToInt()
        val trackOffsetY = (sliderHeight - trackPlaceable.height) / 2
        val thumbOffsetY = (sliderHeight - thumbPlaceable.height) / 2

        layout(
            width = constraints.maxWidth,
            height = thumbPlaceable.height
        ) {
            trackPlaceable.placeRelative(trackOffsetX, trackOffsetY)
            thumbPlaceable.placeRelative(thumbOffsetX, thumbOffsetY)
        }
    }
}

@Composable
private fun SliderThumb(
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val thumbSize = 20.dp
    val internalThumbSize = thumbSize - 8.dp
    val thumbShape = CircleShape

    val interactions = remember { mutableStateListOf<Interaction>() }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> interactions.add(interaction)
                is PressInteraction.Release -> interactions.remove(interaction.press)
                is PressInteraction.Cancel -> interactions.remove(interaction.press)
                is DragInteraction.Start -> interactions.add(interaction)
                is DragInteraction.Stop -> interactions.remove(interaction.start)
                is DragInteraction.Cancel -> interactions.remove(interaction.start)
            }
        }
    }

    val elevation = if (interactions.isNotEmpty()) 4.dp else 1.dp

    Surface(
        color = Color.Green,
        shape = thumbShape,
        modifier = modifier
            .size(thumbSize)
            .indication(interactionSource = interactionSource, indication = null)
            .hoverable(interactionSource = interactionSource, enabled = enabled)
            .shadow(
                elevation = elevation,
                shape = thumbShape,
                clip = false,
            ),
    ) {
        Spacer(
            modifier = Modifier
                .padding(4.dp)
                .size(internalThumbSize)
                .indication(interactionSource = interactionSource, indication = null)
                .hoverable(interactionSource = interactionSource, enabled = enabled)
                .shadow(
                    elevation = elevation,
                    shape = thumbShape,
                    clip = false,
                )
                .background(Color.White, thumbShape)
        )
    }
}

@Composable
private fun SliderTrack(
    sliderStartPosition: Float,
    sliderEndPosition: Float,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val trackHeight = 4.dp
    val inactiveTrackColor = Color.Gray
    val activeTrackColor = Color.Green

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight),
    ) {
        val start = Offset(0f, center.y)
        val end = Offset(size.width, center.y)
        val trackStroke = trackHeight.toPx()

        drawLine(
            color = inactiveTrackColor,
            start = start,
            end = end,
            strokeWidth = trackStroke,
            cap = StrokeCap.Round
        )

        val sliderValueEnd = Offset(
            start.x +
                    (end.x - start.x) * sliderEndPosition,
            center.y
        )

        val sliderValueStart = Offset(
            start.x + (end.x - start.x) * sliderStartPosition,
            center.y
        )

        drawLine(
            color = activeTrackColor,
            start = sliderValueStart,
            end = sliderValueEnd,
            strokeWidth = trackStroke,
            cap = StrokeCap.Round
        )

    }
}

private class SliderDraggableState(
    val onDelta: (Float) -> Unit
) : DraggableState {

    var isDragging by mutableStateOf(false)
        private set

    private val dragScope: DragScope = object : DragScope {
        override fun dragBy(pixels: Float): Unit = onDelta(pixels)
    }

    private val scrollMutex = MutatorMutex()

    override suspend fun drag(
        dragPriority: MutatePriority,
        block: suspend DragScope.() -> Unit
    ): Unit = coroutineScope {
        isDragging = true
        scrollMutex.mutateWith(dragScope, dragPriority, block)
        isDragging = false
    }

    override fun dispatchRawDelta(delta: Float) {
        return onDelta(delta)
    }
}

fun lerp(amt: Float, start: Float, stop: Float): Float {
    return start + (stop - start) * amt
}

fun norm(value: Float, start: Float, stop: Float): Float {
    return (value / (stop - start)).coerceIn(0f, 1f)
}

@Composable
@Preview
fun CustomSliderPreview() {
    CustomSlider(
        value = 0.5f,
        onValueChange = { /*TODO*/ }
    )
}