package com.checkers.model;

public class Player {
    private String name;
    private Types.PlayerColor color;
    private Types.PlayerType type;
    private int elapsedSeconds;

    public Player(String name, Types.PlayerColor color, Types.PlayerType type) {
        this.name = name;
        this.color = color;
        this.type = type;
        this.elapsedSeconds = 0;
    }

    public String getName() { return name; }
    public Types.PlayerColor getColor() { return color; }
    public Types.PlayerType getType() { return type; }
    
    public int getElapsedSeconds() { return elapsedSeconds; }
    public void incrementTime() { this.elapsedSeconds++; }

    public Player copy() {
        Player p = new Player(this.name, this.color, this.type);
        p.elapsedSeconds = this.elapsedSeconds;
        return p;
    }
}