package com.cytricos.races.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor universal y centralizado de colores y formateo para CYTRICOS RPG (Paper 26.2 / Java 25).
 * Resuelve definitivamente los problemas de códigos legacy (&0-&9, &a-&f, &l, &o, &n, &m, &k, &r)
 * incluyendo variantes en mayúsculas (&L, &O, etc.), códigos hexadecimales (&#RRGGBB y <#RRGGBB>)
 * y compatibilidad transparente con MiniMessage sin fugas de estilo.
 *
 * Lead Developers: Dandan_13 & Wolfit12
 */
public final class ColorUtils {

    private static final Pattern HEX_AMPERSAND_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_TAG_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern LEGACY_UPPERCASE_PATTERN = Pattern.compile("&([0-9A-FK-ORa-fk-or])");

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    // Serializador ampersand con soporte hex
    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    // Serializador section con soporte hex
    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private ColorUtils() {}

    /**
     * Convierte cualquier texto formateado (Legacy &, §, Hex &#, MiniMessage <>) en un Adventure Component.
     * Desactiva automáticamente el cursivo no deseado en ítems y lore de Minecraft.
     */
    public static Component component(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        // 1. Si contiene tags XML de MiniMessage (excepto tags simples de hex <#RRGGBB>)
        if (text.contains("<") && text.contains(">") && !HEX_TAG_PATTERN.matcher(text).matches()) {
            try {
                return MINI_MESSAGE.deserialize(text).decoration(TextDecoration.ITALIC, false);
            } catch (Throwable ignored) {
                // Fallback directo a legacy ampersand
            }
        }

        // 2. Normalizar hex <#RRGGBB> a &#RRGGBB
        String normalized = normalizeHex(text);

        // 3. Normalizar caracteres de sección a ampersands para serialización uniforme
        normalized = normalized.replace('§', '&');

        // 4. Asegurar que las letras de código (&A-&F, &L, etc.) se procesen correctamente
        normalized = normalizeLegacyCodes(normalized);

        return AMPERSAND_SERIALIZER.deserialize(normalized).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Convierte una lista de textos a componentes de Adventure API.
     */
    public static List<Component> componentList(List<String> list) {
        if (list == null) return new ArrayList<>();
        List<Component> components = new ArrayList<>(list.size());
        for (String line : list) {
            components.add(component(line));
        }
        return components;
    }

    /**
     * Traduce códigos de formato a una cadena con formato legacy de sección (§),
     * soportando códigos tradicionales (mayúsculas/minúsculas) y hexadecimales Bungee (§x§r§r§g§g§b§b).
     */
    public static String color(String text) {
        if (text == null || text.isEmpty()) return "";

        // Si contiene tags de MiniMessage
        if (text.contains("<") && text.contains(">") && !HEX_TAG_PATTERN.matcher(text).matches()) {
            try {
                Component comp = MINI_MESSAGE.deserialize(text);
                return SECTION_SERIALIZER.serialize(comp);
            } catch (Throwable ignored) {}
        }

        String normalized = normalizeHex(text);
        normalized = translateHexColorCodes(normalized);
        return ChatColor.translateAlternateColorCodes('&', normalized);
    }

    /**
     * Traduce una lista de cadenas a formato legacy (§).
     */
    public static List<String> colorList(List<String> list) {
        if (list == null) return new ArrayList<>();
        List<String> colored = new ArrayList<>(list.size());
        for (String line : list) {
            colored.add(color(line));
        }
        return colored;
    }

    /**
     * Normaliza códigos de color ampersand en mayúsculas a minúsculas para máxima compatibilidad.
     */
    private static String normalizeLegacyCodes(String message) {
        if (message == null) return "";
        Matcher matcher = LEGACY_UPPERCASE_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "&" + matcher.group(1).toLowerCase());
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Traduce &#RRGGBB al formato Bungee §x§R§R§G§G§B§B para compatibilidad en APIs clásicas.
     */
    public static String translateHexColorCodes(String message) {
        if (message == null) return "";
        Matcher matcher = HEX_AMPERSAND_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(Character.toLowerCase(c));
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    /**
     * Convierte <#RRGGBB> a &#RRGGBB.
     */
    private static String normalizeHex(String message) {
        if (message == null) return "";
        Matcher matcher = HEX_TAG_PATTERN.matcher(message);
        return matcher.replaceAll("&#$1");
    }

    /**
     * Remueve todo el formato y color de un texto.
     */
    public static String strip(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(color(text));
    }

    /**
     * Convierte un Adventure Component a texto plano sin color.
     */
    public static String toPlainText(Component component) {
        if (component == null) return "";
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * Envía un mensaje con formato Adventure al jugador.
     */
    public static void sendMessage(Player player, String message) {
        if (player != null && message != null) {
            player.sendMessage(component(message));
        }
    }

    /**
     * Envía una barra de acción (Action Bar) al jugador.
     */
    public static void sendActionBar(Player player, String message) {
        if (player != null && message != null) {
            player.sendActionBar(component(message));
        }
    }

    /**
     * Envía un título y subtítulo en pantalla al jugador.
     */
    public static void sendTitle(Player player, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        if (player == null) return;
        Component titleComp = component(title != null ? title : "");
        Component subComp = component(subtitle != null ? subtitle : "");
        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeInTicks * 50L),
                Duration.ofMillis(stayTicks * 50L),
                Duration.ofMillis(fadeOutTicks * 50L)
        );
        player.showTitle(Title.title(titleComp, subComp, times));
    }
}
