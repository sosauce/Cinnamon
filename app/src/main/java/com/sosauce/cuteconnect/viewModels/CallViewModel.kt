package com.sosauce.cuteconnect.viewModels

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telecom.Call
import android.telecom.CallEndpoint
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sosauce.cuteconnect.activities.CallActivity
import com.sosauce.cuteconnect.data.actions.CallAction
import com.sosauce.cuteconnect.data.contact_settings.ContactSettingsDao
import com.sosauce.cuteconnect.data.managers.CallManager
import com.sosauce.cuteconnect.domain.states.CallState
import com.sosauce.cuteconnect.domain.states.CallUiState
import com.sosauce.cuteconnect.utils.AudioTargetDevice
import com.sosauce.cuteconnect.utils.getContactId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CallViewModel(
    private val application: Application,
    private val contactSettingsDao: ContactSettingsDao
): AndroidViewModel(application) {


    private val telecomManager = application.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

    private val _callUIState = MutableStateFlow(CallUiState())
    val callUiState = _callUIState.asStateFlow()


    init {
        viewModelScope.launch {
            CallManager.callUiState.collectLatest { state ->
                _callUIState.update { state }
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            val contactId = callUiState.value.number.getContactId(application)
            val poster = contactSettingsDao.getConversationSettings(contactId).first()?.poster ?: ""

            println("hello poster: $poster")

            _callUIState.update {
                it.copy(
                    poster = poster
                )
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun onHandleCallAction(action: CallAction) {
        when(action) {
            is CallAction.LaunchCall -> {
                val numberAsUri = "tel:${action.number}".toUri()

                if (!telecomManager.isInCall) {
                    telecomManager.placeCall(numberAsUri, null)
                }
            }
            is CallAction.StartTone -> CallManager.startTone(action.char)
            is CallAction.DeclineCall -> CallManager.declineCall()
            is CallAction.AnswerCall -> CallManager.answerCall()
            is CallAction.ToggleHold -> CallManager.toggleHold()
            is CallAction.ToggleMute -> CallManager.toggleMute(action.mute)
            is CallAction.HangUp -> CallManager.hangupOngoingCall()
            is CallAction.SwitchAudioTarget -> CallManager.switchAudioRoute(action.route)
        }
    }
}