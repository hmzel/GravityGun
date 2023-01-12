package me.zelha.gravitygun;

public enum Mode {
    VELOCITY("§6Velocity"),
    TELEPORT("§5Teleport"),
    BLOCKS("§9Blocks");

    private final String check;

    private Mode(String check) {
        this.check = check;
    }

    public String getCheck() {
        return check;
    }
}
