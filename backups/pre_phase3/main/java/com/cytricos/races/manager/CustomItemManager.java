package com.cytricos.races.manager;

import com.cytricos.races.CytricosRaces;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Gestor del Ítem Especial de Reencarnación Ancestral (Custom Lore Token).
 * Material: NETHER_STAR, CustomModelData: 10099, Glow y Lore dinámico.
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class CustomItemManager {
    private final CytricosRaces plugin;

    public CustomItemManager(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    /**
     * Construye un ItemStack del ítem especial de cambio de raza configurado.
     */
    public ItemStack createChangeTokenItem() {
        FileConfiguration config = plugin.getConfig();
        String matName = config.getString("race_change_settings.custom_item_change.item_spec.material", "NETHER_STAR");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.NETHER_STAR;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = config.getString("race_change_settings.custom_item_change.item_spec.display_name",
                    "&6&lEsencia de Reencarnación Ancestral");
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));

            int cmd = config.getInt("race_change_settings.custom_item_change.item_spec.custom_model_data", 10099);
            meta.setCustomModelData(cmd);

            List<String> rawLore = config.getStringList("race_change_settings.custom_item_change.item_spec.lore");
            List<String> coloredLore = new ArrayList<>();
            for (String line : rawLore) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            meta.setLore(coloredLore);

            if (config.getBoolean("race_change_settings.custom_item_change.item_spec.glow", true)) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Verifica si el ítem entregado corresponde al token especial de reencarnación.
     */
    public boolean isChangeTokenItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        int expectedCmd = plugin.getConfig().getInt("race_change_settings.custom_item_change.item_spec.custom_model_data", 10099);
        if (meta.hasCustomModelData() && meta.getCustomModelData() == expectedCmd) {
            return true;
        }

        String expectedName = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("race_change_settings.custom_item_change.item_spec.display_name", ""));
        return meta.hasDisplayName() && meta.getDisplayName().equalsIgnoreCase(expectedName);
    }

    /**
     * Verifica si el jugador tiene el token en su inventario.
     */
    public boolean hasChangeTokenItem(Player player) {
        if (player == null) return false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isChangeTokenItem(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Consume la cantidad especificada de tokens del inventario del jugador.
     */
    public boolean consumeChangeTokenItem(Player player, int amount) {
        if (player == null || amount <= 0) return false;
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();

        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (isChangeTokenItem(item)) {
                int count = item.getAmount();
                if (count <= remaining) {
                    player.getInventory().setItem(i, null);
                    remaining -= count;
                } else {
                    item.setAmount(count - remaining);
                    remaining = 0;
                }
                if (remaining <= 0) {
                    player.updateInventory();
                    return true;
                }
            }
        }
        player.updateInventory();
        return false;
    }

    /**
     * Guarda el ítem que el admin tiene en mano como la nueva especificación del token en config.yml.
     */
    public void setHeldItemAsToken(Player player) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "¡Debes sostener un ítem en tu mano principal!");
            return;
        }

        FileConfiguration config = plugin.getConfig();
        config.set("race_change_settings.custom_item_change.item_spec.material", held.getType().name());

        ItemMeta meta = held.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                config.set("race_change_settings.custom_item_change.item_spec.display_name",
                        meta.getDisplayName().replace('§', '&'));
            }
            if (meta.hasCustomModelData()) {
                config.set("race_change_settings.custom_item_change.item_spec.custom_model_data", meta.getCustomModelData());
            }
            if (meta.hasLore()) {
                List<String> raw = new ArrayList<>();
                for (String l : meta.getLore()) {
                    raw.add(l.replace('§', '&'));
                }
                config.set("race_change_settings.custom_item_change.item_spec.lore", raw);
            }
        }
        plugin.saveConfig();
        player.sendMessage(ChatColor.GREEN + "El ítem sostenido ha sido registrado como el nuevo Token Oficial de Cambio de Raza.");
    }
}
