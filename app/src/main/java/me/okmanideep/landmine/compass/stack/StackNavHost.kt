package me.okmanideep.landmine.compass.stack

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import me.okmanideep.landmine.compass.NavController
import me.okmanideep.landmine.compass.model.Page

/**
 * Provides a place in the Compose hierarchy for self contained stack based navigation to occur.
 *
 * Once this is called, any Page within the given [StackGraphBuilder] can be navigated using the
 * provided [navController].
 *
 * @param navController the navController for this host
 * @param startDestination the page you want to load first
 * @param modifier the modifier to be applied to the container Box.
 * @param builder the builder used to construct the graph
 *
 * @sample me.okmanideep.landmine.compass_sample.stacksample.StackSample
 */
@Composable
fun StackNavHost(
    navController: NavController,
    startDestination: Page,
    modifier: Modifier = Modifier,
    builder: StackGraphBuilder.() -> Unit
) = StackNavHost(
    navController = navController,
    initialStack = listOf(startDestination),
    modifier,
    builder = builder
)

/**
 * Provides a place in the Compose hierarchy for self contained navigation to occur.
 *
 * Once this is called, any Page within the given [StackGraphBuilder] can be navigated using the
 * provided [navController].
 *
 * @param navController the navController for this host
 * @param initialStack the pages you want to load first
 * @param modifier The modifier to be applied to the container Box.
 * @param builder the builder used to construct the graph
 *
 * @sample me.okmanideep.landmine.compass_sample.stacksample.StackSample
 */
@Composable
fun StackNavHost(
    navController: NavController,
    initialStack: List<Page>,
    modifier: Modifier = Modifier,
    builder: StackGraphBuilder.() -> Unit
) {
    // Building the graph
    val graph = remember(builder) { StackGraphBuilder().apply(builder).build() }
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "StackNavHost requires a ViewModelStoreOwner to be provided via LocalViewModelStoreOwner"
    }

    // Getting stackViewModel
    val stackNavViewModel = remember(graph, navController, viewModelStoreOwner) {
        stackNavViewModel(
            initialStack = initialStack,
            graph = graph,
            navController = navController,
            viewModelStoreOwner = viewModelStoreOwner
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    BackHandler(enabled = navController.canGoBack, onBack = { stackNavViewModel.goBack() })
    /**
     * BackHandler needs to above the below DisposableEffect
     *
     * This affects the order of lifecycle observers. The order of observers should be
     * 1. BackHandler
     * 2. StackNavViewModel (propagates lifecycle changes to children)
     * for the lifecycle events to propagate to parent's back handler first
     * before the child's
     *
     * Reversing this order will fail BackHandlingTest.back_handling_when_user_goes_out_and_comes_back_to_the_app
     */
    DisposableEffect(stackNavViewModel, navController, lifecycleOwner) {
        // Attaching navController with navViewModel
        navController.attachNavHostController(stackNavViewModel)
        lifecycleOwner.lifecycle.addObserver(stackNavViewModel)

        onDispose {
            navController.detachNavHostController(stackNavViewModel)
            lifecycleOwner.lifecycle.removeObserver(stackNavViewModel)
            // to clear or unset any UI state
            stackNavViewModel.onDispose()
        }
    }

    PageStack(
        backStack = navController.state,
        graph = graph,
        modifier = modifier,
        onTransitionFinished = {
            stackNavViewModel.onTransitionFinished()
        }
    )
}
