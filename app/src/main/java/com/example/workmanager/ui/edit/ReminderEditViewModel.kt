package com.example.workmanager.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.workmanager.data.local.ReminderEntity
import com.example.workmanager.data.repository.ReminderRepository
import com.example.workmanager.domain.model.ReminderType
import com.example.workmanager.domain.util.ReminderTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

data class EditUiState(
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val title: String = "",
    val note: String = "",
    val type: ReminderType = ReminderType.MEDICINE,
    val hour: Int = 8,
    val minute: Int = 0,
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val startDate: LocalDate = LocalDate.now(),
    val endDate: LocalDate? = null,
    val dosage: String = "",
    val remainingQty: String = "",
    val titleError: Boolean = false,
    val daysError: Boolean = false,
    val dateError: Boolean = false,
    val qtyError: Boolean = false
)

sealed interface EditEvent {
    data object Saved : EditEvent
    data object Close : EditEvent
}

@HiltViewModel
class ReminderEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReminderRepository
) : ViewModel() {

    private val reminderId: Long = savedStateHandle.get<Long>("reminderId") ?: -1L
    private var original: ReminderEntity? = null

    private val _uiState = MutableStateFlow(
        EditUiState(isEditing = reminderId != -1L, isLoading = reminderId != -1L)
    )
    val uiState: StateFlow<EditUiState> = _uiState.asStateFlow()

    private val _events = Channel<EditEvent>(Channel.BUFFERED)
    val events: Flow<EditEvent> = _events.receiveAsFlow()

    init {
        if (reminderId != -1L) load()
    }

    private fun load() {
        viewModelScope.launch {
            val reminder = repository.observe(reminderId).first()
            if (reminder == null) {
                _events.send(EditEvent.Close)
                return@launch
            }
            original = reminder
            _uiState.value = EditUiState(
                isLoading = false,
                isEditing = true,
                title = reminder.title,
                note = reminder.note,
                type = reminder.type,
                hour = reminder.hour,
                minute = reminder.minute,
                days = ReminderTimeUtils.maskToDays(reminder.daysOfWeek),
                startDate = LocalDate.ofEpochDay(reminder.startDate),
                endDate = reminder.endDate?.let { LocalDate.ofEpochDay(it) },
                dosage = reminder.dosage.orEmpty(),
                remainingQty = reminder.remainingQty?.toString().orEmpty()
            )
        }
    }

    fun onTitleChange(value: String) =
        _uiState.update { it.copy(title = value, titleError = false) }

    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }

    fun onTypeChange(value: ReminderType) = _uiState.update { it.copy(type = value) }

    fun onTimeChange(hour: Int, minute: Int) =
        _uiState.update { it.copy(hour = hour, minute = minute) }

    fun onToggleDay(day: DayOfWeek) = _uiState.update {
        val newDays = if (day in it.days) it.days - day else it.days + day
        it.copy(days = newDays, daysError = false)
    }

    fun onStartDateChange(date: LocalDate) =
        _uiState.update { it.copy(startDate = date, dateError = false) }

    fun onEndDateChange(date: LocalDate?) =
        _uiState.update { it.copy(endDate = date, dateError = false) }

    fun onDosageChange(value: String) = _uiState.update { it.copy(dosage = value) }

    fun onQuantityChange(value: String) =
        _uiState.update { it.copy(remainingQty = value, qtyError = false) }

    fun save() {
        val s = _uiState.value
        if (s.isSaving) return

        val end = s.endDate
        val isMedicine = s.type == ReminderType.MEDICINE
        val qtyText = s.remainingQty.trim()
        val qty = qtyText.toIntOrNull()

        val titleError = s.title.isBlank()
        val daysError = s.days.isEmpty()
        val dateError = end != null && end.isBefore(s.startDate)
        val qtyError = isMedicine && qtyText.isNotEmpty() && (qty == null || qty < 0)

        if (titleError || daysError || dateError || qtyError) {
            _uiState.update {
                it.copy(
                    titleError = titleError,
                    daysError = daysError,
                    dateError = dateError,
                    qtyError = qtyError
                )
            }
            return
        }

        val old = original
        val entity = ReminderEntity(
            id = old?.id ?: 0,
            title = s.title.trim(),
            note = s.note.trim(),
            type = s.type,
            hour = s.hour,
            minute = s.minute,
            daysOfWeek = ReminderTimeUtils.daysToMask(s.days),
            startDate = s.startDate.toEpochDay(),
            endDate = end?.toEpochDay(),
            dosage = if (isMedicine) s.dosage.trim().ifEmpty { null } else null,
            remainingQty = if (isMedicine) qty else null,
            isActive = old?.isActive ?: true,
            createdAt = old?.createdAt ?: System.currentTimeMillis()
        )

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            if (old != null) repository.update(entity) else repository.add(entity)
            _events.send(EditEvent.Saved)
        }
    }
}