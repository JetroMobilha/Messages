package org.fossify.messages.services

import android.annotation.TargetApi
import android.net.Uri
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.messages.extensions.vaultCallDB
import org.fossify.messages.extensions.vaultContactDB
import org.fossify.messages.models.VaultCall

@TargetApi(Build.VERSION_CODES.N)
class VaultCallScreeningService : CallScreeningService() {
    override fun onScreenCall(callDetails: Call.Details) {
        val phoneNumber = getPhoneNumber(callDetails)
        
        if (phoneNumber.isEmpty()) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        ensureBackgroundThread {
            try {
                val vaultContact = applicationContext.vaultContactDB.getByPhoneNumber(phoneNumber)
                if (vaultContact != null && vaultContact.blockCalls) {
                    // Number is in Vault and Block Calls is TRUE -> Intercept and Reject
                    val response = CallResponse.Builder()
                        .setDisallowCall(true)
                        .setRejectCall(true)
                        .setSkipCallLog(true)
                        .setSkipNotification(true)
                        .build()

                    respondToCall(callDetails, response)

                    // Log call in the vault
                    val vaultCall = VaultCall(
                        phoneNumber = phoneNumber,
                        date = System.currentTimeMillis(),
                        type = 1 // 1 for BLOCKED/INTERCEPTED
                    )
                    applicationContext.vaultCallDB.insert(vaultCall)
                } else {
                    // Normal call OR Vault Contact with blockCalls=false -> Allow
                    respondToCall(callDetails, CallResponse.Builder().build())
                }
            } catch (_: Exception) {
                // Defensive fallback: Always allow call if an error occurs to prevent telecom freeze
                respondToCall(callDetails, CallResponse.Builder().build())
            }
        }
    }

    private fun getPhoneNumber(callDetails: Call.Details): String {
        val handle: Uri? = callDetails.handle
        return if (handle != null && handle.scheme == "tel") {
            handle.schemeSpecificPart
        } else {
            ""
        }
    }
}
