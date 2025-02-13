// InGameView.kt  (전체 코드)
package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import android.widget.TextView
import android.os.CountDownTimer // CountDownTimer import
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlin.math.abs


class InGameView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val gameGrid = GameGrid()
    private var gridData: Array<IntArray> = emptyArray()
    private var cellSize: Float = 0f
    private val cellPadding: Float = 8f

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 60f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val selectedRectPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.teal_700)
        strokeWidth = 5f
    }

    private var startX = -1f
    private var startY = -1f
    private var endX = -1f
    private var endY = -1f

    private val appleBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.apple2)

    private val selectedCells = mutableSetOf<Pair<Int, Int>>() // 선택된 셀 (row, col) 저장

    private var score = 0 // 점수를 저장할 변수
    private var scoreTextView: TextView? = null //점수 TextView
    private var timerTextView: TextView? = null //타이머 TextView
    private var countDownTimer: CountDownTimer? = null // CountDownTimer 변수 추가
    private var timeLeftInMillis: Long = 60000 // 60초 (원하는 시간으로 변경)
    private var timerRunning: Boolean = false


    private val handler = Handler(Looper.getMainLooper()) // 메인 스레드의 Handler 생성

    // Runnable 정의: 그리드 업데이트 (숫자 떨어뜨리기, 다시 채우기, 화면 갱신)
    private val updateGridRunnable = object : Runnable {
        override fun run() {
            //gameGrid.dropNumbers() // 숫자들을 아래로 떨어뜨림
            // gameGrid.refillGrid()  // 이 부분을 제거하거나 주석 처리합니다.
            gridData = gameGrid.getGrid() // 변경된 그리드 데이터를 가져옴
            invalidate() // 화면을 다시 그림

            if (!gameGrid.hasValidMoves()) {
                // 더 이상 움직일 수 없으면 게임 오버
                Toast.makeText(context, "게임 오버!", Toast.LENGTH_LONG).show()
                handler.removeCallbacks(this) // Runnable 제거 (반복 중지)
                stopTimer() //타이머 종료

                return //run()메소드 종료
            }

            handler.postDelayed(this, 500) // 500ms 후에 다시 실행
        }
    }

    fun setScoreTextView(textView: TextView) {
        scoreTextView = textView
        updateScoreDisplay() // 초기 점수 표시
    }

    fun setTimerTextView(textView: TextView) {
        timerTextView = textView
        updateCountdownText() // 초기 타이머 텍스트 설정
    }



    // 점수를 업데이트하고 TextView에 표시하는 함수
    private fun updateScore(removedCellsCount: Int) {
        score += removedCellsCount * removedCellsCount// 제거된 셀 개수만큼 점수 증가
        updateScoreDisplay()
    }

    private fun updateScoreDisplay(){
        scoreTextView?.text = "$score" // TextView에 점수 표시
    }

    init {
        resetGrid()
        startTimer()

    }

    private fun resetGrid() {
        gameGrid.resetGrid()
        gridData = gameGrid.getGrid()
        invalidate()
        score = 0 // 점수 초기화
        updateScoreDisplay() // 점수 표시 업데이트
        timeLeftInMillis = 60000 // 타이머 초기화
        updateCountdownText()
        handler.postDelayed(updateGridRunnable, 500) // 게임 루프 시작
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cellSize = if (w > 0 && h > 0) {
            (w / 17f).coerceAtMost(h / 10f)
        } else {
            0f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (gridData.isEmpty()) return

        val gridWidth = cellSize * 17
        val gridHeight = cellSize * 10
        val startX = (width - gridWidth) / 2
        val startY = (height - gridHeight) / 2

        val scaledAppleBitmap = Bitmap.createScaledBitmap(
            appleBitmap,
            (cellSize - 2 * cellPadding).toInt(),
            (cellSize - 2 * cellPadding).toInt(),
            true
        )

        for (row in gridData.indices) {
            for (col in gridData[row].indices) {
                val x = startX + col * cellSize + cellPadding
                val y = startY + row * cellSize + cellPadding

                if (gridData[row][col] != 0) {
                    canvas.drawBitmap(scaledAppleBitmap, x, y, null)

                    val text = gridData[row][col].toString()
                    val textX = x + scaledAppleBitmap.width / 2f
                    val textY = y + scaledAppleBitmap.height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
                    canvas.drawText(text, textX, textY, textPaint)
                }
            }
        }

        //사각형 범위
        if (selectedCells.isNotEmpty()) {
            val minRow = selectedCells.minOf { it.first }
            val maxRow = selectedCells.maxOf { it.first }
            val minCol = selectedCells.minOf { it.second }
            val maxCol = selectedCells.maxOf { it.second }

            val drawStartX = startX + minCol * cellSize + cellPadding
            val drawStartY = startY + minRow * cellSize + cellPadding
            val drawEndX = startX + (maxCol + 1) * cellSize - cellPadding
            val drawEndY = startY + (maxRow + 1) * cellSize - cellPadding

            canvas.drawRect(drawStartX, drawStartY, drawEndX, drawEndY, selectedRectPaint)
        }

    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedCells.clear() // 선택 초기화
                val (row, col) = getCellAtPoint(event.x, event.y)
                if (row != -1 && col != -1) {
                    startX = event.x
                    startY = event.y
                    selectedCells.add(Pair(row, col))
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val (row, col) = getCellAtPoint(event.x, event.y)
                if (row != -1 && col != -1) {
                    // 새 셀이 이전에 선택된 셀과 인접한지 확인
                    val lastSelected = selectedCells.lastOrNull() // 마지막으로 선택된 셀
                    if (lastSelected != null && isAdjacent(lastSelected, Pair(row, col))) {
                        // 인접하고, 아직 추가되지 않은 경우에만 추가
                        if (!selectedCells.contains(Pair(row, col))) {
                            selectedCells.add(Pair(row, col))
                        }
                        endX = event.x
                        endY = event.y
                        invalidate()
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val sum = calculateSelectedSum()

                if (sum == 10) {
                    val removedCellsCount = selectedCells.size// 제거될 셀의 개수
                    gameGrid.removeCells(selectedCells) // 셀 제거
                    updateScore(removedCellsCount)       // 점수 업데이트

                    handler.removeCallbacks(updateGridRunnable) // 기존 Runnable 제거
                    handler.post(updateGridRunnable)             // 즉시 새 Runnable 실행 (드롭/리필 시작)

                } else {
                    // 합이 10이 아니면 선택 해제
                    startX = -1f
                    startY = -1f
                    endX = -1f
                    endY = -1f
                    selectedCells.clear()
                    invalidate()

                }
                return true

            }
        }
        return super.onTouchEvent(event)

    }
    // 두 셀이 인접한지 확인하는 함수
    private fun isAdjacent(cell1: Pair<Int, Int>, cell2: Pair<Int, Int>): Boolean {
        val (row1, col1) = cell1
        val (row2, col2) = cell2
        return abs(row1 - row2) <= 1 && abs(col1 - col2) <= 1
    }

    // 좌표를 셀 (row, col)로 변환하는 함수
    private fun getCellAtPoint(x: Float, y: Float): Pair<Int, Int> {
        val gridWidth = cellSize * 17
        val gridHeight = cellSize * 10
        val startX = (width - gridWidth) / 2
        val startY = (height - gridHeight) / 2

        val col = ((x - startX) / cellSize).toInt()
        val row = ((y - startY) / cellSize).toInt()

        return if (row in 0 until 10 && col in 0 until 17) {
            Pair(row, col)
        } else {
            Pair(-1, -1) // 유효하지 않은 셀
        }
    }

    // 선택된 셀들의 합을 계산하는 함수
    private fun calculateSelectedSum(): Int {
        var sum = 0
        for ((row, col) in selectedCells) {
            sum += gridData[row][col]
        }
        return sum
    }

    private fun startTimer() {
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateCountdownText()
            }

            override fun onFinish() {
                timerRunning = false
                timeLeftInMillis = 0
                updateCountdownText()
                showGameOverDialog() // 게임 종료 다이얼로그 표시
                handler.removeCallbacks(updateGridRunnable) //updateGridRunnable 중지

            }
        }.start()

        timerRunning = true
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        timerRunning = false
    }

    private fun updateCountdownText() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60

        val timeLeftFormatted = String.format("%02d:%02d", minutes, seconds)
        timerTextView?.text = timeLeftFormatted
    }

    private fun showGameOverDialog() {
        AlertDialog.Builder(context)
            .setTitle("게임 종료")
            .setMessage("시간이 초과되었습니다!")
            .setCancelable(false)
            .setPositiveButton("다시 시작") { _, _ ->
                resetGrid() // 그리드, 점수, 타이머 초기화
                startTimer() // 타이머 다시 시작

            }
            .setNegativeButton("종료") { _, _ ->
                // 필요하다면 액티비티 종료 또는 다른 작업 수행
                (context as? MainActivity)?.finish() // MainActivity 종료 시도
            }
            .show()
    }
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        countDownTimer?.cancel() // 뷰가 화면에서 제거될 때 타이머 취소
    }

}