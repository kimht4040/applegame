// MainActivity.kt
package com.example.myapplication
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var timerProgressBar: ProgressBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val inGameView: InGameView = findViewById(R.id.inGameView)
        val scoreTextView: TextView = findViewById(R.id.scoreTextView)

        val timerProgressBar: ProgressBar = findViewById(R.id.timerProgressBar)
        inGameView.setScoreTextView(scoreTextView)
       // InGameView에 타이머 TextView 설정
        inGameView.setTimerProgressBar(timerProgressBar)
    }
}