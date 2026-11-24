package io.github.natan307.cuiticket.ticket;

import io.github.natan307.cuiticket.CUITicketPlugin;
import io.github.natan307.cuiticket.storage.TicketStorage;
import io.github.natan307.cuiticket.util.Chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Creates, finds and changes tickets. All tickets are kept in memory and written to the storage in
 * the background, so reading is instant and safe from any thread (including Folia region threads).
 *
 * <p>This is also the API for other plugins: {@code CUITicket.getTicketManager()}.</p>
 */
public final class TicketManager {

    private static final int TARGET_MAX = 32;
    private static final int EVIDENCE_MAX = 200;

    private final CUITicketPlugin plugin;
    private final TicketStorage storage;
    private final TreeMap<Integer, Ticket> tickets = new TreeMap<>();
    private int nextNumber = 1;

    public TicketManager(CUITicketPlugin plugin, TicketStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    /** Reads every ticket from the storage. Called once at startup. */
    public synchronized void load() throws Exception {
        tickets.clear();
        for (Ticket ticket : storage.loadAll()) {
            tickets.put(ticket.number(), ticket);
        }
        nextNumber = tickets.isEmpty() ? 1 : tickets.lastKey() + 1;
    }

    // ------------------------------------------------------------ creating

    /**
     * Creates a ticket. Cooldown and the limit of open tickets are checked by the menus and commands,
     * not here, so other plugins can always create tickets.
     *
     * @throws IllegalArgumentException when the category is unknown, the reason is empty, or a
     *                                  player name is missing for a category that needs one
     */
    public Ticket create(UUID playerId, String playerName, String categoryId, String target, String reason,
                         String evidence) {
        TicketCategory category = plugin.settings().category(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("Unknown ticket category: " + categoryId);
        }
        return create(playerId, playerName, category, target, reason, evidence);
    }

    public Ticket create(UUID playerId, String playerName, TicketCategory category, String target, String reason,
                         String evidence) {
        String cleanReason = Chat.sanitize(reason, plugin.settings().reasonMaxLength());
        if (cleanReason.isEmpty()) {
            throw new IllegalArgumentException("A ticket needs a reason.");
        }
        String cleanTarget = Chat.sanitize(target, TARGET_MAX);
        if (cleanTarget.isEmpty()) {
            cleanTarget = null;
        }
        if (category.requiresTarget() && cleanTarget == null) {
            throw new IllegalArgumentException("This category needs a player name.");
        }
        String cleanEvidence = Chat.sanitize(evidence, EVIDENCE_MAX);
        if (cleanEvidence.isEmpty()) {
            cleanEvidence = null;
        }

        long now = System.currentTimeMillis();
        Ticket ticket;
        synchronized (this) {
            int number = nextNumber++;
            ticket = new Ticket(number, playerId, Chat.sanitize(playerName, TARGET_MAX), category.id(), cleanTarget,
                    cleanReason, cleanEvidence, TicketStatus.OPEN, null, null, now, now, false);
            tickets.put(number, ticket);
        }
        storage.save(ticket);
        plugin.notifier().newTicket(ticket);
        return ticket;
    }

    // ------------------------------------------------------------ finding

    public synchronized Ticket get(int number) {
        return tickets.get(number);
    }

    /** Finds a ticket by {@code TKT-007}, {@code tkt-7}, {@code #7} or {@code 7}. Returns null when not found. */
    public Ticket find(String text) {
        int number = parseNumber(text);
        return number < 0 ? null : get(number);
    }

    /** The number inside an id such as {@code TKT-007}, or -1 when the text is not an id. */
    public static int parseNumber(String text) {
        if (text == null) {
            return -1;
        }
        String s = text.trim().toLowerCase(Locale.ROOT);
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.startsWith("tkt-")) {
            s = s.substring(4);
        }
        try {
            int number = Integer.parseInt(s);
            return number > 0 ? number : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Every ticket, newest first. */
    public List<Ticket> findAll() {
        return filter(t -> true);
    }

    public List<Ticket> findByStatus(TicketStatus status) {
        return filter(t -> t.status() == status);
    }

    public List<Ticket> findByPlayer(UUID playerId) {
        return filter(t -> t.playerId().equals(playerId));
    }

    /** Tickets matching the filter, newest first. */
    public synchronized List<Ticket> filter(Predicate<Ticket> filter) {
        List<Ticket> out = new ArrayList<>();
        for (Ticket ticket : tickets.values()) {
            if (filter.test(ticket)) {
                out.add(ticket);
            }
        }
        Collections.reverse(out);
        return out;
    }

    public synchronized int count(Predicate<Ticket> filter) {
        int count = 0;
        for (Ticket ticket : tickets.values()) {
            if (filter.test(ticket)) {
                count++;
            }
        }
        return count;
    }

    public int getTotalCount() {
        return count(t -> true);
    }

    /** How many tickets of this player are not closed yet. */
    public int openCount(UUID playerId) {
        return count(t -> t.playerId().equals(playerId) && !t.status().isClosed());
    }

    /** Seconds the player still has to wait before creating another ticket, or 0. */
    public long cooldownRemainingSeconds(UUID playerId) {
        int cooldown = plugin.settings().cooldownSeconds();
        if (cooldown <= 0) {
            return 0;
        }
        long last;
        synchronized (this) {
            last = 0;
            for (Ticket ticket : tickets.values()) {
                if (ticket.playerId().equals(playerId)) {
                    last = Math.max(last, ticket.createdAt());
                }
            }
        }
        long remaining = cooldown - (System.currentTimeMillis() - last) / 1000;
        return Math.max(0, remaining);
    }

    /** Tickets whose owner has not been told about the latest reply or closing yet. */
    public List<Ticket> unreadFor(UUID playerId) {
        return filter(t -> t.playerId().equals(playerId) && t.playerUnread());
    }

    public synchronized Map<String, Integer> countsByCategory() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Ticket ticket : tickets.values()) {
            counts.merge(ticket.categoryId(), 1, Integer::sum);
        }
        return counts;
    }

    /** The id of the category with the most tickets, or "none". */
    public String getMostUsedCategory() {
        String best = "none";
        int most = 0;
        for (Map.Entry<String, Integer> entry : countsByCategory().entrySet()) {
            if (entry.getValue() > most) {
                most = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    // ------------------------------------------------------------ changing

    /**
     * Adds a reply. A staff reply marks the ticket as replied and tells the owner; a reply from the
     * owner puts the ticket back to open. Returns false when the ticket is closed or the text is empty.
     */
    public boolean reply(Ticket ticket, UUID authorId, String authorName, String message, boolean staff) {
        String text = Chat.sanitize(message, plugin.settings().reasonMaxLength());
        if (text.isEmpty() || ticket.status().isClosed()) {
            return false;
        }
        long now = System.currentTimeMillis();
        synchronized (this) {
            ticket.addReply(new TicketReply(authorId, Chat.sanitize(authorName, TARGET_MAX), text, now, staff));
            ticket.setStatus(staff ? TicketStatus.REPLIED : TicketStatus.OPEN);
            ticket.setPlayerUnread(staff);
            ticket.setUpdatedAt(now);
        }
        storage.save(ticket);
        if (staff) {
            plugin.notifier().replied(ticket, authorName, text);
        }
        return true;
    }

    /**
     * Closes a ticket. {@code note} is optional and becomes the last reply. When someone other than
     * the owner closes it, the owner is told.
     *
     * @return false when the ticket was already closed
     */
    public boolean close(Ticket ticket, UUID byId, String byName, String note, boolean byOwner) {
        if (ticket.status().isClosed()) {
            return false;
        }
        String text = Chat.sanitize(note, plugin.settings().reasonMaxLength());
        long now = System.currentTimeMillis();
        synchronized (this) {
            if (!text.isEmpty()) {
                ticket.addReply(new TicketReply(byId, Chat.sanitize(byName, TARGET_MAX), text, now, !byOwner));
            }
            ticket.setStatus(TicketStatus.CLOSED);
            ticket.setPlayerUnread(!byOwner);
            ticket.setUpdatedAt(now);
        }
        storage.save(ticket);
        plugin.notifier().closed(ticket, byName, text, byOwner);
        return true;
    }

    /** The owner cancels their own ticket. */
    public boolean cancel(Ticket ticket) {
        return close(ticket, ticket.playerId(), ticket.playerName(), plugin.messages().get("cancelled-note"), true);
    }

    /** Opens a closed ticket again. */
    public boolean reopen(Ticket ticket) {
        if (!ticket.status().isClosed()) {
            return false;
        }
        synchronized (this) {
            ticket.setStatus(TicketStatus.OPEN);
            ticket.setUpdatedAt(System.currentTimeMillis());
        }
        storage.save(ticket);
        return true;
    }

    /** Assigns a ticket to a staff member, or to nobody when {@code staffId} is null. */
    public void assign(Ticket ticket, UUID staffId, String staffName, String byName) {
        synchronized (this) {
            ticket.setAssigned(staffId, staffId == null ? null : Chat.sanitize(staffName, TARGET_MAX));
            ticket.setUpdatedAt(System.currentTimeMillis());
        }
        storage.save(ticket);
        if (staffId != null) {
            plugin.notifier().assigned(ticket, staffId, byName);
        }
    }

    /** The owner has now been told about the latest reply or closing. */
    public void markSeen(Ticket ticket) {
        boolean changed;
        synchronized (this) {
            changed = ticket.playerUnread();
            ticket.setPlayerUnread(false);
        }
        if (changed) {
            storage.save(ticket);
        }
    }

    /** Deletes closed tickets that were not touched for this many days. Returns how many were deleted. */
    public int archiveOlderThan(int days) {
        if (days <= 0) {
            return 0;
        }
        long limit = System.currentTimeMillis() - days * 86_400_000L;
        List<Ticket> old = new ArrayList<>();
        synchronized (this) {
            for (Ticket ticket : tickets.values()) {
                if (ticket.status().isClosed() && ticket.updatedAt() < limit) {
                    old.add(ticket);
                }
            }
            for (Ticket ticket : old) {
                tickets.remove(ticket.number());
            }
        }
        for (Ticket ticket : old) {
            storage.delete(ticket.number());
        }
        return old.size();
    }
}
