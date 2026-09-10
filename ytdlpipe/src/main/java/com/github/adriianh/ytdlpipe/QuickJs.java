package com.github.adriianh.ytdlpipe;

import android.content.Context;

import java.io.File;

/**
 * Locates the bundled quickjs (qjs) executable built for the device ABI.
 *
 * <p>The binary ships as a native library ({@code jniLibs/<abi>/libqjs.so}) so
 * Android's SELinux policy lets the app {@code exec} it. Binaries unpacked to
 * the app data dir are in {@code app_data_file} context, which modern Android
 * denies executing ({@code EACCES}) for {@code untrusted_app} domains.</p>
 */
public final class QuickJs {

    private QuickJs() {
    }

    /**
     * @return an executable qjs binary for this device, or null if unavailable.
     */
    public static File ensure(Context context) {
        File nativeLib = new File(context.getApplicationInfo().nativeLibraryDir, "libqjs.so");
        return nativeLib.isFile() ? nativeLib : null;
    }
}