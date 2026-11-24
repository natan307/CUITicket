package io.github.natan307.cuiticket.menu;

import io.github.natan307.crossui.api.CrossUI;
import io.github.natan307.crossui.api.platform.ClientPlatform;
import io.github.natan307.cuiticket.CUITicketPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Picks the button icon of a category for the player's client.
 *
 * <ul>
 *   <li>Bedrock forms show a picture from the CrossUI icon pack (a web address, {@code url:...}).</li>
 *   <li>Old Java clients get a chest menu, where the icon is the item itself.</li>
 *   <li>Java dialogs have no icons.</li>
 * </ul>
 */
public final class Icons {

    public enum Style {
        /** Bedrock form: picture from a web address. */
        URL,
        /** Chest menu: the item itself. */
        ITEM,
        /** Java dialog: no icons. */
        NONE
    }

    private final CUITicketPlugin plugin;
    private final Set<String> packIds = new HashSet<>();

    public Icons(CUITicketPlugin plugin) {
        this.plugin = plugin;
        try (InputStream in = Icons.class.getResourceAsStream("/pack-ids.txt")) {
            if (in != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        packIds.add(line);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not read pack-ids.txt, Bedrock pictures are off: " + e.getMessage());
        }
    }

    /** Looks at the player's client once, so a whole menu can reuse the answer. */
    public Style styleFor(Player player) {
        ClientPlatform platform;
        try {
            platform = CrossUI.platform(player);
        } catch (RuntimeException e) {
            return Style.NONE;
        }
        switch (platform) {
            case BEDROCK:
                return plugin.settings().icons() ? Style.URL : Style.NONE;
            case JAVA_LEGACY:
                return Style.ITEM;
            default:
                return Style.NONE;
        }
    }

    /** The icon descriptor for a CrossUI button, or {@code null} for no icon. */
    public String icon(Style style, String itemName) {
        if (itemName == null || itemName.isBlank()) {
            return null;
        }
        switch (style) {
            case URL:
                String id = itemName.trim().toLowerCase(Locale.ROOT);
                String base = plugin.settings().iconPackUrl();
                if (base == null || base.isBlank() || !packIds.contains(id)) {
                    return null;
                }
                return "url:" + (base.endsWith("/") ? base : base + "/") + id + ".png";
            case ITEM:
                Material material = Material.matchMaterial(itemName.trim());
                return material == null ? null : material.name();
            default:
                return null;
        }
    }
}
