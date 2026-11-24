package io.github.natan307.cuiticket.metrics;

import io.github.natan307.cuiticket.CUITicketPlugin;
import io.github.natan307.cuiticket.ticket.TicketStatus;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;

/** bStats: anonymous usage numbers (storage type and ticket counts). No player data is sent. */
public final class MetricsManager {

    /** The bStats id of CUITicket. */
    private static final int PLUGIN_ID = 35529;

    private final CUITicketPlugin plugin;
    private Metrics metrics;

    public MetricsManager(CUITicketPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.settings().metricsEnabled()) {
            plugin.getLogger().info("bStats disabled by config.");
            return;
        }
        this.metrics = new Metrics(plugin, PLUGIN_ID);
        metrics.addCustomChart(new SimplePie("storage_type", plugin::storageType));
        metrics.addCustomChart(new SingleLineChart("total_tickets", () -> plugin.tickets().getTotalCount()));
        metrics.addCustomChart(new SingleLineChart("open_tickets",
                () -> plugin.tickets().findByStatus(TicketStatus.OPEN).size()));
        metrics.addCustomChart(new SimplePie("top_category", () -> plugin.tickets().getMostUsedCategory()));
        plugin.getLogger().info("bStats enabled (plugin id " + PLUGIN_ID + ").");
    }

    public void shutdown() {
        if (metrics != null) {
            metrics.shutdown();
        }
    }
}
