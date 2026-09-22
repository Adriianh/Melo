package com.github.adriianh.cli.tui.player

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Guid
import com.sun.jna.platform.win32.Ole32
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Real-time per-process volume controller for Windows using Core Audio (WASAPI).
 *
 * ffplay uses SDL2 for audio output, which creates an audio session in the Windows
 * Audio Session API (WASAPI). This controller enumerates active audio sessions via
 * [IAudioSessionManager2], matches the session whose process ID equals [pid],
 * and uses [ISimpleAudioVolume] to set the session master volume dynamically (0.0 to 1.0)
 * without restarting or interrupting playback.
 */
internal object WindowsVolumeController {

    private val CLSID_MMDeviceEnumerator =
        Guid.CLSID(Guid.GUID.fromString("{BCDE0395-E52F-467C-8E3D-C4579291692E}"))
    private val IID_IMMDeviceEnumerator =
        Guid.IID(Guid.GUID.fromString("{A95664D2-9614-4F35-A746-DE8DB63617E6}"))
    private val IID_IAudioSessionManager2 =
        Guid.IID(Guid.GUID.fromString("{77AA99A0-1BD6-484F-8BC7-2C654C9A9B6F}"))
    private val IID_IAudioSessionControl2 =
        Guid.IID(Guid.GUID.fromString("{BFB7FF88-7239-4FC9-8FA2-07C950BE9C6D}"))
    private val IID_ISimpleAudioVolume =
        Guid.IID(Guid.GUID.fromString("{87CE5498-68D6-44E5-9215-6DA47EF883D8}"))

    private const val S_OK = 0
    private const val eRender = 0
    private const val eConsole = 0
    private const val CLSCTX_ALL = 23

    val isAvailable: Boolean by lazy {
        if (!FfplayProcessManager.isWindows) return@lazy false
        try {
            val hr = Ole32.INSTANCE.CoInitializeEx(Pointer.NULL, Ole32.COINIT_MULTITHREADED)
            val initialized = (hr.toInt() == S_OK || hr.toInt() == 1 /* S_FALSE */)
            val ppEnumerator = PointerByReference()
            val hrCreate = Ole32.INSTANCE.CoCreateInstance(
                CLSID_MMDeviceEnumerator,
                null,
                CLSCTX_ALL,
                IID_IMMDeviceEnumerator,
                ppEnumerator
            )
            val success = (hrCreate.toInt() == S_OK && ppEnumerator.value != null && ppEnumerator.value != Pointer.NULL)
            if (success) {
                release(ppEnumerator.value)
            }
            if (initialized) {
                try {
                    Ole32.INSTANCE.CoUninitialize()
                } catch (_: Throwable) {
                }
            }
            success
        } catch (_: Throwable) {
            false
        }
    }

    fun applyVolume(scope: CoroutineScope, pid: Long, pct: Int) {
        if (!isAvailable) return
        scope.launch(Dispatchers.IO) {
            val clampedPct = pct.coerceIn(0, 100)
            (0 until 10).forEach { _ ->
                val applied = setProcessVolume(pid, clampedPct)
                if (applied) return@launch
                delay(100.milliseconds)
            }
        }
    }

    internal fun setProcessVolume(pid: Long, pct: Int): Boolean {
        var coInitialized = false
        var pEnumerator: Pointer? = null
        var pDevice: Pointer? = null
        var pManager: Pointer? = null
        var pSessionEnum: Pointer? = null

        try {
            val hrInit = Ole32.INSTANCE.CoInitializeEx(Pointer.NULL, Ole32.COINIT_MULTITHREADED)
            coInitialized = (hrInit.toInt() == S_OK || hrInit.toInt() == 1 /* S_FALSE */)

            val ppEnumerator = PointerByReference()
            val hrCreate = Ole32.INSTANCE.CoCreateInstance(
                CLSID_MMDeviceEnumerator,
                null,
                CLSCTX_ALL,
                IID_IMMDeviceEnumerator,
                ppEnumerator
            )
            if (hrCreate.toInt() != S_OK) return false
            pEnumerator = ppEnumerator.value

            // IMMDeviceEnumerator::GetDefaultAudioEndpoint (vtable index 4)
            // (int dataFlow, int role, IMMDevice** ppEndpoint)
            val ppDevice = PointerByReference()
            val hrGetDevice = callCom(pEnumerator, 4, eRender, eConsole, ppDevice)
            if (hrGetDevice != S_OK) return false
            pDevice = ppDevice.value

            // IMMDevice::Activate (vtable index 3)
            // (REFIID iid, DWORD dwClsCtx, PROPVARIANT* pActivationParams, void** ppInterface)
            val ppManager = PointerByReference()
            val hrActivate = callCom(pDevice, 3, IID_IAudioSessionManager2.pointer, CLSCTX_ALL, Pointer.NULL, ppManager)
            if (hrActivate != S_OK) return false
            pManager = ppManager.value

            // IAudioSessionManager2::GetSessionEnumerator (vtable index 5)
            // (IAudioSessionEnumerator** ppSessionEnum)
            val ppSessionEnum = PointerByReference()
            val hrEnum = callCom(pManager, 5, ppSessionEnum)
            if (hrEnum != S_OK) return false
            pSessionEnum = ppSessionEnum.value

            // IAudioSessionEnumerator::GetCount (vtable index 3)
            // (int* SessionCount)
            val pCount = IntByReference()
            val hrCount = callCom(pSessionEnum, 3, pCount)
            if (hrCount != S_OK) return false

            val sessionCount = pCount.value
            var found = false

            for (i in 0 until sessionCount) {
                var pSession: Pointer? = null
                var pSessionControl2: Pointer? = null
                var pVolume: Pointer? = null
                try {
                    // IAudioSessionEnumerator::GetSession (vtable index 4)
                    val ppSession = PointerByReference()
                    val hrSession = callCom(pSessionEnum, 4, i, ppSession)
                    if (hrSession != S_OK) continue
                    pSession = ppSession.value

                    // QueryInterface for IAudioSessionControl2 (vtable index 0)
                    val ppControl2 = PointerByReference()
                    val hrQi = callCom(pSession, 0, IID_IAudioSessionControl2.pointer, ppControl2)
                    if (hrQi != S_OK) continue
                    pSessionControl2 = ppControl2.value

                    // IAudioSessionControl2::GetProcessId (vtable index 14)
                    val pSessionPid = IntByReference()
                    val hrPid = callCom(pSessionControl2, 14, pSessionPid)
                    if (hrPid != S_OK) continue

                    val currentPid = pSessionPid.value.toLong() and 0xFFFFFFFFL
                    if (currentPid == pid) {
                        // QueryInterface for ISimpleAudioVolume (vtable index 0)
                        val ppVol = PointerByReference()
                        val hrQiVol = callCom(pSession, 0, IID_ISimpleAudioVolume.pointer, ppVol)
                        if (hrQiVol == S_OK) {
                            pVolume = ppVol.value
                            val level = pct / 100.0f
                            // ISimpleAudioVolume::SetMasterVolume (vtable index 3)
                            val hrSet = callCom(pVolume, 3, level, Pointer.NULL)
                            if (hrSet == S_OK) {
                                found = true
                                break
                            }
                        }
                    }
                } finally {
                    release(pVolume)
                    release(pSessionControl2)
                    release(pSession)
                }
            }
            return found
        } catch (_: Throwable) {
            return false
        } finally {
            release(pSessionEnum)
            release(pManager)
            release(pDevice)
            release(pEnumerator)
            if (coInitialized) {
                try {
                    Ole32.INSTANCE.CoUninitialize()
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun callCom(pInterface: Pointer, vtableIndex: Int, vararg args: Any?): Int {
        val vtbl = pInterface.getPointer(0)
        val methodPtr = vtbl.getPointer(vtableIndex.toLong() * Native.POINTER_SIZE)
        val func = Function.getFunction(methodPtr, Function.ALT_CONVENTION)
        return func.invokeInt(arrayOf(pInterface, *args))
    }

    private fun release(pInterface: Pointer?) {
        if (pInterface != null && pInterface != Pointer.NULL) {
            try {
                callCom(pInterface, 2 /* IUnknown::Release */)
            } catch (_: Throwable) {
            }
        }
    }
}