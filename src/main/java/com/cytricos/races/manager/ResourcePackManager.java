package com.cytricos.races.manager;

import com.cytricos.races.CytricosRaces;
import com.cytricos.races.util.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.logging.Level;

/**
 * Gestor automático de descarga, cálculo asíncrono SHA-1 (20 bytes) y entrega del Resource Pack
 * al conectarse los jugadores o al recargar el plugin.
 * Lead Developers: Dandan_13 & Wolfit12
 */
public class ResourcePackManager {
    private final CytricosRaces plugin;
    private volatile byte[] cachedSha1Bytes = null;
    private volatile String cachedSha1Hex = "";

    public ResourcePackManager(CytricosRaces plugin) {
        this.plugin = plugin;
    }

    public void initAsyncSha1() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::computeAndCacheSha1);
    }

    public void computeAndCacheSha1() {
        cachedSha1Bytes = null;
        cachedSha1Hex = "";

        String configSha1 = plugin.getConfig().getString("cosmetics_settings.resource_pack.sha1", "");
        if (configSha1 != null && configSha1.trim().length() == 40) {
            byte[] parsed = hexToBytes(configSha1.trim());
            if (parsed != null) {
                cachedSha1Bytes = parsed;
                cachedSha1Hex = configSha1.trim().toLowerCase();
                plugin.getLogger().info("SHA-1 de Resource Pack cargado desde config.yml: " + cachedSha1Hex);
                return;
            }
        }

        // 1. Intentar calcular desde archivo local del servidor
        String localName = plugin.getConfig().getString("cosmetics_settings.resource_pack.local_file", "CytricosRaces-ResourcePack.zip");
        File localFile = new File(plugin.getDataFolder(), localName);
        if (!localFile.exists()) {
            localFile = new File(localName);
        }
        if (localFile.exists() && localFile.isFile()) {
            try (InputStream is = new BufferedInputStream(new FileInputStream(localFile))) {
                byte[] digest = digestSha1(is);
                cachedSha1Bytes = digest;
                cachedSha1Hex = HexFormat.of().formatHex(digest);
                plugin.getLogger().info("SHA-1 calculado desde archivo local (" + localFile.getName() + "): " + cachedSha1Hex);
                return;
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "No se pudo calcular SHA-1 local de " + localFile.getName(), ex);
            }
        }

        // 2. Intentar descargar desde URL remota de forma asíncrona
        String url = plugin.getConfig().getString("cosmetics_settings.resource_pack.url", "");
        if (url != null && !url.isBlank() && !url.contains("TU-USUARIO") && url.startsWith("http")) {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(25))
                        .GET()
                        .build();
                HttpResponse<InputStream> resp = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    try (InputStream body = new BufferedInputStream(resp.body())) {
                        byte[] digest = digestSha1(body);
                        cachedSha1Bytes = digest;
                        cachedSha1Hex = HexFormat.of().formatHex(digest);
                        plugin.getLogger().info("SHA-1 calculado automáticamente desde URL del Resource Pack: " + cachedSha1Hex);
                    }
                } else {
                    plugin.getLogger().warning("No se pudo obtener el Resource Pack remoto (HTTP " + resp.statusCode() + ").");
                }
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Error descargando Resource Pack para calcular SHA-1: " + ex.getMessage());
            }
        }
    }

    public boolean isAutoSendEnabled() {
        return plugin.getConfig().getBoolean("cosmetics_settings.resource_pack.enabled", true)
                || plugin.getConfig().getBoolean("cosmetics_settings.resource_pack.auto_send_on_join", false);
    }

    public void sendPack(Player player, boolean silentIfUnconfigured) {
        if (player == null || !player.isOnline()) return;

        String url = plugin.getConfig().getString("cosmetics_settings.resource_pack.url", "");
        boolean required = plugin.getConfig().getBoolean("cosmetics_settings.resource_pack.required", false);
        String prompt = plugin.getConfig().getString("cosmetics_settings.resource_pack.prompt",
                "&e&lCYTRICOS RPG &7- Instala el paquete de recursos oficial para ver alas 3D, coronas y reliquias.");

        if (url == null || url.isBlank() || url.contains("TU-USUARIO")) {
            if (!silentIfUnconfigured) {
                ColorUtils.sendMessage(player, "&e&l[RESOURCE PACK] &7Configura una URL válida en &fconfig.yml &7-> &ecosmetics_settings.resource_pack.url&7.");
            }
            return;
        }

        try {
            byte[] hash = cachedSha1Bytes;
            if (hash != null && hash.length == 20) {
                player.setResourcePack(url, hash, ColorUtils.component(prompt), required);
            } else {
                player.setResourcePack(url, (byte[]) null, ColorUtils.component(prompt), required);
            }
            if (!silentIfUnconfigured) {
                ColorUtils.sendMessage(player, "&a&l[RESOURCE PACK] &7Paquete de recursos enviado correctamente a tu cliente.");
            }
        } catch (Throwable t) {
            if (!silentIfUnconfigured) {
                ColorUtils.sendMessage(player, "&cError al enviar el Resource Pack: " + t.getMessage());
            }
        }
    }

    public String getCachedSha1Hex() {
        return cachedSha1Hex;
    }

    private byte[] digestSha1(InputStream is) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            md.update(buffer, 0, read);
        }
        return md.digest();
    }

    private byte[] hexToBytes(String hex) {
        try {
            byte[] bytes = new byte[20];
            for (int i = 0; i < 20; i++) {
                bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
            return bytes;
        } catch (Exception e) {
            return null;
        }
    }
}
