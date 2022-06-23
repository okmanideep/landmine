package me.okmanideep.landmine.compass.tab

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import me.okmanideep.landmine.compass.NavController

/**
 * Provides a place in the Compose hierarchy for self contained Tab/Bottom navigation to occur.
 *
 * Once this is called, any Page within the given [builder] can be navigated using the
 * provided [navController].
 *
 * @param modifier The modifier to be applied to the container Box.
 * @param navController the navController for this host
 * @param startTransition tab's enter animation
 * @param endTransition tab's exit animation
 * @param builder the builder used to construct the graph
 *
 * @sample me.okmanideep.landmine.compass_sample.instagramsample.InstagramSample()
 */
@ExperimentalAnimationApi
@Composable
fun TabNavHost(
    modifier: Modifier = Modifier,
    navController: NavController,
    startTransition: ContentTransform = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(initialAlpha = 0.8f)
        with slideOutHorizontally(targetOffsetX = { it }) + fadeOut(targetAlpha = 0.8f),
    endTransition: ContentTransform = slideInHorizontally(initialOffsetX = { it }) + fadeIn(initialAlpha = 0.8f)
        with slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(targetAlpha = 0.8f),
    builder: TabGraphBuilder.() -> Unit
) {
    // Building the graph
    val graph = remember(builder) { TabGraphBuilder().apply(builder).build(startTransition, endTransition) }
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "TabNavHost requires a ViewModelStoreOwner to be provided via a LocalViewModelStoreOwner"
    }

    // Getting TabNavViewModel
    val tabNavViewModel = remember(graph, navController, viewModelStoreOwner) {
        tabNavViewModel(graph, navController, viewModelStoreOwner)
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    BackHandler(enabled = navController.canGoBack, onBack = { tabNavViewModel.goBack() })
    /**
     * BackHandler needs to above the below DisposableEffect
     *
     * This affects the order of lifecycle observers. The order of observers should be
     * 1. BackHandler
     * 2. TabNavViewModel (propagates lifecycle changes to children)
     * for the lifecycle events to propagate to parent's back handler first
     * before the child's
     *
     * Reversing this order will fail BackHandlingTest.back_handling_when_user_goes_out_and_comes_back_to_the_app
     */
    DisposableEffect(tabNavViewModel, navController, lifecycleOwner) {
        // Attaching navController with navViewModel
        navController.attachNavHostController(tabNavViewModel)
        lifecycleOwner.lifecycle.addObserver(tabNavViewModel)

        onDispose {
            navController.detachNavHostController(tabNavViewModel)
            lifecycleOwner.lifecycle.removeObserver(tabNavViewModel)
            tabNavViewModel.onDispose()
        }
    }

    TabStack(
        backStack = navController.state,
        graph = graph,
        modifier = modifier,
        onTransitionFinished = {
            tabNavViewModel.onTransitionFinished()
        }
    )
}
