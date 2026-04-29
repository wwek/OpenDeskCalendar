package org.opendeskcalendar.app.data;

public final class ErrorLogEntry {
    public final long timeMillis;
    public final String module;
    public final String message;

    public ErrorLogEntry(long timeMillis, String module, String message) {
        this.timeMillis = timeMillis;
        this.module = module;
        this.message = message;
    }
}
