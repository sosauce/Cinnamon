package com.sosauce.cinnamon.domain.states

enum class CallState {
    RINGING, // When we're receiving a call
    DIALING, // When we're calling
    ONGOING,
    ENDED
}