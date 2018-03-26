package net.novucs.slither;

import android.graphics.Rect;

import java.util.Collection;

public class Vector2i {

    private final int x;
    private final int y;

    public Vector2i() {
        this(0, 0);
    }

    public Vector2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public Vector2i setX(int x) {
        return new Vector2i(x, y);
    }

    public int getY() {
        return y;
    }

    public Vector2i setY(int y) {
        return new Vector2i(x, y);
    }

    public Vector2i add(int x, int y) {
        return new Vector2i(this.x + x, this.y + y);
    }

    public Vector2i add(Vector2i other) {
        return add(other.x, other.y);
    }

    public Rect toBlock(int width, int height) {
        return new Rect(x * width, y * height, (x + 1) * width, (y + 1) * height);
    }

    public boolean isDefault() {
        return x == 0 && y == 0;
    }

    public boolean collides(Vector2i other) {
        return x == other.x && other.y == this.y;
    }

    public boolean collides(Collection<Vector2i> locations) {
        for (Vector2i other : locations) {
            if (collides(other)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Vector2i{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
