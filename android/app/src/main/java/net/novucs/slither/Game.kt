package net.novucs.slither

import java.util.*
import kotlin.math.max

/**
 * The game controller, contains all the logic for ticking players and rewards.
 */
class Game(private val view: GameView,
           val player1: Player,
           val player2: Player) : Runnable {

    private val random = Random()
    private val rewards = LinkedList<Vector2i>()
    private var winner: Player? = null
    private var loser: Player? = null
    var state = GameState.CONNECT

    /**
     * Runs the game until the thread is interrupted.
     */
    override fun run() {
        reset()

        while (!Thread.interrupted()) {
            tick()
            view.snapshot.set(snapshot())
            view.invalidate()

            try {
                Thread.sleep(TICK_RATE)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
    }

    /**
     * Resets the game, updates all reward locations and resets all players.
     */
    private fun reset() {
        rewards.clear()

        for (i in 0 until REWARD_COUNT) {
            rewards.add(nextValidSpawn())
        }

        resetPlayer(player1)
        resetPlayer(player2)
    }

    /**
     * Resets a player. Sets their size back to minimum and spawns them in a
     * new location. Resets the players score back to zero.
     */
    private fun resetPlayer(player: Player) {
        player.body.clear()
        player.body.add(nextValidSpawn())
        player.score = 0
        player.growthTicks = (MIN_SIZE - 1)
    }

    /**
     * Gets an immutable snapshot of the game, used for rendering.
     */
    private fun snapshot(): GameSnapshot {
        return when (state) {
            GameState.CONNECT -> GameSnapshot.Connect("Waiting for players to connect...")
            GameState.PLAY -> GameSnapshot.Play(ArrayList(rewards),
                    LinkedList(player1.body), LinkedList(player2.body),
                    player1.score, player2.score)
            GameState.COMPLETE -> GameSnapshot.Complete("""
                |Game over! Winner: Player ${if (player1.score > player2.score) '1' else '2'}
                |Player 1: ${player1.score} points
                |Player 2: ${player2.score} points
                """.trimMargin(), player1.score, player2.score)
        }
    }

    /**
     * Gets the next valid spawn location that does not collide with any other
     * entities currently existing on the map.
     */
    private fun nextValidSpawn(): Vector2i {
        while (true) {
            val x = random.nextInt(MAP_WIDTH)
            val y = random.nextInt(MAP_HEIGHT)
            val location = Vector2i(x, y)

            if (!location.collides(player1.body) &&
                    !location.collides(player2.body) &&
                    !location.collides(rewards)) {
                return location
            }
        }
    }

    /**
     * Ticks the game. Handles all player movements when in play state, and
     * resets the game state back to playing after a set duration on completion.
     */
    private fun tick() {
        if (state == GameState.PLAY) {
            tickPlayer(player1, player2)
            tickPlayer(player2, player1)
        } else if (state == GameState.COMPLETE) {
            Thread.sleep(RESTART_MILLIS)
            reset()

            if (state == GameState.COMPLETE) {
                state = GameState.PLAY
            }
        }
    }

    /**
     * Ticks the player, validates their direction and moves the player
     * depending on their speed.
     */
    private fun tickPlayer(player: Player, opponent: Player) {
        // Player has not made a move from their starting position yet.
        val movement = player.direction.get()
        if (movement.isDefault) {
            return
        }

        for (i in 0 until player.speed.get()) {
            move(player, opponent, movement)
        }
    }

    /**
     * Handles all player movement logic. Collisions with map boundaries
     * results in being teleported to the other side of the map. More
     * segments are added on each movement when growing. Player is rewarded
     * when killing or eating. Once a player has one, updates the game state
     * to complete.
     */
    private fun move(player: Player, opponent: Player, movement: Vector2i) {
        val head = player.body.last.add(movement)

        // Wrap movements around the map.
        when {
            head.x > MAP_WIDTH ->
                head.x = 0

            head.x < 0 ->
                head.x = MAP_WIDTH

            head.y > MAP_HEIGHT ->
                head.y = 0

            head.y < 0 ->
                head.y = MAP_HEIGHT
        }

        player.body.addLast(head.copy())

        if (player.growthTicks > 0) {
            player.growthTicks -= 1
        } else {
            player.body.removeFirst()
        }

        // Reward the player when they consume a reward.
        val rewardIterator = rewards.iterator()
        while (rewardIterator.hasNext()) {
            val reward = rewardIterator.next()
            if (!head.collides(reward)) continue

            rewardIterator.remove()
            player.score += SCORE_REWARD_FOOD
            player.grow(GROWTH_REWARD_FOOD, MAX_SIZE)
            rewards.add(nextValidSpawn())
            checkWinner(player, opponent)
            break
        }

        // Reset the player if they collide with their opponent.
        if (head.collides(opponent.body)) {
            player.body.clear()
            player.body.add(nextValidSpawn())
            player.grow(MIN_SIZE, MAX_SIZE)
            player.score = max(player.score - SCORE_PENALTY_DEATH, MINIMUM_SCORE)
            opponent.score += SCORE_REWARD_KILL
            opponent.grow(GROWTH_REWARD_KILL, MAX_SIZE)
            checkWinner(opponent, player)
        }
    }

    /**
     * Checks if a player has won the game. Updates the game state and notifies
     * each of the client devices of the completion.
     */
    private fun checkWinner(player: Player, opponent: Player) {
        if (player.score < WINNING_SCORE) return

        state = GameState.COMPLETE
        winner = player
        loser = opponent
        player.connection?.sendMessage("WINNER!")
        opponent.connection?.sendMessage("LOSER!")
    }

    companion object {
        const val MAP_HEIGHT = 24
        const val MAP_WIDTH = 32

        const val PLAYER_COUNT = 2
        const val REWARD_COUNT = 5

        const val TICK_RATE = 250L

        const val MAX_SIZE = 20
        const val MIN_SIZE = 3

        const val GROWTH_REWARD_FOOD = 1
        const val GROWTH_REWARD_KILL = 5

        const val WINNING_SCORE = 100
        const val MINIMUM_SCORE = 0
        const val SCORE_REWARD_FOOD = 5
        const val SCORE_REWARD_KILL = 25
        const val SCORE_PENALTY_DEATH = 10

        const val RESTART_MILLIS = 5000L
    }
}
