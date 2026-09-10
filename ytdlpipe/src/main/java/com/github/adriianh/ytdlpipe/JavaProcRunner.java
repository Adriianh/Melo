package com.github.adriianh.ytdlpipe;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Runs a native process on Android from Python. CPython's subprocess module is
 * not supported on Android (it would crash in _posixsubprocess), but yt-dlp
 * depends on executing the quickjs (qjs) runtime to solve JS challenges. This
 * class bridges that gap: Python calls {{@link #exec}} and gets stdout, stderr
 * and the exit code back, without ever using CPython subprocess.
 */
public final class JavaProcRunner {

    public static final class Result {
        public byte[] stdout = new byte[0];
        public byte[] stderr = new byte[0];
        public int exitCode = -1;
        public boolean timedOut;
        public String error;
    }

    private JavaProcRunner() {
    }

    public static Result exec(List<String> argv, byte[] stdinData, String[] envPairs, long timeoutMs, String cwd) {
        Result result = new Result();
        ProcessBuilder builder = new ProcessBuilder(argv);
        Map<String, String> env = builder.environment();
        if (envPairs != null) {
            for (String pair : envPairs) {
                int sep = pair.indexOf('=');
                if (sep > 0) {
                    env.put(pair.substring(0, sep), pair.substring(sep + 1));
                }
            }
        }
        if (cwd != null && !cwd.isEmpty()) {
            builder.directory(new File(cwd));
        }
        builder.redirectErrorStream(false);
        try {
            Process process = builder.start();
            ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
            ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();
            Thread stdoutThread = new Thread(() -> pump(process.getInputStream(), outBuffer));
            Thread stderrThread = new Thread(() -> pump(process.getErrorStream(), errBuffer));
            stdoutThread.start();
            stderrThread.start();

            OutputStream stdin = process.getOutputStream();
            if (stdinData != null && stdinData.length > 0) {
                stdin.write(stdinData);
                stdin.flush();
            }
            stdin.close();

            if (timeoutMs > 0) {
                long deadline = System.currentTimeMillis() + timeoutMs;
                while (process.isAlive() && System.currentTimeMillis() < deadline) {
                    Thread.sleep(50);
                }
                if (process.isAlive()) {
                    result.timedOut = true;
                    process.destroyForcibly();
                }
            }
            process.waitFor();
            stdoutThread.join(5000);
            stderrThread.join(5000);

            result.stdout = outBuffer.toByteArray();
            result.stderr = errBuffer.toByteArray();
            if (!result.timedOut) {
                result.exitCode = process.exitValue();
            }
        } catch (IOException e) {
            result.error = String.valueOf(e);
        } catch (Exception e) {
            result.error = String.valueOf(e);
        }
        return result;
    }

    private static void pump(InputStream in, ByteArrayOutputStream out) {
        byte[] buffer = new byte[8192];
        int read;
        try {
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (IOException ignored) {
            // The process may have exited before finishing the stream.
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }
}