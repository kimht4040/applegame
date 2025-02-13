package com.example.myapplication

import kotlin.random.Random

class GameGrid : IGameGrid {

    private var grid: Array<IntArray> = emptyArray()
    private val rows = 10
    private val cols = 17
    private var onGridChangedListener: OnGridChangedListener? = null

    init {
        resetGrid()
    }

    override fun setOnGridChangedListener(listener: OnGridChangedListener?) {
        onGridChangedListener = listener
    }

    override fun getGrid(): Array<IntArray> {
        return grid
    }

    override fun resetGrid() {
        grid = generateEvenlyDistributedRandomGrid()
        // 변경: 깊은 복사해서 전달
        onGridChangedListener?.onGridChanged(grid.copyOf())
    }

    override fun removeCells(cells: Set<Pair<Int, Int>>) {
        if (cells.isEmpty()) return

        val tempGrid = grid.map { it.clone() }.toTypedArray() // 깊은 복사

        for ((row, col) in cells) {
            if (row in 0 until rows && col in 0 until cols && tempGrid[row][col] != 0) {
                tempGrid[row][col] = 0
            }
        }

        grid = tempGrid
        onGridChangedListener?.onGridChanged(grid.copyOf())
    }



    private fun generateEvenlyDistributedRandomGrid(): Array<IntArray> {
        val newGrid = Array(rows) { IntArray(cols) }
        val numbers = mutableListOf<Int>()

        val totalCells = rows * cols
        val countPerNumber = totalCells / 9
        val remainder = totalCells % 9

        for (i in 1..9) {
            repeat(countPerNumber) {
                numbers.add(i)
            }
        }
        for (i in 1..remainder) {
            numbers.add(i)
        }

        numbers.shuffle()

        var index = 0
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                newGrid[i][j] = numbers[index++]
            }
        }

        return newGrid
    }

}