// MainActivity.kt
package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val inGameView: InGameView = findViewById(R.id.inGameView)
        val scoreTextView: TextView = findViewById(R.id.scoreTextView)
        val timerTextView: TextView = findViewById(R.id.countdown_text) // 타이머 TextView 추가

        inGameView.setScoreTextView(scoreTextView)
        inGameView.setTimerTextView(timerTextView) // InGameView에 타이머 TextView 설정

    }
}