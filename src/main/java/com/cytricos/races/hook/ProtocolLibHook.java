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

public class ProtocolLibHook {
    private final CytricosRaces plugin;
    private final boolean available;
    private final Map<Integer, UUID> hiddenEntityOwners = new ConcurrentHashMap<>();
    private Object listener;

    public ProtocolLibHook(CytricosRaces plugin) {
        this.plugin = plugin;
        boolean enabled = false;
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
                Class.forName("com.comphenix.protocol.ProtocolLibrary");
                enabled = true;
            }
        } catch (Throwable ignored) {
            enabled = false;
        }
        this.available = enabled;
        if (this.available) {
            this.listener = ProtocolLibBridge.register(this.plugin, hiddenEntityOwners);
            this.plugin.getLogger().info("Integración con ProtocolLib activada para ocultación de cosméticos en 1ra persona.");
        } else {
            this.plugin.getLogger().info("ProtocolLib no detectado; usando ocultación nativa Paper (hideEntity/showEntity).");
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
            ProtocolLibBridge.unregister(listener);
            listener = null;
        }
        hiddenEntityOwners.clear();
    }

    private static final class ProtocolLibBridge {
        private static Object register(final CytricosRaces racesPlugin, final Map<Integer, UUID> hiddenEntityOwners) {
            try {
                ProtocolManager pm = ProtocolLibrary.getProtocolManager();
                PacketAdapter adapter = new PacketAdapter(
                        racesPlugin,
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
                        PlayerData data = racesPlugin.getPlayerDataManager().get(ownerUuid);
                        if (data != null && data.isFirstPersonWingsHidden()) {
                            event.setCancelled(true);
                        }
                    }
                };
                pm.addPacketListener(adapter);
                return adapter;
            } catch (Throwable t) {
                racesPlugin.getLogger().warning("No se pudo registrar filtro ProtocolLib: " + t.getMessage());
                return null;
            }
        }

        private static void unregister(Object listener) {
            try {
                if (listener instanceof PacketAdapter adapter) {
                    ProtocolLibrary.getProtocolManager().removePacketListener(adapter);
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
