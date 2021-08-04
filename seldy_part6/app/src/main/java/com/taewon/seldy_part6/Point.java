package com.taewon.seldy_part6;

public class Point {
    public float x, y;
    public int color;
    public float strokeWidth;
    public boolean checked;
    public Point(float x, float y, int color, float strokeWidth, boolean checked){
        this.x = x;
        this.y = y;
        this.strokeWidth = strokeWidth;
        this.color = color;
        this.checked = checked;
    }
}
