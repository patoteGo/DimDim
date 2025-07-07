package com.EYP.dimdim.presentation.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.EYP.dimdim.data.util.ContentUriHelper
import com.EYP.dimdim.data.util.ImageInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShareReceiverUiState(
    val isLoading: Boolean = false,
    val imageInfoList: List<ImageInfo> = emptyList(),
    val error: String? = null,
    val lastProcessedUris: List<Uri>? = null
)

@HiltViewModel
class ShareReceiverViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ShareReceiverUiState())
    val uiState: StateFlow<ShareReceiverUiState> = _uiState.asStateFlow()

    fun processImages(uris: List<Uri>) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null,
            lastProcessedUris = uris
        )

        viewModelScope.launch {
            try {
                val result = ContentUriHelper.processSharedImages(getApplication(), uris)
                
                result.fold(
                    onSuccess = { imageInfoList ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            imageInfoList = imageInfoList,
                            error = null
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to process images: ${exception.message}",
                            imageInfoList = emptyList()
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Unexpected error: ${e.message}",
                    imageInfoList = emptyList()
                )
            }
        }
    }

    fun retry() {
        val lastUris = _uiState.value.lastProcessedUris
        if (lastUris != null) {
            processImages(lastUris)
        }
    }

    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = message,
            imageInfoList = emptyList()
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}