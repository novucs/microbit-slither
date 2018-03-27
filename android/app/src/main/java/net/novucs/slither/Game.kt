package net.novucs.slither

import java.util.*

class Game(private val view: GameView,
           val player1: Player,
           val player2: Player) : Runnable {

    private val random = Random()
    private val rewards = LinkedList<Vector2i>()
    var state = GameState.CONNECT

    override fun run() {
        startPlay()

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

    private fun startPlay() {
        player1.body.clear()
        player2.body.clear()
        rewards.clear()

        for (i in 0 until INITIAL_REWARD_COUNT) {
            rewards.add(nextValidSpawn())
        }

        player1.body.add(nextValidSpawn())
        player2.body.add(nextValidSpawn())
    }

    private fun snapshot(): GameSnapshot {
        return when (state) {
            GameState.CONNECT -> GameSnapshot.Connect("Waiting for players to connect...")
            GameState.PLAY -> GameSnapshot.Play(ArrayList(rewards), LinkedList(player1.body), LinkedList(player2.body))
            else -> GameSnapshot.Complete("Game is complete?")
        }
    }

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

    private fun tick() {
        tickPlayer(player1, player2)
        tickPlayer(player2, player1)
    }

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
            player.decrementGrowthTicks()
        } else {
            player.body.removeFirst()
        }

        // Reward the player when they consume a reward.
        val rewardIterator = rewards.iterator()
        while (rewardIterator.hasNext()) {
            val reward = rewardIterator.next()
            if (head.collides(reward)) {
                rewardIterator.remove()
                player.reward()
                rewards.add(nextValidSpawn())
                break
            }
        }

        // Reset the player if they collide with their opponent.
        if (head.collides(opponent.body)) {
            player.body.clear()
            player.score = 0
            player.body.add(nextValidSpawn())
        }
    }

    companion object {
        const val MAP_HEIGHT = 24
        const val MAP_WIDTH = 32
        const val REQUIRED_PLAYER_COUNT = 2
        const val INITIAL_REWARD_COUNT = 5
        const val TICK_RATE = 250L
    }
}
