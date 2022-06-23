package me.okmanideep.landmine

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import me.okmanideep.landmine.compass.getNavController
import me.okmanideep.landmine.compass.model.Page
import me.okmanideep.landmine.compass.stack.PageOrientation
import me.okmanideep.landmine.compass.stack.StackNavHost

object Destination {
    const val HOME = "HOME"
    const val DETAIL = "DETAIL"
    const val WATCH = "WATCH"
}

const val AUTOMATE_CRASH = true

@Composable
fun Navigation() {
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