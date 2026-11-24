package io.github.natan307.cuiticket.config;

import io.github.natan307.cuiticket.CUITicketPlugin;
import io.github.natan307.cuiticket.util.Chat;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Texts from the {@code messages} section of config.yml. Missing keys fall back to the defaults
 * inside the jar. Placeholders such as {@code %id%} are filled in after the colors are applied,
 * so text typed by players can never add colors.
 */
public final class Messages {

    private final CUITicketPlugin plugin;

    public Messages(CUITicketPlugin plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration c() {
        return plugin.getConfig();
    }

    /** A colored message. {@code replacements} are pairs: placeholder, value, placeholder, value, ... */
    public String get(String key, String... replacements) {
        String raw = c().getString("messages." + key);
        if (raw == null) {
            return "<missing message: " + key + ">";
        }
        return fill(Chat.color(raw), replacements);
    }

    /** The message with the prefix in front. */
    public String prefixed(String key, String... replacements) {
        return Chat.color(c().getString("messages.prefix", "")) + get(key, replacements);
    }

    /** A message that is a list of lines (such as the help text). */
    public List<String> lines(String key, String... replacements) {
        List<String> out = new ArrayList<>();
        for (String line : c().getStringList("messages." + key)) {
            out.add(fill(Chat.color(line), replacements));
        }
        return out;
    }

    private static String fill(String text, String... replacements) {
        String result = text;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            result = result.replace(replacements[i], replacements[i + 1] == null ? "" : replacements[i + 1]);
        }
        return result;
    }
}
