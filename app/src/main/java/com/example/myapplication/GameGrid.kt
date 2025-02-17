package com.example.myapplication

import kotlin.random.Random

class GameGrid {
    private var grid: Array<IntArray> = emptyArray() // Add a field to store the grid
    private val rows = 10
    private val cols = 17

    init {
        resetGrid() // Initialize the grid when the GameGrid object is created
    }

    fun generateEvenlyDistributedRandomGrid(): Array<IntArray> {
        val grid = Array(rows) { IntArray(cols) }
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
                grid[i][j] = numbers[index]
                index++
            }
        }
        return grid
    }


    // Method to reset the grid (useful for starting a new game)
    fun resetGrid() {
        grid = generateEvenlyDistributedRandomGrid()
    }

    // Method to get the current grid (useful for the UI to access it)
    fun getGrid(): Array<IntArray> {
        return grid
    }
    // Method to remove selected cells (set their values to 0)
    fun removeCells(cells: Set<Pair<Int, Int>>) {
        for ((row, col) in cells) {
            if (row in 0 until rows && col in 0 until cols) { // Check bounds
                grid[row][col] = 0
            }
        }
    }

    // Method to check if there are any valid moves left (optional, for game over condition)
    fun hasValidMoves(): Boolean {
        for(i in 0 until rows){
            for(j in 0 until cols){
                // Check horizontally
                if (j + 1 < cols && grid[i][j] + grid[i][j+1] == 10) return true;
                // Check vertically
                if (i + 1 < rows && grid[i][j] + grid[i+1][j] == 10) return true;
                // Check diagonally (top-left to bottom-right)
                if (i + 1 < rows && j + 1 < cols && grid[i][j] + grid[i + 1][j + 1] == 10) return true;
                // Check diagonally (top-right to bottom-left)
                if (i + 1 < rows && j - 1 >= 0 && grid[i][j] + grid[i + 1][j - 1] == 10) return true;

            }
        }
        return false
    }

    // Method to drop down numbers to fill empty cells
    fun dropNumbers() {
        for (col in 0 until cols) {
            //From bottom to top
            var emptyRow = rows - 1
            for(row in rows -1 downTo 0){
                if(grid[row][col] != 0){
                    if(row != emptyRow){
                        grid[emptyRow][col] = grid[row][col]
                        grid[row][col] = 0
                    }
                    emptyRow--
                }
            }
        }
    }
    // Method to refill empty cells with random numbers
    fun refillGrid() {
        for (row in grid.indices) {
            for (col in grid[row].indices) {
                if (grid[row][col] == 0) {
                    grid[row][col] = Random.nextInt(1, 10) // Fill with random numbers 1-9
                }
            }
        }
    }
}