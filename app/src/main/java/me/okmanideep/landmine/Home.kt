package me.okmanideep.landmine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun Home(
    onDetailClick: () -> Unit
) {
    Surface {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(100, key = {it}) {
                    ButtonWithoutElevation(
                        onClick = onDetailClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .height(144.dp)
                            .clip(shape = RoundedCornerShape(4.dp))
                            .background(Color(0xFF373C4D))
                    ){}
                }
            }

            ButtonWithoutElevation(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .fillMaxWidth()
                    .height(48.dp),
                onClick = onDetailClick
            ) {
                Text(if (AUTOMATE_CRASH) "REPRODUCE_CRASH" else "OPEN DETAIL")
            }

        }
    }
}