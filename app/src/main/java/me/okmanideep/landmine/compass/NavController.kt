package me.okmanideep.landmine.compass

import android.app.Application
import android.os.Parcelable
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import me.okmanideep.landmine.compass.core.rememberRootNavController
import me.okmanideep.landmine.compass.model.NavBackStack

/**
 * Provides the [NavController] for the current scope
 *
 * @return NavController
 */
@Composable
fun getNavController(): NavController {
    val app = LocalContext.current.applicationContext as Application
    return LocalNavController.current
        ?: rememberRootNavController {
            NavController(app, null)
        }
}

/**
 * An interface to control the NavHost. Usually implemented in the NavHost's ViewModel.
 *
 * See usage :
 * 1. [StackNavViewModel][me.okmanideep.landmine.compass.stack.StackNavViewModel]
 * 2. [TabNavViewModel][me.okmanideep.landmine.compass.tab.TabNavViewModel]
 */
interface NavHostController {
    /**
     * To listen changes in the NavBackStack.
     *
     * @param listener
     */
    fun setStateChangedListener(listener: (NavBackStack) -> Unit)

    /**
     * To check if the `NavHost` can navigate to the `pageType`
     *
     * @param pageType
     * @return `true` if yes, `false` otherwise.
     */
    fun canNavigateTo(pageType: String): Boolean

    /**
     * To navigate to a new destination.
     *
     * @param pageType the pageType of the destination.
     * @param args the arguments that you need to pass to the NavEntry.
     * @param replace Weather you want to replace the current item in the stack with this pageType.
     * If true, it'll will remove the current NavEntry from the stack and replace the given pageType
     * in its position.
     */
    fun navigateTo(pageType: String, args: Parcelable? = null, replace: Boolean = false)

    /**
     * To simply check if there's any NavEntry to pop
     * @return `true` if yes, `false` otherwise.
     */
    fun canGoBack(): Boolean

    /**
     * To navigate back to the previous entry
     * @return `true` if navigated, `false` otherwise
     */
    fun goBack(): Boolean

    /**
     * To pop up to given pageType. If page doesn't exist, this method will clear (reset) the stack.
     */
    fun popUpTo(pageType: String): Boolean
}

/**
 * [NavController] manages app navigation within a `NavHost`.
 *
 * Apps will generally obtain a controller by using [getNavController].
 * it'll NOT create a new [NavController] when you call it within a [NavEntry] scope.
 * Rather, it'll get the [NavController] via the [LocalNavController] compositionLocal with
 * [parent][NavController.parent] pointing to the [NavController] of the parent `NavHost`.
 * Allows you to navigate hierarchically
 *
 * @property app the applicationContext
 * @property parent the parent [NavController]. This will be null for the root NavHost's controller.
 * nested NavHosts will have its entry's parent
 */
class NavController(
    val app: Application,
    private val parent: NavController?
) {
    // To watch nav state and pass navigate calls to host's viewModel
    private var hostController: NavHostController? = null

    // Current navStack
    var state by mutableStateOf(NavBackStack())
        private set
    var canGoBack by mutableStateOf(false)
        private set

    /**
     * To attach [NavController] with a NavHost (or more specifically, NavHost's ViewModel)
     *
     * @param navHostController NavHost's controller (usually a ViewModel).
     * @throws IllegalStateException if already a host attached to this controller. You need to call
     * [detachNavHostController] first to detach the host controller.
     */
    fun attachNavHostController(navHostController: NavHostController) {
        val currentNavHostController = hostController
        if (currentNavHostController != null) {
            detachNavHostController(currentNavHostController)
        }

        hostController = navHostController
        navHostController.setStateChangedListener { state ->
            this.state = state
            this.canGoBack = navHostController.canGoBack()
        }
    }

    /**
     * To detach NavHost's controller (usually a ViewModel)
     */
    fun detachNavHostController(navHostController: NavHostController) {
        if (navHostController == hostController) hostController = null
    }

    /**
     * To navigate to a `pageType`/destination.
     *
     * @param pageType the `pageType` of the destination
     * @param args the arguments that you need to pass to the NavEntry
     * @param replace Whether you want to replace the current item in the stack with this pageType.
     * If true, it'll will remove the current NavEntry from the stack and replace the given pageType
     * in its position.
     * @return `true` if navigated
     * @throws IllegalArgumentException if [pageType] doesn't exist or unreachable from current graph
     */
    fun navigateTo(pageType: String, args: Parcelable? = null, replace: Boolean = false): Boolean {
        hostController?.let { hostController ->
            if (hostController.canNavigateTo(pageType)) {
                hostController.navigateTo(pageType, args, replace)
                return true
            }
        }

        val isNavigated = parent?.navigateTo(pageType, args, replace) ?: false
        if (isNavigated) {
            return true
        } else {
            throw IllegalArgumentException(
                """
                $pageType doesn't exist or unreachable from current graph.
                """.trimIndent()
            )
        }
    }

    /**
     * To go back to the previous screen (or more specifically previous `NavEntry`).
     */
    fun goBack() {
        if (hostController?.goBack() != true) {
            parent?.goBack()
        }
    }

    /**
     * To pop up to given pageType.
     * If page doesn't exist, this method will clear (reset) the stack and add the given
     * pageType to the stack.
     */
    fun popUpTo(pageType: String): Boolean {
        if (hostController?.popUpTo(pageType) == true) return true
        return parent?.popUpTo(pageType)
            ?: error("pageType '$pageType' doesn't exist in the graph")
    }
}

/**
 * To hold [NavController] of a [NavEntry]
 */
internal val LocalNavController = compositionLocalOf<NavController?> { null }
