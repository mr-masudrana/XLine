package com.example.sipcaller.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.sipcaller.diagnostics.SipDiagnostics
import com.example.sipcaller.diagnostics.SipDiagnosticsStore
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf(SipDiagnosticsStore.current()) }
    var logs by remember { mutableStateOf(SipDiagnostics.entries()) }

    LaunchedEffect(Unit) {
        while (true) {
            snapshot = SipDiagnosticsStore.current()
            logs = SipDiagnostics.entries()
            delay(1000)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("SIP Diagnostics") },
            navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) },
        bottomBar = {
            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                    val text = buildString {
                        appendLine("SIP Diagnostics")
                        appendLine("Registration: ${snapshot.registrationState}")
                        appendLine("Network: ${snapshot.networkState}")
                        appendLine("Server: ${snapshot.server}")
                        appendLine("Last response: ${snapshot.lastSipResponse}")
                        appendLine("Last error: ${snapshot.lastError}")
                        appendLine("Retries: ${snapshot.retryCount}")
                        appendLine()
                        logs.forEach { appendLine(it) }
                    }
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("SIP diagnostics", text))
                }) { Text("Copy") }
                Button(modifier = Modifier.weight(1f), onClick = {
                    SipDiagnostics.clear()
                    logs = emptyList()
                }) { Text("Clear Logs") }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("Connection", style = MaterialTheme.typography.titleMedium) }
            item { DiagnosticRow("Registration", snapshot.registrationState) }
            item { DiagnosticRow("Network", snapshot.networkState) }
            item { DiagnosticRow("Server", snapshot.server) }
            item { DiagnosticRow("Last SIP response", snapshot.lastSipResponse) }
            item { DiagnosticRow("Last error", snapshot.lastError) }
            item { DiagnosticRow("Retry count", snapshot.retryCount.toString()) }
            item { DiagnosticRow("Last registration", if (snapshot.lastRegistrationAt == 0L) "-" else java.text.DateFormat.getDateTimeInstance().format(java.util.Date(snapshot.lastRegistrationAt))) }
            item { HorizontalDivider(); Text("Recent logs", style = MaterialTheme.typography.titleMedium) }
            items(logs.size) { i -> Text(logs[i], style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable private fun DiagnosticRow(label: String, value: String) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
