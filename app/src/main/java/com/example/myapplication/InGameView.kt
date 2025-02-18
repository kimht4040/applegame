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
import android.widget.ProgressBar


class InGameView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val gameGrid = GameGrid()
    private var gridData: Array<IntArray> = emptyArray()
    private var cellSize: Float = 0f
    private val cellPadding: Float = 8f
    private var timerProgressBar: ProgressBar? = null
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

    private var countDownTimer: CountDownTimer? = null // CountDownTimer 변수 추가
    private var timeLeftInMillis: Long = 120000 // 60초 (원하는 시간으로 변경)
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


    fun setTimerProgressBar(progressBar: ProgressBar) { // ProgressBar 설정 새 함수
        timerProgressBar = progressBar
        timerProgressBar?.progress = (timeLeftInMillis / 1000).toInt() // 초기 진행률 설정
    }


    // 점수를 업데이트하고 TextView에 표시하는 함수
    // 점수를 업데이트하고 TextView에 표시하는 함수
    private fun updateScore(removedCellsCount: Int) {
        score += removedCellsCount // 제거된 셀 개수만큼 점수 증가 (수정됨)
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
        timeLeftInMillis = 120000 // 타이머 초기화
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
                val (currentRow, currentCol) = getCellAtPoint(event.x, event.y)
                if (currentRow != -1 && currentCol != -1) {
                    selectedCells.clear() // 기존 선택 영역 초기화 (사각형 영역 재정의)

                    val startCellRow = getCellAtPoint(startX, startY).first
                    val startCellCol = getCellAtPoint(startX, startY).second

                    if (startCellRow != -1 && startCellCol != -1) {
                        val minRow = minOf(startCellRow, currentRow)
                        val maxRow = maxOf(startCellRow, currentRow)
                        val minCol = minOf(startCellCol, currentCol)
                        val maxCol = maxOf(startCellCol, currentCol)

                        for (row in minRow..maxRow) {
                            for (col in minCol..maxCol) {
                                if (row in 0 until 10 && col in 0 until 17) { // 유효 범위 체크 추가
                                    selectedCells.add(Pair(row, col))
                                }
                            }
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
                    // 제거된 사과 개수 직접 계산 (수정됨)
                    var appleCount = 0
                    for ((row, col) in selectedCells) {
                        if (gridData[row][col] != 0) {
                            appleCount++
                        }
                    }
                    gameGrid.removeCells(selectedCells)
                    updateScore(appleCount) // 계산된 사과 개수를 updateScore()에 전달 (수정됨)

                    handler.removeCallbacks(updateGridRunnable)
                    handler.post(updateGridRunnable)

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
                updateCountdownText() // onTick에서 ProgressBar 업데이트
            }

            override fun onFinish() {
                timerRunning = false
                timeLeftInMillis = 0
                updateCountdownText()
                showGameOverDialog()
                handler.removeCallbacks(updateGridRunnable)
            }
        }.start()
        timerRunning = true
    }

    private fun stopTimer() {
        countDownTimer?.cancel()
        timerRunning = false
    }

    private fun updateCountdownText() {
        val progress = (timeLeftInMillis * 100 / 120000).toInt() // 진행률 퍼센트 계산 (0-100) - ProgressBar에는 불필요
        timerProgressBar?.progress = (timeLeftInMillis / 1000).toInt() // ProgressBar 진행률 (초 단위) 업데이트
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