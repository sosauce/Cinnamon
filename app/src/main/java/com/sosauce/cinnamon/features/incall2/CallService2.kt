package com.sosauce.cinnamon.features.incall2

import android.telecom.Call
import android.telecom.InCallService
import org.koin.android.ext.android.inject
import org.koin.core.component.KoinComponent

class CallService2 : InCallService(), KoinComponent {

    private val inCallRepository2 by inject<InCallRepository2>()

    override fun onCallAdded(call: Call?) {
        super.onCallAdded(call)
        if (call == null) return
        inCallRepository2.onCallAdded(call)
    }


}