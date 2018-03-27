package net.novucs.slither;

import android.graphics.Color;

public enum BlockType {

    BACKGROUND(Color.parseColor("#551A8B")),
    REWARD(Color.parseColor("#ffd700")),
    PLAYER_1(Color.parseColor("#6fa3de")),
    PLAYER_1_HEAD(Color.BLUE),
    PLAYER_2(Color.parseColor("#de6f6f")),
    PLAYER_2_HEAD(Color.RED);

    private final int color;

    BlockType(int color) {
        this.color = color;
    }

    public int getColor() {
        return color;
    }
}
