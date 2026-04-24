package com.sosauce.cinnamon.presentation.screens.voicemail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.domain.model.CuteVoicemail
import com.sosauce.cinnamon.domain.repository.VoicemailsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VoicemailViewModel(
    private val voicemailsRepository: VoicemailsRepository
): ViewModel() {

    private val _state = MutableStateFlow(VoicemailState(isLoading = true))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update {
                it.copy(
                    isLoading = false,
                    voicemails = voicemailsRepository.fetchVoicemails()
                )
            }
        }
    }

}

data class VoicemailState(
    val isLoading: Boolean = false,
    val voicemails: List<CuteVoicemail> = emptyList()
)