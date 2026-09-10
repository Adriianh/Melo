import os
import shutil
import subprocess
import tempfile

# CPython's subprocess module is unavailable on Android (it crashes in
# _posixsubprocess). yt-dlp still needs to run the bundled quickjs (qjs)
# executable to solve JS challenges (nsig / GVS PO Token). We install a drop-in
# Popen replacement backed by Java's ProcessBuilder so the proven desktop flow
# (youtube:js_use_po_token=true with the quickjs runtime) works unchanged.


def _install_popen_shim():
    try:
        from com.github.adriianh.ytdlpipe import JavaProcRunner
        from java.util import ArrayList
        import yt_dlp.utils as _pkg
        import yt_dlp.utils._utils as _u

        class AndroidPopen:
            def __init__(self, args, *remaining, env=None, text=False, shell=False, **kwargs):
                self.__text_mode = bool(
                    text
                    or kwargs.get("encoding")
                    or kwargs.get("errors")
                    or kwargs.get("universal_newlines")
                )
                self._shell = bool(shell)
                self._argv = [str(a) for a in args]
                self._env = env if env is not None else os.environ.copy()
                self._exec_timeout_ms = 60_000
                self.returncode = None

            def _resolve(self):
                if self._shell or len(self._argv) == 1:
                    command = " ".join(self._argv)
                    self._argv = ["/system/bin/sh", "-c", command]
                    self._shell = False
                    return
                exe = self._argv[0]
                if "/" not in exe:
                    if exe == "qjs" and os.environ.get("MELO_QJS"):
                        self._argv[0] = os.environ["MELO_QJS"]
                        return
                    resolved = shutil.which(exe)
                    if resolved is None:
                        raise FileNotFoundError(2, "No such file or directory", exe)
                    self._argv[0] = resolved

            def communicate(self, input=None, timeout=None):
                self._resolve()
                stdin_bytes = None
                if input is not None:
                    stdin_bytes = input if isinstance(input, bytes) else str(input).encode("utf-8")
                effective_timeout = timeout if timeout is not None else self._exec_timeout_ms / 1000.0
                env_pairs = ["%s=%s" % (k, v) for k, v in self._env.items()]
                argv = ArrayList()
                for arg in self._argv:
                    argv.add(arg)
                result = JavaProcRunner.exec(
                    argv, stdin_bytes, env_pairs, int(effective_timeout * 1000), None
                )
                if result.error:
                    if "No such file" in result.error:
                        raise FileNotFoundError(2, result.error, self._argv[0])
                    raise RuntimeError(result.error)
                if result.timedOut:
                    raise subprocess.TimeoutExpired(self._argv, effective_timeout)
                self.returncode = result.exitCode
                out = bytes(result.stdout)
                err = bytes(result.stderr)
                if self.__text_mode:
                    out = out.decode("utf-8", "replace")
                    err = err.decode("utf-8", "replace")
                return out, err

            def communicate_or_kill(self, *args, **kwargs):
                try:
                    return self.communicate(*args, **kwargs)
                except BaseException:
                    try:
                        self.kill()
                    except BaseException:
                        pass
                    raise

            def kill(self, *, timeout=0):
                self.returncode = -9

            def wait(self, timeout=None):
                if self.returncode is None:
                    _, _ = self.communicate(timeout=timeout)
                return self.returncode

            def __enter__(self):
                return self

            def __exit__(self, exc_type, exc_val, exc_tb):
                return False

            @classmethod
            def run(cls, *args, timeout=None, input=None, **kwargs):
                with cls(*args, **kwargs) as proc:
                    default = "" if proc.__text_mode else b""
                    stdout, stderr = proc.communicate_or_kill(input=input, timeout=timeout)
                    return stdout or default, stderr or default, proc.returncode

        _u.Popen = AndroidPopen
        _pkg.Popen = AndroidPopen
    except Exception:
        pass


def get_audio_url(video_id, cookie_file, age_restricted, quickjs_path):
    from yt_dlp import YoutubeDL

    opts = {
        "format": "bestaudio/best",
        "noplaylist": True,
        "quiet": True,
        "no_warnings": True,
        "cookiefile": cookie_file,
        "cookiejar": None,
        "socket_timeout": 20,
        "retries": 1,
        "noplaylist": True,
    }

    if age_restricted and quickjs_path:
        os.environ["MELO_QJS"] = quickjs_path

        qjs_tmp = os.path.join(os.path.dirname(cookie_file), "qjs_tmp")
        os.makedirs(qjs_tmp, exist_ok=True)
        tempfile.tempdir = qjs_tmp

        opts["js_runtimes"] = {"quickjs": {}}
        opts["extractor_args"] = {"youtube": {"js_use_po_token": True}}
        opts["cachedir"] = os.path.join(os.path.dirname(cookie_file), "ytdlp_cache")

    url = ""
    with YoutubeDL(opts) as ydl:
        info = ydl.extract_info(
            "https://www.youtube.com/watch?v=" + video_id, download=False
        )
        if info:
            url = info.get("url") or ""
            if not url and info.get("requested_formats"):
                first = info["requested_formats"][0] or {}
                url = first.get("url") or ""
    return url


if os.environ.get("ANDROID_ROOT"):
    _install_popen_shim()