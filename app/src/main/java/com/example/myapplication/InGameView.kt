package com.example.myapplication

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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

    private val emptyCellPaint = Paint().apply { // 빈 셀을 위한 Paint
        color = Color.LTGRAY // 밝은 회색. 원하는 색상으로 변경 가능.
        style = Paint.Style.FILL
    }

    init {
        resetGrid()
    }

    private fun resetGrid() {
        gridData = gameGrid.getGrid()
        invalidate()
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

                // 값이 0이면 아무것도 그리지 않고 건너뜀 (투명하게 처리)
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
                    //이미 추가 된 경우에, 무시
                    if (!selectedCells.contains(Pair(row, col))) {
                        selectedCells.add(Pair(row, col))
                    }
                    endX = event.x
                    endY = event.y
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val sum = calculateSelectedSum()
                //Toast.makeText(context, "선택된 셀들의 합: $sum", Toast.LENGTH_SHORT).show()

                if (sum == 10) {
                    gameGrid.removeCells(selectedCells) // 셀 제거
                    gridData = gameGrid.getGrid()   //데이터 다시 가져옴
                    Toast.makeText(context, "10을 만들었습니다!", Toast.LENGTH_SHORT).show()

                }

                // 선택 해제 및 초기화
                startX = -1f
                startY = -1f
                endX = -1f
                endY = -1f
                selectedCells.clear()
                invalidate()
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
}