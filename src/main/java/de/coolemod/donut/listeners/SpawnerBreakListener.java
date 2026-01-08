package de.coolemod.donut.listeners;

import de.coolemod.donut.DonutPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class SpawnerBreakListener implements Listener {
    private final DonutPlugin plugin;

    public SpawnerBreakListener(DonutPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        plugin.getSpawnerManager().handleBlockBreak(e);
    }
}
