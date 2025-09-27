package org.olavorw.legendaries.boss;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Controls an instance of the Enlightened Evoker boss.
 */
public class EnlightenedEvokerBoss {
    public static final String BOSS_PDC_KEY = "enlightened_evoker";

    private final Plugin plugin;
    private final NamespacedKey bossKey;
    private Evoker evoker;
    private final List<BukkitTask> tasks = new ArrayList<>();
    private final Random random = new Random();

    private static final List<Component> CHAT_LINES = List.of(
            Component.text("\u201cYour consciousness will be mine!\u201d").color(NamedTextColor.GOLD),
            Component.text("\u201cFeel the weight of true awareness!\u201d").color(NamedTextColor.GOLD),
            Component.text("\u201cEnlightenment comes through suffering!\u201d").color(NamedTextColor.GOLD)
    );

    public EnlightenedEvokerBoss(Plugin plugin) {
        this.plugin = plugin;
        this.bossKey = new NamespacedKey(plugin, BOSS_PDC_KEY);
    }

    public Evoker spawn(Location loc) {
        World world = loc.getWorld();
        if (world == null) throw new IllegalArgumentException("Location has no world");
        this.evoker = (Evoker) world.spawnEntity(loc, EntityType.EVOKER);
        setupBossEntity(this.evoker);
        startRunnables();
        broadcastNear(evoker.getLocation(), 32, CHAT_LINES.get(random.nextInt(CHAT_LINES.size())));
        world.playSound(loc, Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 2.0f, 0.8f);
        return evoker;
    }

    private void setupBossEntity(Evoker e) {
        e.customName(Component.text("The Enlightened Evoker").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        e.setCustomNameVisible(true);
        e.setRemoveWhenFarAway(false);
        e.setCanPickupItems(false);
        e.setGlowing(true);
        // Tag as boss via PDC
        PersistentDataContainer pdc = e.getPersistentDataContainer();
        pdc.set(bossKey, PersistentDataType.INTEGER, 1);
        // Health 250 (between 200-300)
        var attr = e.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(250.0);
            e.setHealth(250.0);
        }
        // Try to enlarge model to ~1.5x if API supports scaling (1.20.5+)
        try {
            var m = e.getClass().getMethod("setScale", float.class);
            m.invoke(e, 1.5f);
        } catch (Throwable ignored) {
            // ignore if not supported
        }
        // Team colored glow (gold)
        try {
            var board = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
            var team = board.getTeam("enlightened_evoker");
            if (team == null) {
                team = board.registerNewTeam("enlightened_evoker");
                team.color(NamedTextColor.GOLD);
            }
            team.addEntity(e);
        } catch (Throwable ignored) {}
    }

    public static boolean isBoss(Plugin plugin, Entity entity) {
        if (!(entity instanceof LivingEntity le)) return false;
        NamespacedKey key = new NamespacedKey(plugin, BOSS_PDC_KEY);
        Integer val = le.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        return val != null && val == 1;
    }

    private void startRunnables() {
        // Aura particles task
        tasks.add(new BukkitRunnable() {
            @Override public void run() {
                if (!isValid()) { cancel(); return; }
                var w = evoker.getWorld();
                var l = evoker.getLocation().add(0, 1.2, 0);
                w.spawnParticle(Particle.END_ROD, l, 10, 0.5, 0.5, 0.5, 0);
                w.spawnParticle(Particle.GLOW, l, 6, 0.4, 0.4, 0.4, 0);
                w.spawnParticle(Particle.TOTEM_OF_UNDYING, l, 2, 0.2, 0.2, 0.2, 0);
            }
        }.runTaskTimer(plugin, 20L, 10L));

        // Vex swarm every 15s (6-8 vexes)
        tasks.add(new BukkitRunnable() {
            @Override public void run() {
                if (!isValid()) { cancel(); return; }
                castVexSwarm();
            }
        }.runTaskTimer(plugin, 100L, 20L * 15));

        // Fangs barrage every 12s
        tasks.add(new BukkitRunnable() {
            @Override public void run() {
                if (!isValid()) { cancel(); return; }
                castFangsBarrage();
            }
        }.runTaskTimer(plugin, 140L, 20L * 12));

        // Mind blast every 18s
        tasks.add(new BukkitRunnable() {
            @Override public void run() {
                if (!isValid()) { cancel(); return; }
                castMindBlast();
            }
        }.runTaskTimer(plugin, 180L, 20L * 18));

        // Teleport spam with randomized interval 8-10s
        scheduleNextTeleport(20L * (8 + random.nextInt(3)));
    }

    private void scheduleNextTeleport(long delay) {
        tasks.add(new BukkitRunnable() {
            @Override public void run() {
                if (!isValid()) { cancel(); return; }
                doTeleport();
                scheduleNextTeleport(20L * (8 + random.nextInt(3)));
            }
        }.runTaskLater(plugin, delay));
    }

    private boolean isValid() {
        return evoker != null && evoker.isValid() && !evoker.isDead();
    }

    private void castVexSwarm() {
        var w = evoker.getWorld();
        int count = 6 + random.nextInt(3); // 6-8
        broadcastNear(evoker.getLocation(), 32, Component.text("The Enlightened Evoker summons a vex swarm!").color(NamedTextColor.GOLD));
        w.playSound(evoker.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 2f, 1.0f);
        LivingEntity target = findTarget();
        for (int i = 0; i < count; i++) {
            Location base = evoker.getLocation().clone().add(randomOffset(2.5));
            Vex vex = (Vex) w.spawnEntity(base, EntityType.VEX);
            if (target != null) vex.setTarget(target);
            try { vex.setCharging(true); } catch (Throwable ignored) {}
        }
    }

    private void castFangsBarrage() {
        var w = evoker.getWorld();
        broadcastNear(evoker.getLocation(), 32, Component.text("Fangs barrage!").color(NamedTextColor.GOLD));
        w.playSound(evoker.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.HOSTILE, 2f, 1.2f);
        LivingEntity target = findTarget();
        Location center = target != null ? target.getLocation() : evoker.getLocation();
        // Create overlapping rings of fangs around the center
        for (double r = 1.5; r <= 4.5; r += 1.5) {
            int points = (int) Math.round(8 + r * 4);
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI / points) * i;
                double x = center.getX() + r * Math.cos(angle);
                double z = center.getZ() + r * Math.sin(angle);
                Location l = new Location(center.getWorld(), x, center.getY(), z);
                l = findGround(l);
                EvokerFangs fangs = (EvokerFangs) w.spawnEntity(l, EntityType.EVOKER_FANGS);
                try { fangs.setOwner(evoker); } catch (Throwable ignored) {}
            }
        }
    }

    private void castMindBlast() {
        var w = evoker.getWorld();
        broadcastNear(evoker.getLocation(), 32, Component.text("Mind Blast!").color(NamedTextColor.GOLD));
        w.playSound(evoker.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 2f, 1.2f);
        double radius = 12.0;
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(evoker.getLocation()) <= radius * radius) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 5, 0, true, true, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 5, 0, true, true, true));
                w.spawnParticle(Particle.SONIC_BOOM, p.getLocation(), 1, 0, 0, 0, 0);
            }
        }
    }

    private void doTeleport() {
        var w = evoker.getWorld();
        Location from = evoker.getLocation();
        w.spawnParticle(Particle.GLOW, from.clone().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
        w.spawnParticle(Particle.END_ROD, from.clone().add(0, 1, 0), 30, 0.6, 0.6, 0.6, 0.01);
        w.playSound(from, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.6f, 1.4f);
        Location dest = findTeleportDestination(from, 10);
        if (dest != null) {
            evoker.teleport(dest);
            w.spawnParticle(Particle.TOTEM_OF_UNDYING, dest.clone().add(0, 1, 0), 20, 0.3, 0.6, 0.3, 0.01);
            w.spawnParticle(Particle.END_ROD, dest.clone().add(0, 1, 0), 30, 0.6, 0.6, 0.6, 0.01);
            w.playSound(dest, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 1.2f, 1.8f);
        }
    }

    private LivingEntity findTarget() {
        // Prefer nearest player
        double best = Double.MAX_VALUE;
        LivingEntity picked = null;
        for (Player p : evoker.getWorld().getPlayers()) {
            double d = p.getLocation().distanceSquared(evoker.getLocation());
            if (d < best) { best = d; picked = p; }
        }
        return picked;
    }

    private Location findTeleportDestination(Location center, int radius) {
        World w = center.getWorld();
        if (w == null) return null;
        for (int i = 0; i < 12; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double r = 2 + random.nextDouble() * radius;
            double x = center.getX() + Math.cos(angle) * r;
            double z = center.getZ() + Math.sin(angle) * r;
            Location l = new Location(w, x, center.getY(), z);
            l = findGround(l);
            if (l != null) return l.add(0, 1, 0);
        }
        return null;
    }

    private Location findGround(Location l) {
        World w = l.getWorld();
        if (w == null) return l;
        int y = l.getBlockY();
        y = Math.max(2, Math.min(w.getMaxHeight() - 2, y));
        // Search downwards for solid ground
        for (int i = 0; i < 16; i++) {
            Location test = new Location(w, l.getX(), y - i, l.getZ());
            if (test.getBlock().getType().isSolid()) {
                return test;
            }
        }
        return l;
    }

    private Vector randomOffset(double radius) {
        double a = random.nextDouble() * Math.PI * 2;
        double r = random.nextDouble() * radius;
        return new Vector(Math.cos(a) * r, 0, Math.sin(a) * r);
    }

    private void broadcastNear(Location loc, double radius, Component component) {
        World w = loc.getWorld();
        if (w == null) return;
        double r2 = radius * radius;
        for (Player p : w.getPlayers()) {
            if (p.getLocation().distanceSquared(loc) <= r2) {
                p.sendMessage(component);
            }
        }
    }

    public void cleanup() {
        for (BukkitTask t : tasks) {
            try { t.cancel(); } catch (Throwable ignored) {}
        }
        tasks.clear();
    }
}
