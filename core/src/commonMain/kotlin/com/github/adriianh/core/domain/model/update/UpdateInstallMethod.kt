package com.github.adriianh.core.domain.model.update

/**
 * How the running application was installed on disk.
 *
 * It drives two decisions: which release asset is the right one to download, and
 * whether the update can be applied in place or has to be handed to the user.
 */
enum class UpdateInstallMethod {
    /** Linux: self-contained AppImage; the file exported as `$APPIMAGE` is the install target. */
    APPIMAGE,

    /** Portable install laid out by `install.sh`, located through its `.melo-install` marker. */
    TARBALL,

    /** Owned by the system package manager (`.deb`/`.rpm`): it cannot be replaced in place. */
    PACKAGE_MANAGER,

    /** Android sideloaded APK. */
    APK,

    /** Anything else: hand the downloaded file over to the operating system. */
    UNKNOWN,
}
