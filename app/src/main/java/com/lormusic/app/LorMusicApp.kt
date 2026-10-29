package com.lormusic.app

import android.app.Application
import android.content.Intent
import android.os.Process
import android.util.Log
import java.io.File
import kotlin.system.exitProcess

class LorMusicApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Log.e("LorMusicCrash", "Uncaught exception", throwable)
                val crashText = Log.getStackTraceString(throwable)
                try {
                    val file = File(getExternalFilesDir(null) ?: filesDir, "crash.txt")
                    file.writeText(crashText)
                } catch (_: Throwable) {
                }

                val intent = Intent(this, CrashActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("crash_stacktrace", crashText)
                }
                startActivity(intent)
                Process.killProcess(Process.myPid())
                exitProcess(10)
            } catch (t: Throwable) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
