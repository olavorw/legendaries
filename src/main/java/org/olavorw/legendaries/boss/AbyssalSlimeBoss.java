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
 * Controls an instance of The Abyssal Slime boss.
 */
public class AbyssalSlimeBoss {
    public static final String BOSS_PDC_KEY = "abyssal_slime";

    private final Plugin plugin;
    private final NamespacedKey bossKey;
    private Slime slime;
    private final List<BukkitTask> tasks = new ArrayList<>();
    private final Random random = new Random();

    private double lastHealth = -1;
    private final Set<UUID> shadowSlimes = new HashSet<>();

    public AbyssalSlimeBoss(Plugin plugin) {
        this.plugin = plugin;
        this.bossKey = new NamespacedKey(plugin, BOSS_PDC_KEY);
    }

    public Slime spawn(Location loc) {
        World world = loc.getWorld();
        if (world == null) throw new IllegalArgumentException("Location has no world");
        this.slime = (Slime) world.spawnEntity(loc, EntityType.SLIME);
        setupBossEntity(this.slime);
        startRunnables();
        world.playSound(loc, Sound.AMBIENT_CAVE, SoundCategory.HOSTILE, 2.0f, 0.6f);
        broadcastNear(slime.getLocation(), 40, Component.text("The Abyssal Slime stirs from the void...")
                .color(NamedTextColor.DARK_PURPLE));
        return slime;
    }

    private void setupBossEntity(Slime s) {
        s.customName(Component.text("The Abyssal Slime").color(NamedTextColor.DARK_PURPLE).decoration(TextDecoration.BOLD, true));
        s.setCustomNameVisible(true);
        s.setRemoveWhenFarAway(false);
        s.setCanPickupItems(false);
        s.setGlowing(true);
        s.setSize(4); // vanilla maximum; we then try to scale the model bigger

        // Tag as boss via PDC
        PersistentDataContainer pdc = s.getPersistentDataContainer();
        pdc.set(bossKey, PersistentDataType.INTEGER, 1);
        // Health 450 (between 400-500)
        var attr = s.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(450.0);
            s.setHealth(450.0);
        }
        // Try to enlarge model to ~8-10x if API supports scaling (1.20.5+)
        try {
            float scale = 8.0f + random.nextFloat() * 2.0f; // 8-10
            var m = s.getClass().getMethod("setScale", float.class);
            m.invoke(s, scale);
        } catch (Throwable ignored) {
            // ignore if not supported
        }
        // Team colored glow (purple)
        try {
            var board = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
            var team = board.getTeam("abyssal_slime");
            if (team == null) {
                team = board.registerNewTeam("abyssal_slime");
                team.color(NamedTextColor.DARK_PURPLE);
            }
            team.addEntity(s);
        } catch (Throwable ignored) {}
    }

    public static boolean isBoss(Plugin plugin, Entity entity) {
        if (!(entity instanceof LivingEntity le)) return false;
        NamespacedKey key = new NamespacedKey(plugin, BOSS_PDC_KEY);
        Integer val = le.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        return val != null && val == 1;
    }

    private void startRunnables() {
        // Aura particles and passive effects
        tasks.add(new BukkitRunnable() {
            @Override public void run() {
                if (!isValid()) { cancel(); return; }
                var w = slime.getWorld();
                var l = slime.getLocation().add(0, slime.getHeight() * 0.6, 0);
                w.spawnParticle(Particle.REVERSE_PORTAL, l, 20, 1.4, 0.8, 1.4, 0.02);
                w.spawnParticle(Particle.DRAGON_BREATH, l, 10, 1.2, 0.6, 1.2, 0.01);
                w.spawnParticle(Particle.SMALL_FLAME, l, 4, 0.8, 0.4, 0.8, 0);
            }
        }.runTaskTimer(plugin, 20L, 10L));

        // Unconscious Drain every second: 2 hearts (4.0) + slowness + darkness in 30 block radius
        tasks.add(new BukkitRunnable() {
            @Override public void run() {
                if (!isValid()) { cancel(); return; }
                unconciousDrain();
            }
        }.runTaskTimer(plugin, 60L, 20L));

        // Void Slam every ~14s
        tasks.add(new BukkitRunnable() {
            @Override public void run() {
                if (!isValid()) { cancel(); return; }
                voidSlam();
            }
        }.runTaskTimer(plugin, 120L, 20L * 14));

        // Reality Warp every ~25s
        tasks.add(new BukkitRunnable() {
            @Override public void run() {
                if (!isValid()) { cancel(); return; }
                realityWarp();
            }
        }.runTaskTimer(plugin, 200L, 20L * 25));

        // Damage detector for Split Echo + hurt particles (every 5 ticks)
        tasks.add(new BukkitRunnable() {
            @Override public void run() {
                if (!isValid()) { cancel(); return; }
                double hp = slime.getHealth();
                if (lastHealth < 0) { lastHealth = hp; return; }
                if (hp < lastHealth - 0.1) {
                    onDamaged();
                }
                lastHealth = hp;
            }
        }.runTaskTimer(plugin, 20L, 5L));

        // Shadow slime updater (movement + hit detection)
        tasks.add(new BukkitRunnable() {
            @Override public void run() {
                if (!isValid()) { cancel(); return; }
                updateShadowSlimes();
            }
        }.runTaskTimer(plugin, 40L, 2L));
    }

    private boolean isValid() {
        return slime != null && slime.isValid() && !slime.isDead();
    }

    private void unconciousDrain() {
        var w = slime.getWorld();
        double radius = 30.0;
        broadcastNear(slime.getLocation(), radius, Component.text("The Abyssal Slime drains your will...").color(NamedTextColor.DARK_PURPLE));
        w.playSound(slime.getLocation(), Sound.ENTITY_WARDEN_ANGRY, SoundCategory.HOSTILE, 1.4f, 0.5f);
        for (Player p : w.getPlayers()) {
            if (p.isDead()) continue;
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            double d2 = p.getLocation().distanceSquared(slime.getLocation());
            if (d2 <= radius * radius) {
                p.damage(4.0, slime);
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, true, true, true));
                // Screen darkening via Darkness
                try { p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 40, 0, true, true, true)); } catch (Throwable ignored) {}
                w.spawnParticle(Particle.SCULK_SOUL, p.getLocation().add(0, 1, 0), 6, 0.5, 0.5, 0.5, 0.01);
            }
        }
    }

    private void voidSlam() {
        var w = slime.getWorld();
        Location c = slime.getLocation();
        // Charge-up
        w.playSound(c, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 1.8f, 0.4f);
        broadcastNear(c, 30, Component.text("The Abyssal Slime gathers void energy...").color(NamedTextColor.DARK_PURPLE));
        // Short delay then slam
        new BukkitRunnable() { @Override public void run() {
            if (!isValid()) return;
            Location loc = slime.getLocation();
            w.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1, 0, 0, 0, 0);
            w.spawnParticle(Particle.REVERSE_PORTAL, loc, 80, 3, 0.8, 3, 0.1);
            w.spawnParticle(Particle.DRAGON_BREATH, loc, 50, 3, 0.8, 3, 0.02);
            w.playSound(loc, Sound.ENTITY_SLIME_DEATH, SoundCategory.HOSTILE, 2.0f, 0.5f);
            double radius = 20.0;
            for (Player p : w.getPlayers()) {
                if (p.isDead()) continue;
                if (p.getGameMode() == GameMode.SPECTATOR) continue;
                double d2 = p.getLocation().distanceSquared(loc);
                if (d2 <= radius * radius) {
                    p.damage(7.0, slime); // 6-8, use 7
                    Vector kb = p.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.2).setY(0.6);
                    p.setVelocity(p.getVelocity().add(kb));
                    w.spawnParticle(Particle.LARGE_SMOKE, p.getLocation(), 12, 0.6, 0.6, 0.6, 0.02);
                }
            }
        }}.runTaskLater(plugin, 30L);
    }

    private void realityWarp() {
        var w = slime.getWorld();
        Location c = slime.getLocation();
        broadcastNear(c, 30, Component.text("Reality warps around you!").color(NamedTextColor.DARK_PURPLE));
        w.playSound(c, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 1.4f, 0.5f);
        double radius = 24.0;
        for (Player p : w.getPlayers()) {
            if (p.isDead()) continue;
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            if (p.getLocation().distanceSquared(c) <= radius * radius) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 6, 0, true, true, true));
                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 4, 0, true, true, true));
                try { p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 20 * 6, 0, true, true, true)); } catch (Throwable ignored) {}
                w.spawnParticle(Particle.WARPED_SPORE, p.getLocation().add(0, 1, 0), 30, 0.7, 0.7, 0.7, 0.01);
            }
        }
    }

    private void onDamaged() {
        var w = slime.getWorld();
        Location l = slime.getLocation();
        // Hurt particles
        w.spawnParticle(Particle.LARGE_SMOKE, l.add(0, 1, 0), 20, 1.2, 0.6, 1.2, 0.02);
        w.spawnParticle(Particle.REVERSE_PORTAL, l, 30, 1.0, 0.6, 1.0, 0.02);
        w.playSound(l, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.HOSTILE, 1.2f, 0.6f);
        // Split Echo: spawn 2-4 shadow slimes
        int count = 2 + random.nextInt(3);
        for (int i = 0; i < count; i++) spawnShadowSlime();
    }

    private void spawnShadowSlime() {
        var w = slime.getWorld();
        Location base = slime.getLocation().clone().add(randomOffset(2.5));
        Slime s = (Slime) w.spawnEntity(base, EntityType.SLIME);
        s.setSize(1);
        s.customName(Component.text("Shadow Slime").color(NamedTextColor.DARK_PURPLE).decoration(TextDecoration.ITALIC, false));
        s.setGlowing(true);
        s.setCollidable(false);
        s.setRemoveWhenFarAway(true);
        // Tag via scoreboard tag for easier filtering
        s.addScoreboardTag("abyssal_shadow");
        shadowSlimes.add(s.getUniqueId());
        // Lifetime removal
        new BukkitRunnable() { @Override public void run() {
            if (s.isValid() && !s.isDead()) s.remove();
            shadowSlimes.remove(s.getUniqueId());
        }}.runTaskLater(plugin, 20L * 10);
    }

    private void updateShadowSlimes() {
        World w = slime.getWorld();
        if (w == null) return;
        List<UUID> toRemove = new ArrayList<>();
        for (UUID id : shadowSlimes) {
            Entity e = Bukkit.getEntity(id);
            if (!(e instanceof Slime s) || s.isDead() || !s.isValid()) { toRemove.add(id); continue; }
            Player target = nearestPlayer(s.getLocation());
            if (target != null) {
                Vector dir = target.getLocation().toVector().subtract(s.getLocation().toVector());
                if (dir.lengthSquared() > 0.0001) dir.normalize();
                Location next = s.getLocation().clone().add(dir.multiply(1.2));
                // Teleport forward small steps to "phase" through blocks
                s.teleport(next);
                // Hit detection
                if (s.getLocation().distanceSquared(target.getLocation()) < 1.5) {
                    target.damage(3.0, s);
                    w.spawnParticle(Particle.SCULK_SOUL, target.getLocation().add(0,1,0), 8, 0.5,0.5,0.5,0.01);
                }
            }
            // trailing particles
            w.spawnParticle(Particle.DRAGON_BREATH, s.getLocation().add(0,0.6,0), 4, 0.2,0.2,0.2,0.0);
            w.spawnParticle(Particle.REVERSE_PORTAL, s.getLocation(), 2, 0.2,0.2,0.2,0.0);
        }
        shadowSlimes.removeAll(toRemove);
    }

    private Player nearestPlayer(Location loc) {
        Player out = null;
        double best = Double.MAX_VALUE;
        World w = loc.getWorld();
        if (w == null) return null;
        for (Player p : w.getPlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            double d = p.getLocation().distanceSquared(loc);
            if (d < best) { best = d; out = p; }
        }
        return out;
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
        shadowSlimes.clear();
    }
}
