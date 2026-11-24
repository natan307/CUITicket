package io.github.natan307.cuiticket.menu;

import io.github.natan307.cuiticket.CUITicketPlugin;
import io.github.natan307.cuiticket.config.Messages;
import io.github.natan307.cuiticket.ticket.Ticket;
import io.github.natan307.cuiticket.ticket.TicketReply;
import io.github.natan307.cuiticket.ticket.TicketStatus;
import io.github.natan307.cuiticket.util.Time;

import java.util.List;
import java.util.Map;

/** The texts shown inside ticket menus and in the text output of the commands. */
public final class TicketText {

    private TicketText() {
    }

    /**
     * A ticket with all its details and the last replies.
     *
     * @param staff true for staff (shows who sent the ticket and who handles it)
     */
    public static String detail(CUITicketPlugin plugin, Ticket ticket, boolean staff) {
        Messages m = plugin.messages();
        StringBuilder out = new StringBuilder();
        out.append(m.get("detail.status", "%status%", m.get(ticket.status().messageKey()))).append('\n');
        out.append(m.get("detail.category", "%category%", plugin.settings().categoryLabel(ticket.categoryId()))).append('\n');
        if (staff) {
            out.append(m.get("detail.from", "%player%", ticket.playerName())).append('\n');
        }
        if (ticket.target() != null) {
            out.append(m.get("detail.target", "%target%", ticket.target())).append('\n');
        }
        out.append(m.get("detail.reason", "%reason%", ticket.reason())).append('\n');
        if (ticket.evidence() != null) {
            out.append(m.get("detail.evidence", "%evidence%", ticket.evidence())).append('\n');
        }
        if (staff) {
            out.append(m.get("detail.assigned", "%admin%",
                    ticket.assignedName() == null ? m.get("detail.nobody") : ticket.assignedName())).append('\n');
        }
        out.append(m.get("detail.created", "%ago%", Time.ago(ticket.createdAt()), "%date%", Time.date(ticket.createdAt())));

        List<TicketReply> replies = ticket.replies();
        if (!replies.isEmpty()) {
            int shown = Math.min(plugin.settings().maxRepliesShown(), replies.size());
            out.append("\n\n").append(m.get("detail.replies-header", "%count%", String.valueOf(replies.size())));
            if (replies.size() > shown) {
                out.append('\n').append(m.get("detail.earlier", "%count%", String.valueOf(replies.size() - shown)));
            }
            for (int i = replies.size() - shown; i < replies.size(); i++) {
                TicketReply reply = replies.get(i);
                out.append('\n').append(m.get(reply.staff() ? "detail.reply-staff" : "detail.reply-player",
                        "%who%", reply.authorName(), "%ago%", Time.ago(reply.createdAt()), "%message%", reply.message()));
            }
        }
        return out.toString();
    }

    /** Short summary used when a player confirms a new ticket. */
    public static String summary(CUITicketPlugin plugin, String categoryLabel, String target, String reason, String evidence) {
        Messages m = plugin.messages();
        StringBuilder out = new StringBuilder();
        out.append(m.get("detail.category", "%category%", categoryLabel)).append('\n');
        if (target != null) {
            out.append(m.get("detail.target", "%target%", target)).append('\n');
        }
        out.append(m.get("detail.reason", "%reason%", reason));
        if (evidence != null && !evidence.isEmpty()) {
            out.append('\n').append(m.get("detail.evidence", "%evidence%", evidence));
        }
        return out.toString();
    }

    /** The statistics shown by {@code /ticketadmin stats}. */
    public static String stats(CUITicketPlugin plugin) {
        Messages m = plugin.messages();
        var tickets = plugin.tickets();
        StringBuilder out = new StringBuilder();
        out.append(m.get("stats.total", "%count%", String.valueOf(tickets.getTotalCount()))).append('\n');
        out.append(m.get("stats.open", "%count%", String.valueOf(tickets.findByStatus(TicketStatus.OPEN).size()))).append('\n');
        out.append(m.get("stats.replied", "%count%", String.valueOf(tickets.findByStatus(TicketStatus.REPLIED).size()))).append('\n');
        out.append(m.get("stats.closed", "%count%", String.valueOf(tickets.findByStatus(TicketStatus.CLOSED).size())));
        Map<String, Integer> byCategory = tickets.countsByCategory();
        if (!byCategory.isEmpty()) {
            out.append("\n\n").append(m.get("stats.by-category"));
            for (Map.Entry<String, Integer> entry : byCategory.entrySet()) {
                out.append('\n').append(m.get("stats.category-line", "%category%",
                        plugin.settings().categoryLabel(entry.getKey()), "%count%", String.valueOf(entry.getValue())));
            }
        }
        return out.toString();
    }
}
