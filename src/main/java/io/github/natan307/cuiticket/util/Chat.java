package io.github.natan307.cuiticket.util;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/** Small helpers for colored and clickable chat messages, and for cleaning text typed by players. */
public final class Chat {

    /** An {@code &} that starts a color or format code, such as &c or &l. */
    private static final Pattern AMP_CODE = Pattern.compile("&(?=[0-9a-fk-orA-FK-OR#x])");

    private Chat() {
    }

    /** Turns {@code &} color codes into real color codes. */
    public static String color(String text) {
        return text == null ? "" : ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * Cleans text typed by a player so it cannot inject colors or line breaks into what staff read:
     * color characters and control characters are removed, spaces are collapsed and the text is cut
     * to {@code maxLength} characters (0 or less means no limit).
     */
    public static String sanitize(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' || c < 32 || c == 127) {
                if (c == '\n' || c == '\t') {
                    out.append(' ');
                }
                continue;
            }
            out.append(c);
        }
        String clean = AMP_CODE.matcher(out).replaceAll("").replaceAll(" {2,}", " ").trim();
        if (maxLength > 0 && clean.length() > maxLength) {
            clean = clean.substring(0, maxLength).trim();
        }
        return clean;
    }

    /** Sends a message that may contain several lines. */
    public static void send(CommandSender to, String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        for (String line : message.split("\n")) {
            to.sendMessage(line);
        }
    }

    /**
     * Sends {@code text} followed by a clickable button that runs {@code command}. When the server
     * cannot show clickable text, the command is written out instead.
     */
    public static void sendWithButton(Player player, String text, String label, String command, String hover) {
        try {
            List<BaseComponent> parts = new ArrayList<>();
            if (text != null && !text.isEmpty()) {
                parts.addAll(Arrays.asList(TextComponent.fromLegacyText(text + " ")));
            }
            TextComponent button = new TextComponent(TextComponent.fromLegacyText(label));
            button.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
            button.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hover)));
            parts.add(button);
            player.spigot().sendMessage(parts.toArray(new BaseComponent[0]));
        } catch (Throwable t) {
            player.sendMessage(text + " " + label + ChatColor.GRAY + " (" + command + ")");
        }
    }
}
