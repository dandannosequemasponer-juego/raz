package com.cytricos.races.manager;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.util.ColorUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Gestor Independiente de Objetos Personalizados, Reliquias y Coleccionables para CYTRICOS RPG.
 * Separa claramente objetos Funcionales, Cosméticos (flotantes sobre cabeza) y Coleccionables,
 * asigna rarezas, requisitos de desbloqueo, clave PDC anti-falsificación y UUID único anti-duplicación.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class CustomItemManager {
    private final CytricosRaces plugin;
    private final NamespacedKey itemKey;
    private final NamespacedKey uniqueUidKey;
    private final Map<String, CustomItemSpec> itemRegistry = new LinkedHashMap<>();

    public enum ItemRarity {
        COMUN("&7Común"),
        RARO("&9Raro"),
        EPICO("&5Épico"),
        LEGENDARIO("&6Legendario"),
        MITICO("&d&lMítico");

        public final String label;

        ItemRarity(String label) {
            this.label = label;
        }
    }

    public enum ItemCategory {
        FUNCIONAL("&aFuncional", "Armas, herramientas y amuletos activos/pasivos"),
        COSMETICO("&bCosmético", "Accesorios 3D flotantes desacoplados del casco"),
        COLECCIONABLE("&eColeccionable", "Reliquias de prestigio y fragmentos");

        public final String label;
        public final String subtitle;

        ItemCategory(String label, String subtitle) {
            this.label = label;
            this.subtitle = subtitle;
        }
    }

    public static class CustomItemSpec {
        public final String id;
        public final Material material;
        public final int customModelData;
        public final String displayName;
        public final List<String> lore;
        public final boolean glow;
        public final ItemCategory category;
        public final ItemRarity rarity;
        public final int requiredLevel;
        public final String requiredRace;
        public final boolean stackable;

        public CustomItemSpec(String id, Material material, int customModelData, String displayName,
                              List<String> lore, boolean glow, ItemCategory category,
                              ItemRarity rarity, int requiredLevel, String requiredRace, boolean stackable) {
            this.id = id;
            this.material = material;
            this.customModelData = customModelData;
            this.displayName = displayName;
            this.lore = lore;
            this.glow = glow;
            this.category = category;
            this.rarity = rarity;
            this.requiredLevel = requiredLevel;
            this.requiredRace = requiredRace == null ? "" : requiredRace;
            this.stackable = stackable;
        }
    }

    public CustomItemManager(CytricosRaces plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "cytricos_item_id");
        this.uniqueUidKey = new NamespacedKey(plugin, "cytricos_item_uid");
        initRegistry();
    }

    private void initRegistry() {
        register(new CustomItemSpec(
                "vampire_blood_chalice", Material.POTION, 21001,
                "&4&lCáliz de Sangre Carmesí",
                List.of(
                        "&7Un cáliz antiguo rebosante de vitalidad escarlata.",
                        "&e▶ Clic derecho: &7Restaura salud, hambre y +35 de Recurso Racial (30s).",
                        "&b▶ En Mano Secundaria: &7+15% robo de vida cuerpo a cuerpo."
                ),
                true, ItemCategory.FUNCIONAL, ItemRarity.RARO, 1, "", false
        ));

        register(new CustomItemSpec(
                "abyssal_demon_blade", Material.NETHERITE_SWORD, 21002,
                "&c&lHoja Abisal del Averno",
                List.of(
                        "&7Forjada en las llamas profundas del Inframundo.",
                        "&e▶ Pasiva de Combate: &7Incendia objetivos e inflige daño ígneo extra.",
                        "&b▶ En Mano Secundaria: &7+10% daño contra entidades sin armadura."
                ),
                true, ItemCategory.FUNCIONAL, ItemRarity.LEGENDARIO, 3, "", false
        ));

        register(new CustomItemSpec(
                "celestial_seraph_staff", Material.BLAZE_ROD, 21003,
                "&f&lBáculo del Serafín",
                List.of(
                        "&7Reliquia luminosa de los guardianes alados.",
                        "&e▶ Clic derecho: &7Pulso curativo en radio de 5 bloques (25s).",
                        "&b▶ En Mano Secundaria: &7Regeneración I y Caída Lenta continuas."
                ),
                true, ItemCategory.FUNCIONAL, ItemRarity.EPICO, 2, "", false
        ));

        register(new CustomItemSpec(
                "dwarven_mithril_pickaxe", Material.DIAMOND_PICKAXE, 21004,
                "&6&lPico de Mithril Enano",
                List.of(
                        "&7Herramienta legendaria de las minas profundas.",
                        "&e▶ Al minar: &7Otorga Prisa Minera II breve en vetas.",
                        "&b▶ En Mano Secundaria: &7Prisa Minera I y Visión Nocturna subterránea."
                ),
                true, ItemCategory.FUNCIONAL, ItemRarity.EPICO, 2, "", false
        ));

        register(new CustomItemSpec(
                "fairy_dew_amulet", Material.FEATHER, 21005,
                "&d&lAmuleto de Rocío Feérico",
                List.of(
                        "&7Talismán ligero bendecido por el bosque encantado.",
                        "&e▶ Clic derecho: &7Impulso ascendente y Caída Lenta 6s (18s).",
                        "&b▶ En Mano Secundaria: &7Caída Lenta I y Salto Impulsado I continuos."
                ),
                true, ItemCategory.FUNCIONAL, ItemRarity.RARO, 1, "", false
        ));

        register(new CustomItemSpec(
                "elf_sylvan_bow", Material.BOW, 21006,
                "&a&lArco Silvano de Lórien",
                List.of(
                        "&7Tallado en madera viva de árboles milenarios.",
                        "&e▶ Pasiva de Disparo: &7Flechas veloces con estela esmeralda.",
                        "&b▶ En Mano Secundaria: &7Velocidad I y agilidad élfica continua."
                ),
                true, ItemCategory.FUNCIONAL, ItemRarity.EPICO, 2, "", false
        ));

        register(new CustomItemSpec(
                "celestial_crown", Material.CARVED_PUMPKIN, 21007,
                "&e&lCorona Celestial",
                List.of(
                        "&7Diadema real 3D flotante independiente del casco.",
                        "&e▶ Clic derecho o /raza cosmeticos: &7Equipa la entidad 3D",
                        "&7  sobre tu cabeza sin ocupar tu ranura de armadura."
                ),
                false, ItemCategory.COSMETICO, ItemRarity.LEGENDARIO, 1, "", false
        ));

        register(new CustomItemSpec(
                "cytricos_relic_coin", Material.GOLD_NUGGET, 21008,
                "&6&lReliquia Antigua de Cytricos",
                List.of(
                        "&7Moneda rúnica de eras antiguas y gran prestigio.",
                        "&b▶ En Mano Secundaria: &7Otorga Suerte I permanente."
                ),
                true, ItemCategory.COLECCIONABLE, ItemRarity.COMUN, 1, "", true
        ));

        register(new CustomItemSpec(
                "void_reaper_scythe", Material.NETHERITE_HOE, 21009,
                "&3&lGuadaña Segadora del Vacío",
                List.of(
                        "&7Arma mítica forjada con fragmentos del End.",
                        "&e▶ Pasiva: &7Drena vitalidad al golpear y aplica Marchitamiento.",
                        "&e▶ Clic derecho: &7Destello sombrío hacia adelante (16s).",
                        "&b▶ En Mano Secundaria: &7Visión Nocturna e inmunidad a Marchitamiento."
                ),
                true, ItemCategory.FUNCIONAL, ItemRarity.MITICO, 4, "", false
        ));

        register(new CustomItemSpec(
                "nature_warden_talisman", Material.EMERALD, 21010,
                "&a&lTalismán Guardián del Bosque",
                List.of(
                        "&7Amuleto ancestral imbricado con savia viva.",
                        "&e▶ Clic derecho: &7Absorción y Regeneración II durante 8s (24s).",
                        "&b▶ En Mano Secundaria: &7Regeneración bajo el sol y fertilidad vegetal."
                ),
                true, ItemCategory.FUNCIONAL, ItemRarity.EPICO, 2, "", false
        ));

        register(new CustomItemSpec(
                "phoenix_ember_ring", Material.BLAZE_POWDER, 21011,
                "&6&lAnillo de Brasas de Fénix",
                List.of(
                        "&7Sortija solar que conserva calor inmortal.",
                        "&e▶ Clic derecho: &7Resistencia al Fuego y Fuerza I durante 10s (30s).",
                        "&b▶ En Mano Secundaria: &7Resistencia al Fuego permanente, inmunidad",
                        "&7  a quemaduras y aura solar que enciende atacantes."
                ),
                true, ItemCategory.FUNCIONAL, ItemRarity.LEGENDARIO, 3, "", false
        ));

        register(new CustomItemSpec(
                "shadow_cloak_brooch", Material.PHANTOM_MEMBRANE, 21012,
                "&8&lBroche del Manto Sombrío",
                List.of(
                        "&7Accesorio sigiloso preferido por exploradores nocturnos.",
                        "&e▶ Clic derecho: &7Velo de Invisibilidad (5s) y Velocidad II (28s).",
                        "&b▶ En Mano Secundaria: &7Visión Nocturna y Velocidad I al agacharse."
                ),
                true, ItemCategory.FUNCIONAL, ItemRarity.EPICO, 2, "", false
        ));

        register(new CustomItemSpec(
                "dragon_heart_gem", Material.AMETHYST_SHARD, 21013,
                "&5&lCorazón Cristalino de Dragón",
                List.of(
                        "&7Cristal arcano que pulsa con energía dracónica pura.",
                        "&b▶ En Mano Secundaria: &7Resistencia I y Resistencia al Fuego constantes."
                ),
                true, ItemCategory.COLECCIONABLE, ItemRarity.MITICO, 1, "", true
        ));

        register(new CustomItemSpec(
                "astralis_halo", Material.CARVED_PUMPKIN, 21014,
                "&b&lAureola Astral Divina",
                List.of(
                        "&7Anillo luminoso 3D suspendido sobre la cabeza.",
                        "&e▶ Clic derecho o /raza cosmeticos: &7Equipa la aureola",
                        "&7  flotante sin ocupar tu ranura de casco."
                ),
                false, ItemCategory.COSMETICO, ItemRarity.MITICO, 1, "", false
        ));

        register(new CustomItemSpec(
                "talent_shard", Material.AMETHYST_SHARD, 21015,
                "&d&lFragmento de Talento Racial",
                List.of(
                        "&7Esencia cristalizada obtenida al cazar enemigos.",
                        "&e▶ Clic derecho: &7Consume el fragmento para obtener",
                        "&7  &b+1 Punto de Talento &7en tu Árbol de Talentos."
                ),
                true, ItemCategory.COLECCIONABLE, ItemRarity.RARO, 1, "", true
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

    public boolean meetsItemRequirements(Player player, PlayerData data, CustomItemSpec spec) {
        if (spec == null) return false;
        if (player != null && player.hasPermission("cytricosraces.admin")) {
            return true;
        }
        if (data == null) return false;
        if (data.getLevel() < spec.requiredLevel) {
            return false;
        }
        if (!spec.requiredRace.isEmpty() && !spec.requiredRace.equalsIgnoreCase(data.getRace())) {
            return false;
        }
        return true;
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

            // IMPORTANTE (Fase 3): Las coronas y aureolas NO deben llevar EquippableComponent en HEAD
            // para impedir que entren en el slot de casco y mantener protección real de casco solar.

            List<String> fullLore = new ArrayList<>(spec.lore);
            fullLore.add("");
            fullLore.add("&7Categoría: " + spec.category.label + " &8| &7Rareza: " + spec.rarity.label);
            if (spec.requiredLevel > 1 || !spec.requiredRace.isEmpty()) {
                String reqRace = spec.requiredRace.isEmpty() ? "Cualquier Raza" : spec.requiredRace.toUpperCase();
                fullLore.add("&7Requisito de uso: &eNivel " + spec.requiredLevel + "+ &8(" + reqRace + ")");
            } else {
                fullLore.add("&7Requisito de uso: &aSin restricción (Nivel 1+)");
            }
            fullLore.add("&8ID: &fcytricos:" + spec.id + " &7(CMD " + spec.customModelData + ")");
            meta.lore(ColorUtils.componentList(fullLore));

            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(itemKey, PersistentDataType.STRING, spec.id);
            if (!spec.stackable) {
                pdc.set(uniqueUidKey, PersistentDataType.STRING, UUID.randomUUID().toString());
            }

            if (spec.id.equals("abyssal_demon_blade")) {
                meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
                meta.addEnchant(Enchantment.SHARPNESS, 4, true);
            } else if (spec.id.equals("dwarven_mithril_pickaxe")) {
                meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
                meta.addEnchant(Enchantment.FORTUNE, 3, true);
            } else if (spec.id.equals("elf_sylvan_bow")) {
                meta.addEnchant(Enchantment.POWER, 4, true);
                meta.addEnchant(Enchantment.INFINITY, 1, true);
            } else if (spec.id.equals("void_reaper_scythe")) {
                meta.addEnchant(Enchantment.SHARPNESS, 3, true);
                meta.addEnchant(Enchantment.UNBREAKING, 3, true);
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

    public String getUniqueItemUid(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(uniqueUidKey, PersistentDataType.STRING);
    }

    public boolean isAnyCustomItem(ItemStack item) {
        return getCustomItemId(item) != null || isChangeTokenItem(item);
    }

    public void giveItemSafely(Player player, ItemStack item) {
        if (player == null || item == null || item.getType() == Material.AIR) return;
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        if (!leftovers.isEmpty()) {
            for (ItemStack rem : leftovers.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), rem);
            }
            ColorUtils.sendMessage(player, "&e&l[INVENTARIO] &7Tu inventario estaba lleno; el objeto cayó a tus pies.");
        }
    }

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
        return meta.hasDisplayName() && ColorUtils.toPlainText(meta.displayName()).equalsIgnoreCase(expectedName);
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
            ColorUtils.sendMessage(player, "&c¡Debes sostener un ítem en tu mano principal!");
            return;
        }

        FileConfiguration config = plugin.getConfig();
        config.set("race_change_settings.custom_item_change.item_spec.material", held.getType().name());

        ItemMeta meta = held.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName() && meta.displayName() != null) {
                config.set("race_change_settings.custom_item_change.item_spec.display_name",
                        ColorUtils.toPlainText(meta.displayName()));
            }
            if (meta.hasCustomModelData()) {
                config.set("race_change_settings.custom_item_change.item_spec.custom_model_data", meta.getCustomModelData());
            }
            if (meta.hasLore() && meta.lore() != null) {
                List<String> raw = new ArrayList<>();
                for (net.kyori.adventure.text.Component line : meta.lore()) {
                    raw.add(ColorUtils.toPlainText(line));
                }
                config.set("race_change_settings.custom_item_change.item_spec.lore", raw);
            }
        }
        plugin.saveConfig();
        ColorUtils.sendMessage(player, "&aEl ítem sostenido ha sido registrado como el nuevo Token Oficial de Cambio de Raza.");
    }
}
