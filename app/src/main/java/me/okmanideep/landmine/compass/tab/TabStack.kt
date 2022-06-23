package me.okmanideep.landmine.compass.tab

import androidx.compose.animation.*
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import me.okmanideep.landmine.compass.model.NavBackStack

/**
 * [TabStack] is where we render the current [NavBackStack].
 *
 *
 * @param backStack navigation stack to render
 * @param graph The navigation complete navigation graph
 * @param modifier Modifier for the container Box
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
internal fun TabStack(
    backStack: NavBackStack,
    graph: TabGraph,
    modifier: Modifier = Modifier,
    onTransitionFinished: () -> Unit = {}
) {
    // don't render anything if the stack doesn't belong to the graph
    if (!backStack.entries.all { graph.hasPageType(it.pageType) }) return

    val transition = updateTransition(backStack, label = "Tab Stack Transition")
    if (transition.currentState == transition.targetState) {
        onTransitionFinished()
    }
    val saveableStateHolder = rememberSaveableStateHolder()

    Box(modifier = modifier) {
        transition.AnimatedContent(
            transitionSpec = {
                val prevTab = initialState.entries.lastOrNull()?.pageType
                val nextTab = targetState.entries.lastOrNull()?.pageType

                val tabs = graph.tabs
                val prevTabIndex = tabs.indexOf(prevTab)
                val nextTabIndex = tabs.indexOf(nextTab)

                when {
                    nextTabIndex > prevTabIndex -> {
                        graph.endTransition
                    }
                    nextTabIndex < prevTabIndex -> {
                        graph.startTransition
                    }
                    else -> {
                        EnterTransition.None with ExitTransition.None
                    }
                }
            },
            contentKey = { targetState -> targetState.entries.lastOrNull()?.id },
        ) {
            val entry = it.entries.lastOrNull()
            entry?.Render(saveableStateHolder, graph)
        }
    }
}
