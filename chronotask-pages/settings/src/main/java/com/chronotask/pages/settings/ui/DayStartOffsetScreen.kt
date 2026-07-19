package com.chronotask.pages.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chronotask.components.common.appDataStore
import com.chronotask.components.ui.R
import kotlinx.coroutines.launch

private val OFFSET_OPTIONS = listOf(0, 1, 2, 3, 4, 5, 6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayStartOffsetScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val currentOffset by appDataStore.dayStartOffsetHours.collectAsState(initial = 0)
    val currentIndex = OFFSET_OPTIONS.indexOf(currentOffset).coerceAtLeast(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.refresh_time), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.refresh_time_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = formatOffset(currentOffset),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Slider(
                        value = currentIndex.toFloat(),
                        onValueChange = { idx ->
                            val clamped = idx.toInt().coerceIn(0, OFFSET_OPTIONS.lastIndex)
                            scope.launch { appDataStore.setDayStartOffsetHours(OFFSET_OPTIONS[clamped]) }
                        },
                        valueRange = 0f..OFFSET_OPTIONS.lastIndex.toFloat(),
                        steps = OFFSET_OPTIONS.lastIndex - 1
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = OFFSET_OPTIONS.joinToString(" / ") { formatOffset(it) },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.refresh_time_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatOffset(hour: Int): String {
    return if (hour == 0) "00:00 ( Midnight )"
    else String.format("%02d:00", hour)
}
