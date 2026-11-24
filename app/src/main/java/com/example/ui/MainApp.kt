package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.ExitPaymentDialog
import com.example.ui.components.TicketReceiptDialog
import com.example.ui.screens.ActiveParkedScreen
import com.example.ui.screens.HistoryReportsScreen
import com.example.ui.screens.PlateEntryExitScreen
import com.example.ui.screens.SettingsScreen
import com.example.util.AppUpdateController
import com.example.util.AppUpdateState
import kotlinx.coroutines.flow.MutableStateFlow
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
    viewModel: ParkingViewModel,
    appUpdateController: AppUpdateController? = null
) {
    val context = LocalContext.current
    var currentDestination by remember { mutableStateOf(AppDestination.ENTRY_EXIT) }
    val snackbarHostState = remember { SnackbarHostState() }

    val activeCount by viewModel.activeCount.collectAsStateWithLifecycle()
    val rateConfig by viewModel.rateConfig.collectAsStateWithLifecycle()
    val selectedTicketForExit by viewModel.selectedTicketForExit.collectAsStateWithLifecycle()
    val selectedTicketForReceipt by viewModel.selectedTicketForReceipt.collectAsStateWithLifecycle()

    val fallbackUpdateFlow = remember { MutableStateFlow<AppUpdateState>(AppUpdateState.Idle) }
    val updateState by (appUpdateController?.updateState ?: fallbackUpdateFlow).collectAsStateWithLifecycle()

    // Listen for snackbar messages
    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Google Play best practice: notify user with an indefinite Snackbar when flexible update is downloaded
    val downloadedMessage = stringResource(R.string.update_downloaded_message)
    val restartLabel = stringResource(R.string.update_action_restart)
    LaunchedEffect(updateState) {
        if (updateState is AppUpdateState.Downloaded) {
            val result = snackbarHostState.showSnackbar(
                message = downloadedMessage,
                actionLabel = restartLabel,
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                appUpdateController?.completeUpdate()
            }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PlayUpdateNotificationBanner(
                updateState = updateState,
                onStartUpdate = { appUpdateController?.startFlexibleOrImmediateUpdate() },
                onDismiss = { appUpdateController?.dismissBanner() },
                onCompleteUpdate = { appUpdateController?.completeUpdate() }
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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
                            viewModel = viewModel,
                            appUpdateController = appUpdateController
                        )
                    }
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

@Composable
private fun PlayUpdateNotificationBanner(
    updateState: AppUpdateState,
    onStartUpdate: () -> Unit,
    onDismiss: () -> Unit,
    onCompleteUpdate: () -> Unit
) {
    val showBanner = (updateState is AppUpdateState.Available && !updateState.isBannerDismissed) ||
        updateState is AppUpdateState.Downloading ||
        updateState is AppUpdateState.Downloaded

    AnimatedVisibility(
        visible = showBanner,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        val isDownloaded = updateState is AppUpdateState.Downloaded
        val containerColor = if (isDownloaded) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
        val contentColor = if (isDownloaded) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("play_update_banner"),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                when (updateState) {
                    is AppUpdateState.Available -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = stringResource(R.string.update_available_title),
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.update_available_title),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = contentColor
                                )
                                Text(
                                    text = stringResource(R.string.update_available_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = contentColor.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("update_dismiss_button")
                            ) {
                                Text(stringResource(R.string.update_action_later))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = onStartUpdate,
                                modifier = Modifier.testTag("update_install_button")
                            ) {
                                Text(stringResource(R.string.update_action_install))
                            }
                        }
                    }

                    is AppUpdateState.Downloading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.update_downloading_progress, updateState.progressPercent),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = contentColor
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { updateState.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    is AppUpdateState.Downloaded -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = stringResource(R.string.update_downloaded_message),
                                tint = contentColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(R.string.update_downloaded_message),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = contentColor,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = onCompleteUpdate,
                                modifier = Modifier.testTag("update_restart_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.update_action_restart))
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}

