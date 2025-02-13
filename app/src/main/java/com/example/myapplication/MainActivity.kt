package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // InGameView와 TextView를 찾아서 연결
        val inGameView: InGameView = findViewById(R.id.inGameView)
        val scoreTextView: TextView = findViewById(R.id.scoreTextView)

        inGameView.setScoreTextView(scoreTextView) // InGameView에 TextView 설정
    }
}