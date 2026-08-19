package com.example.ui.taskdetails

import com.example.data.entity.ActivityTask

sealed interface TaskDetailsUiState {
    data object Loading : TaskDetailsUiState
    data class Success(val task: ActivityTask) : TaskDetailsUiState
    data object NotFound : TaskDetailsUiState
    data class Error(val message: String) : TaskDetailsUiState
}
