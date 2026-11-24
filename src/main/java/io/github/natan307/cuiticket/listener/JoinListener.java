package io.github.natan307.cuiticket.listener;

import io.github.natan307.cuiticket.CUITicketPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Gives players their ticket updates and staff their waiting-tickets summary when they join. */
public final class JoinListener implements Listener {

    private final CUITicketPlugin plugin;

    public JoinListener(CUITicketPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.notifier().onJoin(event.getPlayer());
    }
}
