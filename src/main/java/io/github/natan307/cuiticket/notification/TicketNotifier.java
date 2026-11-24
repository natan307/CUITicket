package io.github.natan307.cuiticket.notification;

import io.github.natan307.cuiticket.CUITicketPlugin;
import io.github.natan307.cuiticket.config.Messages;
import io.github.natan307.cuiticket.config.Settings;
import io.github.natan307.cuiticket.ticket.Ticket;
import io.github.natan307.cuiticket.ticket.TicketStatus;
import io.github.natan307.cuiticket.util.Chat;
import io.github.natan307.cuiticket.util.Time;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Tells staff about new tickets (now, or when they join), and tells players about replies and
 * closings (now, or when they join if they were offline).
 */
public final class TicketNotifier {

    public static final String NOTIFY_PERMISSION = "cuiticket.notify";

    private static final int SNIPPET = 120;

    private final CUITicketPlugin plugin;

    public TicketNotifier(CUITicketPlugin plugin) {
        this.plugin = plugin;
    }

    private Messages m() {
        return plugin.messages();
    }

    private Settings s() {
        return plugin.settings();
    }

    private static String snippet(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= SNIPPET ? text : text.substring(0, SNIPPET - 3) + "...";
    }

    // ------------------------------------------------------------ live

    /** A new ticket was created: tell every staff member who is online right now. */
    public void newTicket(Ticket ticket) {
        plugin.getLogger().info("New ticket " + ticket.id() + " from " + ticket.playerName() + " ("
                + ticket.categoryId() + "): " + snippet(ticket.reason()));
        if (!s().notifyAdminsOnline()) {
            return;
        }
        String text = m().prefixed("new-ticket", "%id%", ticket.id(), "%player%", ticket.playerName(),
                "%category%", s().categoryLabel(ticket.categoryId()), "%reason%", snippet(ticket.reason()));
        String label = m().get("view-button");
        String hover = m().get("view-hover", "%id%", ticket.id());

        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (!staff.hasPermission(NOTIFY_PERMISSION) || staff.getUniqueId().equals(ticket.playerId())) {
                continue;
            }
            plugin.scheduler().runForPlayer(staff, () -> {
                Chat.sendWithButton(staff, text, label, "/ticketadmin view " + ticket.number(), hover);
                playSound(staff);
            });
        }
    }

    /** Staff replied: tell the owner if they are online. Otherwise they are told when they join. */
    public void replied(Ticket ticket, String by, String message) {
        if (!s().notifyPlayerOnReply()) {
            return;
        }
        Player owner = Bukkit.getPlayer(ticket.playerId());
        if (owner == null) {
            return;
        }
        String text = m().prefixed("ticket-replied", "%id%", ticket.id(), "%admin%", by, "%message%", snippet(message));
        plugin.scheduler().runForPlayer(owner, () -> {
            Chat.sendWithButton(owner, text, m().get("read-button"), "/ticket " + ticket.number(),
                    m().get("read-hover", "%id%", ticket.id()));
            playSound(owner);
        });
        plugin.tickets().markSeen(ticket);
    }

    /** Staff closed the ticket: tell the owner if they are online. */
    public void closed(Ticket ticket, String by, String note, boolean byOwner) {
        if (byOwner || !s().notifyPlayerOnClose()) {
            return;
        }
        Player owner = Bukkit.getPlayer(ticket.playerId());
        if (owner == null) {
            return;
        }
        String text = m().prefixed("ticket-closed", "%id%", ticket.id(), "%admin%", by, "%message%", snippet(note));
        plugin.scheduler().runForPlayer(owner, () -> {
            Chat.sendWithButton(owner, text, m().get("read-button"), "/ticket " + ticket.number(),
                    m().get("read-hover", "%id%", ticket.id()));
            playSound(owner);
        });
        plugin.tickets().markSeen(ticket);
    }

    /** A ticket was assigned: tell the staff member who got it. */
    public void assigned(Ticket ticket, UUID staffId, String by) {
        Player staff = Bukkit.getPlayer(staffId);
        if (staff == null || by == null || by.equalsIgnoreCase(staff.getName())) {
            return;
        }
        String text = m().prefixed("ticket-assigned-to-you", "%id%", ticket.id(), "%admin%", by);
        plugin.scheduler().runForPlayer(staff, () -> Chat.sendWithButton(staff, text, m().get("view-button"),
                "/ticketadmin view " + ticket.number(), m().get("view-hover", "%id%", ticket.id())));
    }

    private void playSound(Player player) {
        String sound = s().sound();
        if (sound == null || sound.isBlank()) {
            return;
        }
        try {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (RuntimeException ignored) {
            // an unknown sound name must never break a ticket
        }
    }

    // ------------------------------------------------------------ on join

    /** Called when a player joins: updates on their own tickets first, then the staff summary. */
    public void onJoin(Player player) {
        long delay = s().joinDelaySeconds() * 20L;
        plugin.scheduler().runLaterForPlayer(player, () -> {
            if (!player.isOnline()) {
                return;
            }
            ownTicketUpdates(player);
            staffSummary(player);
        }, delay);
    }

    private void ownTicketUpdates(Player player) {
        List<Ticket> unread = plugin.tickets().unreadFor(player.getUniqueId());
        if (unread.isEmpty()) {
            return;
        }
        boolean shown = false;
        for (Ticket ticket : unread) {
            boolean closed = ticket.status() == TicketStatus.CLOSED;
            boolean wanted = closed ? s().notifyPlayerOnClose() : s().notifyPlayerOnReply();
            plugin.tickets().markSeen(ticket);
            if (!wanted) {
                continue;
            }
            player.sendMessage(m().prefixed(closed ? "join-ticket-closed" : "join-ticket-replied",
                    "%id%", ticket.id(), "%category%", s().categoryLabel(ticket.categoryId())));
            shown = true;
        }
        if (shown) {
            Chat.sendWithButton(player, "", m().get("my-tickets-button"), "/ticket mine", m().get("my-tickets-hover"));
        }
    }

    private void staffSummary(Player player) {
        if (!player.hasPermission(NOTIFY_PERMISSION)) {
            return;
        }
        List<Ticket> waiting = plugin.tickets().findByStatus(TicketStatus.OPEN);
        if (waiting.isEmpty()) {
            return;
        }
        player.sendMessage(m().prefixed("join-waiting", "%count%", String.valueOf(waiting.size())));
        int max = Math.min(s().joinListMax(), waiting.size());
        for (int i = 0; i < max; i++) {
            Ticket t = waiting.get(i);
            player.sendMessage(m().get("join-line", "%id%", t.id(), "%player%", t.playerName(),
                    "%category%", s().categoryLabel(t.categoryId()), "%reason%", snippet(t.reason()),
                    "%ago%", Time.ago(t.createdAt())));
        }
        if (waiting.size() > max && max > 0) {
            player.sendMessage(m().get("join-more", "%more%", String.valueOf(waiting.size() - max)));
        }
        Chat.sendWithButton(player, "", m().get("view-all-button"), "/ticketadmin", m().get("view-all-hover"));
    }
}
