package me.okmanideep.landmine.compass.stack

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import me.okmanideep.landmine.compass.LocalOwnersProvider
import me.okmanideep.landmine.compass.NavEntry

/**
 * To represent UI part of a destination
 *
 * @param content Page's content @Composable
 * @param isTransparent if true, PageStack will also render the previous entry in the stack
 * @param pageOrientation To set the orientation of the page.
 * @param enterTransition animation when the destination becomes topDestination
 * @param exitTransition animation when the destination pops from the current stack
 * Mostly when pressing the back button / navigating back
 *
 * See [Page][me.okmanideep.landmine.compass.model.Page] for the data part
 */
internal class StackDestination(
    val content: @Composable (NavEntry) -> Unit,
    val isTransparent: Boolean,
    val isSingleEntry: Boolean,
    val pageOrientation: PageOrientation,
    val enterTransition: EnterTransition,
    val exitTransition: ExitTransition,
    val visibilityTransitions: PageVisibilityTransitions?
)

/**
 * To specify the orientation a page.
 */
enum class PageOrientation {
    /**
     * The orientation is determined by the device orientation sensor for any of the 4 orientations.
     */
    FULL_SENSOR,

    /**
     * Portrait orientation (the display is taller than it is wide).
     */
    PORTRAIT,

    /**
     * Landscape orientation (the display is wider than it is tall).
     */
    LANDSCAPE,

    /**
     * Compass won't manage the orientation for you.
     * This is useful when you want to control the orientation from the page.
     */
    CUSTOM
}

/**
 * StackGraph is a collection of [StackDestination] nodes.
 * A StackGraph serves as a 'virtual' destination: while the StackGraph itself will not appear
 * on the back stack, navigating to the StackGraph will cause the
 * [starting destination][destinationFor] to be added to the back stack.
 *
 * @property destinationByType A map containing the [Page#type](Page#type) as the key and [StackDestination]
 * as the value
 */
internal class StackGraph(
    private val destinationByType: Map<String, StackDestination>
) {

    /**
     * To get [StackDestination] for the given pageType
     * @param pageType
     * @return [StackDestination] if exist
     * @throws IllegalArgumentException if doesn't exist
     */
    fun destinationFor(pageType: String): StackDestination {
        return destinationByType[pageType]
            ?: throw IllegalArgumentException("No destination for `$pageType`")
    }

    /**
     * To check if the given pageType exists in the destination map
     * @param pageType
     * @return if exists true, false otherwise.
     */
    fun hasPageType(pageType: String): Boolean {
        return destinationByType.containsKey(pageType)
    }

    fun key(): String {
        return destinationByType.keys.joinToString(",")
    }
}

/**
 * To define page visibility transition.
 * @param pushBackTransition Screen is going back in the stack and some other screen is coming on top
 * @param pullFrontTransition Screen is coming back to fore front of the stack
 */
class PageVisibilityTransitions(
    val pullFrontTransition: EnterTransition,
    val pushBackTransition: ExitTransition
)

/**
 * DSL for constructing a new [StackGraph]
 *
 * @property destinationByType
 */
class StackGraphBuilder
internal constructor(
    private val destinationByType: HashMap<String, StackDestination> = HashMap(),
) {
    /**
     * To create a new destination
     *
     * @param type Unique page type. Eg. login, home etc
     * @param isTransparent To render both this destination and previous entry behind it
     * @param isSingleEntry Only keep single entry of this page type in the stack, remove the old entries
     * @param pageOrientation To set the orientation of the page.
     * @param enterTransition Animation : screen is created and is added to the stack
     * @param exitTransition Animation : screen is going away, being removed from the stack
     * @param visibilityTransitions See [PageVisibilityTransitions]'s desc
     * @param content the content @Composable of the page
     */
    fun page(
        type: String,
        isTransparent: Boolean = false,
        isSingleEntry: Boolean = false,
        pageOrientation: PageOrientation = PageOrientation.CUSTOM,
        enterTransition: EnterTransition = slideInHorizontally(initialOffsetX = { it }),
        exitTransition: ExitTransition = slideOutHorizontally(targetOffsetX = { it }),
        visibilityTransitions: PageVisibilityTransitions? = null,
        content: @Composable (NavEntry) -> Unit,
    ) {
        destinationByType[type] = StackDestination(
            content = content,
            isTransparent = isTransparent,
            isSingleEntry = isSingleEntry,
            pageOrientation = pageOrientation,
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            visibilityTransitions = visibilityTransitions
        )
    }

    internal fun build() = StackGraph(destinationByType)
}

@Composable
internal fun NavEntry.Render(
    saveableStateHolder: SaveableStateHolder,
    graph: StackGraph,
) {
    // Changing orientation
    val activity = LocalContext.current as Activity
    LaunchedEffect(Unit) {
        // If it's CUSTOM, we don't do anything with the orientation.
        val activityOrientation: Int? = when (graph.destinationFor(pageType).pageOrientation) {
            PageOrientation.FULL_SENSOR -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            PageOrientation.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            PageOrientation.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            PageOrientation.CUSTOM -> null
        }
        if (activityOrientation != null) {
            activity.requestedOrientation = activityOrientation
        }
    }

    LocalOwnersProvider(saveableStateHolder) {
        val destination = graph.destinationFor(pageType)
        val currentOrientation = LocalConfiguration.current.orientation
        if (shouldRenderDestination(currentOrientation, destination.pageOrientation)) {
            destination.content(this)
        }
    }
}

private fun shouldRenderDestination(
    currentOrientation: Int,
    destinationOrientation: PageOrientation,
): Boolean {
    return when (destinationOrientation) {
        PageOrientation.PORTRAIT -> currentOrientation == Configuration.ORIENTATION_PORTRAIT
        PageOrientation.LANDSCAPE -> currentOrientation == Configuration.ORIENTATION_LANDSCAPE
        else -> true
    }
}
