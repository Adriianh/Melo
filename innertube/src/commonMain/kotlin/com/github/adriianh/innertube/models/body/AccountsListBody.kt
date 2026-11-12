package com.github.adriianh.innertube.models.body

import com.github.adriianh.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class AccountsListBody(
    val context: Context,
    val requestType: String = REQUEST_TYPE_CHANNEL_SWITCHER,
    val callCircumstance: String = CALL_CIRCUMSTANCE_SWITCHING_USERS_FULL,
) {
    companion object {
        const val REQUEST_TYPE_CHANNEL_SWITCHER = "ACCOUNTS_LIST_REQUEST_TYPE_CHANNEL_SWITCHER"
        const val CALL_CIRCUMSTANCE_SWITCHING_USERS_FULL = "SWITCHING_USERS_FULL"
    }
}
