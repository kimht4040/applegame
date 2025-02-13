// StartMenuActivity.kt
package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class StartMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_menu) // 기존에 만들어둔 레이아웃 사용


        // findViewById 대신에 View Binding을 사용하거나,  id를 button1로 맞춰서 사용
        val startButton: Button = findViewById(R.id.button1)  // ID를 button1으로 변경
        startButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}