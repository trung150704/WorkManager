@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.workmanager.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.workmanager.R
import com.example.workmanager.data.local.ReminderLogItem
import com.example.workmanager.domain.model.LogStatus
import com.example.workmanager.domain.util.WeeklyStats
import com.example.workmanager.ui.components.AmbientBackground
import com.example.workmanager.ui.components.HistorySkeletonLoader
import com.example.workmanager.ui.theme.StatusError
import com.example.workmanager.ui.theme.StatusErrorContainer
import com.example.workmanager.ui.theme.StatusNeutral
import com.example.workmanager.ui.theme.StatusNeutralContainer
import com.example.workmanager.ui.theme.StatusSuccess
import com.example.workmanager.ui.theme.StatusSuccessContainer
import com.example.workmanager.ui.theme.StatusWarning
import com.example.workmanager.ui.theme.StatusWarningContainer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm · dd/MM/yyyy") }

    AmbientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.history_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { padding ->
            when {
                state.isLoading -> HistorySkeletonLoader(
                    modifier = Modifier.padding(padding)
                )

                state.items.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            stringResource(R.string.history_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { WeeklyCard(state.weekly) }

                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Lịch sử tương tác gần đây",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(state.items, key = { it.log.id }) { item ->
                        Box(modifier = Modifier.animateItem()) {
                            LogRow(item, formatter)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogRow(item: ReminderLogItem, formatter: DateTimeFormatter) {
    val scheduled = Instant.ofEpochMilli(item.log.scheduledTime)
        .atZone(ZoneId.systemDefault())
        .format(formatter)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadgeIcon(item.log.status)

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.reminderTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.history_scheduled_at, scheduled),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(8.dp))

            StatusPill(item.log.status)
        }
    }
}

@Composable
private fun StatusBadgeIcon(status: LogStatus) {
    val (bgColor, iconColor, icon) = when (status) {
        LogStatus.TAKEN -> Triple(StatusSuccessContainer, StatusSuccess, Icons.Filled.Check)
        LogStatus.SKIPPED -> Triple(StatusNeutralContainer, StatusNeutral, Icons.Filled.Close)
        LogStatus.SNOOZED -> Triple(StatusWarningContainer, StatusWarning, Icons.Filled.Snooze)
        LogStatus.MISSED -> Triple(StatusErrorContainer, StatusError, Icons.Filled.Warning)
    }

    Surface(
        shape = CircleShape,
        color = bgColor,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StatusPill(status: LogStatus) {
    val (bgColor, textColor, labelRes) = when (status) {
        LogStatus.TAKEN -> Triple(StatusSuccessContainer, StatusSuccess, R.string.status_taken)
        LogStatus.SKIPPED -> Triple(StatusNeutralContainer, StatusNeutral, R.string.status_skipped)
        LogStatus.SNOOZED -> Triple(StatusWarningContainer, StatusWarning, R.string.status_snoozed)
        LogStatus.MISSED -> Triple(StatusErrorContainer, StatusError, R.string.status_missed)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun WeeklyCard(stats: WeeklyStats) {
    val labels = stringArrayResource(R.array.day_short)
    val barAreaHeight = 72
    val maxCount = (stats.days.maxOfOrNull { it.taken + it.notTaken } ?: 0).coerceAtLeast(1)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        stringResource(R.string.stats_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stats.ratePercent
                            ?.let { stringResource(R.string.stats_rate, it) }
                            ?: stringResource(R.string.stats_no_data),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = stringResource(
                            R.string.stats_detail, stats.taken, stats.skipped, stats.missed
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Biểu đồ cột
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                stats.days.forEach { day ->
                    val total = day.taken + day.notTaken
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Column(
                            modifier = Modifier.height(barAreaHeight.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            if (total == 0) {
                                Box(
                                    Modifier
                                        .width(18.dp)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                            } else {
                                if (day.notTaken > 0) {
                                    Box(
                                        Modifier
                                            .width(18.dp)
                                            .height((barAreaHeight * day.notTaken / maxCount.toFloat()).dp)
                                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                            .background(StatusError)
                                    )
                                }
                                if (day.taken > 0) {
                                    Box(
                                        Modifier
                                            .width(18.dp)
                                            .height((barAreaHeight * day.taken / maxCount.toFloat()).dp)
                                            .clip(
                                                if (day.notTaken > 0) RoundedCornerShape(0.dp)
                                                else RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                            )
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            labels[day.date.dayOfWeek.value - 1],
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}