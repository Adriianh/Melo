package com.github.adriianh.melo.ui.i18n

import androidx.compose.runtime.Composable
import com.github.adriianh.core.domain.model.update.UpdateErrorReason
import com.github.adriianh.core.domain.model.update.UpdateState
import com.github.adriianh.melo.resources.Res
import com.github.adriianh.melo.resources.install_error_android_open_failed
import com.github.adriianh.melo.resources.install_error_appimage_not_found
import com.github.adriianh.melo.resources.install_error_helper_prepare_failed
import com.github.adriianh.melo.resources.install_error_no_android_context
import com.github.adriianh.melo.resources.install_error_no_write_permission
import com.github.adriianh.melo.resources.install_error_package_not_found
import com.github.adriianh.melo.resources.install_error_portable_no_permission
import com.github.adriianh.melo.resources.install_error_portable_not_found
import com.github.adriianh.melo.resources.install_error_start_helper_failed
import com.github.adriianh.melo.resources.install_error_start_system_failed
import com.github.adriianh.melo.resources.install_error_unsupported_platform
import com.github.adriianh.melo.resources.update_error_check
import com.github.adriianh.melo.resources.update_error_check_with_detail
import com.github.adriianh.melo.resources.update_error_download
import com.github.adriianh.melo.resources.update_error_download_with_detail
import com.github.adriianh.melo.resources.update_error_install
import com.github.adriianh.melo.resources.update_error_install_with_detail
import com.github.adriianh.melo.resources.update_error_unknown
import com.github.adriianh.melo.util.InstallErrorReason
import com.github.adriianh.melo.util.UpdateInstallResult
import org.jetbrains.compose.resources.stringResource

private const val INSTALL_REASON_PREFIX = "@melo.install.error:"

internal fun UpdateInstallResult.Failed.encodeForErrorState(): String =
    if (reason != null) {
        "$INSTALL_REASON_PREFIX${reason.name}:${detail.orEmpty()}"
    } else {
        message
    }

@Composable
internal fun UpdateState.Error.localizedMessage(): String {
    val decodedInstall = decodeInstallError(message)
    if (decodedInstall != null) {
        return decodedInstall.first.localizedMessage(decodedInstall.second)
    }

    return when (reason) {
        UpdateErrorReason.CHECK_FAILED ->
            resolveWithDetail(message, Res.string.update_error_check, Res.string.update_error_check_with_detail)

        UpdateErrorReason.DOWNLOAD_FAILED ->
            resolveWithDetail(message, Res.string.update_error_download, Res.string.update_error_download_with_detail)

        UpdateErrorReason.INSTALL_FAILED ->
            message.ifBlank { stringResource(Res.string.update_error_install) }

        UpdateErrorReason.UNKNOWN ->
            resolveWithDetail(message, Res.string.update_error_unknown, Res.string.update_error_install_with_detail)

        null -> message.ifBlank { stringResource(Res.string.update_error_unknown) }
    }
}

private fun decodeInstallError(message: String): Pair<InstallErrorReason, String>? =
    if (!message.startsWith(INSTALL_REASON_PREFIX)) {
        null
    } else {
        val payload = message.removePrefix(INSTALL_REASON_PREFIX)
        val separatorIndex = payload.indexOf(':')
        val reasonName = if (separatorIndex >= 0) payload.substring(0, separatorIndex) else payload
        val detail = if (separatorIndex >= 0) payload.substring(separatorIndex + 1) else ""
        InstallErrorReason.entries.find { it.name == reasonName }?.let { it to detail }
    }

@Composable
private fun resolveWithDetail(
    detail: String,
    fallbackRes: org.jetbrains.compose.resources.StringResource,
    withDetailRes: org.jetbrains.compose.resources.StringResource,
): String =
    if (detail.isNotBlank()) {
        stringResource(withDetailRes, detail)
    } else {
        stringResource(fallbackRes)
    }

@Composable
private fun InstallErrorReason.localizedMessage(detail: String): String =
    when (this) {
        InstallErrorReason.NO_ANDROID_CONTEXT -> stringResource(Res.string.install_error_no_android_context)
        InstallErrorReason.PACKAGE_NOT_FOUND -> stringResource(Res.string.install_error_package_not_found, detail)
        InstallErrorReason.ANDROID_INSTALLER_OPEN_FAILED ->
            stringResource(Res.string.install_error_android_open_failed, detail)

        InstallErrorReason.APPIMAGE_NOT_FOUND -> stringResource(Res.string.install_error_appimage_not_found)
        InstallErrorReason.NO_WRITE_PERMISSION -> stringResource(Res.string.install_error_no_write_permission, detail)
        InstallErrorReason.PORTABLE_INSTALL_NOT_FOUND -> stringResource(Res.string.install_error_portable_not_found)
        InstallErrorReason.PORTABLE_NO_PERMISSION ->
            stringResource(Res.string.install_error_portable_no_permission, detail)

        InstallErrorReason.HELPER_PREPARE_FAILED ->
            stringResource(Res.string.install_error_helper_prepare_failed, detail)

        InstallErrorReason.START_SYSTEM_FAILED ->
            stringResource(Res.string.install_error_start_system_failed, detail)

        InstallErrorReason.START_HELPER_FAILED ->
            stringResource(Res.string.install_error_start_helper_failed, detail)

        InstallErrorReason.UNSUPPORTED_PLATFORM -> stringResource(Res.string.install_error_unsupported_platform)
    }
