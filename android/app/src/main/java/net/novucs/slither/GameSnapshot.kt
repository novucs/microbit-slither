package net.novucs.slither

import java.util.*

/**
 * An immutable snapshot of the game, used for rendering.
 */
interface GameSnapshot {

    val state: GameState

    /**
     * Displays the reconnecting message when in the connection state.
     */
    class Connect(val message: String) : GameSnapshot {
        override val state: GameState
            get() = GameState.CONNECT
    }

    /**
     * Displays all rewards, players and their scores when in play state.
     */
    class Play(val rewards: List<Vector2i>,
               val player1: Deque<Vector2i>,
               val player2: Deque<Vector2i>,
               val score1: Int,
               val score2: Int) : GameSnapshot {
        override val state: GameState
            get() = GameState.PLAY
    }

    /**
     * Displays a completion message and player scores when in complete state.
     */
    class Complete(val message: String,
                   val score1: Int,
                   val score2: Int) : GameSnapshot {
        override val state: GameState
            get() = GameState.COMPLETE
    }
}
