package net.novucs.slither;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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

        switch (snapshot.getState()) {
            case CONNECT:
                drawGameConnect(canvas, (GameSnapshot.Connect) snapshot);
                break;
            case PLAY:
                drawGamePlay(canvas, (GameSnapshot.Play) snapshot);
                break;
            case COMPLETE:
                break;
        }
    }

    private void drawGameConnect(Canvas canvas, GameSnapshot.Connect snapshot) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(BlockType.BACKGROUND.getColor());
        canvas.drawPaint(paint);

        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(16);
        drawCenteredMessage(canvas, paint, snapshot.getMessage());

        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1);
        drawCenteredMessage(canvas, paint, snapshot.getMessage());
    }

    private void drawCenteredMessage(Canvas canvas, Paint paint, String message) {
        String[] lines = message.split("\n");
        Rect rect = new Rect();
        canvas.getClipBounds(rect);
        int canvasWidth = rect.width();
        int canvasHeight = rect.height();
        paint.setTextSize(64);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            paint.setTextAlign(Paint.Align.LEFT);
            paint.getTextBounds(line, 0, line.length(), rect);

            float x = (canvasWidth / 2f) - (rect.width() / 2f) - rect.left;
            float lineSpace = (rect.height()) * (i - lines.length + 1.5f);
            float y = (canvasHeight / 2f) + lineSpace - rect.bottom;

            canvas.drawText(line, x, y, paint);
            paint.setTextSize(48);
        }
    }

    private void drawGamePlay(Canvas canvas, GameSnapshot.Play snapshot) {
        int blockWidth = getWidth() / Game.MAP_WIDTH;
        int blockHeight = getHeight() / Game.MAP_HEIGHT;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(BlockType.BACKGROUND.getColor());
        canvas.drawPaint(paint);

        drawBlocks(canvas, BlockType.REWARD, snapshot.getRewards(), blockWidth, blockHeight);
        drawBlocks(canvas, BlockType.PLAYER_1, snapshot.getPlayer1(), blockWidth, blockHeight);
        drawBlocks(canvas, BlockType.PLAYER_2, snapshot.getPlayer2(), blockWidth, blockHeight);

        paint.setColor(BlockType.PLAYER_1_HEAD.getColor());
        canvas.drawRect(snapshot.getPlayer1().getLast().toBlock(blockWidth, blockHeight), paint);

        paint.setColor(BlockType.PLAYER_2_HEAD.getColor());
        canvas.drawRect(snapshot.getPlayer2().getLast().toBlock(blockWidth, blockHeight), paint);
    }

    private void drawBlocks(Canvas canvas, BlockType type, Collection<Vector2i> locations,
                            int blockWidth, int blockHeight) {
        paint.setColor(type.getColor());
        for (Vector2i location : locations) {
            canvas.drawRect(location.toBlock(blockWidth, blockHeight), paint);
        }
    }
}
