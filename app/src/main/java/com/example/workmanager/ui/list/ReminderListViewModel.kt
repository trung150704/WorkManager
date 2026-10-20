package com.example.workmanager.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workmanager.data.local.ReminderEntity
import com.example.workmanager.data.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReminderListUiState(
    val reminders: List<ReminderEntity> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface ListEvent {
    data class Deleted(val reminder: ReminderEntity) : ListEvent
}

@HiltViewModel
class ReminderListViewModel @Inject constructor(
    private val repository: ReminderRepository
) : ViewModel() {

    val uiState: StateFlow<ReminderListUiState> = repository.observeAll()
        .map { ReminderListUiState(reminders = it, isLoading = false) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ReminderListUiState()
        )

    // Sự kiện một lần (Snackbar), không bị phát lại khi xoay màn hình
    private val _events = Channel<ListEvent>(Channel.BUFFERED)
    val events: Flow<ListEvent> = _events.receiveAsFlow()

    fun onToggle(id: Long, isActive: Boolean) {
        viewModelScope.launch { repository.setActive(id, isActive) }
    }

    fun onDelete(reminder: ReminderEntity) {
        viewModelScope.launch {
            repository.delete(reminder)
            _events.send(ListEvent.Deleted(reminder))
        }
    }

    // Chèn lại đúng id cũ (DAO dùng OnConflictStrategy.REPLACE)
    fun onUndoDelete(reminder: ReminderEntity) {
        viewModelScope.launch { repository.add(reminder) }
    }
}