package net.novucs.slither;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

public class GameView extends View {

    private final Paint paint = new Paint();
    private final AtomicReference<GameSnapshot> snapshot = new AtomicReference<>();

    public GameView(Context context) {
        super(context);
    }

    public GameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GameView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public AtomicReference<GameSnapshot> getSnapshot() {
        return snapshot;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        GameSnapshot snapshot = this.snapshot.get();
        if (snapshot == null) {
            return;
        }

        int blockWidth = getWidth() / Game.MAP_WIDTH;
        int blockHeight = getHeight() / Game.MAP_HEIGHT;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(BlockType.BACKGROUND.getColor());
        canvas.drawPaint(paint);

        drawBlocks(canvas, BlockType.REWARD, snapshot.getRewards(), blockWidth, blockHeight);
        drawBlocks(canvas, BlockType.PLAYER_1, snapshot.getPlayer1(), blockWidth, blockHeight);
        drawBlocks(canvas, BlockType.PLAYER_2, snapshot.getPlayer2(), blockWidth, blockHeight);
    }

    private void drawBlocks(Canvas canvas, BlockType type, Collection<Vector2i> locations,
                            int blockWidth, int blockHeight) {
        paint.setColor(type.getColor());
        for (Vector2i location : locations) {
            canvas.drawRect(location.toBlock(blockWidth, blockHeight), paint);
        }
    }
}
