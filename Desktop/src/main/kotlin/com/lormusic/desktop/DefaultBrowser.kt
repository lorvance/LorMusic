package com.lormusic.desktop

import java.io.File
import java.util.concurrent.TimeUnit

data class BrowserChoice(val executable: String, val chromium: Boolean, val name: String)

object DefaultBrowser {
    private fun command(vararg args: String): String? =
        runCatching {
                val p = ProcessBuilder(*args).redirectError(ProcessBuilder.Redirect.DISCARD).start()
                if (!p.waitFor(4, TimeUnit.SECONDS)) {
                    p.destroyForcibly()
                    return@runCatching null
                }
                p.inputStream
                    .bufferedReader()
                    .use { it.readText() }
                    .trim()
                    .takeIf { p.exitValue() == 0 }
            }
            .getOrNull()

    fun parseExecutable(exec: String): String? {
        val tokens =
            Regex("\"([^\"]+)\"|'([^']+)'|([^\\s]+)")
                .findAll(exec)
                .map { m -> m.groupValues.drop(1).first { it.isNotEmpty() } }
                .toList()
        return tokens
            .dropWhile { it == "env" || it.contains('=') }
            .firstOrNull()
            ?.takeIf { !it.startsWith('%') }
    }

    fun classify(path: String): BrowserChoice {
        val name = File(path).name.lowercase()
        return BrowserChoice(
            path,
            listOf("chrome", "chromium", "msedge", "microsoft-edge", "brave", "vivaldi", "opera")
                .any { name.contains(it) },
            name,
        )
    }

    private fun locate(path: String): String? =
        if (File(path).isAbsolute) path.takeIf { File(it).isFile }
        else
            System.getenv("PATH")
                .orEmpty()
                .split(File.pathSeparator)
                .map { File(it, path) }
                .firstOrNull { it.canExecute() }
                ?.absolutePath

    fun detect(): BrowserChoice? {
        if (System.getProperty("os.name").startsWith("Windows")) {
            val raw =
                command(
                    "reg",
                    "query",
                    "HKCU\\Software\\Microsoft\\Windows\\Shell\\Associations\\UrlAssociations\\https\\UserChoice",
                    "/v",
                    "ProgId",
                ) ?: return null
            val id =
                Regex("ProgId\\s+REG_SZ\\s+(.+)", RegexOption.IGNORE_CASE)
                    .find(raw)
                    ?.groupValues
                    ?.get(1)
                    ?.trim() ?: return null
            val text =
                command("reg", "query", "HKCR\\$id\\shell\\open\\command", "/ve") ?: return null
            val exec = text.substringAfter("REG_SZ", "").trim()
            return parseExecutable(exec)?.takeIf { File(it).isFile }?.let(::classify)
        }
        val desktop =
            command("xdg-settings", "get", "default-web-browser")?.lineSequence()?.firstOrNull()
                ?: command("xdg-mime", "query", "default", "x-scheme-handler/https")
                ?: return null
        if (File(desktop).name != desktop || !desktop.endsWith(".desktop")) return null
        val roots =
            listOf(
                File(
                    System.getenv("XDG_DATA_HOME")
                        ?: "${System.getProperty("user.home")}/.local/share"
                )
            ) +
                System.getenv("XDG_DATA_DIRS")
                    .orEmpty()
                    .ifBlank { "/usr/local/share:/usr/share" }
                    .split(':')
                    .map(::File)
        val entry =
            roots.map { File(it, "applications/$desktop") }.firstOrNull { it.isFile } ?: return null
        val exec =
            entry.readLines().firstOrNull { it.startsWith("Exec=") }?.removePrefix("Exec=")
                ?: return null
        return parseExecutable(exec)?.let(::locate)?.let(::classify)
    }
}
