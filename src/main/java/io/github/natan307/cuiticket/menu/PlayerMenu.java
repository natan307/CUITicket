package io.github.natan307.cuiticket.menu;

import io.github.natan307.crossui.api.CrossUI;
import io.github.natan307.crossui.api.builder.MenuBuilder;
import io.github.natan307.cuiticket.CUITicketPlugin;
import io.github.natan307.cuiticket.config.Messages;
import io.github.natan307.cuiticket.config.Settings;
import io.github.natan307.cuiticket.ticket.Ticket;
import io.github.natan307.cuiticket.ticket.TicketCategory;
import io.github.natan307.cuiticket.ticket.TicketManager;
import io.github.natan307.cuiticket.ticket.TicketStatus;
import io.github.natan307.cuiticket.util.Chat;
import io.github.natan307.cuiticket.util.Time;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * What a player sees: choose a category, fill in the form, confirm, and later read their tickets.
 * CrossUI shows each step as a Bedrock form, a Java dialog or a chest menu, whichever the player's
 * client supports.
 */
public final class PlayerMenu {

    public static final String CREATE_PERMISSION = "cuiticket.create";
    public static final String BYPASS_PERMISSION = "cuiticket.bypass";

    /** What the player has filled in so far. */
    private record Draft(TicketCategory category, String target, String reason, String evidence) {
        Draft withTarget(String value) {
            return new Draft(category, value, reason, evidence);
        }

        Draft withReason(String value) {
            return new Draft(category, target, value, evidence);
        }

        Draft withEvidence(String value) {
            return new Draft(category, target, reason, value);
        }
    }

    private final CUITicketPlugin plugin;
    private final Icons icons;

    public PlayerMenu(CUITicketPlugin plugin) {
        this.plugin = plugin;
        this.icons = new Icons(plugin);
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

    /** Opens the next screen two ticks later so the previous form or dialog is fully closed. */
    private void later(Player player, Runnable task) {
        plugin.scheduler().runLaterForPlayer(player, () -> {
            if (player.isOnline()) {
                task.run();
            }
        }, 2L);
    }

    private boolean allowed(Player player) {
        if (player.hasPermission(CREATE_PERMISSION)) {
            return true;
        }
        player.sendMessage(m().prefixed("no-permission"));
        return false;
    }

    /** Whether the player may create another ticket now. Tells the player why not. */
    private boolean checkLimits(Player player) {
        if (player.hasPermission(BYPASS_PERMISSION)) {
            return true;
        }
        int open = tickets().openCount(player.getUniqueId());
        if (open >= s().maxOpenPerPlayer()) {
            player.sendMessage(m().prefixed("max-open", "%count%", String.valueOf(open),
                    "%max%", String.valueOf(s().maxOpenPerPlayer())));
            return false;
        }
        long wait = tickets().cooldownRemainingSeconds(player.getUniqueId());
        if (wait > 0) {
            player.sendMessage(m().prefixed("cooldown", "%seconds%", String.valueOf(wait)));
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------ new ticket

    /** Step 1: choose a category. */
    public void open(Player player) {
        if (!allowed(player) || !checkLimits(player)) {
            return;
        }
        int open = tickets().openCount(player.getUniqueId());
        int mine = tickets().count(t -> t.playerId().equals(player.getUniqueId()));

        MenuBuilder menu = CrossUI.menu(player)
                .title(m().get("ui.new-title"))
                .content(m().get("ui.new-content", "%open%", String.valueOf(open),
                        "%max%", String.valueOf(s().maxOpenPerPlayer())));

        Icons.Style style = icons.styleFor(player);
        for (TicketCategory category : s().categories()) {
            menu.button(category.label(), null, icons.icon(style, category.icon()),
                    p -> later(p, () -> start(p, category)));
        }
        if (mine > 0) {
            menu.button(m().get("ui.my-tickets-button", "%count%", String.valueOf(mine)),
                    p -> later(p, () -> mine(p, 0)));
        }
        menu.open();
    }

    private void start(Player player, TicketCategory category) {
        Draft draft = new Draft(category, null, null, null);
        if (category.requiresTarget()) {
            askTarget(player, draft);
        } else {
            askReason(player, draft);
        }
    }

    private void cancelled(Player player) {
        player.sendMessage(m().prefixed("flow-cancelled"));
    }

    /** Step 2 (only for categories about a player): who. */
    private void askTarget(Player player, Draft draft) {
        CrossUI.input(player)
                .title(draft.category().label())
                .label(m().get("ui.target-label"))
                .placeholder(m().get("ui.target-placeholder"))
                .onSubmit((p, text) -> later(p, () -> {
                    String name = Chat.sanitize(text, 32);
                    if (name.isEmpty()) {
                        p.sendMessage(m().prefixed("target-empty"));
                        askTarget(p, draft);
                        return;
                    }
                    Player online = Bukkit.getPlayerExact(name);
                    if (online != null) {
                        name = online.getName();
                    }
                    if (name.equalsIgnoreCase(p.getName())) {
                        p.sendMessage(m().prefixed("target-self"));
                        askTarget(p, draft);
                        return;
                    }
                    askReason(p, draft.withTarget(name));
                }))
                .onCancel(this::cancelled)
                .open();
    }

    /** Step 3: what happened. */
    private void askReason(Player player, Draft draft) {
        CrossUI.input(player)
                .title(draft.category().label())
                .label(m().get("ui.reason-label", "%max%", String.valueOf(s().reasonMaxLength())))
                .placeholder(m().get("ui.reason-placeholder"))
                .onSubmit((p, text) -> later(p, () -> {
                    String reason = Chat.sanitize(text, s().reasonMaxLength());
                    if (reason.isEmpty()) {
                        p.sendMessage(m().prefixed("reason-empty"));
                        askReason(p, draft);
                        return;
                    }
                    askEvidence(p, draft.withReason(reason));
                }))
                .onCancel(this::cancelled)
                .open();
    }

    /** Step 4: an optional link with proof. Leaving it empty skips the step. */
    private void askEvidence(Player player, Draft draft) {
        CrossUI.input(player)
                .title(draft.category().label())
                .label(m().get("ui.evidence-label"))
                .placeholder(m().get("ui.evidence-placeholder"))
                .onSubmit((p, text) -> later(p, () -> confirm(p, draft.withEvidence(Chat.sanitize(text, 200)))))
                .onCancel(this::cancelled)
                .open();
    }

    /** Step 5: look it over and send. */
    private void confirm(Player player, Draft draft) {
        String summary = TicketText.summary(plugin, draft.category().label(), draft.target(), draft.reason(), draft.evidence());
        CrossUI.confirm(player)
                .title(m().get("ui.confirm-title"))
                .body(summary + "\n\n" + m().get("ui.confirm-question"))
                .yesText(m().get("ui.send"))
                .noText(m().get("ui.cancel"))
                .onYes(p -> submit(p, draft))
                .onNo(this::cancelled)
                .open();
    }

    private void submit(Player player, Draft draft) {
        if (!allowed(player) || !checkLimits(player)) {
            return;
        }
        try {
            Ticket ticket = tickets().create(player.getUniqueId(), player.getName(), draft.category(), draft.target(),
                    draft.reason(), draft.evidence());
            player.sendMessage(m().prefixed("ticket-created", "%id%", ticket.id()));
        } catch (IllegalArgumentException e) {
            player.sendMessage(m().prefixed("ticket-invalid"));
        }
    }

    // ------------------------------------------------------------ own tickets

    /** The player's own tickets, newest first. */
    public void mine(Player player, int requestedPage) {
        if (!allowed(player)) {
            return;
        }
        List<Ticket> all = tickets().findByPlayer(player.getUniqueId());
        if (all.isEmpty()) {
            player.sendMessage(m().prefixed("no-tickets"));
            return;
        }
        int size = s().pageSize();
        int pages = Math.max(1, (all.size() + size - 1) / size);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        int from = page * size;
        int to = Math.min(all.size(), from + size);

        MenuBuilder menu = CrossUI.menu(player)
                .title(m().get("ui.mine-title"))
                .content(m().get("ui.mine-content", "%from%", String.valueOf(from + 1), "%to%", String.valueOf(to),
                        "%total%", String.valueOf(all.size())));

        for (int i = from; i < to; i++) {
            Ticket ticket = all.get(i);
            int number = ticket.number();
            int currentPage = page;
            menu.button(m().get("ui.ticket-button", "%id%", ticket.id(),
                            "%category%", s().categoryLabel(ticket.categoryId())),
                    m().get(ticket.playerUnread() ? "ui.ticket-sub-new" : "ui.ticket-sub", "%status%",
                            m().get(ticket.status().messageKey()), "%ago%", Time.ago(ticket.updatedAt())),
                    null, p -> later(p, () -> detail(p, number, currentPage)));
        }
        if (page > 0) {
            int previous = page - 1;
            menu.button(m().get("ui.prev-page"), p -> later(p, () -> mine(p, previous)));
        }
        if (page < pages - 1) {
            int next = page + 1;
            menu.button(m().get("ui.next-page"), p -> later(p, () -> mine(p, next)));
        }
        menu.open();
    }

    /** One of the player's own tickets. Other people's tickets cannot be opened here. */
    public void detail(Player player, int number, int backPage) {
        if (!allowed(player)) {
            return;
        }
        Ticket ticket = tickets().get(number);
        if (ticket == null || !ticket.playerId().equals(player.getUniqueId())) {
            player.sendMessage(m().prefixed("ticket-not-found", "%id%", Ticket.idOf(number)));
            return;
        }
        tickets().markSeen(ticket);

        MenuBuilder menu = CrossUI.menu(player)
                .title(m().get("ui.detail-title", "%id%", ticket.id()))
                .content(TicketText.detail(plugin, ticket, false));
        if (ticket.status() == TicketStatus.OPEN) {
            menu.button(m().get("ui.cancel-ticket"), p -> later(p, () -> confirmCancel(p, number, backPage)));
        }
        menu.button(m().get("ui.back"), p -> later(p, () -> mine(p, backPage)));
        menu.open();
    }

    private void confirmCancel(Player player, int number, int backPage) {
        CrossUI.confirm(player)
                .title(m().get("ui.cancel-title", "%id%", Ticket.idOf(number)))
                .body(m().get("ui.cancel-body"))
                .yesText(m().get("ui.cancel-yes"))
                .noText(m().get("ui.cancel-no"))
                .onYes(p -> cancelTicket(p, number))
                .onNo(p -> later(p, () -> detail(p, number, backPage)))
                .open();
    }

    /** Cancels one of the player's own open tickets. Used by the menu and by {@code /ticket cancel}. */
    public void cancelTicket(Player player, int number) {
        Ticket ticket = tickets().get(number);
        if (ticket == null || !ticket.playerId().equals(player.getUniqueId())) {
            player.sendMessage(m().prefixed("ticket-not-found", "%id%", Ticket.idOf(number)));
            return;
        }
        if (ticket.status() != TicketStatus.OPEN) {
            player.sendMessage(m().prefixed("cancel-not-open", "%id%", ticket.id()));
            return;
        }
        if (tickets().cancel(ticket)) {
            player.sendMessage(m().prefixed("ticket-cancelled", "%id%", ticket.id()));
        }
    }
}
