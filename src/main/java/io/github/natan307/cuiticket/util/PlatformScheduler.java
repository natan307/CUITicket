package io.github.natan307.cuiticket.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Runs tasks on the correct thread for Spigot, Paper, Purpur and Folia.
 *
 * <p>The plugin is compiled against spigot-api, which has no Folia scheduler classes, so the Folia
 * schedulers are reached through reflection. Other servers use the normal Bukkit scheduler.</p>
 */
public final class PlatformScheduler {

    private final Plugin plugin;
    private final boolean folia;

    private Method entityGetScheduler;
    private Method entityRun;
    private Method entityRunDelayed;

    public PlatformScheduler(Plugin plugin) {
        this.plugin = plugin;
        this.folia = classExists("io.papermc.paper.threadedregions.RegionizedServer");
        if (folia) {
            try {
                entityGetScheduler = Class.forName("org.bukkit.entity.Entity").getMethod("getScheduler");
                Class<?> entityScheduler = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
                entityRun = entityScheduler.getMethod("run", Plugin.class, Consumer.class, Runnable.class);
                entityRunDelayed = entityScheduler.getMethod("runDelayed", Plugin.class, Consumer.class,
                        Runnable.class, long.class);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Folia detected but its scheduler could not be resolved", e);
            }
        }
    }

    /** Runs the task on the thread that owns the player. */
    public void runForPlayer(Player player, Runnable task) {
        try {
            if (!folia) {
                Bukkit.getScheduler().runTask(plugin, task);
                return;
            }
            Object scheduler = entityGetScheduler.invoke(player);
            Consumer<Object> consumer = ignored -> task.run();
            entityRun.invoke(scheduler, plugin, consumer, null);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not schedule a task for " + player.getName(), e);
        }
    }

    /** Runs the task on the thread that owns the player after the given number of ticks. */
    public void runLaterForPlayer(Player player, Runnable task, long delayTicks) {
        long delay = Math.max(1L, delayTicks);
        try {
            if (!folia) {
                Bukkit.getScheduler().runTaskLater(plugin, task, delay);
                return;
            }
            Object scheduler = entityGetScheduler.invoke(player);
            Consumer<Object> consumer = ignored -> task.run();
            entityRunDelayed.invoke(scheduler, plugin, consumer, null, delay);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Could not schedule a task for " + player.getName(), e);
        }
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
