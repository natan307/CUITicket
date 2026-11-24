package io.github.natan307.cuiticket.menu;

import io.github.natan307.crossui.api.CrossUI;
import io.github.natan307.crossui.api.builder.MenuBuilder;
import io.github.natan307.cuiticket.CUITicketPlugin;
import io.github.natan307.cuiticket.config.Messages;
import io.github.natan307.cuiticket.config.Settings;
import io.github.natan307.cuiticket.notification.TicketNotifier;
import io.github.natan307.cuiticket.ticket.Ticket;
import io.github.natan307.cuiticket.ticket.TicketManager;
import io.github.natan307.cuiticket.ticket.TicketStatus;
import io.github.natan307.cuiticket.util.Chat;
import io.github.natan307.cuiticket.util.Time;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/** The staff screens: ticket lists, ticket details, reply, assign, close and statistics. */
public final class AdminMenu {

    public static final String ADMIN_PERMISSION = "cuiticket.admin";
    public static final String REPLY_PERMISSION = "cuiticket.reply";

    /** Which tickets a list shows. */
    public enum Filter {
        OPEN,
        REPLIED,
        CLOSED,
        ALL,
        MINE;

        public boolean matches(Ticket ticket, Player viewer) {
            switch (this) {
                case OPEN:
                    return ticket.status() == TicketStatus.OPEN;
                case REPLIED:
                    return ticket.status() == TicketStatus.REPLIED;
                case CLOSED:
                    return ticket.status() == TicketStatus.CLOSED;
                case MINE:
                    return !ticket.status().isClosed() && ticket.assignedTo() != null
                            && ticket.assignedTo().equals(viewer.getUniqueId());
                default:
                    return true;
            }
        }

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final CUITicketPlugin plugin;

    public AdminMenu(CUITicketPlugin plugin) {
        this.plugin = plugin;
    }

    private Messages m() {
        return plugin.messages();
    }

    private Settings s() {
        return plugin.settings();
    }

    private TicketManager tickets() {
        return plugin.tickets();
    }

    private void later(Player player, Runnable task) {
        plugin.scheduler().runLaterForPlayer(player, () -> {
            if (player.isOnline()) {
                task.run();
            }
        }, 2L);
    }

    private boolean allowed(Player player) {
        if (player.hasPermission(ADMIN_PERMISSION)) {
            return true;
        }
        player.sendMessage(m().prefixed("no-permission"));
        return false;
    }

    private boolean canReply(Player player) {
        if (player.hasPermission(REPLY_PERMISSION)) {
            return true;
        }
        player.sendMessage(m().prefixed("no-permission"));
        return false;
    }

    private String count(Player player, Filter filter) {
        return String.valueOf(tickets().count(t -> filter.matches(t, player)));
    }

    // ------------------------------------------------------------ home

    public void home(Player player) {
        if (!allowed(player)) {
            return;
        }
        MenuBuilder menu = CrossUI.menu(player)
                .title(m().get("admin.home-title"))
                .content(m().get("admin.home-content", "%open%", count(player, Filter.OPEN),
                        "%replied%", count(player, Filter.REPLIED)));
        for (Filter filter : Filter.values()) {
            menu.button(m().get("admin.filter-" + filter.key(), "%count%", count(player, filter)),
                    p -> later(p, () -> list(p, filter, 0)));
        }
        menu.button(m().get("admin.stats-button"), p -> later(p, () -> stats(p)));
        menu.open();
    }

    // ------------------------------------------------------------ list

    public void list(Player player, Filter filter, int requestedPage) {
        if (!allowed(player)) {
            return;
        }
        List<Ticket> all = tickets().filter(t -> filter.matches(t, player));
        String title = m().get("admin.list-title-" + filter.key());

        if (all.isEmpty()) {
            CrossUI.menu(player)
                    .title(title)
                    .content(m().get("admin.list-empty"))
                    .button(m().get("ui.back"), p -> later(p, () -> home(p)))
                    .open();
            return;
        }

        int size = s().pageSize();
        int pages = Math.max(1, (all.size() + size - 1) / size);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        int from = page * size;
        int to = Math.min(all.size(), from + size);

        MenuBuilder menu = CrossUI.menu(player)
                .title(title)
                .content(m().get("admin.list-content", "%from%", String.valueOf(from + 1), "%to%", String.valueOf(to),
                        "%total%", String.valueOf(all.size())));

        for (int i = from; i < to; i++) {
            Ticket ticket = all.get(i);
            int number = ticket.number();
            int currentPage = page;
            menu.button(m().get("admin.ticket-button", "%id%", ticket.id(),
                            "%category%", s().categoryLabel(ticket.categoryId())),
                    m().get("admin.ticket-sub", "%player%", ticket.playerName(), "%status%",
                            m().get(ticket.status().messageKey()), "%ago%", Time.ago(ticket.createdAt())),
                    null, p -> later(p, () -> detail(p, number, filter, currentPage)));
        }
        if (page > 0) {
            int previous = page - 1;
            menu.button(m().get("ui.prev-page"), p -> later(p, () -> list(p, filter, previous)));
        }
        if (page < pages - 1) {
            int next = page + 1;
            menu.button(m().get("ui.next-page"), p -> later(p, () -> list(p, filter, next)));
        }
        menu.button(m().get("ui.back"), p -> later(p, () -> home(p)));
        menu.open();
    }

    // ------------------------------------------------------------ detail

    /** Opens a ticket, for example from the clickable message sent when it was created. */
    public void detail(Player player, int number) {
        detail(player, number, Filter.OPEN, 0);
    }

    public void detail(Player player, int number, Filter back, int backPage) {
        if (!allowed(player)) {
            return;
        }
        Ticket ticket = tickets().get(number);
        if (ticket == null) {
            player.sendMessage(m().prefixed("ticket-not-found", "%id%", Ticket.idOf(number)));
            return;
        }

        MenuBuilder menu = CrossUI.menu(player)
                .title(m().get("admin.detail-title", "%id%", ticket.id()))
                .content(TicketText.detail(plugin, ticket, true));

        if (ticket.status().isClosed()) {
            menu.button(m().get("admin.reopen"), p -> reopen(p, number, back, backPage));
        } else {
            if (player.hasPermission(REPLY_PERMISSION)) {
                menu.button(m().get("admin.reply"), p -> later(p, () -> askReply(p, number, back, backPage)));
            }
            menu.button(m().get("admin.assign"), p -> later(p, () -> assignMenu(p, number, back, backPage)));
            menu.button(m().get("admin.close"), p -> later(p, () -> askClose(p, number, back, backPage)));
        }
        menu.button(m().get("ui.back"), p -> later(p, () -> list(p, back, backPage)));
        menu.open();
    }

    // ------------------------------------------------------------ reply

    public void askReply(Player player, int number, Filter back, int backPage) {
        if (!allowed(player) || !canReply(player)) {
            return;
        }
        CrossUI.input(player)
                .title(m().get("admin.reply-title", "%id%", Ticket.idOf(number)))
                .label(m().get("admin.reply-label"))
                .placeholder(m().get("admin.reply-placeholder"))
                .onSubmit((p, text) -> later(p, () -> {
                    if (Chat.sanitize(text, 0).isEmpty()) {
                        p.sendMessage(m().prefixed("reply-empty"));
                        askReply(p, number, back, backPage);
                        return;
                    }
                    reply(p, number, text);
                    detail(p, number, back, backPage);
                }))
                .onCancel(p -> later(p, () -> detail(p, number, back, backPage)))
                .open();
    }

    /** Sends a staff reply and tells the staff member how it went. Used by the menu and by the command. */
    public void reply(Player admin, int number, String message) {
        if (!canReply(admin)) {
            return;
        }
        Ticket ticket = tickets().get(number);
        if (ticket == null) {
            admin.sendMessage(m().prefixed("ticket-not-found", "%id%", Ticket.idOf(number)));
            return;
        }
        if (tickets().reply(ticket, admin.getUniqueId(), admin.getName(), message, true)) {
            admin.sendMessage(m().prefixed("reply-sent", "%id%", ticket.id()));
        } else {
            admin.sendMessage(m().prefixed(ticket.status().isClosed() ? "ticket-already-closed" : "reply-empty",
                    "%id%", ticket.id()));
        }
    }

    // ------------------------------------------------------------ close and reopen

    public void askClose(Player player, int number, Filter back, int backPage) {
        if (!allowed(player)) {
            return;
        }
        CrossUI.input(player)
                .title(m().get("admin.close-title", "%id%", Ticket.idOf(number)))
                .label(m().get("admin.close-label"))
                .placeholder(m().get("admin.close-placeholder"))
                .onSubmit((p, text) -> later(p, () -> {
                    close(p, number, text);
                    detail(p, number, back, backPage);
                }))
                .onCancel(p -> later(p, () -> detail(p, number, back, backPage)))
                .open();
    }

    /** Closes a ticket with an optional note for the player. */
    public void close(Player admin, int number, String note) {
        if (!allowed(admin)) {
            return;
        }
        Ticket ticket = tickets().get(number);
        if (ticket == null) {
            admin.sendMessage(m().prefixed("ticket-not-found", "%id%", Ticket.idOf(number)));
            return;
        }
        if (tickets().close(ticket, admin.getUniqueId(), admin.getName(), note, false)) {
            admin.sendMessage(m().prefixed("ticket-closed-staff", "%id%", ticket.id()));
        } else {
            admin.sendMessage(m().prefixed("ticket-already-closed", "%id%", ticket.id()));
        }
    }

    private void reopen(Player admin, int number, Filter back, int backPage) {
        if (!allowed(admin)) {
            return;
        }
        Ticket ticket = tickets().get(number);
        if (ticket != null && tickets().reopen(ticket)) {
            admin.sendMessage(m().prefixed("ticket-reopened", "%id%", ticket.id()));
        }
        later(admin, () -> detail(admin, number, back, backPage));
    }

    // ------------------------------------------------------------ assign

    private void assignMenu(Player player, int number, Filter back, int backPage) {
        if (!allowed(player)) {
            return;
        }
        MenuBuilder menu = CrossUI.menu(player)
                .title(m().get("admin.assign-title", "%id%", Ticket.idOf(number)))
                .content(m().get("admin.assign-content"));

        menu.button(m().get("admin.assign-me"), p -> {
            assign(p, number, p);
            later(p, () -> detail(p, number, back, backPage));
        });
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (staff.getUniqueId().equals(player.getUniqueId()) || !staff.hasPermission(TicketNotifier.NOTIFY_PERMISSION)) {
                continue;
            }
            menu.button(staff.getName(), p -> {
                assign(p, number, staff);
                later(p, () -> detail(p, number, back, backPage));
            });
        }
        menu.button(m().get("admin.unassign"), p -> {
            assign(p, number, null);
            later(p, () -> detail(p, number, back, backPage));
        });
        menu.button(m().get("ui.back"), p -> later(p, () -> detail(p, number, back, backPage)));
        menu.open();
    }

    /** Assigns a ticket to a staff member, or to nobody when {@code target} is null. */
    public void assign(Player admin, int number, Player target) {
        if (!allowed(admin)) {
            return;
        }
        Ticket ticket = tickets().get(number);
        if (ticket == null) {
            admin.sendMessage(m().prefixed("ticket-not-found", "%id%", Ticket.idOf(number)));
            return;
        }
        if (target == null) {
            tickets().assign(ticket, null, null, admin.getName());
            admin.sendMessage(m().prefixed("ticket-unassigned", "%id%", ticket.id()));
        } else {
            tickets().assign(ticket, target.getUniqueId(), target.getName(), admin.getName());
            admin.sendMessage(m().prefixed("ticket-assigned", "%id%", ticket.id(), "%admin%", target.getName()));
        }
    }

    // ------------------------------------------------------------ stats

    public void stats(Player player) {
        if (!allowed(player)) {
            return;
        }
        CrossUI.menu(player)
                .title(m().get("admin.stats-title"))
                .content(TicketText.stats(plugin))
                .button(m().get("ui.back"), p -> later(p, () -> home(p)))
                .open();
    }
}
