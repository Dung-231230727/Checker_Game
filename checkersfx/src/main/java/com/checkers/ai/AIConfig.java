package com.checkers.ai;

public class AIConfig {
    public enum Mode {
        ATTACK("Tấn công"), 
        DEFENSE("Phòng thủ"), 
        BALANCED("Cân bằng");

        private final String displayName;

        Mode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
    
    private Mode currentMode;
    private int searchDepth; 
    
    public double materialWeight;
    public double positionWeight;
    public double structureWeight;
    public double mobilityWeight;

    public AIConfig(Mode mode, int depth) {
        this.searchDepth = depth;
        setMode(mode);
    }

    public void setMode(Mode mode) {
        this.currentMode = mode;
        // Các trọng số này sẽ được nhân với "Intensity" nếu bạn muốn làm thêm thanh độ khó
        switch (mode) {
            case ATTACK:
                materialWeight = 1.0; positionWeight = 1.5; 
                structureWeight = 0.7; mobilityWeight = 1.2;
                break;
            case DEFENSE:
                materialWeight = 1.2; positionWeight = 0.8; 
                structureWeight = 1.8; mobilityWeight = 0.9;
                break;
            case BALANCED:
            default:
                materialWeight = 1.0; positionWeight = 1.0; 
                structureWeight = 1.0; mobilityWeight = 1.0;
                break;
        }
    }

    public int getSearchDepth() { return searchDepth; }
    public void setSearchDepth(int depth) { this.searchDepth = depth; }
    public Mode getCurrentMode() { return currentMode; }
}