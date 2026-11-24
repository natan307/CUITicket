package io.github.natan307.cuiticket.command;

import io.github.natan307.cuiticket.CUITicketPlugin;
import io.github.natan307.cuiticket.ticket.Ticket;
import io.github.natan307.cuiticket.ticket.TicketManager;
import io.github.natan307.cuiticket.ticket.TicketStatus;
import io.github.natan307.cuiticket.util.Chat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /ticket [mine | &lt;id&gt; | cancel &lt;id&gt; | help] */
public final class PlayerCommand implements CommandExecutor, TabCompleter {

    private final CUITicketPlugin plugin;

    public PlayerCommand(CUITicketPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("help")) {
            help(sender);
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.messages().prefixed("players-only"));
            return true;
        }

        if (args.length == 0) {
            plugin.playerMenu().open(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("mine")) {
            plugin.playerMenu().mine(player, 0);
            return true;
        }
        if (sub.equals("cancel")) {
            int number = args.length > 1 ? TicketManager.parseNumber(args[1]) : -1;
            if (number < 0) {
                player.sendMessage(plugin.messages().prefixed("usage-cancel"));
                return true;
            }
            plugin.playerMenu().cancelTicket(player, number);
            return true;
        }

        int number = TicketManager.parseNumber(args[0]);
        if (number < 0) {
            help(sender);
            return true;
        }
        plugin.playerMenu().detail(player, number, 0);
        return true;
    }

    private void help(CommandSender sender) {
        for (String line : plugin.messages().lines("help-player", "%version%", plugin.version())) {
            Chat.send(sender, line);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            String start = args[0].toLowerCase(Locale.ROOT);
            for (String sub : List.of("mine", "cancel", "help")) {
                if (sub.startsWith(start)) {
                    result.add(sub);
                }
            }
            if (sender instanceof Player player) {
                for (Ticket ticket : plugin.tickets().findByPlayer(player.getUniqueId())) {
                    if (ticket.id().toLowerCase(Locale.ROOT).startsWith(start)) {
                        result.add(ticket.id());
                    }
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("cancel") && sender instanceof Player player) {
            String start = args[1].toLowerCase(Locale.ROOT);
            for (Ticket ticket : plugin.tickets().findByPlayer(player.getUniqueId())) {
                if (ticket.status() == TicketStatus.OPEN && ticket.id().toLowerCase(Locale.ROOT).startsWith(start)) {
                    result.add(ticket.id());
                }
            }
        }
        return result;
    }
}
