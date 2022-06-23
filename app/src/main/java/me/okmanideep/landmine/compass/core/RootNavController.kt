package me.okmanideep.landmine.compass.core

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import me.okmanideep.landmine.compass.NavController

/**
 * To hold root navController during activity recreation.
 *
 * @property navController root NavController you want store.
 */
private class RootNavControllerViewModel constructor(
    val navController: NavController
) : ViewModel()

/**
 * To remember root NavController during activity recreation.
 * TODO: Sifar to write tests for this method. Planned to do after page orientation support.
 *
 * @param createNewNavController lambda to create new navController.
 * @return [NavController] root NavController
 */
@Composable
internal fun rememberRootNavController(
    createNewNavController: () -> NavController
): NavController {
    val factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return RootNavControllerViewModel(createNewNavController()) as T
        }
    }
    val viewModelStoreOwner =
        LocalViewModelStoreOwner.current ?: error("No viewModelStoreOwner found")
    return ViewModelProvider(
        viewModelStoreOwner,
        factory
    ).get(RootNavControllerViewModel::class.java).navController
}
