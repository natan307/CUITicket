package io.github.natan307.cuiticket.ticket;

import java.util.UUID;

/**
 * One message in a ticket's history.
 *
 * @param authorId   who wrote it (the console uses the all-zero id)
 * @param authorName name of the author at that time
 * @param message    the text
 * @param createdAt  time in milliseconds since the epoch
 * @param staff      true when it was written by staff, false when it was written by the ticket's owner
 */
public record TicketReply(UUID authorId, String authorName, String message, long createdAt, boolean staff) {
}
