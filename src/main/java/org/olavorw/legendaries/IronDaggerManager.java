package org.olavorw.legendaries;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class IronDaggerManager {
    public static final String PDC_KEY_ITEM_ID = "legendary_id";
    public static final String IRON_DAGGER_ID = "iron_dagger"; // legacy ID for backward compatibility
    public static final String LEGENDARY_ECHO_SHARD_ID = "legendary_echo_shard";
    public static final String CORE_CONSCIOUSNESS_ID = "core_consciousness";
    public static final String CORE_UNCONSCIOUS_ID = "core_unconscious";

    private final Legendaries plugin;
    private final NamespacedKey keyItemId;

    public IronDaggerManager(Legendaries plugin) {
        this.plugin = plugin;
        this.keyItemId = new NamespacedKey(plugin, PDC_KEY_ITEM_ID);
    }

    public ItemStack createLegendaryEchoShard() {
        // Prefer new config section, fall back to legacy one for compatibility
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("legendary_echo_shard");
        if (section == null) {
            section = plugin.getConfig().getConfigurationSection("iron_dagger");
        }
        String materialName = section != null ? section.getString("material", "ECHO_SHARD") : "ECHO_SHARD";
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.ECHO_SHARD;

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();

        String name = section != null ? section.getString("name", "Legendary Echo Shard") : "Legendary Echo Shard";
        Component display = Component.text(name).color(NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false);
        meta.displayName(display);

        List<String> loreLines = section != null ? section.getStringList("lore") : List.of();
        List<Component> loreComponents = new ArrayList<>();
        for (String line : loreLines) {
            loreComponents.add(Component.text(line).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        if (!loreComponents.isEmpty()) {
            meta.lore(loreComponents);
        }

        // Mark as legendary echo shard via PDC; write new ID
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyItemId, PersistentDataType.STRING, LEGENDARY_ECHO_SHARD_ID);

        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Legacy name kept for source compatibility. Creates a Legendary Echo Shard.
     */
    public ItemStack createIronDagger() {
        return createLegendaryEchoShard();
    }

    public boolean isLegendaryEchoShard(ItemStack itemStack) {
        if (itemStack == null) return false;
        if (!itemStack.hasItemMeta()) return false;
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String id = pdc.get(keyItemId, PersistentDataType.STRING);
        return LEGENDARY_ECHO_SHARD_ID.equals(id) || IRON_DAGGER_ID.equals(id);
    }

    /**
     * Create Core of Consciousness item with tooltip and PDC id.
     */
    public ItemStack createCoreOfConsciousness() {
        ItemStack stack = new ItemStack(Material.GLOWSTONE_DUST);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("Core of Consciousness").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("\"The essence of awakened mind\"").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(keyItemId, PersistentDataType.STRING, CORE_CONSCIOUSNESS_ID);
        stack.setItemMeta(meta);
        return stack;
    }

    /**
     * Create Core of Unconscious item with tooltip and PDC id.
     */
    public ItemStack createCoreOfUnconscious() {
        ItemStack stack = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text("Core of Unconscious").color(NamedTextColor.DARK_PURPLE).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("\"The depths of hidden thoughts\"").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(keyItemId, PersistentDataType.STRING, CORE_UNCONSCIOUS_ID);
        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isCoreOfConsciousness(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return false;
        String id = itemStack.getItemMeta().getPersistentDataContainer().get(keyItemId, PersistentDataType.STRING);
        return CORE_CONSCIOUSNESS_ID.equals(id);
    }

    public boolean isCoreOfUnconscious(ItemStack itemStack) {
        if (itemStack == null || !itemStack.hasItemMeta()) return false;
        String id = itemStack.getItemMeta().getPersistentDataContainer().get(keyItemId, PersistentDataType.STRING);
        return CORE_UNCONSCIOUS_ID.equals(id);
    }

    /**
     * Legacy name kept for source compatibility. Checks for Legendary Echo Shard.
     */
    public boolean isIronDagger(ItemStack itemStack) {
        return isLegendaryEchoShard(itemStack);
    }
}
