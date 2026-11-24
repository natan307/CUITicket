package io.github.natan307.cuiticket;

import io.github.natan307.cuiticket.ticket.TicketManager;

/**
 * Entry point for other plugins.
 *
 * <pre>{@code
 * TicketManager tickets = CUITicket.getTicketManager();
 * tickets.create(player.getUniqueId(), player.getName(), "BUG_REPORT", null, "Plugin X error when clicking", null);
 * }</pre>
 *
 * <p>Add {@code depend: [CUITicket]} (or {@code softdepend}) to your plugin.yml and compile against
 * the CUITicket jar.</p>
 */
public final class CUITicket {

    private static volatile CUITicketPlugin plugin;

    private CUITicket() {
    }

    static void setPlugin(CUITicketPlugin instance) {
        plugin = instance;
    }

    static void clear() {
        plugin = null;
    }

    /** Whether CUITicket is enabled and ready. */
    public static boolean isAvailable() {
        return plugin != null;
    }

    /**
     * The ticket manager.
     *
     * @throws IllegalStateException when CUITicket is not enabled
     */
    public static TicketManager getTicketManager() {
        CUITicketPlugin instance = plugin;
        if (instance == null) {
            throw new IllegalStateException("CUITicket is not enabled.");
        }
        return instance.tickets();
    }
}
