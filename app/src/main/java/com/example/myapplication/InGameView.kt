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
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import kotlin.math.abs

interface IGameGrid {
    fun getGrid(): Array<IntArray>
    fun removeCells(cells: Set<Pair<Int, Int>>)
    fun resetGrid()
    fun setOnGridChangedListener(listener: OnGridChangedListener?)
}

interface OnGridChangedListener {
    fun onGridChanged(newGrid: Array<IntArray>)
    fun onGameOver() // 이 함수는 이제 시간 초과 시에만 호출됩니다.
}

class InGameView(context: Context, attrs: AttributeSet?) : View(context, attrs), OnGridChangedListener {

    companion object {
        private const val GRID_ROWS = 10
        private const val GRID_COLS = 17
        private const val CELL_PADDING = 8f
        private const val TEXT_SIZE = 60f
        private const val STROKE_WIDTH = 5f
        private const val INITIAL_TIME_MILLIS = 60000L
    }

    private var gameGrid: IGameGrid = GameGrid()
    private var gridData: Array<IntArray> = emptyArray()
    private var cellSize: Float = 0f

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = TEXT_SIZE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val selectedRectPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.teal_700)
        strokeWidth = STROKE_WIDTH
    }

    private var startX = -1f
    private var startY = -1f
    private var endX = -1f
    private var endY = -1f

    private val appleBitmap: Bitmap =
        BitmapFactory.decodeResource(resources, R.drawable.apple2)

    private val selectedCells = mutableSetOf<Pair<Int, Int>>()

    private val emptyCellPaint = Paint().apply {
        color = Color.LTGRAY
        style = Paint.Style.FILL
    }

    private var score = 0
    private var scoreTextView: TextView? = null
    private var timerTextView: TextView? = null
    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = INITIAL_TIME_MILLIS
    private var timerRunning: Boolean = false
    private val handler = Handler(Looper.getMainLooper())


    fun setScoreTextView(textView: TextView) {
        scoreTextView = textView
        updateScoreDisplay()
    }

    fun setTimerTextView(textView: TextView) {
        timerTextView = textView
        updateCountdownText()
    }

    private fun updateScore(removedCellsCount: Int) {
        score += removedCellsCount * removedCellsCount
        updateScoreDisplay()
    }

    private fun updateScoreDisplay() {
        scoreTextView?.text = "Score: $score"
    }

    private fun isAdjacent(cell1: Pair<Int, Int>, cell2: Pair<Int, Int>): Boolean {
        val rowDiff = abs(cell1.first - cell2.first)
        val colDiff = abs(cell1.second - cell2.second)
        return (rowDiff == 1 && colDiff == 0) || (rowDiff == 0 && colDiff == 1)
    }

    private fun isValidSelection(newCell: Pair<Int, Int>): Boolean {
        if (selectedCells.isEmpty()) return true

        // 마지막으로 선택된 셀과 인접해있는지만 확인
        return selectedCells.any { isAdjacent(it, newCell) }
    }

    init {
        gameGrid.setOnGridChangedListener(this)
        resetGame()
    }

    private fun resetGame() {
        gameGrid.resetGrid()
        score = 0
        updateScoreDisplay()
        timeLeftInMillis = INITIAL_TIME_MILLIS
        updateCountdownText()
        startTimer()
        selectedCells.clear()
        invalidate()
    }

    override fun onGridChanged(newGrid: Array<IntArray>) {
        gridData = newGrid.copyOf()
        handler.post {
            invalidate()
            //  if (!gameGrid.hasValidMoves()) { // 이 부분 제거
            //      onGameOver()
            //  }
        }
    }


    override fun onGameOver() {
        handler.post {
            Toast.makeText(context, "게임 오버!", Toast.LENGTH_LONG).show()
            stopTimer()
            showGameOverDialog()
        }

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        cellSize = if (w > 0 && h > 0) {
            (w / GRID_COLS.toFloat()).coerceAtMost(h / GRID_ROWS.toFloat())
        } else {
            0f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (gridData.isEmpty()) return

        val gridWidth = cellSize * GRID_COLS
        val gridHeight = cellSize * GRID_ROWS
        val startX = (width - gridWidth) / 2
        val startY = (height - gridHeight) / 2

        val scaledAppleBitmap = Bitmap.createScaledBitmap(
            appleBitmap,
            (cellSize - 2 * CELL_PADDING).toInt(),
            (cellSize - 2 * CELL_PADDING).toInt(),
            true
        )

        for (row in gridData.indices) {
            for (col in gridData[row].indices) {
                val x = startX + col * cellSize + CELL_PADDING
                val y = startY + row * cellSize + CELL_PADDING

                if (gridData[row][col] == 0) {
                    canvas.drawRect(
                        x, y, x + cellSize - 2 * CELL_PADDING, y + cellSize - 2 * CELL_PADDING,
                        emptyCellPaint
                    )
                } else {
                    canvas.drawBitmap(scaledAppleBitmap, x, y, null)
                    val text = gridData[row][col].toString()
                    val textX = x + scaledAppleBitmap.width / 2f
                    val textY =
                        y + scaledAppleBitmap.height / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
                    canvas.drawText(text, textX, textY, textPaint)
                }
            }
        }

        if (selectedCells.isNotEmpty()) {
            val minRow = selectedCells.minOf { it.first }
            val maxRow = selectedCells.maxOf { it.first }
            val minCol = selectedCells.minOf { it.second }
            val maxCol = selectedCells.maxOf { it.second }

            val drawStartX = startX + minCol * cellSize + CELL_PADDING
            val drawStartY = startY + minRow * cellSize + CELL_PADDING
            val drawEndX = startX + (maxCol + 1) * cellSize - CELL_PADDING
            val drawEndY = startY + (maxRow + 1) * cellSize - CELL_PADDING

            canvas.drawRect(drawStartX, drawStartY, drawEndX, drawEndY, selectedRectPaint)
        }
    }



    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                selectedCells.clear()
                val (row, col) = getCellAtPoint(event.x, event.y)
                if (row != -1 && col != -1) {
                    startX = event.x
                    startY = event.y
                    selectedCells.add(Pair(row, col))
                    println("Selected first cell at ($row, $col) with value ${gridData[row][col]}")
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val (row, col) = getCellAtPoint(event.x, event.y)
                if (row != -1 && col != -1) {
                    val newCell = Pair(row, col)
                    if (!selectedCells.contains(newCell) && isValidSelection(newCell)) {
                        val lastCell = selectedCells.last()
                        val cellsInPath = getCellsInPath(lastCell, newCell)

                        for (cell in cellsInPath) {
                            if (!selectedCells.contains(cell) && isValidSelection(cell)) {
                                selectedCells.add(cell)
                                println("Added cell in path: $cell with value ${gridData[cell.first][cell.second]}")
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
                println("Final selection - Cells: $selectedCells, Sum: $sum")

                if (sum == 10) {
                    val nonEmptyCellCount = selectedCells.count { (row, col) -> gridData[row][col] != 0 }

                    // 빈 셀만 선택된 경우 실행 안 함
                    if (nonEmptyCellCount > 0) {
                        gameGrid.removeCells(selectedCells)
                        updateScore(nonEmptyCellCount)
                    }
                }

                selectedCells.clear()
                invalidate()
                return true
            }

        }
        return super.onTouchEvent(event)
    }
    private fun logSelectionState() {
        println("Selected cells: $selectedCells")
        println("Path valid: ${isValidPath()}")
        println("Sum: ${calculateSelectedSum()}")
    }

    private fun isValidPath(): Boolean {
        if (selectedCells.size < 2) return false

        // 경로의 연속성만 확인
        val cellsList = selectedCells.toList()
        for (i in 0 until cellsList.size - 1) {
            if (!isAdjacent(cellsList[i], cellsList[i + 1])) {
                return false
            }
        }

        // 선택된 셀들 중 최소한 하나는 숫자가 있어야 함
        return selectedCells.any { (row, col) -> gridData[row][col] != 0 }
    }

    private fun getCellsInPath(from: Pair<Int, Int>, to: Pair<Int, Int>): List<Pair<Int, Int>> {
        val cells = mutableListOf<Pair<Int, Int>>()

        val rowStep = if (to.first > from.first) 1 else if (to.first < from.first) -1 else 0
        val colStep = if (to.second > from.second) 1 else if (to.second < from.second) -1 else 0

        var currentRow = from.first
        var currentCol = from.second

        while (currentRow != to.first || currentCol != to.second) {
            currentRow += rowStep
            currentCol += colStep
            if (currentRow in 0 until GRID_ROWS && currentCol in 0 until GRID_COLS) {
                cells.add(Pair(currentRow, currentCol))
            }
        }

        return cells
    }


    private fun getCellAtPoint(x: Float, y: Float): Pair<Int, Int> {
        val gridWidth = cellSize * GRID_COLS
        val gridHeight = cellSize * GRID_ROWS
        val startX = (width - gridWidth) / 2
        val startY = (height - gridHeight) / 2

        // 수정: x에서 startX를 빼고, y에서 startY를 빼도록 순서 변경
        val col = ((x - startX) / cellSize).toInt()
        val row = ((y - startY) / cellSize).toInt()

        return if (row in 0 until GRID_ROWS && col in 0 until GRID_COLS) {
            Pair(row, col)
        } else {
            Pair(-1, -1)
        }
    }

    private fun calculateSelectedSum(): Int {
        return selectedCells.sumOf { (row, col) ->
            if (row in gridData.indices && col in gridData[0].indices && gridData[row][col] != 0) {
                gridData[row][col]
            } else 0
        }
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
                showGameOverDialog()

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
            .setMessage("시간 초과") // 메시지 수정
            .setCancelable(false)
            .setPositiveButton("다시 시작") { _, _ ->
                resetGame()
            }
            .setNegativeButton("종료") { _, _ ->
                (context as? MainActivity)?.finish()
            }
            .show()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        countDownTimer?.cancel()
        gameGrid.setOnGridChangedListener(null)
    }
}