package com.sosauce.cinnamon.features.incall.presentation

enum class CallUiState {
    RINGING, // When we're receiving a call
    DIALING, // When we're calling
    ONGOING,
    ENDED
}