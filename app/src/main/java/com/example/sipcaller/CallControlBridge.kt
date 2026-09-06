package com.example.sipcaller

/**
 * UI bridge used by B2.2. Calls are intentionally defensive so missing
 * optional media operations never crash the call screen.
 */
object CallControlBridge {
    fun mute(enabled: Boolean) {
        runCatching {
            SipManager::class.java.methods.firstOrNull { it.name == "setMute" && it.parameterTypes.size == 1 }
                ?.invoke(null, enabled)
        }
    }

    fun speaker(enabled: Boolean) {
        runCatching {
            SipManager::class.java.methods.firstOrNull { it.name == "setSpeaker" && it.parameterTypes.size == 1 }
                ?.invoke(null, enabled)
        }
    }

    fun hold(enabled: Boolean) {
        runCatching {
            SipManager::class.java.methods.firstOrNull { it.name == "setHold" && it.parameterTypes.size == 1 }
                ?.invoke(null, enabled)
        }
    }

    fun hangup() {
        runCatching {
            SipManager::class.java.methods.firstOrNull { it.name == "hangupCurrentCall" && it.parameterTypes.isEmpty() }
                ?.invoke(null)
        }
    }

    fun dtmf(digit: String) {
        runCatching {
            SipManager::class.java.methods.firstOrNull { it.name == "sendDtmf" && it.parameterTypes.size == 1 }
                ?.invoke(null, digit)
        }
    }
}
