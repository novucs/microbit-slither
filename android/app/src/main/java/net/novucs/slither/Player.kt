package net.novucs.slither

import android.widget.ImageView
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class Player(val avatar: ImageView) {
    val direction = AtomicReference(Vector2i())
    val speed = AtomicInteger(1)
    val body: Deque<Vector2i> = LinkedList()
    var connection: PlayerConnection? = null
    var growthTicks = 0
    var score = 0

    fun decrementGrowthTicks() {
        this.growthTicks--
    }

    fun reward() {
        score += 1
        growthTicks += 3
    }
}
