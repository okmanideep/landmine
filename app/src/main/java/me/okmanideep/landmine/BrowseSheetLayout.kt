package me.okmanideep.landmine

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.LocalOverScrollConfiguration
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.statusBarsPadding

enum class BrowseSheetValue {
    FULL_PAGE,
    COLLAPSED,
    DISMISSED,
    INITIALIZED,
}

private val SHEET_MARGIN_WHEN_COLLAPSED = 12.dp
private val SHEET_RADIUS_WHEN_COLLAPSED = 12.dp
private val HANDLE_WIDTH = 32.dp
private val HANDLE_HEIGHT = 4.dp
private val HANDLE_PADDING = 8.dp
private const val ELEVATION_BACKGROUND = 0F
private const val ELEVATION_SHEET = 1F
private const val ELEVATION_HEADER = 2F
private const val ELEVATION_CLICK_AREA = 1F
private const val ELEVATION_TOOLBAR = 3F

/** Ration of the screens last portion where fade starts for the sheet content*/
private const val SHEET_FADE_AREA_RATIO = 0.1f

/** Ration of the screens last portion where fade starts for the overlay*/
private const val OVERLAY_FADE_AREA_RATIO = 0.3f

/**
 * Holds all calculations regarding BrowseSheet measurements based on the input metrics
 *
 * @see: [Browse Sheet Measurements | Android - Hotstar X](https://hotstar.atlassian.net/wiki/spaces/HP2/pages/3385229497/Browse+Sheet+Measurements)
 */
private data class Measurements(
    val density: Density,
    val maxWidth: Dp,
    val maxHeight: Dp,
    val toolbarHeight: Dp,
    val statusBarPadding: Dp,
) {
    val initialSheetTop = maxHeight * 0.3f
    val handleSpace = HANDLE_HEIGHT + (2 * HANDLE_PADDING)
    val headerHeight = initialSheetTop - handleSpace - statusBarPadding
    val minOffset = with(density) { (toolbarHeight - headerHeight - handleSpace).toPx() }
    val collapsedOffset = 0f
    val maxOffset = with(density) { (maxHeight * 0.5f).toPx() }
    val minScale = with(density) {
        (maxWidth - SHEET_MARGIN_WHEN_COLLAPSED * 2).toPx() / maxWidth.toPx()
    }
    val sheetHeight = maxHeight - toolbarHeight - statusBarPadding
    val maxSheetBorderRadius = with(density) {
        SHEET_RADIUS_WHEN_COLLAPSED.toPx()
    }

    val fullWidthOffset = minOffset + ((collapsedOffset - minOffset) / 2)

    val clickAreaHeight = initialSheetTop

    val anchors = mapOf(
        minOffset to BrowseSheetValue.FULL_PAGE,
        collapsedOffset to BrowseSheetValue.COLLAPSED,
        maxOffset to BrowseSheetValue.DISMISSED,
        maxOffset * 1.5f to BrowseSheetValue.INITIALIZED,
    )

    fun toolbarOffset(currentOffset: Float): Float {
        return minOffset - currentOffset
    }

    fun sheetOffset(currentOffset: Float): Float {
        return with(density) {
            currentOffset + initialSheetTop.toPx()
        }
    }

    fun sheetScale(currentOffset: Float): Float {
        if (currentOffset <= fullWidthOffset) return 1.0f
        if (currentOffset >= collapsedOffset) return minScale

        return minScale + ((collapsedOffset - currentOffset) / (collapsedOffset - fullWidthOffset)) * (1.0f - minScale)
    }

    fun contentAlpha(browseSheetValue: BrowseSheetValue, currentOffset: Float): Float {
        if (browseSheetValue == BrowseSheetValue.INITIALIZED && currentOffset == 0f) return 0f
        if (currentOffset < maxOffset * (1 - SHEET_FADE_AREA_RATIO)) return 1f
        return calculateOpacityForLastPortion(
            currentOffset = currentOffset,
            screenRatio = SHEET_FADE_AREA_RATIO
        )
    }

    private fun calculateOpacityForLastPortion(currentOffset: Float, screenRatio: Float) =
        ((maxOffset * screenRatio - (currentOffset - maxOffset * (1 - screenRatio))) / (maxOffset * screenRatio - collapsedOffset))
            .coerceIn(0f, 1f)

    fun overlayAlpha(browseSheetValue: BrowseSheetValue, currentOffset: Float): Float {
        if (browseSheetValue == BrowseSheetValue.INITIALIZED && currentOffset == 0f)
            return 0f
        return calculateOpacityForLastPortion(
            currentOffset = currentOffset,
            screenRatio = OVERLAY_FADE_AREA_RATIO
        )
    }

    fun headerScale(currentOffset: Float): Float {
        if (currentOffset >= collapsedOffset) return 1.0f
        if (currentOffset <= fullWidthOffset) return 0.8f

        return 0.8f + (0.2f * ((currentOffset - fullWidthOffset) / (collapsedOffset - fullWidthOffset)))
    }

    fun sheetBorderRadius(currentOffset: Float): Float {
        if (currentOffset >= fullWidthOffset) return maxSheetBorderRadius

        return ((currentOffset - minOffset) / (fullWidthOffset - minOffset)) * maxSheetBorderRadius
    }

    fun handleOffset(currentOffset: Float): Float {
        return with(density) {
            sheetTop(currentOffset) - handleSpace.toPx()
        }
    }

    private fun sheetTop(currentOffset: Float): Float {
        val sheetScale = sheetScale(currentOffset)

        return with(density) {
            sheetOffset(currentOffset) + (sheetHeight.toPx() * (1 - sheetScale) * 0.5f)
        }
    }

    private fun headerOffset(currentOffset: Float): Float {
        return with(density) { statusBarPadding.toPx() + currentOffset }
    }

    private fun clickAreaOffset(currentOffset: Float): Float {
        return with(density) { currentOffset }
    }

    @ExperimentalMaterialApi
    fun BrowseSheetState.getGraphics(): Graphics {
        val currentOffset = when (currentValue) {
            else -> offset.value
        }
        return Graphics(
            sheetScale = sheetScale(currentOffset),
            sheetOffset = sheetOffset(currentOffset),
            sheetBorderRadius = sheetBorderRadius(currentOffset),
            headerOffset = headerOffset(currentOffset),
            headerScale = headerScale(currentOffset),
            toolbarOffset = toolbarOffset(currentOffset),
            overlayAlpha = overlayAlpha(
                browseSheetValue = targetValue,
                currentOffset = currentOffset
            ),
            contentAlpha = contentAlpha(
                browseSheetValue = targetValue,
                currentOffset = currentOffset
            ),
            handleOffset = handleOffset(currentOffset),
            clickAreaOffset = clickAreaOffset(currentOffset)
        )
    }
}

@Composable
private fun rememberMeasurements(
    maxWidth: Dp,
    maxHeight: Dp,
    toolbarHeight: Dp,
): Measurements {
    val density = LocalDensity.current
    val statusBarInsets =
        rememberInsetsPaddingValues(insets = LocalWindowInsets.current.statusBars)

    return remember(density, maxWidth, maxHeight, toolbarHeight, statusBarInsets) {
        Measurements(
            density = density,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            toolbarHeight = toolbarHeight,
            statusBarPadding = statusBarInsets.calculateTopPadding(),
        )
    }
}

/**
 * Holds all the graphics layer manipulation values that are derived from sheet state
 *
 * Instead of multiple state (or derived states), we can have one single state
 * that provides all the graphics layer manipulation values with a class like this
 */
private data class Graphics(
    val sheetScale: Float,
    val sheetOffset: Float,
    val sheetBorderRadius: Float,
    val headerOffset: Float,
    val headerScale: Float,
    val handleOffset: Float,
    val toolbarOffset: Float,
    val contentAlpha: Float,
    val overlayAlpha: Float,
    val clickAreaOffset: Float,
)

@ExperimentalMaterialApi
class BrowseSheetState(
    initialValue: BrowseSheetValue
) : SwipeableState<BrowseSheetValue>(initialValue) {

    companion object {
        /**
         * The default [Saver] implementation for [BrowseSheetState].
         */
        fun Saver() = Saver<BrowseSheetState, BrowseSheetValue>(
            save = { it.currentValue },
            restore = { BrowseSheetState(it) }
        )
    }
}

@Composable
@ExperimentalMaterialApi
fun rememberBrowseSheetState(initialValue: BrowseSheetValue): BrowseSheetState {
    return rememberSaveable(saver = BrowseSheetState.Saver()) {
        BrowseSheetState(initialValue)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalMaterialApi
@Composable
fun BrowseSheetLayout(
    modifier: Modifier = Modifier,
    title: String = "",
    toolbarHeight: Dp = 48.dp,
    header: @Composable BoxScope.() -> Unit,
    elevation: Dp = 0.dp,
    onCloseClick: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(),
    state: BrowseSheetState = rememberBrowseSheetState(initialValue = BrowseSheetValue.COLLAPSED),
    content: @Composable (contentPadding: PaddingValues) -> Unit,
) {

    BoxWithConstraints(
        modifier = modifier
    ) {
        val measurements = rememberMeasurements(
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            toolbarHeight = toolbarHeight,
        )
        val scrollConnection = remember(state) {
            BrowseSheetScrollConnection(state, minOffset = measurements.minOffset)
        }

        val graphics = derivedStateOf {
            with(measurements) {
                state.getGraphics()
            }
        }

        // Background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(graphics.value.overlayAlpha)
                .background(Black.copy(alpha = 0.5f))
                .zIndex(ELEVATION_BACKGROUND)
        )

        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    translationY = graphics.value.headerOffset
                    scaleX = graphics.value.headerScale
                    scaleY = graphics.value.headerScale
                }
                .alpha(graphics.value.contentAlpha)
                .height(measurements.headerHeight)
                .zIndex(ELEVATION_HEADER)
        ) {
            header()
        }

        // Handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = graphics.value.handleOffset
                }
                .alpha(graphics.value.contentAlpha)
                .zIndex(ELEVATION_SHEET)
        ) {
            Handle()
        }

        // Sheet
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(measurements.sheetHeight)
                .nestedScroll(scrollConnection)
                .swipeable(
                    state = state,
                    anchors = measurements.anchors,
                    orientation = Orientation.Vertical,
                    resistance = SwipeableDefaults.resistanceConfig(
                        anchors = measurements.anchors.keys,
                        // no overflow
                        factorAtMin = 0f,
                        factorAtMax = 0f
                    )
                )
                .graphicsLayer {
                    scaleX = graphics.value.sheetScale
                    scaleY = graphics.value.sheetScale
                    translationY = graphics.value.sheetOffset
                }
                .alpha(graphics.value.contentAlpha)
                .clip(RoundedCornerShape(graphics.value.sheetBorderRadius))
                .zIndex(ELEVATION_SHEET),
            elevation = elevation,
            color = Color(0xFF0F1014),
        ) {
            // show over scroll effect inside the sheet content, only when the browse sheet is expanded
            val overScrollConfiguration =
                if (state.offset.value == measurements.minOffset) LocalOverScrollConfiguration.current else null
            CompositionLocalProvider(LocalOverScrollConfiguration provides overScrollConfiguration) {
                content(sheetContentPadding(contentPadding))
            }
        }

        // Header click area
        Box(
            modifier = Modifier
                .height(measurements.clickAreaHeight)
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = graphics.value.clickAreaOffset
                }
                .clickable(
                    onClick = onCloseClick,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .zIndex(ELEVATION_CLICK_AREA)
        )

        // Toolbar
        if (title.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = graphics.value.toolbarOffset
                    }
                    .fillMaxWidth()
                    .background(Color(0xFF0F1014))
                    .statusBarsPadding()
                    .height(toolbarHeight)
                    .align(Alignment.TopCenter)
                    .zIndex(ELEVATION_TOOLBAR)
            ) {
                Toolbar(
                    title = title,
                    onCloseClick = onCloseClick,
                    onBackClick = onBackClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun sheetContentPadding(contentPadding: PaddingValues): PaddingValues {
    val layoutDirection = LocalLayoutDirection.current
    return PaddingValues(
        top = 0.dp,
        start = contentPadding.calculateStartPadding(layoutDirection),
        bottom = contentPadding.calculateBottomPadding(),
        end = contentPadding.calculateEndPadding(layoutDirection)
    )
}

@Composable
private fun Toolbar(
    title: String,
    modifier: Modifier = Modifier,
    onCloseClick: () -> Unit,
    onBackClick: (() -> Unit)? = null,
) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.onSurface) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier,
        ) {
            AnimatedVisibility(visible = onBackClick != null) {
                IconButton(onClick = {
                    onBackClick?.invoke()
                }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            Text(
                modifier = Modifier.weight(1f),
                text = title,
                style = MaterialTheme.typography.h6,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AnimatedVisibility(visible = onBackClick == null) {
                if (onBackClick == null) {
                    IconButton(
                        modifier = Modifier.padding(12.dp),
                        onClick = {
                        onCloseClick.invoke()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.Handle() {
    Divider(
        modifier = Modifier
            .padding(HANDLE_PADDING)
            .align(Alignment.Center)
            .clip(MaterialTheme.shapes.small)
            .requiredSize(width = HANDLE_WIDTH, height = HANDLE_HEIGHT),

        color = MaterialTheme.colors.onSurface,
    )
}

@ExperimentalMaterialApi
private class BrowseSheetScrollConnection(
    val state: SwipeableState<BrowseSheetValue>,
    val minOffset: Float,
) : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val delta = available.y
        // if the user is scrolling up, then consume all scroll unless the browse sheet is full expanded to page
        return if (delta < 0 && state.offset.value > minOffset) {
            state.performDrag(delta).toOffset()
        } else {
            super.onPreScroll(available, source)
        }
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val delta = available.y
        return state.performDrag(delta).toOffset()
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        val velocity = available.y
        // swiping up and browse sheet is not yet and expanded fully
        return if (velocity < 0 && state.offset.value > minOffset) {
            state.performFling(velocity)
            available
        } else {
            super.onPreFling(available)
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        state.performFling(available.y)
        return super.onPostFling(consumed, available)
    }
}

private fun Float.toOffset(): Offset {
    return Offset(x = 0f, y = this)
}
