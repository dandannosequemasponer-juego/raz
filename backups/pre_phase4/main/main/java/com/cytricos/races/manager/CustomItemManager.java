package com.cytricos.races.manager;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.util.ColorUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Gestor Independiente de Objetos Personalizados y Reliquias para CYTRICOS RPG.
 * Utiliza PersistentDataContainer (PDC) y CustomModelData vinculado al Resource Pack nativo,
 * sin dependencias de ItemsAdder ni Oraxen.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class CustomItemManager {
    private final CytricosRaces plugin;
    private final NamespacedKey itemKey;
    private final Map<String, CustomItemSpec> itemRegistry = new LinkedHashMap<>();

    public static class CustomItemSpec {
        public final String id;
        public final Material material;
        public final int customModelData;
        public final String displayName;
        public final List<String> lore;
        public final boolean glow;
        public final String category;

        public CustomItemSpec(String id, Material material, int customModelData, String displayName,
                              List<String> lore, boolean glow, String category) {
            this.id = id;
            this.material = material;
            this.customModelData = customModelData;
            this.displayName = displayName;
            this.lore = lore;
            this.glow = glow;
            this.category = category;
        }
    }

    public CustomItemManager(CytricosRaces plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "cytricos_item_id");
        initRegistry();
    }

    private void initRegistry() {
        register(new CustomItemSpec(
                "vampire_blood_chalice", Material.POTION, 21001,
                "&4&lCáliz de Sangre Carmesí",
                List.of(
                        "&7Un cáliz antiguo rebosante de vitalidad.",
                        "&e▶ Clic derecho: &7Restaura salud, hambre y",
                        "&7  +35 de Recurso Racial (Cooldown: 30s).",
                        "",
                        "&8Categoría: &cAmuleto / Consumible",
                        "&8ID: &fcytricos:vampire_blood_chalice &7(CMD 21001)"
                ),
                true, "Amuleto"
        ));

        register(new CustomItemSpec(
                "abyssal_demon_blade", Material.NETHERITE_SWORD, 21002,
                "&c&lHoja Abisal del Averno",
                List.of(
                        "&7Forjada en las llamas profundas del Inframundo.",
                        "&e▶ Pasiva de Combate: &7Incendia a tus objetivos",
                        "&7  e inflige daño ígneo adicional al impactar.",
                        "",
                        "&8Categoría: &4Arma Personalizada",
                        "&8ID: &fcytricos:abyssal_demon_blade &7(CMD 21002)"
                ),
                true, "Arma"
        ));

        register(new CustomItemSpec(
                "celestial_seraph_staff", Material.BLAZE_ROD, 21003,
                "&f&lBáculo del Serafín",
                List.of(
                        "&7Reliquia luminosa de los guardianes alados.",
                        "&e▶ Clic derecho: &7Emite un pulso curativo que",
                        "&7  regenera vida en un radio de 5 bloques (Cooldown: 25s).",
                        "",
                        "&8Categoría: &bArma / Soporte",
                        "&8ID: &fcytricos:celestial_seraph_staff &7(CMD 21003)"
                ),
                true, "Arma"
        ));

        register(new CustomItemSpec(
                "dwarven_mithril_pickaxe", Material.DIAMOND_PICKAXE, 21004,
                "&6&lPico de Mithril Enano",
                List.of(
                        "&7Herramienta legendaria de las minas profundas.",
                        "&e▶ Al minar: &7Otorga Prisa Minera II breve",
                        "&7  para perforar vetas minerales con rapidez.",
                        "",
                        "&8Categoría: &eHerramienta",
                        "&8ID: &fcytricos:dwarven_mithril_pickaxe &7(CMD 21004)"
                ),
                true, "Herramienta"
        ));

        register(new CustomItemSpec(
                "fairy_dew_amulet", Material.FEATHER, 21005,
                "&d&lAmuleto de Rocío Feérico",
                List.of(
                        "&7Talisman ligero bendecido por el bosque encantado.",
                        "&e▶ Clic derecho: &7Impulso suave ascendente y",
                        "&7  Caída Lenta protectora durante 6s (Cooldown: 18s).",
                        "",
                        "&8Categoría: &dAmuleto",
                        "&8ID: &fcytricos:fairy_dew_amulet &7(CMD 21005)"
                ),
                true, "Amuleto"
        ));

        register(new CustomItemSpec(
                "elf_sylvan_bow", Material.BOW, 21006,
                "&a&lArco Silvano de Lórien",
                List.of(
                        "&7Tallado en madera viva de árboles milenarios.",
                        "&e▶ Pasiva de Disparo: &7Estela de partículas verdes",
                        "&7  y bonificación de precisión en bosques.",
                        "",
                        "&8Categoría: &2Arma a Distancia",
                        "&8ID: &fcytricos:elf_sylvan_bow &7(CMD 21006)"
                ),
                true, "Arma"
        ));

        register(new CustomItemSpec(
                "celestial_crown", Material.CARVED_PUMPKIN, 21007,
                "&e&lCorona Celestial",
                List.of(
                        "&7Diadema real visible al equiparse en la cabeza.",
                        "&e▶ Apariencia: &7Modelo 3D personalizado del",
                        "&7  Resource Pack oficial de CYTRICOS RPG.",
                        "",
                        "&8Categoría: &6Armadura / Cosmético",
                        "&8ID: &fcytricos:celestial_crown &7(CMD 21007)"
                ),
                false, "Armadura"
        ));

        register(new CustomItemSpec(
                "cytricos_relic_coin", Material.GOLD_NUGGET, 21008,
                "&6&lReliquia Antigua de Cytricos",
                List.of(
                        "&7Moneda rúnica coleccionable de épocas pasadas.",
                        "&7Símbolo de prestigio y exploración en el reino.",
                        "",
                        "&8Categoría: &eColeccionable",
                        "&8ID: &fcytricos:cytricos_relic_coin &7(CMD 21008)"
                ),
                true, "Coleccionable"
        ));
    }

    private void register(CustomItemSpec spec) {
        itemRegistry.put(spec.id.toLowerCase(), spec);
    }

    public Collection<CustomItemSpec> getAllCustomItems() {
        return Collections.unmodifiableCollection(itemRegistry.values());
    }

    public CustomItemSpec getSpec(String id) {
        if (id == null) return null;
        return itemRegistry.get(id.toLowerCase());
    }

    public ItemStack createCustomItem(String id) {
        if (id == null) return null;
        if (id.equalsIgnoreCase("reincarnation_essence")) {
            return createChangeTokenItem();
        }
        CustomItemSpec spec = getSpec(id);
        if (spec == null) return null;

        ItemStack item = new ItemStack(spec.material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtils.component(spec.displayName));
            meta.setCustomModelData(spec.customModelData);
            meta.lore(ColorUtils.componentList(spec.lore));
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, spec.id);

            if (spec.id.equals("abyssal_demon_blade")) {
                meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
                meta.addEnchant(Enchantment.SHARPNESS, 4, true);
            } else if (spec.id.equals("dwarven_mithril_pickaxe")) {
                meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
                meta.addEnchant(Enchantment.FORTUNE, 3, true);
            } else if (spec.id.equals("elf_sylvan_bow")) {
                meta.addEnchant(Enchantment.POWER, 4, true);
                meta.addEnchant(Enchantment.INFINITY, 1, true);
            } else if (spec.glow) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    public String getCustomItemId(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
    }

    public boolean isAnyCustomItem(ItemStack item) {
        return getCustomItemId(item) != null || isChangeTokenItem(item);
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
            meta.displayName(ColorUtils.component(name));

            int cmd = config.getInt("race_change_settings.custom_item_change.item_spec.custom_model_data", 10099);
            meta.setCustomModelData(cmd);

            List<String> rawLore = config.getStringList("race_change_settings.custom_item_change.item_spec.lore");
            meta.lore(ColorUtils.componentList(rawLore));
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, "reincarnation_essence");

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

        String pdcId = meta.getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        if ("reincarnation_essence".equals(pdcId)) {
            return true;
        }

        int expectedCmd = plugin.getConfig().getInt("race_change_settings.custom_item_change.item_spec.custom_model_data", 10099);
        if (meta.hasCustomModelData() && meta.getCustomModelData() == expectedCmd) {
            return true;
        }

        String expectedName = ColorUtils.strip(
                plugin.getConfig().getString("race_change_settings.custom_item_change.item_spec.display_name", ""));
        return meta.hasDisplayName() && ChatColor.stripColor(meta.getDisplayName()).equalsIgnoreCase(expectedName);
    }

    public boolean hasChangeTokenItem(Player player) {
        if (player == null) return false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isChangeTokenItem(item)) {
                return true;
            }
        }
        return false;
    }

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
