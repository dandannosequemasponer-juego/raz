package com.cytricos.races.hook;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.cytricos.races.CytricosRaces;
import com.cytricos.races.model.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Integración con ProtocolLib 5.3.0 para ocultar paquetes SPAWN_ENTITY / ENTITY_METADATA
 * de cosméticos 3D (alas y coronas/aureolas) al dueño en primera persona sin afectar a otros jugadores.
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class ProtocolLibHook {
    private final CytricosRaces plugin;
    private final boolean available;
    private final Map<Integer, UUID> hiddenEntityOwners = new ConcurrentHashMap<>();
    private PacketAdapter listener;

    public ProtocolLibHook(CytricosRaces plugin) {
        this.plugin = plugin;
        this.available = Bukkit.getPluginManager().isPluginEnabled("ProtocolLib");
        if (this.available) {
            registerPacketFilter();
            plugin.getLogger().info("Integración con ProtocolLib activada para ocultación de cosméticos en 1ra persona.");
        } else {
            plugin.getLogger().info("ProtocolLib no detectado; usando ocultación nativa Paper (hideEntity/showEntity).");
        }
    }

    private void registerPacketFilter() {
        try {
            ProtocolManager pm = ProtocolLibrary.getProtocolManager();
            this.listener = new PacketAdapter(
                    plugin,
                    ListenerPriority.NORMAL,
                    PacketType.Play.Server.SPAWN_ENTITY,
                    PacketType.Play.Server.ENTITY_METADATA
            ) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    Player receiver = event.getPlayer();
                    if (receiver == null) return;
                    Integer entityId = event.getPacket().getIntegers().readSafely(0);
                    if (entityId == null) return;
                    UUID ownerUuid = hiddenEntityOwners.get(entityId);
                    if (ownerUuid == null || !ownerUuid.equals(receiver.getUniqueId())) {
                        return;
                    }
                    PlayerData data = plugin.getPlayerDataManager().get(ownerUuid);
                    if (data != null && data.isFirstPersonWingsHidden()) {
                        event.setCancelled(true);
                    }
                }
            };
            pm.addPacketListener(this.listener);
        } catch (Throwable t) {
            plugin.getLogger().warning("No se pudo registrar filtro ProtocolLib: " + t.getMessage());
        }
    }

    public void registerTrackedEntity(int entityId, UUID ownerUuid) {
        if (entityId > 0 && ownerUuid != null) {
            hiddenEntityOwners.put(entityId, ownerUuid);
        }
    }

    public void unregisterTrackedEntity(int entityId) {
        hiddenEntityOwners.remove(entityId);
    }

    public void unregisterOwner(UUID ownerUuid) {
        if (ownerUuid == null) return;
        hiddenEntityOwners.entrySet().removeIf(e -> ownerUuid.equals(e.getValue()));
    }

    public boolean isAvailable() {
        return available;
    }

    public void shutdown() {
        if (available && listener != null) {
            try {
                ProtocolLibrary.getProtocolManager().removePacketListener(listener);
            } catch (Throwable ignored) {
            }
        }
        hiddenEntityOwners.clear();
    }
}
