package com.checkers.model;

public class Piece {
    private Types.PlayerColor color;
    private boolean isKing;

    public Piece(Types.PlayerColor color) {
        this.color = color;
        this.isKing = false;
    }

    public Types.PlayerColor getColor() { return color; }
    public boolean isKing() { return isKing; }
    public void promote() { this.isKing = true; }

    public Piece copy() {
        Piece p = new Piece(this.color);
        p.isKing = this.isKing;
        return p;
    }
}