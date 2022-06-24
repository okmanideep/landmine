package me.okmanideep.landmine

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.okmanideep.landmine.compass.getNavController
import me.okmanideep.landmine.compass.model.Page
import me.okmanideep.landmine.compass.stack.PageOrientation
import me.okmanideep.landmine.compass.stack.StackNavHost
import me.okmanideep.landmine.compass.tab.TabNavHost

object Tab {
    const val MAIN = "MAIN"
    const val EXPLORE = "EXPLORE"
    const val PROFILE = "PROFILE"
}

object Destination {
    const val HOME = "HOME"
    const val DETAIL = "DETAIL"
    const val WATCH = "WATCH"
}

const val AUTOMATE_CRASH = true

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Navigation() {
    val navController = getNavController()
    Column(modifier = Modifier.fillMaxSize()) {
        TabNavHost(
            modifier = Modifier.weight(1.0f),
            navController = navController
        ) {
            tab(Tab.MAIN, isDefault = true) {
                TabContent()
            }
            tab(Tab.EXPLORE) {
                TabContent()
            }
            tab(Tab.PROFILE) {
                TabContent()
            }
        }
        BottomNavigation {
            BottomNavigationItem(
                selected = navController.state.entries.lastOrNull()?.pageType == Tab.MAIN,
                onClick = { navController.navigateTo(Tab.MAIN) },
                icon = {
                    Icon(imageVector = Icons.Filled.Home, contentDescription = Tab.MAIN)
                }
            )
            BottomNavigationItem(
                selected = navController.state.entries.lastOrNull()?.pageType == Tab.EXPLORE,
                onClick = { navController.navigateTo(Tab.EXPLORE) },
                icon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = Tab.EXPLORE)
                }
            )
            BottomNavigationItem(
                selected = navController.state.entries.lastOrNull()?.pageType == Tab.PROFILE,
                onClick = { navController.navigateTo(Tab.PROFILE) },
                icon = {
                    Icon(imageVector = Icons.Filled.Person, contentDescription = Tab.PROFILE)
                }
            )
        }
    }
}

@Composable
fun TabContent() {
    val navController = getNavController()
    StackNavHost(
        navController = navController,
        startDestination = Page(Destination.HOME)
    ) {
        page(
            type = Destination.HOME,
            pageOrientation = PageOrientation.PORTRAIT
        ) {
            Home(
                onDetailClick = { navController.navigateTo(Destination.DETAIL) }
            )
        }

        page(
            type = Destination.DETAIL,
            pageOrientation = PageOrientation.PORTRAIT,
            isTransparent = true,
            enterTransition = EnterTransition.None,
            exitTransition = ExitTransition.None
        ) {
            Detail(
                onClose = { navController.goBack() },
                onContentClick = { navController.navigateTo(Destination.WATCH) }
            )
        }

        page(
            type = Destination.WATCH,
            pageOrientation = PageOrientation.CUSTOM,
            enterTransition = EnterTransition.None,
            exitTransition = ExitTransition.None
        ) {
            Watch(
                onClick =  {navController.goBack()}
            )
        }
    }
}