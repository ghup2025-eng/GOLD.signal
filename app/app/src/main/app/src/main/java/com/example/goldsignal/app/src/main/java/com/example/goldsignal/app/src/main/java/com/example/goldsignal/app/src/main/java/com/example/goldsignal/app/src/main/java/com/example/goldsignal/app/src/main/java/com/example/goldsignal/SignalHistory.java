package com.example.goldsignal;

public class Signal {
    public enum Type { CALL, PUT, WAIT }
    public final Type type;
    public final double price;
    public final long timestamp;

    public Signal(Type type, double price) {
        this.type = type;
        this.price = price;
        this.timestamp = System.currentTimeMillis();
    }
}
