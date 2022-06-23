package me.okmanideep.landmine.compass.stack

import android.os.Parcelable
import android.util.Log
import androidx.lifecycle.*
import me.okmanideep.landmine.compass.NavController
import me.okmanideep.landmine.compass.NavEntry
import me.okmanideep.landmine.compass.NavHostController
import me.okmanideep.landmine.compass.core.StackInternal
import me.okmanideep.landmine.compass.model.NavBackStack
import me.okmanideep.landmine.compass.model.Page

/**
 * StackNavViewModel is the brain of the StackNavHost.
 * It remembers the [nav entries][stack] and their states.
 * Since this is a `ViewModel` which is kept in a retained `ViewModelStore` by the `StackNavHost`,
 * this is retained across activity configuration changes.
 * [StackNavViewModel] is responsible to [navigate][navigateTo] between pages, update lifecycle of
 * the entries in the [stack].
 *
 * @property graph The navigation graph
 * @property navController The nav controller
 * @constructor
 *
 * @param initialStack the initial stack that you want to render without calling [navigateTo]
 */
internal class StackNavViewModel(
    initialStack: List<Page>,
    var graph: StackGraph,
    private val navController: NavController
) : ViewModel(), NavHostController, LifecycleEventObserver {
    private var hostLifecycleState: Lifecycle.State = Lifecycle.State.RESUMED
    private val stack = StackInternal<NavEntry>()
    private var listener: ((NavBackStack) -> Unit)? = null
    private var currentOrdinal: Int = 0
    private val poppedEntries = mutableSetOf<NavEntry>()
    private var isTransitionInProgress = false
    private var logTag: String = "Default"

    init {
        // Rendering initial stack
        for (page in initialStack) {
            stack.add(getEntry(page.type, page.args))
            logTag = page.type
            updateLifecycles()
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        Log.v("StackNavHost", "$logTag: lifecycle event - $event")
        hostLifecycleState = source.lifecycle.currentState

        updateLifecycles()
        onStateUpdated()
    }

    override fun setStateChangedListener(listener: (NavBackStack) -> Unit) {
        this.listener = listener
        onStateUpdated()
    }

    override fun canNavigateTo(pageType: String): Boolean {
        return graph.hasPageType(pageType)
    }

    override fun navigateTo(pageType: String, args: Parcelable?, replace: Boolean) {
        Log.v("StackNavHost", "navigateTo($pageType,replace=$replace)")
        val entry = getEntry(pageType, args)

        if (replace) {
            val poppedEntry = stack.replace(entry)
            onEntryRemoved(poppedEntry)
        } else {
            stack.add(entry)
        }

        if (isSingleEntryPage(pageType)) {
            removePrevPagesOfType(pageType = pageType)
        }

        isTransitionInProgress = true
        updateLifecycles()
        onStateUpdated()
    }

    override fun canGoBack(): Boolean {
        return stack.canPop()
    }

    override fun goBack(): Boolean {
        Log.v("StackNavHost", "goBack()")
        val canGoBack = stack.canPop()
        if (canGoBack) {
            val poppedEntry = stack.pop()
            onEntryRemoved(poppedEntry)
            isTransitionInProgress = true
            updateLifecycles()
            onStateUpdated()
        }

        return canGoBack
    }

    /**
     * Invoked after finishing the page transition/animation.
     * To clean popped entry's resources.
     */
    fun onTransitionFinished() {
        Log.v("StackNavHost", "$logTag - onTransitionFinished()")
        if (!isTransitionInProgress) return

        poppedEntries.forEach { poppedEntry ->
            Log.v("StackNavHost", "Clearing VM for ${poppedEntry.pageType}")
            poppedEntry.viewModelStore.clear()
        }
        poppedEntries.clear()
        unsetTransitionInProgress()
    }

    fun onDispose() {
        // reset any UI state like animation state etc
        unsetTransitionInProgress()
    }

    private fun unsetTransitionInProgress() {
        if (isTransitionInProgress) {
            isTransitionInProgress = false
            updateLifecycles()
        }
    }

    private fun isPresent(pageType: String): Boolean {
        return stack.toList().any { it.pageType == pageType }
    }

    override fun popUpTo(pageType: String): Boolean {
        // Checking if current graph has the given pageType
        if (graph.hasPageType(pageType)) {

            // Checking if the current stack has given page type
            if (isPresent(pageType)) {
                // Popping up to given pageType
                while (stack.peek()?.pageType != pageType) {
                    val poppedEntry = stack.pop()
                    onEntryRemoved(poppedEntry)
                }
                // Update state
                isTransitionInProgress = true
                updateLifecycles()
                onStateUpdated()
            } else {
                // Reset stack
                resetStack()
                // Navigating to given page
                navigateTo(pageType)
            }

            // Handled popUpTo
            return true
        } else {
            // this host doesn't know anything about the pageType, delegating the call to parent.
            return false
        }
    }

    private fun resetStack() {
        val tempStack = stack.toList()
        stack.clear()
        tempStack.forEach {
            onEntryRemoved(it)
        }
    }

    private fun onEntryRemoved(poppedEntry: NavEntry) {
        poppedEntry.setLifecycleState(capToHostLifecycle(Lifecycle.State.STARTED))
        poppedEntries.add(poppedEntry) // storing for cleaning later. see [cleanPoppedEntries]
    }

    override fun onCleared() {
        clearStack()

        super.onCleared()
    }

    /**
     * This will clear [NavEntry.viewModelStore] and set the lifecycle to
     * [Lifecycle.State.DESTROYED] of all entries. It'll also remove all entries from the [stack]
     * at the end.
     */
    private fun clearStack() {
        for (entry in stack.toList()) {
            entry.viewModelStore.clear()
            entry.setLifecycleState(Lifecycle.State.DESTROYED)
        }

        stack.clear()
    }

    /**
     * Invoked in two scenarios
     * 1. when a [NavEntry] added or popped from the [stack]
     * 2. when a [NavEntry]'s lifecycle state updated in the [stack]
     */
    private fun onStateUpdated() {
        listener?.invoke(NavBackStack(stack.toList()))
        Log.v("StackNavHost", "Stack updated")
    }

    /**
     * To update lifecycle state of each entry
     *
     * Sets topEntry state to [Lifecycle.State.RESUMED] and other
     * entry's state to [Lifecycle.State.STARTED]
     */
    private fun updateLifecycles() {
        val entries = stack.toList()
        for ((index, entry) in entries.withIndex()) {
            val state = if (index == entries.lastIndex && !isTransitionInProgress) {
                // topEntry, after transition is finished
                Lifecycle.State.RESUMED
            } else {
                Lifecycle.State.STARTED
            }

            entry.setLifecycleState(capToHostLifecycle(state))
        }
    }

    private fun capToHostLifecycle(state: Lifecycle.State): Lifecycle.State {
        // If the activity is DESTROYED, we'll take entry's state
        if (hostLifecycleState == Lifecycle.State.DESTROYED) return state
        return if (state.ordinal > hostLifecycleState.ordinal) {
            hostLifecycleState
        } else {
            state
        }
    }

    private fun isSingleEntryPage(pageType: String): Boolean {
        return graph.destinationFor(pageType).isSingleEntry
    }

    /**
     * To remove pages of given [pageType].
     * NOTE: This method won't remove the pageType if its the first entry in the stack.
     */
    private fun removePrevPagesOfType(pageType: String) {
        do {
            val removedEntry = stack.remove { index, entry ->
                /**
                 * Entry shouldn't be the last item. Because we call this method after adding the
                 * new entry to the stack. If we remove the last item, we'll remove the new entry
                 * as well.
                 */
                index != stack.lastIndex && entry.pageType == pageType
            }
            removedEntry?.let { onEntryRemoved(it) }
        } while (removedEntry != null)
    }

    private fun getEntry(pageType: String, args: Parcelable?): NavEntry {
        return NavEntry(
            app = navController.app,
            ordinal = currentOrdinal++,
            pageType = pageType,
            args = args,
            parentNavController = navController
        )
    }
}

internal fun stackNavViewModel(
    initialStack: List<Page>,
    graph: StackGraph,
    navController: NavController,
    viewModelStoreOwner: ViewModelStoreOwner
): StackNavViewModel {
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return StackNavViewModel(initialStack, graph, navController) as T
        }
    }

    return ViewModelProvider(viewModelStoreOwner, factory).get(graph.key(), StackNavViewModel::class.java).apply {
        this.graph = graph
    }
}
