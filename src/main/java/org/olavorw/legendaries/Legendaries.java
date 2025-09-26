package org.olavorw.legendaries;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.olavorw.legendaries.commands.IronDaggerCommand;
import org.olavorw.legendaries.commands.CoreCommand;

public final class Legendaries extends JavaPlugin {

    private IronDaggerManager ironDaggerManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.ironDaggerManager = new IronDaggerManager(this);
        // Register listeners
        Bukkit.getPluginManager().registerEvents(new IronDaggerListener(this, ironDaggerManager), this);

        // Register crafting recipe for Legendary Echo Shard (Iron Dagger legacy)
        registerLegendaryEchoShardRecipe();

        // Start particle task for Cores when held
        startCoreParticlesTask();

        // Commands
        IronDaggerCommand cmd = new IronDaggerCommand(this, ironDaggerManager);
        if (getCommand("legendaryechoshard") != null) {
            getCommand("legendaryechoshard").setExecutor(cmd);
            getCommand("legendaryechoshard").setTabCompleter(cmd);
        }
        if (getCommand("irondagger") != null) { // legacy alias for backward compatibility
            getCommand("irondagger").setExecutor(cmd);
            getCommand("irondagger").setTabCompleter(cmd);
        }
        CoreCommand coreCmd = new CoreCommand(this, ironDaggerManager);
        if (getCommand("coreconsciousness") != null) {
            getCommand("coreconsciousness").setExecutor(coreCmd);
            getCommand("coreconsciousness").setTabCompleter(coreCmd);
        }
        if (getCommand("coreunconscious") != null) {
            getCommand("coreunconscious").setExecutor(coreCmd);
            getCommand("coreunconscious").setTabCompleter(coreCmd);
        }
    }

    private void registerLegendaryEchoShardRecipe() {
        NamespacedKey key = new NamespacedKey(this, "legendary_echo_shard_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, ironDaggerManager.createLegendaryEchoShard());
        recipe.shape("NCN", " U ", "   ");
        recipe.setIngredient('N', Material.NETHERITE_INGOT);
        recipe.setIngredient('C', new RecipeChoice.ExactChoice(ironDaggerManager.createCoreOfConsciousness()));
        recipe.setIngredient('U', new RecipeChoice.ExactChoice(ironDaggerManager.createCoreOfUnconscious()));
        Bukkit.addRecipe(recipe);
    }

    private void startCoreParticlesTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                ItemStack main = p.getInventory().getItemInMainHand();
                ItemStack off = p.getInventory().getItemInOffHand();
                boolean hasConscious = ironDaggerManager.isCoreOfConsciousness(main) || ironDaggerManager.isCoreOfConsciousness(off);
                boolean hasUnconscious = ironDaggerManager.isCoreOfUnconscious(main) || ironDaggerManager.isCoreOfUnconscious(off);
                if (!hasConscious && !hasUnconscious) continue;

                var world = p.getWorld();
                var loc = p.getLocation().add(0, 1.2, 0);
                if (hasConscious) {
                    // Glowing white/gold particle effects around the player
                    world.spawnParticle(Particle.END_ROD, loc, 6, 0.3, 0.3, 0.3, 0.0);
                    world.spawnParticle(Particle.FLAME, loc, 6, 0.3, 0.3, 0.3, 0.0);
                }
                if (hasUnconscious) {
                    // Dark purple/void particle effects around the player
                    world.spawnParticle(Particle.REVERSE_PORTAL, loc, 6, 0.3, 0.3, 0.3, 0.0);
                    world.spawnParticle(Particle.DRAGON_BREATH, loc, 4, 0.2, 0.4, 0.2, 0.0);
                }
            }
        }, 20L, 10L);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
