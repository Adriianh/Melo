package com.github.adriianh.ytdlpipe;

import android.content.Context;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Runs yt-dlp inside the app via Chaquopy and returns the direct bestaudio URL
 * for a YouTube videoId, using the given Netscape-format cookies file content.
 * Used as an age-gate-capable fallback when the InnerTube clients refuse a
 * video with "Sign in to confirm your age".
 */
public final class YtDlpEngine {

    private final Context appContext;
    private volatile PyObject module;

    public YtDlpEngine(Context context) {
        this.appContext = context.getApplicationContext();
    }

    /**
     * @return the resolved audio stream URL, or empty string if extraction failed.
     */
    public String getAudioUrl(String videoId, String cookiesNetscape) {
        return getAudioUrl(videoId, cookiesNetscape, false, null);
    }

    /**
     * Resolves the audio URL. For age-restricted content, {@code quickJsRuntime}
     * must point at the bundled qjs binary: yt-dlp then enables
     * {@code youtube:js_use_po_token} so it can solve the BotGuard challenge and
     * clear the age-gate entirely on-device.
     *
     * @return the resolved audio stream URL, or empty string if extraction failed.
     */
    public String getAudioUrl(String videoId, String cookiesNetscape, boolean ageRestricted, File quickJsRuntime) {
        File cookieFile = writeCookies(cookiesNetscape);
        if (cookieFile == null) {
            return "";
        }
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(appContext));
        }
        PyObject mod = module;
        if (mod == null) {
            mod = Python.getInstance().getModule("ytdlp_engine");
            module = mod;
        }
        String quickJsPath = quickJsRuntime == null ? "" : quickJsRuntime.getAbsolutePath();
        Object result = mod.callAttr(
                "get_audio_url", videoId, cookieFile.getAbsolutePath(), ageRestricted, quickJsPath);
        return result == null ? "" : result.toString();
    }

    private File writeCookies(String cookiesNetscape) {
        if (cookiesNetscape == null || cookiesNetscape.isEmpty()) {
            return null;
        }
        try {
            File file = new File(appContext.getFilesDir(), "ytdlp_cookies.txt");
            try (FileOutputStream out = new FileOutputStream(file)) {
                out.write(cookiesNetscape.getBytes(StandardCharsets.UTF_8));
            }
            return file;
        } catch (Exception e) {
            return null;
        }
    }
}