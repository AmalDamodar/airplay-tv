package io.github.jqssun.airplay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.jqssun.airplay.R
import io.github.jqssun.airplay.viewmodel.MainViewModel

@Composable
fun LogsScreen(viewModel: MainViewModel) {
    val logs by viewModel.logs.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.tab_logs), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.logs_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { viewModel.exportLogs() }, modifier = Modifier.dpadFocus()) {
                Text(stringResource(R.string.btn_export))
            }
            TextButton(onClick = { viewModel.clearLogs() }, modifier = Modifier.dpadFocus()) {
                Text(stringResource(R.string.btn_clear))
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        ) {
            if (logs.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.logs_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(6.dp).dpadFocus().focusable(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    itemsIndexed(logs) { index, line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                                .background(if (index % 2 == 0) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surfaceContainerLow)
                                .padding(horizontal = 14.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
    }
}
