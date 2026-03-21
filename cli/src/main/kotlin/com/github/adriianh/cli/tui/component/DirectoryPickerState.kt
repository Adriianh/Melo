package com.github.adriianh.cli.tui.component

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Represents a single entry in the directory listing.
 */
data class DirEntry(
    val name: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val isReadable: Boolean = true
) {
    companion object {
        fun parentDir() = DirEntry(name = "..", isDirectory = true, size = 0)
    }
}

/**
 * Immutable state for the directory picker overlay.
 * Allows browsing directories to select a path for settings like DOWNLOAD_PATH or CACHE_PATH.
 */
data class DirectoryPickerState(
    val currentDirectory: Path = Path.of(System.getProperty("user.home")),
    val entries: List<DirEntry> = emptyList(),
    val cursor: Int = 0,
    val scrollOffset: Int = 0,
    val visibleRows: Int = 15,
    val targetItem: SettingsItem? = null,
    val markedPaths: Set<String> = emptySet(),
    val isCreatingDir: Boolean = false,
    val newDirName: String = "",
    val isConfirmingDelete: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Refreshes the directory listing, showing only directories and a parent entry.
 */
fun DirectoryPickerState.refresh(): DirectoryPickerState {
    val dirEntries = mutableListOf<DirEntry>()

    // Parent directory entry
    if (currentDirectory.parent != null) {
        dirEntries.add(DirEntry.parentDir())
    }

    try {
        Files.list(currentDirectory).use { stream ->
            stream
                .filter { Files.isDirectory(it) }
                .sorted(compareBy { it.fileName.toString().lowercase() })
                .forEach { path ->
                    dirEntries.add(
                        DirEntry(
                            name = path.fileName.toString(),
                            isDirectory = true,
                            isReadable = Files.isReadable(path)
                        )
                    )
                }
        }
    } catch (_: IOException) {
        // Directory not readable, keep only parent entry
    }

    val newCursor = cursor.coerceIn(0, (dirEntries.size - 1).coerceAtLeast(0))
    return copy(entries = dirEntries, cursor = newCursor).adjustScroll()
}

/**
 * Moves the cursor one position up.
 */
fun DirectoryPickerState.cursorUp(): DirectoryPickerState {
    if (cursor <= 0) return this
    return copy(cursor = cursor - 1).adjustScroll()
}

/**
 * Moves the cursor one position down.
 */
fun DirectoryPickerState.cursorDown(): DirectoryPickerState {
    if (cursor >= entries.size - 1) return this
    return copy(cursor = cursor + 1).adjustScroll()
}

/**
 * Enters the directory under the cursor, or navigates up if ".." is selected.
 */
fun DirectoryPickerState.enter(): DirectoryPickerState {
    val entry = entries.getOrNull(cursor) ?: return this

    return if (entry.name == "..") {
        navigateUp()
    } else if (entry.isDirectory) {
        val target = currentDirectory.resolve(entry.name).toAbsolutePath().normalize()
        if (Files.isDirectory(target) && Files.isReadable(target)) {
            copy(currentDirectory = target, cursor = 0, scrollOffset = 0).refresh()
        } else {
            this
        }
    } else {
        this
    }
}

/**
 * Toggles whether the directory under the cursor is "marked" (selected for multi-select).
 */
fun DirectoryPickerState.toggleMark(): DirectoryPickerState {
    val entry = entries.getOrNull(cursor) ?: return this
    if (entry.name == "..") return this
    
    val path = currentDirectory.resolve(entry.name).toAbsolutePath().normalize().toString()
    return if (markedPaths.contains(path)) {
        copy(markedPaths = markedPaths - path)
    } else {
        copy(markedPaths = markedPaths + path)
    }
}

/**
 * Clears all currently marked paths.
 */
fun DirectoryPickerState.clearMarks(): DirectoryPickerState =
    copy(markedPaths = emptySet())


/**
 * Navigates to the parent directory, placing the cursor on the directory we came from.
 */
fun DirectoryPickerState.navigateUp(): DirectoryPickerState {
    val parent = currentDirectory.parent ?: return this
    val previousDirName = currentDirectory.fileName.toString()
    val newState = copy(currentDirectory = parent, cursor = 0, scrollOffset = 0).refresh()

    // Try to place cursor on the directory we came from
    val index = newState.entries.indexOfFirst { it.name == previousDirName }
    return if (index >= 0) {
        newState.copy(cursor = index).adjustScroll()
    } else {
        newState
    }
}

// ══════════════════════════════════════════════════════════════
// Mkdir functions
// ══════════════════════════════════════════════════════════════

/**
 * Starts the "create directory" mode with an empty name.
 */
fun DirectoryPickerState.startMkdir(): DirectoryPickerState =
    copy(isCreatingDir = true, newDirName = "", errorMessage = null)

/**
 * Cancels mkdir mode without creating anything.
 */
fun DirectoryPickerState.cancelMkdir(): DirectoryPickerState =
    copy(isCreatingDir = false, newDirName = "")

/**
 * Confirms directory creation. Returns the updated state with the directory listing refreshed.
 */
fun DirectoryPickerState.confirmMkdir(): DirectoryPickerState {
    if (newDirName.isBlank()) return cancelMkdir()

    val target = currentDirectory.resolve(newDirName)
    return try {
        Files.createDirectory(target)
        val refreshed = copy(isCreatingDir = false, newDirName = "", errorMessage = null).refresh()
        // Place cursor on the newly created directory
        val index = refreshed.entries.indexOfFirst { it.name == newDirName }
        if (index >= 0) refreshed.copy(cursor = index).adjustScroll() else refreshed
    } catch (e: IOException) {
        copy(errorMessage = "Cannot create: ${e.message}")
    }
}

/**
 * Appends a character to the new directory name.
 */
fun DirectoryPickerState.appendToNewDir(char: Char): DirectoryPickerState =
    copy(newDirName = newDirName + char)

/**
 * Removes the last character from the new directory name.
 */
fun DirectoryPickerState.backspaceNewDir(): DirectoryPickerState =
    copy(newDirName = newDirName.dropLast(1))

// ══════════════════════════════════════════════════════════════
// Delete functions
// ══════════════════════════════════════════════════════════════

/**
 * Starts the delete confirmation for the currently selected directory.
 */
fun DirectoryPickerState.startDelete(): DirectoryPickerState {
    val entry = entries.getOrNull(cursor)
    if (entry == null || entry.name == "..") return this
    return copy(isConfirmingDelete = true, errorMessage = null)
}

/**
 * Cancels delete confirmation.
 */
fun DirectoryPickerState.cancelDelete(): DirectoryPickerState =
    copy(isConfirmingDelete = false)

/**
 * Confirms deletion of the selected directory (only empty directories).
 */
fun DirectoryPickerState.confirmDelete(): DirectoryPickerState {
    val entry = entries.getOrNull(cursor) ?: return cancelDelete()
    if (entry.name == "..") return cancelDelete()

    val target = currentDirectory.resolve(entry.name)
    return try {
        Files.delete(target)
        copy(isConfirmingDelete = false, errorMessage = null).refresh()
    } catch (e: IOException) {
        copy(isConfirmingDelete = false, errorMessage = "Cannot delete: ${e.message}")
    }
}

/**
 * Clears any error message.
 */
fun DirectoryPickerState.clearError(): DirectoryPickerState =
    copy(errorMessage = null)

/**
 * Adjusts the scroll offset to keep the cursor visible within the viewport.
 */
private fun DirectoryPickerState.adjustScroll(): DirectoryPickerState {
    var newOffset = scrollOffset
    if (cursor < newOffset) {
        newOffset = cursor
    } else if (cursor >= newOffset + visibleRows) {
        newOffset = cursor - visibleRows + 1
    }
    return copy(scrollOffset = newOffset)
}
