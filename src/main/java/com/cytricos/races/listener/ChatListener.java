package com.cytricos.races.listener;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import com.cytricos.races.model.Race;
import com.cytricos.races.util.ColorUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Gestor exclusivo de formato de Chat para CYTRICOS RPG (Paper 26.2 / Java 25).
 * Formato oficial: [Título] [Rango] Jugador [Raza - Nivel X]: Mensaje
 * Si el jugador aún no ha elegido raza, muestra [Sin Raza].
 *
 * REGLA ESTRICTA DE AISLAMIENTO CHAT VS TABLIST:
 * Este listener únicamente renderiza el chat y JAMÁS modifica playerListName ni la Tablist.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class ChatListener implements Listener {
    private final CytricosRaces plugin;

    public ChatListener(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data == null) return;

        String titleTag = "";
        if (data.getSelectedTitle() != null && !data.getSelectedTitle().isBlank()) {
            titleTag = data.getSelectedTitle().trim() + " ";
        }

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

        String raceTag;
        if (!data.hasAssignedRace()) {
            raceTag = "&8[&7Sin Raza&8]";
        } else {
            Race race = plugin.getRaceManager().getRace(data.getRace());
            String raceNameSpanish = (race != null) ? ColorUtils.strip(race.getDisplayName()) : "Sin Raza";
            raceTag = "&8[&6" + raceNameSpanish + " &7- &eNivel " + data.getLevel() + "&8]";
        }

        Component header = ColorUtils.component(titleTag + rankPrefix + "&f" + player.getName() + " " + raceTag + "&8: &f");
        event.renderer((source, sourceDisplayName, message, viewer) -> header.append(message));
    }
}
