package com.lormusic.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class CrashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val stackTrace = intent.getStringExtra("crash_stacktrace") ?: "No stack trace provided"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 48)
            setBackgroundColor(0xFF121212.toInt())
        }

        val title = TextView(this).apply {
            text = "LorMusic: Ошибка приложения"
            textSize = 20f
            setTextColor(0xFFFF5252.toInt())
            setPadding(0, 0, 0, 20)
        }
        layout.addView(title)

        val copyButton = Button(this).apply {
            text = "📋 Скопировать текст ошибки"
            setOnClickListener {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Crash StackTrace", stackTrace)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this@CrashActivity, "Текст ошибки скопирован!", Toast.LENGTH_SHORT).show()
            }
        }
        layout.addView(copyButton)

        val restartButton = Button(this).apply {
            text = "Перезапустить приложение"
            setOnClickListener {
                val intent = Intent(this@CrashActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
        layout.addView(restartButton)

        val scrollView = ScrollView(this).apply {
            setPadding(0, 24, 0, 0)
        }

        val textView = TextView(this).apply {
            text = stackTrace
            textSize = 12f
            setTextColor(0xFFE0E0E0.toInt())
            setTextIsSelectable(true)
        }

        scrollView.addView(textView)
        layout.addView(scrollView)

        setContentView(layout)
    }
}
