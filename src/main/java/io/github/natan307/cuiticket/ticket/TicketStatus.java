package io.github.natan307.cuiticket.ticket;

import java.util.Locale;

/** Where a ticket is in its life. */
public enum TicketStatus {
    /** Waiting for staff. */
    OPEN,
    /** Staff replied, the ticket is still being handled. */
    REPLIED,
    /** Finished (closed by staff or cancelled by the player). */
    CLOSED;

    /** The key of the message that names this status, for example {@code status.open}. */
    public String messageKey() {
        return "status." + name().toLowerCase(Locale.ROOT);
    }

    public boolean isClosed() {
        return this == CLOSED;
    }

    public static TicketStatus parse(String text, TicketStatus fallback) {
        if (text != null) {
            try {
                return valueOf(text.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }
        return fallback;
    }
}
