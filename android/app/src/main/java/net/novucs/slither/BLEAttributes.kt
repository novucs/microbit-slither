package net.novucs.slither

import java.util.*

object BLEAttributes {
    val ACCELEROMETER_SERVICE: UUID = UUID.fromString("e95d0753-251d-470a-a062-fa1922dfa9a8")
    val ACCELEROMETER_DATA_CHARACTERISTIC: UUID = UUID.fromString("e95dca4b-251d-470a-a062-fa1922dfa9a8")

    val SNAKE_MOVE_SERVICE: UUID = UUID.fromString("aab79343-9a83-4886-a1d3-32a800259937")
    val SNAKE_DIRECTION_CHARACTERISTIC: UUID = UUID.fromString("e4990f35-28f4-40d8-bfa2-f05118720a28")
    val SNAKE_SPEED_CHARACTERISTIC: UUID = UUID.fromString("e4990e35-28f4-40d8-bfa2-f05118720a28")
}
