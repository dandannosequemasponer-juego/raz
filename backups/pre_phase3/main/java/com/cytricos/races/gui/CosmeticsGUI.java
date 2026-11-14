package com.cytricos.races.gui;

import com.cytricos.races.CytricosRaces;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Menú de Auras y Efectos Cosméticos para CYTRICOS RPG.
 * Permite seleccionar auras de partículas orbitales ligeras y elegantes
 * que no generan lag ni obstruyen la visión de combate.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class CosmeticsGUI {
    public static final String TITLE = ColorUtils.color("&8[&eCYTRICOS&8] &0Auras y Cosméticos");
    public static final Component TITLE_COMPONENT = ColorUtils.component("&8[&eCYTRICOS&8] &0Auras y Cosméticos");

    private final CytricosRaces plugin;

    public CosmeticsGUI(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, TITLE_COMPONENT);

        ItemStack bg = PlayerGUI.createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 36; i++) {
            inv.setItem(i, bg);
        }

        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        String active = data.getSelectedCosmeticAura().toLowerCase();

        // Slot 4: Cabecera informativa
        inv.setItem(4, PlayerGUI.createGuiItem(Material.AMETHYST_SHARD, "&d&lAuras de Partículas Orbitantes", List.of(
                "&7Aura Activa: &e" + active.toUpperCase(),
                "&7Partículas Habilitadas en Ajustes: " + (data.isSettingParticles() ? "&aSí" : "&cNo (desactivadas)"),
                "",
                "&7Selecciona un aura para que te acompañe en tus viajes.",
                "&7Diseñadas con densidad baja para no afectar el rendimiento."
        )));

        // Slot 11: Desactivar Aura (none)
        inv.setItem(11, createAuraItem("none", Material.BARRIER, "&c&lDesactivar Aura",
                List.of("&7Quita cualquier efecto de partículas orbital."), active.equals("none")));

        // Slot 12: Llama Abisal (flame)
        inv.setItem(12, createAuraItem("flame", Material.BLAZE_POWDER, "&6&lLlama Abisal",
                List.of("&7Chispas de fuego y humo ardiente que orbitan tu cuerpo."), active.equals("flame")));

        // Slot 13: Brillo Feérico (fairy)
        inv.setItem(13, createAuraItem("fairy", Material.PRISMARINE_CRYSTALS, "&d&lBrillo Feérico",
                List.of("&7Polvo mágico resplandeciente y destellos celestes."), active.equals("fairy")));

        // Slot 14: Bruma Umbría (shadow)
        inv.setItem(14, createAuraItem("shadow", Material.ECHO_SHARD, "&8&lBruma Umbría",
                List.of("&7Llama de almas azuladas y cenizas etéreas."), active.equals("shadow")));

        // Slot 15: Resplandor Sagrado (holy)
        inv.setItem(15, createAuraItem("holy", Material.NETHER_STAR, "&e&lResplandor Sagrado",
                List.of("&7Destellos dorados y brillo bendito de purificación."), active.equals("holy")));

        // Slot 22: Chispas Rúnicas (runic)
        inv.setItem(22, createAuraItem("runic", Material.CHERRY_LEAVES, "&5&lChispas Rúnicas",
                List.of("&7Pétalos rosados y fragmentos de portales ancestrales."), active.equals("runic")));

        // Slot 27: Volver
        inv.setItem(27, PlayerGUI.createGuiItem(Material.ARROW, "&eVolver al Menú Principal", List.of("&7Regresa al hub principal.")));

        // Slot 35: Cerrar
        inv.setItem(35, PlayerGUI.createGuiItem(Material.BARRIER, "&c&lCerrar", null));

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

    public void handleClick(Player player, int slot) {
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        String selected = null;
        switch (slot) {
            case 11 -> selected = "none";
            case 12 -> selected = "flame";
            case 13 -> selected = "fairy";
            case 14 -> selected = "shadow";
            case 15 -> selected = "holy";
            case 22 -> selected = "runic";
        }

        if (selected != null) {
            data.setSelectedCosmeticAura(selected);
            plugin.getPlayerDataManager().saveToFile(data);
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
            if (!selected.equals("none")) {
                player.getWorld().spawnParticle(Particle.GLOW, player.getLocation().add(0, 1.2, 0), 10, 0.4, 0.4, 0.4, 0.05);
                ColorUtils.sendMessage(player, "&aAura cosmética equipada: &e" + selected.toUpperCase());
            } else {
                ColorUtils.sendMessage(player, "&7Aura cosmética desactivada.");
            }
            open(player);
        }
    }
}
