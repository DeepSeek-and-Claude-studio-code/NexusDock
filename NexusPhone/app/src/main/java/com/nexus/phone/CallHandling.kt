package com.nexus.phone

import android.content.Intent
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile

object ActiveCallHolder {
    @Volatile
    var currentCall: Call? = null
}

class NexusInCallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        ActiveCallHolder.currentCall = call
        startActivity(
            Intent(this, InCallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
    }

    override fun onCallRemoved(call: Call) {
        if (ActiveCallHolder.currentCall === call) ActiveCallHolder.currentCall = null
        super.onCallRemoved(call)
    }

    companion object {
        const val AUDIO_ONLY_VIDEO_STATE = VideoProfile.STATE_AUDIO_ONLY
    }
}
