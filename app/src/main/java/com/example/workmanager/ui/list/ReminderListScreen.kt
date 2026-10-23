@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
package com.example.workmanager.ui.list

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.workmanager.R
import com.example.workmanager.data.local.ReminderEntity
import com.example.workmanager.domain.model.ReminderType
import com.example.workmanager.domain.model.StockRules
import com.example.workmanager.domain.util.ReminderTimeUtils
import com.example.workmanager.ui.components.AmbientBackground
import com.example.workmanager.ui.components.ReminderListSkeletonLoader
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ReminderListScreen(
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onHistory: () -> Unit,
    viewModel: ReminderListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deletedMessage = stringResource(R.string.reminder_deleted)
    val undoLabel = stringResource(R.string.undo)
    val context = LocalContext.current

    var showBatteryHelp by rememberSaveable { mutableStateOf(false) }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }

    val dayLabels = stringArrayResource(R.array.day_short)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM") }
    val today = remember { LocalDate.now() }

    // Lọc danh sách nhắc nhở thực sự khi người dùng chọn ngày trong tuần
    val filteredReminders = remember(state.reminders, selectedDate) {
        val dayBit = 1 shl (selectedDate.dayOfWeek.value - 1)
        val selectedEpoch = selectedDate.toEpochDay()

        state.reminders.filter { reminder ->
            val matchesDay = (reminder.daysOfWeek and dayBit) != 0
            val afterStart = selectedEpoch >= reminder.startDate
            val beforeEnd = reminder.endDate == null || selectedEpoch <= reminder.endDate
            matchesDay && afterStart && beforeEnd
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ListEvent.Deleted -> scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = deletedMessage,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onUndoDelete(event.reminder)
                    }
                }
            }
        }
    }

    AmbientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shadowElevation = 2.dp
                ) {
                    Column {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(
                                        text = stringResource(R.string.list_title),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    // Hiệu ứng chữ chuyển động khi đổi ngày chọn
                                    AnimatedContent(
                                        targetState = Pair(selectedDate, filteredReminders.size),
                                        transitionSpec = {
                                            slideInVertically { height -> height } + fadeIn() togetherWith
                                                    slideOutVertically { height -> -height } + fadeOut()
                                        },
                                        label = "header_subtitle"
                                    ) { (date, count) ->
                                        val dateText = if (date == today) {
                                            "Hôm nay có $count lời nhắc"
                                        } else {
                                            "${dayLabels[date.dayOfWeek.value - 1]}, ${date.format(dateFormatter)} có $count lời nhắc"
                                        }
                                        Text(
                                            text = dateText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            },
                            actions = {
                                IconButton(onClick = { showBatteryHelp = true }) {
                                    Icon(
                                        Icons.Filled.BatteryAlert,
                                        contentDescription = stringResource(R.string.battery_help_icon),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = onHistory) {
                                    Icon(
                                        Icons.Filled.History,
                                        contentDescription = stringResource(R.string.history_title),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )

                        // 1. Thanh chọn ngày ngang (Horizontal Date Selector) hoạt động chuyển dữ liệu thực sự
                        HorizontalDateSelector(
                            selectedDate = selectedDate,
                            onDateSelected = { selectedDate = it }
                        )
                    }
                }
            },
            floatingActionButton = {
                // 3. Nút Thêm nhắc nhở dạng viên thuốc (Pill-shaped Extended FAB)
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    icon = {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    text = {
                        Text(
                            stringResource(R.string.add_reminder),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { padding ->
            when {
                state.isLoading -> ReminderListSkeletonLoader(
                    modifier = Modifier.padding(padding)
                )

                filteredReminders.isEmpty() -> EmptyState(
                    selectedDate = selectedDate,
                    today = today,
                    onAdd = onAdd,
                    modifier = Modifier.padding(padding)
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredReminders, key = { it.id }) { reminder ->
                        Box(modifier = Modifier.animateItem()) {
                            SwipeToDeleteItem(
                                reminder = reminder,
                                onClick = { onEdit(reminder.id) },
                                onToggle = { viewModel.onToggle(reminder.id, it) },
                                onDelete = { viewModel.onDelete(reminder) }
                            )
                        }
                    }
                }
            }

            if (showBatteryHelp) {
                AlertDialog(
                    onDismissRequest = { showBatteryHelp = false },
                    title = { Text(stringResource(R.string.battery_help_title)) },
                    text = { Text(stringResource(R.string.battery_help_message)) },
                    confirmButton = {
                        TextButton(onClick = {
                            showBatteryHelp = false
                            try {
                                context.startActivity(
                                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                )
                            } catch (_: ActivityNotFoundException) {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        }) { Text(stringResource(R.string.battery_help_open)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBatteryHelp = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}

// 1. Thành phần Thanh chọn ngày ngang (Horizontal Date Selector)
@Composable
private fun HorizontalDateSelector(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayLabels = stringArrayResource(R.array.day_short)
    val today = remember { LocalDate.now() }
    val startOfWeek = remember(today) {
        today.minusDays((today.dayOfWeek.value - 1).toLong())
    }
    val weekDays = remember(startOfWeek) {
        (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        weekDays.forEach { date ->
            val isSelected = date == selectedDate
            val isToday = date == today

            val bgColor by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary
                else if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.surfaceContainerLow,
                animationSpec = tween(200),
                label = "date_bg"
            )
            val textColor by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.onPrimary
                else if (isToday) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
                animationSpec = tween(200),
                label = "date_text"
            )

            Surface(
                onClick = { onDateSelected(date) },
                shape = RoundedCornerShape(16.dp),
                color = bgColor,
                modifier = Modifier.width(44.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayLabels[date.dayOfWeek.value - 1],
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }
    }
}

// 2. Màu Pastel Accent cho từng Thẻ Reminder
@Composable
private fun getPastelCardColor(id: Long, isActive: Boolean, darkTheme: Boolean): Color {
    if (!isActive) return if (darkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9)

    val colorsLight = listOf(
        Color(0xFFFFFFFF), // Clear White with border
        Color(0xFFF0FDF4), // Mint Emerald Tint
        Color(0xFFEFF6FF), // Ice Blue Tint
        Color(0xFFF5F3FF)  // Soft Violet Tint
    )
    val colorsDark = listOf(
        Color(0xFF111827),
        Color(0xFF064E3B).copy(alpha = 0.3f),
        Color(0xFF1E3A8A).copy(alpha = 0.3f),
        Color(0xFF312E81).copy(alpha = 0.3f)
    )

    val index = (id % 4).toInt()
    return if (darkTheme) colorsDark[index] else colorsLight[index]
}

@Composable
private fun EmptyState(
    selectedDate: LocalDate,
    today: LocalDate,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = if (selectedDate == today) stringResource(R.string.empty_title) else "Không có lời nhắc cho ngày này",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_reminder))
            }
        }
    }
}

@Composable
private fun SwipeToDeleteItem(
    reminder: ReminderEntity,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    LaunchedEffect(reminder) {
        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.delete),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    ) {
        ReminderItem(reminder = reminder, onClick = onClick, onToggle = onToggle)
    }
}

// 2. Thẻ Nhắc nhở (Reminder Card) với màu Pastel, Icon sinh động, Tiến trình thuốc & Nút Switch
@Composable
private fun ReminderItem(
    reminder: ReminderEntity,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val isMedicine = reminder.type == ReminderType.MEDICINE
    val darkTheme = isSystemInDarkTheme()
    val containerColor = getPastelCardColor(reminder.id, reminder.isActive, darkTheme)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (reminder.isActive) 3.dp else 1.dp
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (reminder.isActive) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Icon thuốc / công việc sinh động
                    Surface(
                        shape = CircleShape,
                        color = if (reminder.isActive) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        shadowElevation = if (reminder.isActive) 2.dp else 0.dp,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isMedicine) Icons.Filled.Medication else Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = if (reminder.isActive) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reminder.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (reminder.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "%02d:%02d".format(reminder.hour, reminder.minute),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (reminder.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Switch Bật / Tắt
                Switch(
                    checked = reminder.isActive,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            }

            Spacer(Modifier.height(10.dp))

            // Badges Row (Mỗi ngày / Thứ lặp lại & Liều lượng)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TagChip(text = daysSummary(reminder.daysOfWeek))

                reminder.dosage?.takeIf { it.isNotBlank() }?.let { dosage ->
                    TagChip(
                        text = dosage,
                        icon = Icons.Filled.Medication
                    )
                }
            }

            // Thanh tiến trình thuốc còn lại (Tỷ lệ % chính xác theo số lượng ban đầu/hiện có)
            reminder.remainingQty?.let { qty ->
                Spacer(Modifier.height(10.dp))
                StockProgressBar(
                    remainingQty = qty,
                    initialQty = reminder.initialQty
                )
            }

            if (reminder.note.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = reminder.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Tiến trình số lượng thuốc linh hoạt & chính xác tỷ lệ % (dựa trên số lượng ban đầu)
@Composable
private fun StockProgressBar(
    remainingQty: Int,
    initialQty: Int? = null
) {
    val total = maxOf(initialQty ?: remainingQty, remainingQty, 1)
    val progress = (remainingQty.toFloat() / total).coerceIn(0f, 1f)
    val isLow = remainingQty <= StockRules.LOW_THRESHOLD

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isLow) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Inventory2,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isLow) "Cảnh báo sắp hết: Còn $remainingQty/$total viên!" else "Số lượng còn lại: $remainingQty/$total viên",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(3.5.dp)),
                color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = if (isLow) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun TagChip(
    text: String,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = contentColor
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun daysSummary(mask: Int): String {
    val labels = stringArrayResource(R.array.day_short)
    val days = ReminderTimeUtils.maskToDays(mask)
    return when {
        days.size == 7 -> stringResource(R.string.every_day)
        days.isEmpty() -> ""
        else -> days.sortedBy { it.value }.joinToString(" ") { labels[it.value - 1] }
    }
}