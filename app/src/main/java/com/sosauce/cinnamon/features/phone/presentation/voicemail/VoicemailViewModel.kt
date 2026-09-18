@file:OptIn(FlowPreview::class)

package com.sosauce.cinnamon.features.phone.presentation.voicemail

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cinnamon.features.phone.data.repository.VoicemailsRepository
import com.sosauce.cinnamon.features.phone.domain.CuteVoicemail
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class VoicemailViewModel(
    private val voicemailsRepository: VoicemailsRepository
) : ViewModel() {

    val textFieldState = TextFieldState()

    val state = combine(
        voicemailsRepository.fetchLatestVoicemails(),
        snapshotFlow { textFieldState.text }.debounce(250.milliseconds)
    ) { voicemails, searchQuery ->
        val filtered = voicemails.fastFilter {
            it.displayName.contains(searchQuery, true) ||
                    it.number.contains(searchQuery, true)
        }
        VoicemailState(
            isLoading = false,
            voicemails = filtered
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        VoicemailState(isLoading = true)
    )

    fun deleteVoicemails(ids: List<Long>) {
        viewModelScope.launch {
            voicemailsRepository.deleteVoicemails(ids)
        }
    }

}

data class VoicemailState(
    val isLoading: Boolean = false,
    val voicemails: List<CuteVoicemail> = emptyList()
)