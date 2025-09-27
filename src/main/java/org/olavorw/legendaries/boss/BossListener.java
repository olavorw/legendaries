package org.olavorw.legendaries.boss;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.olavorw.legendaries.IronDaggerManager;

public class BossListener implements Listener {
    private final Plugin plugin;
    private final IronDaggerManager manager;

    public BossListener(Plugin plugin, IronDaggerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Entity e = event.getEntity();
        if (EnlightenedEvokerBoss.isBoss(plugin, e)) {
            // Ensure 100% drop of Core of Consciousness, boss-exclusive
            event.getDrops().clear();
            event.getDrops().add(manager.createCoreOfConsciousness());
            // Celebrate nearby
            if (e.getWorld() != null) {
                e.getWorld().playSound(e.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.5f, 1.0f);
                for (Player p : e.getWorld().getPlayers()) {
                    if (p.getLocation().distanceSquared(e.getLocation()) <= 48 * 48) {
                        p.sendMessage(Component.text("The Enlightened Evoker has been defeated!").color(NamedTextColor.GOLD));
                    }
                }
            }
        } else if (AbyssalSlimeBoss.isBoss(plugin, e)) {
            // Ensure 100% drop of Core of Unconscious, boss-exclusive
            event.getDrops().clear();
            event.getDrops().add(manager.createCoreOfUnconscious());
            if (e.getWorld() != null) {
                e.getWorld().playSound(e.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 1.5f, 0.7f);
                for (Player p : e.getWorld().getPlayers()) {
                    if (p.getLocation().distanceSquared(e.getLocation()) <= 48 * 48) {
                        p.sendMessage(Component.text("The Abyssal Slime has been vanquished!").color(NamedTextColor.DARK_PURPLE));
                    }
                }
            }
        }
    }
}
