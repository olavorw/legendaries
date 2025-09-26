package org.olavorw.legendaries;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public final class IronDaggerListener implements Listener {
    private final Legendaries plugin;
    private final IronDaggerManager manager;

    static final class StreakInfo {
        UUID targetId;
        int count;
    }

    private final Map<UUID, StreakInfo> streaks = new HashMap<>(); // attacker UUID -> streak info
    private final List<Tier> tiers;
    private final boolean capAtLastTier;

    public IronDaggerListener(Legendaries plugin, IronDaggerManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        // Prefer new config section, fall back to legacy one for compatibility
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("legendary_echo_shard");
        if (section == null) {
            section = plugin.getConfig().getConfigurationSection("iron_dagger");
        }
        this.tiers = loadTiers(section);
        this.capAtLastTier = section == null || section.getBoolean("cap_at_last_tier", true);
    }

    private record Tier(int hits, double bonus) {}

    private List<Tier> loadTiers(ConfigurationSection section) {
        List<Tier> list = new ArrayList<>();
        if (section != null) {
            List<Map<?, ?>> raw = section.getMapList("tiers");
            for (Map<?, ?> m : raw) {
                Object hitsObj = m.get("hits");
                Object bonusObj = m.get("bonus");
                if (hitsObj instanceof Number && bonusObj instanceof Number) {
                    list.add(new Tier(((Number) hitsObj).intValue(), ((Number) bonusObj).doubleValue()));
                }
            }
        }
        if (list.isEmpty()) {
            list.add(new Tier(3, 2.0));
            list.add(new Tier(3, 3.0));
            list.add(new Tier(3, 4.5));
        }
        return List.copyOf(list);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(damager instanceof Player player)) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!manager.isIronDagger(item)) return;

        Entity victimEntity = event.getEntity();
        if (!(victimEntity instanceof LivingEntity victim)) return;

        UUID attackerId = player.getUniqueId();
        UUID targetId = victim.getUniqueId();

        StreakInfo info = streaks.computeIfAbsent(attackerId, k -> new StreakInfo());
        if (info.targetId == null || !info.targetId.equals(targetId)) {
            info.targetId = targetId;
            info.count = 1;
        } else {
            info.count += 1;
        }

        double bonus = getBonusForHitIndex(info.count);
        // Set base damage to 10 and add any applicable bonus
        event.setDamage(10.0 + Math.max(0, bonus));
        if (bonus > 0) {
            // Optional feedback via actionbar
            player.sendActionBar(Component.text("Legendary Echo Shard +" + format(bonus) + " (" + info.count + ")")
                    .color(NamedTextColor.GOLD));
        }
    }

    private static String format(double d) {
        if (Math.abs(d - Math.rint(d)) < 1e-9) return Integer.toString((int) Math.rint(d));
        return String.format(java.util.Locale.US, "%.1f", d);
    }

    private double getBonusForHitIndex(int hitIndex) {
        int remaining = hitIndex;
        double last = 0;
        for (Tier t : tiers) {
            last = t.bonus();
            if (remaining <= t.hits()) {
                return t.bonus();
            }
            remaining -= t.hits();
        }
        // Beyond configured tiers
        return capAtLastTier ? last : last; // both cases return last here; kept for potential future differentiation
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        streaks.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        UUID deadId = event.getEntity().getUniqueId();
        // Remove any streaks targeting this entity
        streaks.entrySet().removeIf(e -> {
            StreakInfo s = e.getValue();
            return s != null && deadId.equals(s.targetId);
        });

        // Rare drop logic for cores (only for mobs killed by players)
        if (!(event.getEntity() instanceof Player)) {
            Player killer = event.getEntity().getKiller();
            if (killer != null) {
                double r = java.util.concurrent.ThreadLocalRandom.current().nextDouble();
                if (r < 0.01) {
                    event.getDrops().add(manager.createCoreOfConsciousness());
                } else if (r < 0.02) {
                    event.getDrops().add(manager.createCoreOfUnconscious());
                }
            }
        }
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result == null || result.getType().isAir()) return;
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (manager.isIronDagger(ingredient)) {
                // Prevent using the Legendary Echo Shard in any default recipes
                event.getInventory().setResult(null);
                break;
            }
        }
    }
}
