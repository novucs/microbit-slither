package net.novucs.slither

import android.widget.ImageView
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.min

class Player(val avatar: ImageView) {
    val direction = AtomicReference(Vector2i())
    val speed = AtomicInteger(1)
    val body: Deque<Vector2i> = LinkedList()
    var connection: PlayerConnection? = null
    var growthTicks = 0
    var score = 0

    fun grow(bonus: Int, maxSize: Int) {
        growthTicks += min(maxSize - (body.size + growthTicks), bonus)
    }
}
