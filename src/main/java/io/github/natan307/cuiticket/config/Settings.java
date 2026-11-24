package io.github.natan307.cuiticket.config;

import io.github.natan307.cuiticket.CUITicketPlugin;
import io.github.natan307.cuiticket.ticket.TicketCategory;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Reads config.yml. Values are read when needed, so {@code /ticketadmin reload} takes effect at once. */
public final class Settings {

    private final CUITicketPlugin plugin;
    private volatile Map<String, TicketCategory> categories = new LinkedHashMap<>();

    public Settings(CUITicketPlugin plugin) {
        this.plugin = plugin;
    }

    private FileConfiguration c() {
        return plugin.getConfig();
    }

    /** Reads the category list. Call after the config was (re)loaded. */
    public void load() {
        Map<String, TicketCategory> loaded = new LinkedHashMap<>();
        for (Map<?, ?> entry : c().getMapList("tickets.categories")) {
            Object rawId = entry.get("id");
            if (rawId == null || rawId.toString().isBlank()) {
                plugin.getLogger().warning("A category in config.yml has no id and was skipped.");
                continue;
            }
            String id = rawId.toString().trim().toUpperCase(Locale.ROOT).replace(' ', '_');
            Object rawLabel = entry.get("label");
            Object rawIcon = entry.get("icon");
            Object rawTarget = entry.get("requires-target");
            String label = rawLabel == null ? id : rawLabel.toString();
            String icon = rawIcon == null ? "" : rawIcon.toString().trim();
            boolean requiresTarget = rawTarget != null && Boolean.parseBoolean(rawTarget.toString());
            if (loaded.containsKey(id)) {
                plugin.getLogger().warning("Category " + id + " is listed twice, the second one was skipped.");
                continue;
            }
            loaded.put(id, new TicketCategory(id, label, icon, requiresTarget));
        }
        if (loaded.isEmpty()) {
            plugin.getLogger().warning("No valid ticket categories in config.yml, using a single \"General\" category.");
            loaded.put("GENERAL", new TicketCategory("GENERAL", "General", "", false));
        }
        this.categories = loaded;
    }

    // ------------------------------------------------------------ tickets

    public int maxOpenPerPlayer() {
        return Math.max(1, c().getInt("tickets.max-open-per-player", 3));
    }

    public int cooldownSeconds() {
        return Math.max(0, c().getInt("tickets.cooldown-seconds", 300));
    }

    public int reasonMaxLength() {
        return Math.max(20, c().getInt("tickets.reason-max-length", 300));
    }

    public int pageSize() {
        return Math.max(3, Math.min(40, c().getInt("tickets.page-size", 10)));
    }

    public int maxRepliesShown() {
        return Math.max(1, c().getInt("tickets.max-replies-shown", 5));
    }

    public List<TicketCategory> categories() {
        return new ArrayList<>(categories.values());
    }

    /** The category with this id, or null. */
    public TicketCategory category(String id) {
        return id == null ? null : categories.get(id.toUpperCase(Locale.ROOT));
    }

    /** The label of a category, or its id when the category was removed from the config. */
    public String categoryLabel(String id) {
        TicketCategory category = category(id);
        return category == null ? String.valueOf(id) : category.label();
    }

    // ------------------------------------------------------------ notifications

    public boolean notifyAdminsOnline() {
        return c().getBoolean("notifications.notify-admins-online", true);
    }

    public boolean notifyPlayerOnReply() {
        return c().getBoolean("notifications.notify-player-on-reply", true);
    }

    public boolean notifyPlayerOnClose() {
        return c().getBoolean("notifications.notify-player-on-close", true);
    }

    public String sound() {
        return c().getString("notifications.sound", "entity.experience_orb.pickup");
    }

    public int joinDelaySeconds() {
        return Math.max(1, c().getInt("notifications.join-delay-seconds", 3));
    }

    public int joinListMax() {
        return Math.max(0, Math.min(20, c().getInt("notifications.join-list-max", 5)));
    }

    // ------------------------------------------------------------ other

    public boolean icons() {
        return c().getBoolean("ui.icons", true);
    }

    public String iconPackUrl() {
        return c().getString("ui.icon-pack-url", "");
    }

    public int archiveAfterDays() {
        return Math.max(0, c().getInt("storage.archive-after-days", 30));
    }

    public boolean metricsEnabled() {
        return c().getBoolean("metrics.enabled", true);
    }
}
