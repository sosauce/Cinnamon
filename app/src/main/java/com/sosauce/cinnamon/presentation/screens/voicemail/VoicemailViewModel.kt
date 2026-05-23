@file:OptIn(FlowPreview::class)

package com.sosauce.cinnamon.presentation.screens.voicemail

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.domain.model.CuteVoicemail
import com.sosauce.cinnamon.domain.repository.VoicemailsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VoicemailViewModel(
    private val voicemailsRepository: VoicemailsRepository
) : ViewModel() {

    private val textFieldState = TextFieldState()
    private val _state = MutableStateFlow(
        VoicemailState(
            isLoading = true,
            textFieldState = textFieldState
        )
    )
    val state = _state.asStateFlow()

    init {

        viewModelScope.launch(Dispatchers.IO) {
            combine(
                voicemailsRepository.fetchLatestVoicemails(),
                snapshotFlow { textFieldState.text }.debounce(250)
            ) { voicemails, searchQuery ->
                voicemails.fastFilter {
                    it.displayName.contains(searchQuery, true) ||
                            it.number.contains(searchQuery, true)
                }
            }.collectLatest { voicemails ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        voicemails = voicemails
                    )
                }
            }
        }
    }

    fun deleteVoicemails(ids: List<Long>) {
        viewModelScope.launch {
            voicemailsRepository.deleteVoicemails(ids)
        }
    }

}

data class VoicemailState(
    val isLoading: Boolean = false,
    val textFieldState: TextFieldState = TextFieldState(),
    val voicemails: List<CuteVoicemail> = emptyList()
)