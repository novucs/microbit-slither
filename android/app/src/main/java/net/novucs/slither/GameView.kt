package net.novucs.slither

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import java.util.concurrent.atomic.AtomicReference

class GameView : View {

    private val paint = Paint()
    val snapshot = AtomicReference<GameSnapshot>()

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val snapshot = this.snapshot.get() ?: return

        when (snapshot.state) {
            GameState.CONNECT -> drawMessage(canvas, (snapshot as GameSnapshot.Connect).message)
            GameState.PLAY -> {
                drawGamePlay(canvas, snapshot as GameSnapshot.Play)
                updatePlayerScore(R.id.playerText1, R.string.player1, snapshot.score1)
                updatePlayerScore(R.id.playerText2, R.string.player2, snapshot.score2)
            }
            GameState.COMPLETE -> {
                drawMessage(canvas, (snapshot as GameSnapshot.Complete).message)
                updatePlayerScore(R.id.playerText1, R.string.player1, snapshot.score1)
                updatePlayerScore(R.id.playerText2, R.string.player2, snapshot.score2)
            }
        }
    }

    private fun updatePlayerScore(viewId: Int, textId: Int, score: Int) {
        val view = (parent as View).findViewById<TextView?>(viewId)
        val text = resources.getString(textId, score)
        view?.text = text
    }

    private fun drawMessage(canvas: Canvas, message: String) {
        paint.style = Paint.Style.FILL
        paint.color = BlockType.BACKGROUND.color
        canvas.drawPaint(paint)

        paint.color = Color.WHITE
        paint.strokeWidth = 16f
        drawCenteredMessage(canvas, paint, message)

        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        drawCenteredMessage(canvas, paint, message)
    }

    private fun drawCenteredMessage(canvas: Canvas, paint: Paint, message: String) {
        val lines = message.split("\n")
        val rect = Rect()
        canvas.getClipBounds(rect)
        val canvasWidth = rect.width()
        val canvasHeight = rect.height()
        paint.textSize = 64f

        for (i in lines.indices) {
            val line = lines[i]
            paint.textAlign = Paint.Align.LEFT
            paint.getTextBounds(line, 0, line.length, rect)

            val x = canvasWidth / 2f - rect.width() / 2f - rect.left.toFloat()
            val lineSpace = rect.height() * (i - lines.size + 1.5f)
            val y = canvasHeight / 2f + lineSpace - rect.bottom

            canvas.drawText(line, x, y, paint)
            paint.textSize = 48f
        }
    }

    private fun drawGamePlay(canvas: Canvas, snapshot: GameSnapshot.Play) {
        val blockWidth = width / Game.MAP_WIDTH
        val blockHeight = height / Game.MAP_HEIGHT

        paint.style = Paint.Style.FILL
        paint.color = BlockType.BACKGROUND.color
        canvas.drawPaint(paint)

        drawBlocks(canvas, BlockType.REWARD, snapshot.rewards, blockWidth, blockHeight)
        drawBlocks(canvas, BlockType.PLAYER_1, snapshot.player1, blockWidth, blockHeight)
        drawBlocks(canvas, BlockType.PLAYER_2, snapshot.player2, blockWidth, blockHeight)

        paint.color = BlockType.PLAYER_1_HEAD.color
        canvas.drawRect(snapshot.player1.last.toBlock(blockWidth, blockHeight), paint)

        paint.color = BlockType.PLAYER_2_HEAD.color
        canvas.drawRect(snapshot.player2.last.toBlock(blockWidth, blockHeight), paint)
    }

    private fun drawBlocks(canvas: Canvas, type: BlockType, locations: Collection<Vector2i>,
                           blockWidth: Int, blockHeight: Int) {
        paint.color = type.color
        for (location in locations) {
            canvas.drawRect(location.toBlock(blockWidth, blockHeight), paint)
        }
    }
}
