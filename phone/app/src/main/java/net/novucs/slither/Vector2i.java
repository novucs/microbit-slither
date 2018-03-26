package net.novucs.slither;

import android.graphics.Rect;

public class Vector2i {

    private int x;
    private int y;

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

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void add(int x, int y) {
        this.x += x;
        this.y += y;
    }

    public Rect toBlock(int width, int height) {
        return new Rect(x * width, y * height, (x + 1) * width, (y + 1) * height);
    }

    @Override
    public String toString() {
        return "Vector2i{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
