package net.novucs.slither

import java.util.*

interface GameSnapshot {

    val state: GameState

    class Connect(val message: String) : GameSnapshot {
        override val state: GameState
            get() = GameState.CONNECT
    }

    class Play(val rewards: List<Vector2i>,
               val player1: Deque<Vector2i>,
               val player2: Deque<Vector2i>,
               val score1: Int,
               val score2: Int) : GameSnapshot {
        override val state: GameState
            get() = GameState.PLAY
    }

    class Complete(val message: String,
                   val score1: Int,
                   val score2: Int) : GameSnapshot {
        override val state: GameState
            get() = GameState.COMPLETE
    }
}
