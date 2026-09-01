package com.etherstories.escore.gui.terminal;

public record TerminalButton(String id, String label, String hover, String raw) {
    public static TerminalButton of(String id, String label) {
        return new TerminalButton(id, label, "", "");
    }

    public static TerminalButton of(String id, String label, String hover) {
        return new TerminalButton(id, label, hover == null ? "" : hover, "");
    }

    public static TerminalButton of(String id, String label, String hover, String raw) {
        return new TerminalButton(id, label, hover == null ? "" : hover, raw == null ? "" : raw);
    }
}
