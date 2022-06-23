package me.okmanideep.landmine.compass.tab

import android.os.Parcelable
import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.lifecycle.*
import me.okmanideep.landmine.compass.NavController
import me.okmanideep.landmine.compass.NavEntry
import me.okmanideep.landmine.compass.NavHostController
import me.okmanideep.landmine.compass.core.StackInternal
import me.okmanideep.landmine.compass.model.NavBackStack

/**
 *
 * [TabNavViewModel] is the brain of the [TabNavHost].
 * It remembers the [nav entries][stack] and their states.
 * Since this `ViewModel` is kept in a retained `ViewModelStore` by the `TabNavHost`,
 * it is retained across activity configuration changes.
 * [TabNavViewModel] is responsible to [navigate][navigateTo] between tabs, update lifecycle of
 * the entries in the [stack].
 *
 * @property graph The navigation graph
 * @property navController The nav controller
 * @constructor
 */
@OptIn(ExperimentalAnimationApi::class)
internal class TabNavViewModel constructor(
    var graph: TabGraph,
    private val navController: NavController,
) : ViewModel(), NavHostController, LifecycleEventObserver {
    // Lifecycle of the TabNavHost
    private var hostLifecycleState: Lifecycle.State = Lifecycle.State.RESUMED
    private val stack = StackInternal<NavEntry>()
    private var listener: ((NavBackStack) -> Unit)? = null

    // To hold tabId and its NavEntry
    private val tabToEntryMap: HashMap<String, NavEntry> = HashMap()
    private val defaultEntry: NavEntry = getEntry(
        type = graph.defaultTab,
        args = graph.defaultTabArgs
    )

    private var isTransitionInProgress = false

    init {
        stack.add(defaultEntry)
        updateState()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        Log.d("TabNavHost", "$defaultEntry.pageType: lifecycle event - $event")
        hostLifecycleState = source.lifecycle.currentState

        updateState()
    }

    override fun setStateChangedListener(listener: (NavBackStack) -> Unit) {
        this.listener = listener
        onStateUpdated()
    }

    override fun canNavigateTo(pageType: String): Boolean {
        return graph.hasPageType(pageType)
    }

    /**
     * To navigate to the given pageType.
     * If the pageType exists in the graph, it'll simply bring the [NavEntry] to [front][StackInternal.bringToFront]
     * otherwise creates a new [NavEntry]
     *
     * See [NavHostController.navigateTo] for more information
     */
    override fun navigateTo(pageType: String, args: Parcelable?, replace: Boolean) {
        if (pageType == graph.defaultTab) {
            stack.popUpTo { it.pageType == graph.defaultTab }
            isTransitionInProgress = true
            updateState()
            return
        }

        val entry = getEntry(pageType, args)
        if (stack.contains(entry)) {
            stack.bringToFront(entry)
        } else {
            stack.add(entry)
        }

        isTransitionInProgress = true
        updateState()
    }

    /**
     * To check if [TabNavHost] has any entry to pop.
     *
     * @return true if stack has at least an entry OR the current entry != defaultTab/firstTab.
     * false otherwise
     *
     * See [NavHostController.canGoBack] for more information
     */
    override fun canGoBack(): Boolean {
        return stack.canPop()
    }

    /**
     * To go back to previous tab OR previous screen
     *
     * @return
     */
    override fun goBack(): Boolean {
        when {
            // If there are more than 2 entries, let's pop
            stack.canPop() -> {
                stack.pop()
            }

            // If there's only one entry and that is not the defaultTab, then we'll
            // replace the currentPage with firstPage (which is defaultTab)
            stack.peek()?.pageType != graph.defaultTab -> {
                stack.replace(defaultEntry)
            }

            // else, we'll tell delegate the call to parent navController
            else -> {
                return false
            }
        }

        isTransitionInProgress = true
        updateState()
        return true
    }

    fun onTransitionFinished() {
        unsetTransitionInProgress()
    }

    fun onDispose() {
        // reset any UI state like animation state etc
        unsetTransitionInProgress()
    }

    private fun unsetTransitionInProgress() {
        if (isTransitionInProgress) {
            isTransitionInProgress = false
            updateState()
        }
    }

    override fun popUpTo(pageType: String): Boolean {
        // Checking if current graph has the given pageType
        if (graph.hasPageType(pageType)) {

            // Checking if the current stack has given page type
            if (isPresent(pageType)) {
                // Popping up to given pageType
                while (stack.peek()?.pageType != pageType) {
                    stack.pop()
                }
                // Update state
                isTransitionInProgress = true
                updateState()
            } else {
                // Pop up to defaultEntry
                while (stack.peek()?.pageType != defaultEntry.pageType) {
                    stack.pop()
                }
                // then add given page on top of defaultEntry
                navigateTo(pageType)
            }

            // Handled popUpTo
            return true
        } else {
            // this host doesn't know anything about the pageType, delegating the call to parent.
            return false
        }
    }

    private fun isPresent(pageType: String): Boolean {
        return stack.toList().any { it.pageType == pageType }
    }

    /**
     * To update the lifecycle state of each tab.
     *
     * Sets current tab's state to [Lifecycle.State.RESUMED] and other
     * tab's state to [Lifecycle.State.STARTED]
     */
    private fun updateState() {
        val currentPageType = stack.peek()?.pageType ?: return

        for (entry in tabToEntryMap.values) {
            val state = if (entry.pageType == currentPageType && !isTransitionInProgress) {
                Lifecycle.State.RESUMED
            } else {
                Lifecycle.State.STARTED
            }
            entry.setLifecycleState(capToHostLifecycle(state))
        }

        onStateUpdated()
    }

    /**
     * Invoked in two scenarios
     * 1. when a [NavEntry] added or popped from the [stack]
     * 2. when a [NavEntry]'s lifecycle state updated in the [stack]
     */
    private fun onStateUpdated() {
        /*
         * If the first tab is not defaultTab, then put defaultTab at the beginning of the stack.
         * Scenario : When user navigates from non-defaultTab to defaultTab, that entry will be the topEntry.
         * When user navigates back, we need to make sure that the user exits through the defaultTab.
         */
        val stackEntries = stack.toList()
        val entries = if (stackEntries.firstOrNull() == defaultEntry) {
            stackEntries
        } else {
            listOf(defaultEntry) + stackEntries
        }

        listener?.invoke(NavBackStack(entries))
    }

    private fun capToHostLifecycle(state: Lifecycle.State): Lifecycle.State {
        return if (state.ordinal > hostLifecycleState.ordinal) {
            hostLifecycleState
        } else {
            state
        }
    }

    /**
     * To get [NavEntry] for the given [type].
     *
     * @param type pageType
     * @param args arguments you want to pass
     * @return If the [map][tabToEntryMap] doesn't have [type], it'll create a new [NavEntry],
     * put that inside [map][tabToEntryMap] and will return the [NavEntry]
     */
    private fun getEntry(type: String, args: Parcelable?): NavEntry {
        return tabToEntryMap.getOrPut(type) {
            NavEntry(
                app = navController.app,
                pageType = type,
                parentNavController = navController,
                args = args,
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
internal fun tabNavViewModel(
    graph: TabGraph,
    navController: NavController,
    viewModelStoreOwner: ViewModelStoreOwner
): TabNavViewModel {
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return TabNavViewModel(graph, navController) as T
        }
    }

    return ViewModelProvider(viewModelStoreOwner, factory).get(graph.key(), TabNavViewModel::class.java).apply {
        this.graph = graph
    }
}
