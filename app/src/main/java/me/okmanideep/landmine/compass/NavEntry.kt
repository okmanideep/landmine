package me.okmanideep.landmine.compass

import android.app.Application
import android.os.Bundle
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import java.util.*

/**
 * Representation of an entry in the back stack of a [NavController].
 * The Lifecycle, ViewModelStore, and SavedStateRegistry provided via this object are valid for the
 * lifetime of this destination on the back stack: when this destination is popped off the
 * back stack, the lifecycle will be destroyed, state will no longer be saved, and ViewModels
 * will be cleared.
 *
 * @property app the ApplicationContext that'll be used inside SavedStateViewModelFactory
 * @property id unique id of the entry
 * @property ordinal indicates the z-index of the entry, lesser ordinal on the bottom
 * @property pageType the unique `pageType`
 * @property args the arguments you want to pass to `NavEntry` scope
 * @property parentNavController if the navEntry is inside another `NavHost`, this will point to its
 * [NavController]
 * @property viewModelStore all viewModels created inside the page will be stored in this store.
 */
class NavEntry internal constructor(
    private val app: Application,
    val id: String = UUID.randomUUID().toString(),
    internal val ordinal: Int = 0,
    val pageType: String,
    val args: Parcelable?,
    private val parentNavController: NavController,
    private val viewModelStore: ViewModelStore = ViewModelStore()
) : LifecycleOwner,
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    SavedStateRegistryOwner {
    private val savedState: Bundle? = null

    companion object {
        internal const val KEY_ARGS = "me.okmanideep.landmine.compass:nav-entry-args"
    }

    private val lifecycleRegistry = LifecycleRegistry(this)

    val controller by lazy { NavController(app, parentNavController) }
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val defaultFactory by lazy {
        SavedStateViewModelFactory(
            app,
            this,
            args?.putInBundle()
        )
    }

    override val savedStateRegistry = savedStateRegistryController.savedStateRegistry

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    internal fun setLifecycleState(state: Lifecycle.State) {
        if (lifecycleRegistry.currentState == Lifecycle.State.INITIALIZED) {
            // Perform the restore just when moving from the INITIALIZED state
            savedStateRegistryController.performRestore(savedState)
        }
        lifecycleRegistry.currentState = state
    }

    override fun getViewModelStore(): ViewModelStore {
        return viewModelStore
    }

    override fun equals(other: Any?): Boolean {
        return (other is NavEntry && other.id == id)
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return "NavEntry:$pageType/$id"
    }

    override fun getDefaultViewModelProviderFactory(): ViewModelProvider.Factory {
        return defaultFactory
    }
}

/**
 * To provide context for the page @Composable, so that from the content `@Composable`,
 * pages will be able to get [NavController], [ViewModelStoreOwner] and [LifecycleOwner]
 *
 * @param content
 */
@Composable
fun NavEntry.LocalOwnersProvider(
    saveableStateHolder: SaveableStateHolder,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalNavController provides this.controller,
        LocalViewModelStoreOwner provides this,
        LocalLifecycleOwner provides this,
        LocalSavedStateRegistryOwner provides this,
    ) {
        saveableStateHolder.SaveableStateProvider(key = id, content)
    }
}

/**
 * To get Parcelable you passed through [NavEntry.args] inside the [ViewModel]
 *
 * @param T parcelable type
 * @return if exists, [T], null otherwise
 */
fun <T : Parcelable> SavedStateHandle.getEntryArgs(): T? {
    return get<T>(NavEntry.KEY_ARGS)
}

/**
 * To put the [Parcelable] inside a Bundle with the key [NavEntry.KEY_ARGS].
 * @return the bundle
 */
fun Parcelable.putInBundle(): Bundle {
    return Bundle().apply {
        putParcelable(NavEntry.KEY_ARGS, this@putInBundle)
    }
}

fun LifecycleOwner.isDestroyed(): Boolean {
    return lifecycle.currentState == Lifecycle.State.DESTROYED
}

fun LifecycleOwner.isAtLeastStarted(): Boolean {
    return lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}

fun LifecycleOwner.isStarted(): Boolean {
    return lifecycle.currentState == Lifecycle.State.STARTED
}

fun LifecycleOwner.isResumed(): Boolean {
    return lifecycle.currentState == Lifecycle.State.RESUMED
}
