package net.novucs.slither

import android.graphics.Rect

data class Vector2i constructor(var x: Int = 0, var y: Int = 0) {

    val isDefault: Boolean
        get() = x == 0 && y == 0

    fun add(other: Vector2i): Vector2i {
        return Vector2i(x + other.x, y + other.y)
    }

    fun toBlock(width: Int, height: Int): Rect {
        return Rect(x * width, y * height, (x + 1) * width, (y + 1) * height)
    }

    fun collides(other: Vector2i): Boolean {
        return x == other.x && other.y == this.y
    }

    fun collides(locations: Collection<Vector2i>): Boolean {
        return locations.any { collides(it) }
    }
}
