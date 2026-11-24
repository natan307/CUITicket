package io.github.natan307.cuiticket.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** English time texts such as "5 minutes ago". */
public final class Time {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private Time() {
    }

    public static String ago(long timestamp) {
        long seconds = Math.max(0, (System.currentTimeMillis() - timestamp) / 1000);
        if (seconds < 60) {
            return "just now";
        }
        long minutes = seconds / 60;
        if (minutes < 60) {
            return plural(minutes, "minute");
        }
        long hours = minutes / 60;
        if (hours < 24) {
            return plural(hours, "hour");
        }
        return plural(hours / 24, "day");
    }

    public static String date(long timestamp) {
        return DATE.format(Instant.ofEpochMilli(timestamp));
    }

    private static String plural(long amount, String unit) {
        return amount + " " + unit + (amount == 1 ? "" : "s") + " ago";
    }
}
