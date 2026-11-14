package com.cytricos.races.gui;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.manager.WingsManager;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.util.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Menú de Auras Cosméticas y Alas 3D Personalizadas para CYTRICOS RPG.
 * Permite equipar 6 auras de partículas ligeras y las 8 alas exclusivas
 * del Resource Pack independiente de CYTRICOS RPG.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class CosmeticsGUI {
    public static final String TITLE = ColorUtils.color("&8[&eCYTRICOS&8] &0Cosméticos y Alas 3D");
    public static final Component TITLE_COMPONENT = ColorUtils.component("&8[&eCYTRICOS&8] &0Cosméticos y Alas 3D");

    private final CytricosRaces plugin;

    public CosmeticsGUI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_COMPONENT);

        ItemStack bg = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, bg);
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        String activeAura = data.getSelectedCosmeticAura().toLowerCase();
        String activeWings = data.getSelectedWings().toLowerCase();

        // Slot 4: Cabecera informativa
        inv.setItem(4, PlayerGUI.createGuiItem(Material.AMETHYST_SHARD, "&d&lCentro de Cosméticos Independientes", List.of(
                "&7Aura Activa: &e" + activeAura.toUpperCase(),
                "&7Alas 3D Activas: &b" + activeWings.toUpperCase(),
                "&7Partículas en Ajustes: " + (data.isSettingParticles() ? "&aActivadas" : "&cDesactivadas"),
                "",
                "&7Sistema 100% gratuito sin ItemsAdder ni Oraxen.",
                "&7Compatible con el Resource Pack oficial de CYTRICOS."
        )));

        // FILA 1 (Slots 10-16): Auras de Partículas
        inv.setItem(10, createAuraItem("none", Material.BARRIER, "&c&lSin Aura",
                List.of("&7Desactiva el efecto orbital de partículas."), activeAura.equals("none")));
        inv.setItem(11, createAuraItem("flame", Material.BLAZE_POWDER, "&6&lLlama Abisal",
                List.of("&7Chispas ígneas y humo orbital."), activeAura.equals("flame")));
        inv.setItem(12, createAuraItem("fairy", Material.PRISMARINE_CRYSTALS, "&d&lBrillo Feérico",
                List.of("&7Polvo mágico resplandeciente."), activeAura.equals("fairy")));
        inv.setItem(13, createAuraItem("shadow", Material.ECHO_SHARD, "&8&lBruma Umbría",
                List.of("&7Llama azulada de almas y cenizas."), activeAura.equals("shadow")));
        inv.setItem(14, createAuraItem("holy", Material.NETHER_STAR, "&e&lResplandor Sagrado",
                List.of("&7Destellos dorados de purificación."), activeAura.equals("holy")));
        inv.setItem(15, createAuraItem("runic", Material.CHERRY_LEAVES, "&5&lChispas Rúnicas",
                List.of("&7Pétalos ancestrales y motas de portal."), activeAura.equals("runic")));

        // Separador visual
        inv.setItem(22, PlayerGUI.createGuiItem(Material.ELYTRA, "&b&lALAS COSMÉTICAS 3D (8 MODELOS)", List.of(
                "&7Selecciona un par de alas para mostrarlas en tu espalda",
                "&7mediante entidad ItemDisplay + Resource Pack.",
                "&7Los admins también pueden entregar élitros físicos."
        )));

        // FILA 3 y 4 (Slots 28-35): Las 8 Alas Personalizadas
        int[] wingSlots = {28, 29, 30, 31, 32, 33, 34, 38};
        int idx = 0;
        for (WingsManager.WingDefinition def : plugin.getWingsManager().getAllWings()) {
            if (idx >= wingSlots.length) break;
            boolean equipped = activeWings.equals(def.id);
            List<String> lore = new ArrayList<>();
            lore.add("&7" + def.description);
            lore.add("&7Raza Afín: &e" + def.recommendedRace.toUpperCase());
            lore.add("&8Modelo Pack: &fcytricos:" + def.id + " &7(CMD " + def.customModelData + ")");
            lore.add("");
            if (equipped) {
                lore.add("&a✔ ALAS EQUIPADAS ACTUALMENTE");
                lore.add("&c▶ Clic para quitar estas alas.");
            } else {
                lore.add("&e▶ Clic izquierdo para equipar alas en tu espalda.");
                if (player.hasPermission("cytricosraces.admin")) {
                    lore.add("&b▶ Clic derecho (Admin): Obtener Élitros 3D físicos.");
                }
            }

            ItemStack icon = new ItemStack(def.iconMaterial);
            ItemMeta meta = icon.getItemMeta();
            if (meta != null) {
                meta.displayName(ColorUtils.component((equipped ? "&a★ " : "") + def.displayName));
                meta.setCustomModelData(def.customModelData);
                meta.lore(ColorUtils.componentList(lore));
                icon.setItemMeta(meta);
            }
            inv.setItem(wingSlots[idx], icon);
            idx++;
        }

        // Slot 40: Quitar alas activas
        inv.setItem(40, PlayerGUI.createGuiItem(Material.RED_DYE, "&c&lQuitar Alas Actuales",
                List.of("&7Retira las alas cosméticas visibles de tu espalda.")));

        // Slot 42: Armería / Objetos Custom
        inv.setItem(42, PlayerGUI.createGuiItem(Material.NETHERITE_SWORD, "&6&lCatálogo de Reliquias y Objetos",
                List.of("&7Inspecciona las armas, amuletos y objetos custom", "&7incluidos en el Resource Pack oficial.")));

        // Slot 45: Volver
        inv.setItem(45, PlayerGUI.createGuiItem(Material.ARROW, "&eVolver al Menú Principal", List.of("&7Regresa al hub principal.")));

        // Slot 49: Aplicar Resource Pack Oficial
        inv.setItem(49, PlayerGUI.createGuiItem(Material.PAINTING, "&a&lInstalar Resource Pack Oficial",
                List.of("&7Solicita la descarga gratuita del Resource Pack", "&7para visualizar alas y modelos 3D reales.")));

        // Slot 53: Cerrar
        inv.setItem(53, PlayerGUI.createGuiItem(Material.BARRIER, "&c&lCerrar", null));

        player.openInventory(inv);
    }

    private ItemStack createAuraItem(String id, Material mat, String name, List<String> desc, boolean isActive) {
        List<String> lore = new ArrayList<>(desc);
        lore.add("");
        if (isActive) {
            lore.add("&a✔ AURA EQUIPADA ACTUALMENTE");
        } else {
            lore.add("&e▶ Clic para activar este efecto.");
        }
        return PlayerGUI.createGuiItem(mat, (isActive ? "&a★ " : "&7") + name, lore);
    }

    public void handleClick(Player player, int slot, boolean isRightClick) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        // Auras
        String selectedAura = switch (slot) {
            case 10 -> "none";
            case 11 -> "flame";
            case 12 -> "fairy";
            case 13 -> "shadow";
            case 14 -> "holy";
            case 15 -> "runic";
            default -> null;
        };

        if (selectedAura != null) {
            data.setSelectedCosmeticAura(selectedAura);
            plugin.getPlayerDataManager().saveToFile(data);
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
            if (!selectedAura.equals("none")) {
                player.getWorld().spawnParticle(Particle.GLOW, player.getLocation().add(0, 1.2, 0), 10, 0.4, 0.4, 0.4, 0.05);
                ColorUtils.sendMessage(player, "&aAura cosmética equipada: &e" + selectedAura.toUpperCase());
            } else {
                ColorUtils.sendMessage(player, "&7Aura cosmética desactivada.");
            }
            open(player);
            return;
        }

        // Alas (Slots 28-34 y 38)
        int[] wingSlots = {28, 29, 30, 31, 32, 33, 34, 38};
        List<WingsManager.WingDefinition> wingsList = new ArrayList<>(plugin.getWingsManager().getAllWings());
        for (int i = 0; i < wingSlots.length && i < wingsList.size(); i++) {
            if (wingSlots[i] == slot) {
                WingsManager.WingDefinition def = wingsList.get(i);
                if (isRightClick && player.hasPermission("cytricosraces.admin")) {
                    ItemStack elytra = plugin.getWingsManager().createWingElytraItem(def.id);
                    if (elytra != null) {
                        player.getInventory().addItem(elytra);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);
                        ColorUtils.sendMessage(player, "&aÉlitros personalizados agregados: " + def.displayName);
                    }
                    return;
                }

                if (data.getSelectedWings().equalsIgnoreCase(def.id)) {
                    plugin.getWingsManager().equipCosmeticWings(player, "none");
                    ColorUtils.sendMessage(player, "&7Has retirado tus alas cosméticas.");
                } else {
                    plugin.getWingsManager().equipCosmeticWings(player, def.id);
                    player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.2f);
                    ColorUtils.sendMessage(player, "&aAlas cosméticas equipadas: " + def.displayName);
                }
                open(player);
                return;
            }
        }

        if (slot == 40) {
            plugin.getWingsManager().equipCosmeticWings(player, "none");
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 0.8f);
            ColorUtils.sendMessage(player, "&7Alas cosméticas desactivadas.");
            open(player);
        } else if (slot == 42) {
            plugin.getCustomItemsGUI().open(player);
        } else if (slot == 49) {
            player.closeInventory();
            plugin.sendConfiguredResourcePack(player);
        }
    }
}
