package com.example.myapp.ui.dashboard;

public class LyricLine implements Comparable<LyricLine> {
    private final int timestamp;
    private final String text;

    public LyricLine(int timestamp, String text) {
        this.timestamp = Math.max(timestamp, 0);
        this.text = text == null ? "" : text;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public String getText() {
        return text;
    }

    @Override
    public int compareTo(LyricLine other) {
        if (other == null) {
            return 1;
        }
        return Integer.compare(this.timestamp, other.timestamp);
    }
}
