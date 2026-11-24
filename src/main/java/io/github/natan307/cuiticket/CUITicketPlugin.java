package io.github.natan307.cuiticket;

import io.github.natan307.crossui.api.CrossUI;
import io.github.natan307.cuiticket.command.AdminCommand;
import io.github.natan307.cuiticket.command.PlayerCommand;
import io.github.natan307.cuiticket.config.Messages;
import io.github.natan307.cuiticket.config.Settings;
import io.github.natan307.cuiticket.listener.JoinListener;
import io.github.natan307.cuiticket.menu.AdminMenu;
import io.github.natan307.cuiticket.menu.PlayerMenu;
import io.github.natan307.cuiticket.metrics.MetricsManager;
import io.github.natan307.cuiticket.notification.TicketNotifier;
import io.github.natan307.cuiticket.storage.SqlStorage;
import io.github.natan307.cuiticket.ticket.TicketManager;
import io.github.natan307.cuiticket.util.PlatformScheduler;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * CUITicket: a ticket system for reports, bugs, suggestions and questions. Players use forms
 * (Bedrock) or dialogs (Java) through CrossUI, staff manage tickets in menus.
 */
public final class CUITicketPlugin extends JavaPlugin {

    private static final String GITHUB = "https://github.com/natan307/CUITicket";

    private PlatformScheduler scheduler;
    private Settings settings;
    private Messages messages;
    private SqlStorage storage;
    private TicketManager tickets;
    private TicketNotifier notifier;
    private PlayerMenu playerMenu;
    private AdminMenu adminMenu;
    private MetricsManager metrics;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("Part of the CrossUI ecosystem.");

        if (!CrossUI.isAvailable()) {
            getLogger().severe("CrossUI is not enabled. CUITicket needs CrossUI 2.0.1 or newer.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.scheduler = new PlatformScheduler(this);
        this.settings = new Settings(this);
        this.settings.load();
        this.messages = new Messages(this);
        this.notifier = new TicketNotifier(this);

        this.storage = new SqlStorage(this);
        this.tickets = new TicketManager(this, storage);
        try {
            storage.init();
            tickets.load();
        } catch (Exception e) {
            getLogger().log(java.util.logging.Level.SEVERE, "Could not open the " + storage.typeName()
                    + " storage. Check storage in config.yml. CUITicket is disabled.", e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        int archived = tickets.archiveOlderThan(settings.archiveAfterDays());
        if (archived > 0) {
            getLogger().info("Deleted " + archived + " closed tickets older than " + settings.archiveAfterDays() + " days.");
        }

        this.playerMenu = new PlayerMenu(this);
        this.adminMenu = new AdminMenu(this);

        register("ticket", new PlayerCommand(this));
        register("ticketadmin", new AdminCommand(this));
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);

        CUITicket.setPlugin(this);

        this.metrics = new MetricsManager(this);
        this.metrics.start();

        getLogger().info("CUITicket v" + version() + " enabled with " + tickets.getTotalCount() + " tickets (" + storage.typeName() + ").");
        getLogger().info("GitHub:  " + GITHUB);
        getLogger().info("Wiki:    " + GITHUB + "/wiki");
        getLogger().info("Support: Paper, Spigot, Purpur, Folia");
    }

    @Override
    public void onDisable() {
        CUITicket.clear();
        if (metrics != null) {
            metrics.shutdown();
        }
        if (storage != null) {
            storage.close();
        }
    }

    /** Reloads config.yml. Changing the storage type needs a restart. */
    public void reloadAll() {
        reloadConfig();
        settings.load();
    }

    private <T extends CommandExecutor & TabCompleter> void register(String name, T handler) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command /" + name + " is missing from plugin.yml");
            return;
        }
        command.setExecutor(handler);
        command.setTabCompleter(handler);
    }

    /** The plugin version, read from a file inside the jar. */
    public String version() {
        try (java.io.InputStream in = getResource("plugin-version.txt")) {
            if (in != null) {
                return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
            }
        } catch (java.io.IOException ignored) {
            // fall through
        }
        return "?";
    }

    public String storageType() {
        return storage == null ? "none" : storage.typeName();
    }

    public PlatformScheduler scheduler() {
        return scheduler;
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public TicketManager tickets() {
        return tickets;
    }

    public TicketNotifier notifier() {
        return notifier;
    }

    public PlayerMenu playerMenu() {
        return playerMenu;
    }

    public AdminMenu adminMenu() {
        return adminMenu;
    }
}
