package net.novucs.slither

import android.graphics.Rect

/**
 * Represents a two dimensional vector of integers.
 */
data class Vector2i constructor(var x: Int = 0, var y: Int = 0) {

    val isDefault: Boolean
        get() = x == 0 && y == 0

    /**
     * Adds two vectors together.
     *
     * @param other the other vector to add to this.
     */
    fun add(other: Vector2i): Vector2i {
        return Vector2i(x + other.x, y + other.y)
    }

    /**
     * Translates the position to a one sized block to be rendered to the game
     * view.
     *
     * @param width the block width.
     * @param height the block height.
     */
    fun toBlock(width: Int, height: Int): Rect {
        return Rect(x * width, y * height, (x + 1) * width, (y + 1) * height)
    }

    /**
     * Checks if another vector is the same or collides with this.
     *
     * @param other the other vector.
     */
    fun collides(other: Vector2i): Boolean {
        return x == other.x && other.y == this.y
    }

    /**
     * Checks if any of a collection of locations collides with this.
     *
     * @param locations the locations to check against this.
     */
    fun collides(locations: Collection<Vector2i>): Boolean {
        return locations.any { collides(it) }
    }
}
