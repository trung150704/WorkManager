package com.example.workmanager.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workmanager.data.local.ReminderLogItem
import com.example.workmanager.data.repository.ReminderRepository
import com.example.workmanager.domain.util.LogStats
import com.example.workmanager.domain.util.WeeklyStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HistoryUiState(
    val items: List<ReminderLogItem> = emptyList(),
    val weekly: WeeklyStats = WeeklyStats(),
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: ReminderRepository
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = repository.observeLogs()
        .map { list ->
            HistoryUiState(
                items = list,
                weekly = LogStats.weekly(list.map { it.log }),
                isLoading = false
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState()
        )
}