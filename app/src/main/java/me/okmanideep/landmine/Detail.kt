package me.okmanideep.landmine

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun Detail(
    onClose: () -> Unit,
    onContentClick: () -> Unit,
) {
    val browseSheetState = rememberBrowseSheetState(initialValue = BrowseSheetValue.INITIALIZED)
    val coroutineScope = rememberCoroutineScope()

    fun dismiss() {
            coroutineScope.launch {
                browseSheetState.animateTo(BrowseSheetValue.DISMISSED)
            }
        }

    LaunchedEffect(browseSheetState.currentValue) {
        when (browseSheetState.currentValue) {
            BrowseSheetValue.DISMISSED -> onClose()
            BrowseSheetValue.INITIALIZED -> browseSheetState.animateTo(BrowseSheetValue.COLLAPSED, anim = springSpec(tension = 100f, friction = 20f))
            else -> {}
        }
    }

    if (AUTOMATE_CRASH) {
        LaunchedEffect(Unit) {
            delay(500)
            onContentClick()
        }
    }

    BackHandler {
        dismiss()
    }

    BrowseSheetLayout(
        state = browseSheetState,
        header = { Header() },
        title = "Detail",
        onCloseClick = { dismiss() }
    ) {
        Content(
            onClick = onContentClick
        )
    }
}

@Composable
fun Header() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colors.onBackground.copy(0.8f))
        )
    }
}

@Composable
fun Content(onClick: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(100, key = {it}) {
            ButtonWithoutElevation(
                onClick = onClick,
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth()
                    .background(Color(0xFF373C4D))
                    .clip(RoundedCornerShape(4.dp)),
            ) {
                Text(
                    "Click Me",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            }
        }
    }
}

fun <T> springSpec(tension: Float, friction: Float, visibilityThreshold: T? = null): SpringSpec<T> {
    return spring(
        stiffness = tension,
        dampingRatio = friction / (2 * sqrt(tension)),
        visibilityThreshold = visibilityThreshold
    )
}