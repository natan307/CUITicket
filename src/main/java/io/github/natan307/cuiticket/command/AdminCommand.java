package io.github.natan307.cuiticket.command;

import io.github.natan307.cuiticket.CUITicketPlugin;
import io.github.natan307.cuiticket.config.Messages;
import io.github.natan307.cuiticket.menu.AdminMenu;
import io.github.natan307.cuiticket.menu.TicketText;
import io.github.natan307.cuiticket.notification.TicketNotifier;
import io.github.natan307.cuiticket.ticket.Ticket;
import io.github.natan307.cuiticket.ticket.TicketManager;
import io.github.natan307.cuiticket.ticket.TicketStatus;
import io.github.natan307.cuiticket.util.Chat;
import io.github.natan307.cuiticket.util.Time;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * /ticketadmin [list | open | closed | view &lt;id&gt; | reply &lt;id&gt; [message] | close &lt;id&gt; [note] |
 * assign &lt;id&gt; &lt;admin|none&gt; | stats | reload | help]
 *
 * <p>Players get menus. The console gets the same information as text.</p>
 */
public final class AdminCommand implements CommandExecutor, TabCompleter {

    private static final UUID CONSOLE_ID = new UUID(0L, 0L);
    private static final String CONSOLE_NAME = "Console";
    private static final int TEXT_LIST_MAX = 20;

    private static final List<String> SUBS = List.of("list", "open", "closed", "view", "reply", "close", "assign",
            "stats", "reload", "help");

    private final CUITicketPlugin plugin;

    public AdminCommand(CUITicketPlugin plugin) {
        this.plugin = plugin;
    }

    private Messages m() {
        return plugin.messages();
    }

    private TicketManager tickets() {
        return plugin.tickets();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player p ? p : null;

        if (args.length == 0) {
            if (player != null) {
                plugin.adminMenu().home(player);
            } else {
                textList(sender, "list-title-open", t -> !t.status().isClosed());
            }
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help":
                for (String line : m().lines("help-admin", "%version%", plugin.version())) {
                    Chat.send(sender, line);
                }
                return true;
            case "list":
                if (player != null) {
                    plugin.adminMenu().list(player, AdminMenu.Filter.ALL, 0);
                } else {
                    textList(sender, "list-title-all", t -> true);
                }
                return true;
            case "open":
                if (player != null) {
                    plugin.adminMenu().list(player, AdminMenu.Filter.OPEN, 0);
                } else {
                    textList(sender, "list-title-open", t -> t.status() == TicketStatus.OPEN);
                }
                return true;
            case "closed":
                if (player != null) {
                    plugin.adminMenu().list(player, AdminMenu.Filter.CLOSED, 0);
                } else {
                    textList(sender, "list-title-closed", t -> t.status() == TicketStatus.CLOSED);
                }
                return true;
            case "stats":
                if (player != null) {
                    plugin.adminMenu().stats(player);
                } else {
                    Chat.send(sender, m().get("stats.title") + "\n" + TicketText.stats(plugin));
                }
                return true;
            case "reload":
                plugin.reloadAll();
                sender.sendMessage(m().prefixed("reloaded"));
                return true;
            case "view":
                return view(sender, player, args);
            case "reply":
                return reply(sender, player, args);
            case "close":
                return close(sender, player, args);
            case "assign":
                return assign(sender, player, args);
            default:
                sender.sendMessage(m().prefixed("usage-admin"));
                return true;
        }
    }

    // ------------------------------------------------------------ sub commands

    private Ticket ticketArg(CommandSender sender, String[] args, String usageKey) {
        if (args.length < 2) {
            sender.sendMessage(m().prefixed(usageKey));
            return null;
        }
        Ticket ticket = tickets().find(args[1]);
        if (ticket == null) {
            sender.sendMessage(m().prefixed("ticket-not-found", "%id%", args[1]));
        }
        return ticket;
    }

    private boolean view(CommandSender sender, Player player, String[] args) {
        Ticket ticket = ticketArg(sender, args, "usage-view");
        if (ticket == null) {
            return true;
        }
        if (player != null) {
            plugin.adminMenu().detail(player, ticket.number());
        } else {
            Chat.send(sender, m().get("admin.detail-title", "%id%", ticket.id()) + "\n"
                    + TicketText.detail(plugin, ticket, true));
        }
        return true;
    }

    private boolean reply(CommandSender sender, Player player, String[] args) {
        Ticket ticket = ticketArg(sender, args, "usage-reply");
        if (ticket == null) {
            return true;
        }
        String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (player != null) {
            if (message.isBlank()) {
                if (player.hasPermission(AdminMenu.REPLY_PERMISSION)) {
                    plugin.adminMenu().askReply(player, ticket.number(), AdminMenu.Filter.OPEN, 0);
                } else {
                    player.sendMessage(m().prefixed("no-permission"));
                }
            } else {
                plugin.adminMenu().reply(player, ticket.number(), message);
            }
            return true;
        }
        if (message.isBlank()) {
            sender.sendMessage(m().prefixed("usage-reply"));
        } else if (tickets().reply(ticket, CONSOLE_ID, CONSOLE_NAME, message, true)) {
            sender.sendMessage(m().prefixed("reply-sent", "%id%", ticket.id()));
        } else {
            sender.sendMessage(m().prefixed("ticket-already-closed", "%id%", ticket.id()));
        }
        return true;
    }

    private boolean close(CommandSender sender, Player player, String[] args) {
        Ticket ticket = ticketArg(sender, args, "usage-close");
        if (ticket == null) {
            return true;
        }
        String note = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (player != null) {
            plugin.adminMenu().close(player, ticket.number(), note);
        } else if (tickets().close(ticket, CONSOLE_ID, CONSOLE_NAME, note, false)) {
            sender.sendMessage(m().prefixed("ticket-closed-staff", "%id%", ticket.id()));
        } else {
            sender.sendMessage(m().prefixed("ticket-already-closed", "%id%", ticket.id()));
        }
        return true;
    }

    private boolean assign(CommandSender sender, Player player, String[] args) {
        Ticket ticket = ticketArg(sender, args, "usage-assign");
        if (ticket == null) {
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(m().prefixed("usage-assign"));
            return true;
        }
        Player target = null;
        if (!args[2].equalsIgnoreCase("none")) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(m().prefixed("assign-offline", "%admin%", args[2]));
                return true;
            }
            if (!target.hasPermission(TicketNotifier.NOTIFY_PERMISSION)) {
                sender.sendMessage(m().prefixed("assign-not-staff", "%admin%", target.getName()));
                return true;
            }
        }
        if (player != null) {
            plugin.adminMenu().assign(player, ticket.number(), target);
        } else if (target == null) {
            tickets().assign(ticket, null, null, CONSOLE_NAME);
            sender.sendMessage(m().prefixed("ticket-unassigned", "%id%", ticket.id()));
        } else {
            tickets().assign(ticket, target.getUniqueId(), target.getName(), CONSOLE_NAME);
            sender.sendMessage(m().prefixed("ticket-assigned", "%id%", ticket.id(), "%admin%", target.getName()));
        }
        return true;
    }

    private void textList(CommandSender sender, String titleKey, Predicate<Ticket> filter) {
        List<Ticket> list = tickets().filter(filter);
        sender.sendMessage(m().get("admin." + titleKey) + " (" + list.size() + ")");
        if (list.isEmpty()) {
            sender.sendMessage(m().get("admin.list-empty"));
            return;
        }
        int max = Math.min(TEXT_LIST_MAX, list.size());
        for (int i = 0; i < max; i++) {
            Ticket t = list.get(i);
            sender.sendMessage(m().get("list-line", "%id%", t.id(), "%player%", t.playerName(),
                    "%category%", plugin.settings().categoryLabel(t.categoryId()), "%status%",
                    m().get(t.status().messageKey()), "%reason%", t.reason(), "%ago%", Time.ago(t.createdAt())));
        }
        if (list.size() > max) {
            sender.sendMessage(m().get("join-more", "%more%", String.valueOf(list.size() - max)));
        }
    }

    // ------------------------------------------------------------ tab completion

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            String start = args[0].toLowerCase(Locale.ROOT);
            for (String sub : SUBS) {
                if (sub.startsWith(start)) {
                    result.add(sub);
                }
            }
        } else if (args.length == 2 && List.of("view", "reply", "close", "assign").contains(args[0].toLowerCase(Locale.ROOT))) {
            String start = args[1].toLowerCase(Locale.ROOT);
            boolean active = !args[0].equalsIgnoreCase("view");
            for (Ticket ticket : tickets().findAll()) {
                if ((!active || !ticket.status().isClosed()) && ticket.id().toLowerCase(Locale.ROOT).startsWith(start)) {
                    result.add(ticket.id());
                }
                if (result.size() >= 30) {
                    break;
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("assign")) {
            String start = args[2].toLowerCase(Locale.ROOT);
            if ("none".startsWith(start)) {
                result.add("none");
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.hasPermission(TicketNotifier.NOTIFY_PERMISSION) && p.getName().toLowerCase(Locale.ROOT).startsWith(start)) {
                    result.add(p.getName());
                }
            }
        }
        return result;
    }
}
