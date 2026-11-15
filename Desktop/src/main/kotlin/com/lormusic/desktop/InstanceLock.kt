package com.lormusic.desktop

import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/** One writer per profile; subsequent launches forward open-file requests to the player. */
object InstanceLock {
    private var channel: FileChannel? = null
    private var lock: FileLock? = null
    private var server: ServerSocket? = null
    private val pending = ConcurrentLinkedQueue<List<String>>()
    @Volatile
    var handler: ((List<String>) -> Unit)? = null
        set(value) {
            field = value
            if (value != null) {
                while (true) {
                    val next = pending.poll() ?: break
                    value(next)
                }
            }
        }

    data class Endpoint(val port: Int, val token: String)

    data class Request(val token: String, val paths: List<String>)

    fun claim(args: Array<String>): Boolean {
        val endpoint = File(dataDir, "instance.json")
        channel =
            FileChannel.open(
                File(dataDir, "instance.lock").toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
            )
        lock = runCatching { channel!!.tryLock() }.getOrNull()
        if (lock == null) {
            try {
                repeat(15) {
                    val info =
                        runCatching { gson.fromJson(endpoint.readText(), Endpoint::class.java) }
                            .getOrNull()
                    if (info != null) {
                        try {
                            Socket(InetAddress.getLoopbackAddress(), info.port).use { socket ->
                                socket.getOutputStream().bufferedWriter().use { w ->
                                    w.write(
                                        gson
                                            .toJson(
                                                Request(
                                                    info.token,
                                                    args.filter { !it.startsWith("--") },
                                                )
                                            )
                                            .replace("\n", "")
                                    )
                                    w.newLine()
                                    w.flush()
                                }
                            }
                            return false
                        } catch (_: Exception) {}
                    }
                    Thread.sleep(100)
                }
                error(
                    "Другой экземпляр LorMusic ещё запускается. Повторите открытие через несколько секунд."
                )
            } finally {
                channel?.close()
            }
        }
        val token = UUID.randomUUID().toString()
        server = ServerSocket(0, 16, InetAddress.getLoopbackAddress())
        endpoint.writeText(gson.toJson(Endpoint(server!!.localPort, token)))
        endpoint.setReadable(false, false)
        endpoint.setReadable(true, true)
        Thread(
                {
                    while (!server!!.isClosed) {
                        try {
                            server!!.accept().use { socket ->
                                socket.soTimeout = 2000
                                val reader = socket.getInputStream().bufferedReader()
                                val line = StringBuilder()
                                while (line.length < 65536) {
                                    val c = reader.read()
                                    if (c < 0 || c == 10) break
                                    line.append(c.toChar())
                                }
                                val request = gson.fromJson(line.toString(), Request::class.java)
                                if (request.token == token) {
                                    val callback = handler
                                    if (callback != null) callback(request.paths)
                                    else pending.add(request.paths)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                },
                "LorMusic open-file listener",
            )
            .apply {
                isDaemon = true
                start()
            }
        Runtime.getRuntime()
            .addShutdownHook(
                Thread {
                    runCatching {
                        server?.close()
                        endpoint.delete()
                        lock?.release()
                        channel?.close()
                    }
                }
            )
        return true
    }
}
