package io.github.natan307.cuiticket.ticket;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A ticket. Tickets are changed only through {@link TicketManager}; other plugins should treat
 * them as read-only.
 */
public final class Ticket {

    private final int number;
    private final UUID playerId;
    private final String categoryId;
    private final String target;
    private final String reason;
    private final String evidence;
    private final long createdAt;
    private final List<TicketReply> replies = new CopyOnWriteArrayList<>();

    private volatile String playerName;
    private volatile TicketStatus status;
    private volatile UUID assignedTo;
    private volatile String assignedName;
    private volatile long updatedAt;
    private volatile boolean playerUnread;

    public Ticket(int number, UUID playerId, String playerName, String categoryId, String target, String reason,
                  String evidence, TicketStatus status, UUID assignedTo, String assignedName, long createdAt,
                  long updatedAt, boolean playerUnread) {
        this.number = number;
        this.playerId = playerId;
        this.playerName = playerName;
        this.categoryId = categoryId;
        this.target = target;
        this.reason = reason;
        this.evidence = evidence;
        this.status = status;
        this.assignedTo = assignedTo;
        this.assignedName = assignedName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.playerUnread = playerUnread;
    }

    /** The number, for example 7. */
    public int number() {
        return number;
    }

    /** The id players and staff see, for example {@code TKT-007}. */
    public String id() {
        return idOf(number);
    }

    public static String idOf(int number) {
        return String.format("TKT-%03d", number);
    }

    public UUID playerId() {
        return playerId;
    }

    public String playerName() {
        return playerName;
    }

    public String categoryId() {
        return categoryId;
    }

    /** The reported player's name, or null when the ticket is not about a player. */
    public String target() {
        return target;
    }

    public String reason() {
        return reason;
    }

    /** A link or note with proof, or null. */
    public String evidence() {
        return evidence;
    }

    public TicketStatus status() {
        return status;
    }

    /** The staff member handling the ticket, or null. */
    public UUID assignedTo() {
        return assignedTo;
    }

    public String assignedName() {
        return assignedName;
    }

    public long createdAt() {
        return createdAt;
    }

    public long updatedAt() {
        return updatedAt;
    }

    /** True when the owner has not yet seen the latest reply or the closing of this ticket. */
    public boolean playerUnread() {
        return playerUnread;
    }

    /** The history, oldest first. The list is a snapshot. */
    public List<TicketReply> replies() {
        return List.copyOf(replies);
    }

    // ---- package-private: used by TicketManager and the storage classes

    public void restoreReply(TicketReply reply) {
        replies.add(reply);
    }

    void addReply(TicketReply reply) {
        replies.add(reply);
    }

    void setStatus(TicketStatus status) {
        this.status = status;
    }

    void setAssigned(UUID uuid, String name) {
        this.assignedTo = uuid;
        this.assignedName = name;
    }

    void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    void setPlayerUnread(boolean unread) {
        this.playerUnread = unread;
    }

    void setPlayerName(String name) {
        this.playerName = name;
    }
}
