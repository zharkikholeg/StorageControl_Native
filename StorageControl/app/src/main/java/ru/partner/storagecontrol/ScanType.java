package ru.partner.storagecontrol;

public enum ScanType {
    Scanner(0),
    Camera(1);

    private final int type;
    ScanType(int type) { this.type = type; }
    public int getValue() { return type; }
}
