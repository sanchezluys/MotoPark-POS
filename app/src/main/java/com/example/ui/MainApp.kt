package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ExitPaymentDialog
import com.example.ui.components.TicketReceiptDialog
import com.example.ui.screens.ActiveParkedScreen
import com.example.ui.screens.HistoryReportsScreen
import com.example.ui.screens.PlateEntryExitScreen
import com.example.ui.screens.SettingsScreen
import kotlinx.coroutines.flow.collectLatest

enum class AppDestination(val title: String, val icon: ImageVector) {
    ENTRY_EXIT("Entrada/Salida", Icons.Default.SwapHoriz),
    ACTIVE("En Parqueo", Icons.Default.LocalParking),
    HISTORY("Reportes", Icons.Default.History),
    SETTINGS("Taller", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    viewModel: ParkingViewModel
) {
    val context = LocalContext.current
    var currentDestination by remember { mutableStateOf(AppDestination.ENTRY_EXIT) }
    val snackbarHostState = remember { SnackbarHostState() }

    val activeCount by viewModel.activeCount.collectAsStateWithLifecycle()
    val rateConfig by viewModel.rateConfig.collectAsStateWithLifecycle()
    val selectedTicketForExit by viewModel.selectedTicketForExit.collectAsStateWithLifecycle()
    val selectedTicketForReceipt by viewModel.selectedTicketForReceipt.collectAsStateWithLifecycle()

    // Listen for snackbar messages
    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = rateConfig.businessName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                windowInsets = TopAppBarDefaults.windowInsets
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                windowInsets = NavigationBarDefaults.windowInsets
            ) {
                AppDestination.values().forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            if (destination == AppDestination.ACTIVE && activeCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge { Text("$activeCount") }
                                    }
                                ) {
                                    Icon(destination.icon, contentDescription = destination.title)
                                }
                            } else {
                                Icon(destination.icon, contentDescription = destination.title)
                            }
                        },
                        label = {
                            Text(
                                text = destination.title,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            )
                        },
                        modifier = Modifier.testTag("nav_${destination.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                AppDestination.ENTRY_EXIT -> {
                    PlateEntryExitScreen(
                        viewModel = viewModel,
                        onNavigateToActiveList = { currentDestination = AppDestination.ACTIVE }
                    )
                }
                AppDestination.ACTIVE -> {
                    ActiveParkedScreen(
                        viewModel = viewModel,
                        onNavigateToNewEntry = { currentDestination = AppDestination.ENTRY_EXIT }
                    )
                }
                AppDestination.HISTORY -> {
                    HistoryReportsScreen(
                        viewModel = viewModel
                    )
                }
                AppDestination.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }

    // Modal Exit & Payment Dialog
    selectedTicketForExit?.let { ticket ->
        ExitPaymentDialog(
            ticket = ticket,
            rateConfig = rateConfig,
            onDismiss = { viewModel.selectTicketForExit(null) },
            onConfirmExit = { exitTicket, method, discount ->
                viewModel.completeExitAndPayment(exitTicket, method, discount) { completedTicket ->
                    // opens receipt modal
                }
            }
        )
    }

    // Modal Ticket Receipt & PDF Share Dialog
    selectedTicketForReceipt?.let { ticket ->
        TicketReceiptDialog(
            ticket = ticket,
            rateConfig = rateConfig,
            onDismiss = { viewModel.selectTicketForReceipt(null) },
            onSharePdf = { t -> viewModel.shareTicketPdf(context, t) },
            onShareText = { t -> viewModel.shareTicketText(context, t) }
        )
    }
}

