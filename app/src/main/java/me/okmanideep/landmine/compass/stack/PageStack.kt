package me.okmanideep.landmine.compass.stack

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import me.okmanideep.landmine.compass.NavEntry
import me.okmanideep.landmine.compass.model.NavBackStack

/**
 * [PageStack] is where we render the current [NavBackStack].
 * It has the ability to render the previous entry (n-2th) if the topEntry (n-1th) entry
 * has `isTransparent` enabled
 *
 * TODO: Initial animation looks little weird. @sifar to investigate root cause if its not expected.
 *  (initial page starts to render from top-left corner to bottom-right corner)
 *
 * @param backStack navigation stack to render
 * @param graph The navigation complete navigation graph
 * @param modifier Modifier for the container Box
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun PageStack(
    backStack: NavBackStack,
    graph: StackGraph,
    modifier: Modifier = Modifier,
    onTransitionFinished: () -> Unit
) {
    val saveableStateHolder = rememberSaveableStateHolder()

    // don't render anything if the stack doesn't belong to the graph
    if (!backStack.entries.all { graph.hasPageType(it.pageType) }) return

    val visibleInTargetState = backStack.visibleEntries(graph)
    val layersToDraw = remember { mutableStateListOf(*visibleInTargetState.toTypedArray()) }
    val transition = updateTransition(backStack, label = "Page Stack Transition")
    if (transition.currentState == transition.targetState) {
        layersToDraw.removeAll { layer -> visibleInTargetState.none { it.id == layer.id } }
        SideEffect { onTransitionFinished() }
    } else {
        visibleInTargetState.forEach { addToLayers(layersToDraw, it) }
    }

    Box(modifier = modifier) {
        layersToDraw.forEach { layer ->
            key(layer.id) {
                val destination = graph.destinationFor(layer.pageType)
                /**
                 * Here, we've two types of rendering.
                 * 1. We'll animate enter, exit, pullFront and pushBack.
                 * 2. We'll only animate enter and exit.
                 */
                if (destination.visibilityTransitions != null) {
                    // Let's animate enter, exit, pullFront and pushBack
                    transition.AnimatedVisibility(
                        visible = { it.visibleEntries(graph).isPresent(layer) },
                        enter = if (!transition.currentState.isPresent(layer)) {
                            // push
                            destination.enterTransition
                        } else {
                            // pop
                            destination.visibilityTransitions.pullFrontTransition
                        },
                        exit = if (!transition.targetState.isPresent(layer)) {
                            // push
                            destination.exitTransition
                        } else {
                            // pop
                            destination.visibilityTransitions.pushBackTransition
                        }
                    ) {
                        layer.Render(saveableStateHolder, graph)
                    }
                } else {
                    // Just animate enter and exit.
                    transition.AnimatedVisibility(
                        visible = { it.isPresent(layer) },
                        enter = destination.enterTransition,
                        exit = destination.exitTransition
                    ) {
                        layer.Render(saveableStateHolder, graph)
                    }
                }
            }
        }
    }
}

private fun NavBackStack.isPresent(entry: NavEntry): Boolean {
    return this.entries.isPresent(entry)
}

private fun List<NavEntry>.isPresent(entry: NavEntry): Boolean {
    return this.any { entry.id == it.id }
}

private fun NavBackStack.visibleEntries(graph: StackGraph): List<NavEntry> {
    if (entries.isEmpty()) return emptyList()

    val firstVisibleIndex = if (graph.destinationFor(entries.last().pageType).isTransparent) {
        require(entries.size >= 2) { "first page can't be transparent" }
        entries.size - 2
    } else {
        entries.size - 1
    }

    return entries.subList(firstVisibleIndex, entries.size)
}

private fun addToLayers(
    layers: SnapshotStateList<NavEntry>,
    item: NavEntry,
) {
    val index = layers.binarySearch { layer ->
        layer.ordinal - item.ordinal
    }
    if (index >= 0) return // nothing to do, item already in layers

    // index = -insertionPoint -1; @see List<T>.binarySearch() doc
    val insertionPoint = -index - 1
    layers.add(insertionPoint, item)
}
