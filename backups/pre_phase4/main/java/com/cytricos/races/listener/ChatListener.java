package com.cytricos.races.listener;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.model.Race;
import com.cytricos.races.model.RaceLevel;
import com.cytricos.races.util.ColorUtils;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Gestor exclusivo de formato de Chat para CYTRICOS RPG (Paper 26.2 / Java 25).
 * Formato oficial: [Rango] [NombreJugador] [Raza - Nivel X]: Mensaje
 * Ejemplo: [Admin] Dandan_13 [Vampiro - Nivel 4]: Hola
 *
 * REGLA FUNDAMENTAL DE SEPARACIÓN TABLIST VS CHAT:
 * - El formato del TABlist NO muestra la raza ni el nivel (se mantiene estrictamente [Rango] NombreJugador).
 * - El formato con [Raza - Nivel] se inyecta EXCLUSIVAMENTE en el canal de chat para evitar conflictos
 *   con TAB, BungeeCord o Velocity.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class ChatListener implements Listener {
    private final CytricosRaces plugin;

    public ChatListener(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        // 1. Obtener prefijo de Rango desde LuckPerms (o fallback vacío)
        String rankPrefix = "";
        if (plugin.isLuckPermsAvailable()) {
            try {
                LuckPerms lp = plugin.getLuckPerms();
                User user = lp.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    String prefix = user.getCachedData().getMetaData().getPrefix();
                    if (prefix != null && !prefix.isEmpty()) {
                        rankPrefix = prefix.trim() + " ";
                    }
                }
            } catch (Throwable ignored) {}
        }

        // 2. Obtener datos de raza y nivel en español
        Race race = plugin.getRaceManager().getRace(data.getRace());
        String raceNameSpanish = (race != null) ? ColorUtils.strip(race.getDisplayName()) : "Humano";
        int level = data.getLevel();

        RaceLevel rLevel = (race != null) ? race.getLevel(level) : null;
        String levelSubName = (rLevel != null && rLevel.getName() != null) ? " (" + rLevel.getName() + ")" : "";

        // Tag de chat: [Raza - Nivel X]
        String raceTag = "&8[&6" + raceNameSpanish + " &7- &eNivel " + level + "&8]";

        // 3. Ensamblar formato: [Rango] Nombre [Raza - Nivel]: Mensaje
        // %1$s = Nombre del jugador (o displayName con rango si otro plugin lo inyectó)
        // %2$s = Mensaje del jugador
        String coloredRank = ColorUtils.color(rankPrefix);
        String coloredTag = ColorUtils.color(raceTag);

        // Si el formato ya tiene un prefijo de rango o el nombre del jugador
        String finalFormat = coloredRank + "%1$s " + coloredTag + "&8: &f%2$s";
        event.setFormat(finalFormat);
    }
}
