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
    public static final String IRON_DAGGER_ID = "iron_dagger";

    private final Legendaries plugin;
    private final NamespacedKey keyItemId;

    public IronDaggerManager(Legendaries plugin) {
        this.plugin = plugin;
        this.keyItemId = new NamespacedKey(plugin, PDC_KEY_ITEM_ID);
    }

    public ItemStack createIronDagger() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("iron_dagger");
        String materialName = section != null ? section.getString("material", "IRON_SWORD") : "IRON_SWORD";
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.IRON_SWORD;

        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();

        String name = section != null ? section.getString("name", "Deathripper Dagger") : "Deathripper Dagger";
        Component display = Component.text(name).color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true);
        meta.displayName(display);

        List<String> loreLines = section != null ? section.getStringList("lore") : List.of();
        List<Component> loreComponents = new ArrayList<>();
        for (String line : loreLines) {
            loreComponents.add(Component.text(line).color(NamedTextColor.GRAY));
        }
        if (!loreComponents.isEmpty()) {
            meta.lore(loreComponents);
        }

        // Mark as legendary iron dagger via PDC
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyItemId, PersistentDataType.STRING, IRON_DAGGER_ID);

        stack.setItemMeta(meta);
        return stack;
    }

    public boolean isIronDagger(ItemStack itemStack) {
        if (itemStack == null) return false;
        if (!itemStack.hasItemMeta()) return false;
        ItemMeta meta = itemStack.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String id = pdc.get(keyItemId, PersistentDataType.STRING);
        return IRON_DAGGER_ID.equals(id);
    }
}
