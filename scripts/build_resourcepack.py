#!/usr/bin/env python3
"""
Generador y Verificador Oficial del Resource Pack Independiente de CytricosRaces v7.0.0.
Crea modelos JSON 3D multi-elemento compatibles con Blockbench (alas combinadas, ala izquierda
y ala derecha articuladas por hombro, corona 3D, aureola astral 3D), texturas RGBA 32x32 reales
sin marcadores vacíos, texturas 64x32 + definiciones equipment/ de Élitros para Paper 26.2,
mapeos duales (overrides legacy y range_dispatch moderno) y empaquetado verificado con SHA-1.
"""

import hashlib
import json
import math
import os
import shutil
import struct
import zipfile
import zlib

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
PACK_DIR = os.path.join(ROOT, "resourcepack", "CytricosRaces-Pack")
ZIP_OUT_PACK = os.path.join(ROOT, "resourcepack", "CytricosRaces-ResourcePack.zip")
ZIP_OUT_PUBLIC = os.path.join(ROOT, "public", "downloads", "CytricosRaces-ResourcePack.zip")


def make_png(width: int, height: int, pixels: list[tuple[int, int, int, int]]) -> bytes:
    """Genera un archivo PNG RGBA válido de alta compatibilidad sin dependencias externas."""
    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack("!I", len(data)) + tag + data + struct.pack("!I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    raw_rows = bytearray()
    for y in range(height):
        raw_rows.append(0)
        for x in range(width):
            r, g, b, a = pixels[y * width + x]
            raw_rows.extend((max(0, min(255, int(r))),
                             max(0, min(255, int(g))),
                             max(0, min(255, int(b))),
                             max(0, min(255, int(a)))))

    ihdr = struct.pack("!IIBBBBB", width, height, 8, 6, 0, 0, 0)
    idat = zlib.compress(bytes(raw_rows), 9)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", idat) + chunk(b"IEND", b"")


def write_png(path: str, width: int, height: int, pixels: list[tuple[int, int, int, int]]):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(make_png(width, height, pixels))


def write_json(path: str, obj: dict):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)


def lerp_color(c1, c2, t: float):
    t = max(0.0, min(1.0, t))
    return (
        int(c1[0] + (c2[0] - c1[0]) * t),
        int(c1[1] + (c2[1] - c1[1]) * t),
        int(c1[2] + (c2[2] - c1[2]) * t),
    )


def shade_outline_32(grid_rgba: list[tuple[int, int, int, int]], outline_rgb) -> list[tuple[int, int, int, int]]:
    """Añade contorno oscuro nítido a siluetas 32x32 para calidad RPG."""
    out = list(grid_rgba)
    for y in range(32):
        for x in range(32):
            if grid_rgba[y * 32 + x][3] == 0:
                has_neighbor = False
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 32 and 0 <= ny < 32 and grid_rgba[ny * 32 + nx][3] > 20:
                        has_neighbor = True
                        break
                if has_neighbor:
                    out[y * 32 + x] = (*outline_rgb, 255)
    return out


def draw_wing_texture_32(primary, secondary, accent, style="feathered") -> list[tuple[int, int, int, int]]:
    """Genera textura RGBA 32x32 rica y sombreada según arquetipo fantástico de ala."""
    w, h = 32, 32
    px = [(0, 0, 0, 0)] * (w * h)
    dark_outline = (max(8, primary[0] // 3), max(8, primary[1] // 3), max(8, primary[2] // 3))

    for y in range(h):
        for x in range(w):
            # Coordenadas normalizadas desde raíz (0,10) hacia punta (30,4) y plumas inferiores
            u = x / 31.0
            v = y / 31.0
            in_shape = False
            col = primary
            alpha = 255

            if style == "feathered":
                # Arco superior del ala + 6 plumas escalonadas hacia abajo-izquierda
                top_edge = 0.12 + 0.14 * ((u - 0.35) ** 2) - 0.08 * u
                bot_edge = 0.86 - 0.56 * (u ** 1.15) + 0.05 * math.sin(u * 26.0)
                if 0.04 <= u <= 0.94 and top_edge <= v <= bot_edge:
                    in_shape = True
                    t = (u * 0.65 + (1.0 - v) * 0.35)
                    col = lerp_color(primary, secondary, t)
                    # Raquis / nervio principal de plumas
                    if abs(v - (top_edge + 0.08)) < 0.045:
                        col = lerp_color(secondary, accent, 0.7)
                    # Puntas luminosas y estrías entre plumas
                    if (int(u * 16) + int(v * 6)) % 4 == 0 and v > 0.35:
                        col = lerp_color(col, accent, 0.55)
            elif style == "membranous":
                # Esqueleto óseo superior + 3 púas de garra + membrana festoneada
                top_arch = 0.10 + 0.22 * math.sin(u * math.pi * 0.85) * -0.35 + 0.12 * u
                scallop = abs(math.sin(u * math.pi * 3.0)) * 0.12
                bot_edge = 0.88 - 0.52 * u - scallop
                if 0.05 <= u <= 0.94 and top_arch <= v <= bot_edge:
                    in_shape = True
                    # Huesos radiales
                    rib1 = abs(v - (0.14 + u * 0.52)) < 0.035
                    rib2 = abs(v - (0.18 + u * 0.32)) < 0.035
                    top_bone = abs(v - top_arch) < 0.055
                    if top_bone or rib1 or rib2:
                        col = lerp_color(primary, accent, 0.35 if u > 0.6 else 0.1)
                    else:
                        col = lerp_color(primary, secondary, (v - top_arch) / max(0.05, bot_edge - top_arch))
                        if (x * y) % 17 == 0:
                            col = lerp_color(col, accent, 0.45)
            elif style == "butterfly":
                # Doble lóbulo superior e inferior translúcido de hada
                cx1, cy1 = 0.46, 0.34
                cx2, cy2 = 0.30, 0.66
                d1 = ((u - cx1) / 0.42) ** 2 + ((v - cy1) / 0.28) ** 2
                d2 = ((u - cx2) / 0.26) ** 2 + ((v - cy2) / 0.22) ** 2
                if d1 <= 1.0 or d2 <= 1.0:
                    in_shape = True
                    alpha = 215
                    radial = min(d1, d2)
                    col = lerp_color(accent, secondary, radial)
                    if radial > 0.72:
                        col = lerp_color(col, primary, 0.85)
                        alpha = 245
                    elif abs(math.sin(u * 18.0) * math.cos(v * 18.0)) < 0.12:
                        col = accent
            elif style == "astral":
                # Cristales estelares + estela nebular
                top_edge = 0.14 + 0.06 * math.sin(u * 12.0)
                bot_edge = 0.84 - 0.48 * u + 0.04 * math.cos(u * 20.0)
                if 0.06 <= u <= 0.94 and top_edge <= v <= bot_edge:
                    in_shape = True
                    col = lerp_color(primary, secondary, u * 0.8)
                    if (x + y * 2) % 9 == 0 or (abs(u - 0.5) < 0.08 and abs(v - 0.38) < 0.08):
                        col = accent
                    alpha = 235 if v > 0.5 else 255
            elif style == "void":
                # Placas gravitacionales flotantes separadas por fisuras
                d_plate1 = 0.06 <= u <= 0.45 and 0.14 <= v <= (0.82 - u * 0.4)
                d_plate2 = 0.50 <= u <= 0.76 and 0.18 <= v <= (0.72 - (u - 0.4) * 0.5)
                d_plate3 = 0.80 <= u <= 0.95 and 0.22 <= v <= 0.48
                if d_plate1 or d_plate2 or d_plate3:
                    in_shape = True
                    col = lerp_color(primary, secondary, u)
                    if abs(v - 0.26) < 0.04 or ((x ^ y) % 11 == 0):
                        col = accent
            elif style == "nature":
                # Ramas entrelazadas y hojas en capas
                branch = abs(v - (0.18 + 0.24 * math.sin(u * 2.2))) < 0.05
                leaf_band = 0.06 <= u <= 0.92 and (0.16 <= v <= (0.85 - 0.48 * u + 0.08 * math.sin(u * 22.0)))
                if branch or leaf_band:
                    in_shape = True
                    col = lerp_color(primary, secondary, u * 0.7 + (1.0 - v) * 0.3)
                    if (x - 8) ** 2 + (y - 12) ** 2 < 10 or (x - 21) ** 2 + (y - 9) ** 2 < 7:
                        col = accent

            if in_shape:
                px[y * w + x] = (*col, alpha)

    return shade_outline_32(px, dark_outline)


def draw_elytra_skin_64x32(primary, secondary, accent, style="feathered") -> list[tuple[int, int, int, int]]:
    """
    Genera textura 64x32 compatible con el layer `wings` de equipment/*.json en Paper 26.2 / 1.21.4+.
    Pinta los paneles UV estándar de élitros (22..46 x 0..22) con arte temático rico.
    """
    w, h = 64, 32
    px = [(0, 0, 0, 0)] * (w * h)
    wing32 = draw_wing_texture_32(primary, secondary, accent, style)

    # Panel principal UV de Élitros vanilla (desde x=22..46, y=0..22)
    for ey in range(22):
        for ex in range(24):
            sx = min(31, int(ex * 32 / 24))
            sy = min(31, int(ey * 32 / 22))
            r, g, b, a = wing32[sy * 32 + sx]
            if a > 0:
                px[ey * w + (22 + ex)] = (r, g, b, a)
                # Rellenar borde lateral y cara posterior en la hoja 64x32
                if ex < 10:
                    px[ey * w + (46 + ex)] = (max(0, r - 20), max(0, g - 20), max(0, b - 20), a)
            else:
                # Borde sutil translúcido en contorno UV para evitar caras invisibles
                if 2 <= ey <= 19 and 2 <= ex <= 11:
                    px[ey * w + (22 + ex)] = (*primary, 195)
    return px


def draw_icon_texture_32(kind: str, c1, c2, c3) -> list[tuple[int, int, int, int]]:
    """Genera iconos RGBA 32x32 con sombreado y contorno nítido para armas, amuletos, reliquias y accesorios."""
    w, h = 32, 32
    px = [(0, 0, 0, 0)] * (w * h)
    dark_out = (max(10, c1[0] // 3), max(10, c1[1] // 3), max(10, c1[2] // 3))

    for y in range(h):
        for x in range(w):
            nx = x / 31.0
            ny = y / 31.0
            hit = False
            col = c2
            alpha = 255

            if kind in ("sword", "scythe"):
                # Hoja diagonal + guarda + empuñadura (o curvatura de guadaña)
                diag = abs((nx + ny) - 1.0)
                if kind == "scythe" and nx > 0.35 and ny < 0.45:
                    # Arco curvo superior de guadaña del vacío
                    r_arc = math.hypot(nx - 0.58, ny - 0.45)
                    if 0.20 <= r_arc <= 0.33 and ny < 0.48:
                        hit = True
                        col = lerp_color(c2, c3, (0.33 - r_arc) / 0.13)
                if not hit and diag < 0.065 and 0.14 <= nx <= 0.88:
                    hit = True
                    col = lerp_color(c1, c3 if nx > 0.55 else c2, nx)
                # Guarda diagonal perpendicular
                if abs((nx - ny) - 0.0) < 0.055 and 0.22 <= nx <= 0.44 and 0.52 <= ny <= 0.78:
                    hit = True
                    col = c3
            elif kind == "staff":
                diag = abs((nx + ny) - 1.0)
                if diag < 0.05 and 0.12 <= nx <= 0.78:
                    hit = True
                    col = lerp_color(c1, c2, nx)
                # Orbe superior del báculo
                dist_orb = math.hypot(nx - 0.77, ny - 0.23)
                if dist_orb < 0.15:
                    hit = True
                    col = lerp_color(c3, c2, dist_orb / 0.15)
            elif kind == "pickaxe":
                # Mango diagonal + cabeza curva de pico
                if abs((nx + ny) - 1.0) < 0.05 and 0.14 <= nx <= 0.82:
                    hit = True
                    col = c1
                head_r = math.hypot(nx - 0.62, ny - 0.38)
                if 0.20 <= head_r <= 0.30 and (nx > 0.42 or ny < 0.58) and nx + ny < 1.25:
                    hit = True
                    col = lerp_color(c2, c3, (head_r - 0.20) / 0.10)
            elif kind == "bow":
                arc_r = math.hypot(nx - 0.28, ny - 0.50)
                if 0.36 <= arc_r <= 0.46 and nx >= 0.28:
                    hit = True
                    col = lerp_color(c1, c2, abs(ny - 0.5) * 2.0)
                if abs(nx - 0.32) < 0.022 and 0.14 <= ny <= 0.86:
                    hit = True
                    col = c3
            elif kind == "chalice":
                # Copa con base y líquido brillante
                in_cup = 0.18 <= ny <= 0.52 and abs(nx - 0.5) <= (0.28 - (ny - 0.18) * 0.35)
                in_stem = 0.52 <= ny <= 0.78 and abs(nx - 0.5) <= 0.06
                in_base = 0.78 <= ny <= 0.86 and abs(nx - 0.5) <= 0.22
                if in_cup or in_stem or in_base:
                    hit = True
                    col = c3 if (in_cup and ny < 0.30 and abs(nx - 0.5) < 0.20) else lerp_color(c1, c2, 1.0 - abs(nx - 0.5) * 2.5)
            elif kind == "potion":
                neck = 0.16 <= ny <= 0.34 and abs(nx - 0.5) <= 0.09
                body = math.hypot(nx - 0.5, ny - 0.60) <= 0.27
                if neck or body:
                    hit = True
                    col = c3 if (body and ny > 0.54) else lerp_color(c1, c2, ny)
            elif kind == "crown":
                base = 0.58 <= ny <= 0.76 and 0.14 <= nx <= 0.86
                p1 = 0.28 <= ny <= 0.58 and abs(nx - 0.22) <= (ny - 0.24) * 0.28
                p2 = 0.20 <= ny <= 0.58 and abs(nx - 0.50) <= (ny - 0.16) * 0.28
                p3 = 0.28 <= ny <= 0.58 and abs(nx - 0.78) <= (ny - 0.24) * 0.28
                if base or p1 or p2 or p3:
                    hit = True
                    col = c3 if ((0.64 <= ny <= 0.70 and abs(nx - 0.5) < 0.08) or ny < 0.27) else lerp_color(c1, c2, 1.0 - ny * 0.5)
            elif kind == "halo":
                d = math.hypot((nx - 0.5) / 0.38, (ny - 0.45) / 0.18)
                if 0.55 <= d <= 1.0:
                    hit = True
                    col = lerp_color(c2, c3, 1.0 - abs(d - 0.78) / 0.22)
            elif kind == "gem":
                diamond = (abs(nx - 0.5) / 0.28 + abs(ny - 0.5) / 0.38) <= 1.0
                if diamond:
                    hit = True
                    col = lerp_color(c1, c3 if nx < 0.5 and ny < 0.5 else c2, 0.7)
            elif kind == "ring":
                d = math.hypot(nx - 0.5, ny - 0.54)
                in_band = 0.18 <= d <= 0.31
                in_jewel = math.hypot(nx - 0.5, ny - 0.25) <= 0.11
                if in_band or in_jewel:
                    hit = True
                    col = c3 if in_jewel else lerp_color(c1, c2, d)
            elif kind == "brooch":
                shield = (abs(nx - 0.5) <= 0.28 - max(0.0, ny - 0.55) * 0.6) and 0.18 <= ny <= 0.84
                if shield:
                    hit = True
                    col = c3 if abs(nx - 0.5) < 0.08 and 0.34 <= ny <= 0.56 else lerp_color(c1, c2, ny)
            else:  # coin / amulet
                d = math.hypot(nx - 0.5, ny - 0.5)
                if d <= 0.36:
                    hit = True
                    col = c3 if (d < 0.14 or abs(d - 0.28) < 0.03) else lerp_color(c1, c2, 1.0 - d)

            if hit:
                px[y * w + x] = (*col, alpha)

    return shade_outline_32(px, dark_out)


def wing_combined_blockbench_model(wing_id: str) -> dict:
    """Modelo 3D combinado compatible con Blockbench para GUI, ítems en mano y Élitros."""
    tex_ref = f"cytricos:item/wings/{wing_id}"
    return {
        "credit": "CytricosRaces 7.0.0 - Modelo Blockbench Dual Combinado (Dandan_13 & Wolfit12)",
        "texture_size": [32, 32],
        "textures": {"0": tex_ref, "particle": tex_ref},
        "elements": [
            {
                "name": "espina_central",
                "from": [6.8, 7.0, 7.4],
                "to": [9.2, 11.5, 9.0],
                "faces": {
                    "north": {"uv": [0, 0, 3, 4], "texture": "#0"},
                    "south": {"uv": [0, 0, 3, 4], "texture": "#0"},
                    "east": {"uv": [0, 0, 2, 4], "texture": "#0"},
                    "west": {"uv": [0, 0, 2, 4], "texture": "#0"},
                    "up": {"uv": [0, 0, 3, 2], "texture": "#0"},
                    "down": {"uv": [0, 0, 3, 2], "texture": "#0"}
                }
            },
            {
                "name": "ala_izquierda_principal",
                "from": [-11, -1, 8.2],
                "to": [7, 17, 8.9],
                "rotation": {"angle": 22.5, "axis": "y", "origin": [7, 8, 8.5]},
                "faces": {
                    "north": {"uv": [0, 0, 16, 16], "texture": "#0"},
                    "south": {"uv": [16, 0, 0, 16], "texture": "#0"},
                    "east": {"uv": [0, 0, 1, 16], "texture": "#0"},
                    "west": {"uv": [0, 0, 1, 16], "texture": "#0"},
                    "up": {"uv": [0, 0, 16, 1], "texture": "#0"},
                    "down": {"uv": [0, 0, 16, 1], "texture": "#0"}
                }
            },
            {
                "name": "ala_derecha_principal",
                "from": [9, -1, 8.2],
                "to": [27, 17, 8.9],
                "rotation": {"angle": -22.5, "axis": "y", "origin": [9, 8, 8.5]},
                "faces": {
                    "north": {"uv": [16, 0, 0, 16], "texture": "#0"},
                    "south": {"uv": [0, 0, 16, 16], "texture": "#0"},
                    "east": {"uv": [0, 0, 1, 16], "texture": "#0"},
                    "west": {"uv": [0, 0, 1, 16], "texture": "#0"},
                    "up": {"uv": [0, 0, 16, 1], "texture": "#0"},
                    "down": {"uv": [0, 0, 16, 1], "texture": "#0"}
                }
            }
        ],
        "display": {
            "thirdperson_righthand": {"translation": [0, 3, 1], "scale": [0.42, 0.42, 0.42]},
            "firstperson_righthand": {"translation": [1, 2, 0], "scale": [0.38, 0.38, 0.38]},
            "gui": {"rotation": [12, 18, 0], "translation": [0, -0.5, 0], "scale": [0.36, 0.36, 0.36]},
            "fixed": {"translation": [0, -1, -2.5], "scale": [0.85, 0.85, 0.85]},
            "head": {"translation": [0, -6, 6.5], "scale": [0.9, 0.9, 0.9]}
        }
    }


def wing_half_blockbench_model(wing_id: str, side: str) -> dict:
    """
    Modelo 3D articulado multi-capa por ala individual (`left` o `right`) centrado en el pivote [8, 8, 8].
    El rig Dual-Hinge rota cada mitad suavemente alrededor de su propio hombro mediante ItemDisplay.
    """
    tex_ref = f"cytricos:item/wings/{wing_id}"
    is_left = (side == "left")
    if is_left:
        # Se extiende hacia +X desde la bisagra x=8 hasta x=27
        elements = [
            {
                "name": "bisagra_hombro",
                "from": [7.5, 6.5, 7.2],
                "to": [11.0, 10.2, 8.8],
                "faces": {f: {"uv": [0, 0, 3, 3], "texture": "#0"} for f in ("north", "south", "east", "west", "up", "down")}
            },
            {
                "name": "panel_plumaje_principal",
                "from": [8.0, -2.0, 7.6],
                "to": [26.0, 17.5, 8.4],
                "faces": {
                    "north": {"uv": [16, 0, 0, 16], "texture": "#0"},
                    "south": {"uv": [0, 0, 16, 16], "texture": "#0"},
                    "east": {"uv": [0, 0, 1, 16], "texture": "#0"},
                    "west": {"uv": [0, 0, 1, 16], "texture": "#0"},
                    "up": {"uv": [0, 0, 16, 1], "texture": "#0"},
                    "down": {"uv": [0, 0, 16, 1], "texture": "#0"}
                }
            },
            {
                "name": "capa_profundidad_secundaria",
                "from": [10.5, 2.0, 8.5],
                "to": [24.0, 15.5, 9.2],
                "rotation": {"angle": 10.0, "axis": "y", "origin": [10.5, 8, 8.5]},
                "faces": {
                    "north": {"uv": [14, 1, 1, 14], "texture": "#0"},
                    "south": {"uv": [1, 1, 14, 14], "texture": "#0"},
                    "east": {"uv": [1, 1, 2, 14], "texture": "#0"},
                    "west": {"uv": [1, 1, 2, 14], "texture": "#0"},
                    "up": {"uv": [1, 1, 14, 2], "texture": "#0"},
                    "down": {"uv": [1, 1, 14, 2], "texture": "#0"}
                }
            }
        ]
    else:
        # Se extiende simétricamente hacia -X desde bisagra x=8 hasta x=-10
        elements = [
            {
                "name": "bisagra_hombro",
                "from": [5.0, 6.5, 7.2],
                "to": [8.5, 10.2, 8.8],
                "faces": {f: {"uv": [0, 0, 3, 3], "texture": "#0"} for f in ("north", "south", "east", "west", "up", "down")}
            },
            {
                "name": "panel_plumaje_principal",
                "from": [-10.0, -2.0, 7.6],
                "to": [8.0, 17.5, 8.4],
                "faces": {
                    "north": {"uv": [0, 0, 16, 16], "texture": "#0"},
                    "south": {"uv": [16, 0, 0, 16], "texture": "#0"},
                    "east": {"uv": [0, 0, 1, 16], "texture": "#0"},
                    "west": {"uv": [0, 0, 1, 16], "texture": "#0"},
                    "up": {"uv": [0, 0, 16, 1], "texture": "#0"},
                    "down": {"uv": [0, 0, 16, 1], "texture": "#0"}
                }
            },
            {
                "name": "capa_profundidad_secundaria",
                "from": [-8.0, 2.0, 8.5],
                "to": [5.5, 15.5, 9.2],
                "rotation": {"angle": -10.0, "axis": "y", "origin": [5.5, 8, 8.5]},
                "faces": {
                    "north": {"uv": [1, 1, 14, 14], "texture": "#0"},
                    "south": {"uv": [14, 1, 1, 14], "texture": "#0"},
                    "east": {"uv": [1, 1, 2, 14], "texture": "#0"},
                    "west": {"uv": [1, 1, 2, 14], "texture": "#0"},
                    "up": {"uv": [1, 1, 14, 2], "texture": "#0"},
                    "down": {"uv": [1, 1, 14, 2], "texture": "#0"}
                }
            }
        ]

    return {
        "credit": f"CytricosRaces 7.0.0 - Segmento Articulado ({side}) Blockbench",
        "texture_size": [32, 32],
        "textures": {"0": tex_ref, "particle": tex_ref},
        "elements": elements,
        "display": {
            "fixed": {"translation": [0, 0, 0], "scale": [1.0, 1.0, 1.0]},
            "gui": {"scale": [0.45, 0.45, 0.45]}
        }
    }


def crown_3d_model(cid: str) -> dict:
    """Modelo 3D verdadero de Corona Celestial compatible con Blockbench para la ranura de cabeza."""
    tex_ref = f"cytricos:item/custom/{cid}"
    gold_faces = {f: {"uv": [2, 9, 14, 12], "texture": "#0"} for f in ("north", "south", "east", "west", "up", "down")}
    gem_faces = {f: {"uv": [7, 4, 9, 6], "texture": "#0"} for f in ("north", "south", "east", "west", "up", "down")}
    return {
        "credit": "CytricosRaces 7.0.0 - Modelo 3D Corona Real",
        "texture_size": [32, 32],
        "textures": {"0": tex_ref, "particle": tex_ref},
        "elements": [
            {"name": "banda_frontal", "from": [3.5, 11.5, 3.5], "to": [12.5, 13.5, 4.5], "faces": gold_faces},
            {"name": "banda_trasera", "from": [3.5, 11.5, 11.5], "to": [12.5, 13.5, 12.5], "faces": gold_faces},
            {"name": "banda_izq", "from": [3.5, 11.5, 4.5], "to": [4.5, 13.5, 11.5], "faces": gold_faces},
            {"name": "banda_der", "from": [11.5, 11.5, 4.5], "to": [12.5, 13.5, 11.5], "faces": gold_faces},
            {"name": "punta_central", "from": [7.0, 13.5, 3.6], "to": [9.0, 16.5, 4.4], "faces": gem_faces},
            {"name": "punta_izq", "from": [4.0, 13.5, 3.7], "to": [5.5, 15.6, 4.3], "faces": gold_faces},
            {"name": "punta_der", "from": [10.5, 13.5, 3.7], "to": [12.0, 15.6, 4.3], "faces": gold_faces}
        ],
        "display": {
            "head": {"translation": [0, 3.5, 0], "scale": [1.35, 1.35, 1.35]},
            "gui": {"rotation": [20, 30, 0], "translation": [0, -3.0, 0], "scale": [1.2, 1.2, 1.2]}
        }
    }


def halo_3d_model(cid: str) -> dict:
    """Modelo 3D verdadero de Aureola Astral flotante sobre la cabeza."""
    tex_ref = f"cytricos:item/custom/{cid}"
    ring_faces = {f: {"uv": [3, 5, 13, 9], "texture": "#0"} for f in ("north", "south", "east", "west", "up", "down")}
    return {
        "credit": "CytricosRaces 7.0.0 - Modelo 3D Aureola Astral",
        "texture_size": [32, 32],
        "textures": {"0": tex_ref, "particle": tex_ref},
        "elements": [
            {"name": "arco_norte", "from": [4.0, 15.0, 3.5], "to": [12.0, 16.0, 4.8], "faces": ring_faces},
            {"name": "arco_sur", "from": [4.0, 15.0, 11.2], "to": [12.0, 16.0, 12.5], "faces": ring_faces},
            {"name": "arco_oeste", "from": [3.5, 15.0, 4.8], "to": [4.8, 16.0, 11.2], "faces": ring_faces},
            {"name": "arco_este", "from": [11.2, 15.0, 4.8], "to": [12.5, 16.0, 11.2], "faces": ring_faces}
        ],
        "display": {
            "head": {"translation": [0, 5.0, 0], "scale": [1.35, 1.35, 1.35]},
            "gui": {"rotation": [28, 35, 0], "translation": [0, -4.5, 0], "scale": [1.15, 1.15, 1.15]}
        }
    }


def main():
    if os.path.exists(PACK_DIR):
        shutil.rmtree(PACK_DIR)
    os.makedirs(PACK_DIR, exist_ok=True)
    os.makedirs(os.path.dirname(ZIP_OUT_PUBLIC), exist_ok=True)

    # 1. pack.mcmeta
    write_json(os.path.join(PACK_DIR, "pack.mcmeta"), {
        "pack": {
            "pack_format": 46,
            "supported_formats": {"min_inclusive": 34, "max_inclusive": 99},
            "description": "§6CytricosRaces v7.0 §7- 11 Alas 3D Animadas y Reliquias CYTRICOS RPG"
        }
    })

    # 2. pack.png (64x64 emblema dorado)
    pack_px = []
    for y in range(64):
        for x in range(64):
            border = x < 4 or x >= 60 or y < 4 or y >= 60
            diag = abs(x - y) < 6 or abs((63 - x) - y) < 6
            if border:
                pack_px.append((212, 160, 23, 255))
            elif diag and 12 < x < 52 and 12 < y < 52:
                pack_px.append((255, 215, 70, 255))
            else:
                pack_px.append((28, 22, 42, 255))
    write_png(os.path.join(PACK_DIR, "pack.png"), 64, 64, pack_px)

    # 3. Todas las 11 Alas (10 obligatorias + dark_wings legado)
    # (wid, cmd_combined, cmd_left, cmd_right, icon_mat, c1, c2, c3, style)
    wings = [
        ("angel_wings", 20001, 20101, 20201, "feather", (215, 220, 240), (255, 255, 255), (255, 218, 120), "feathered"),
        ("demon_wings", 20002, 20102, 20202, "blaze_powder", (45, 12, 18), (150, 22, 25), (255, 115, 22), "membranous"),
        ("vampire_wings", 20003, 20103, 20203, "redstone", (32, 12, 22), (128, 14, 38), (220, 40, 72), "membranous"),
        ("dragon_wings", 20004, 20104, 20204, "dragon_breath", (48, 18, 78), (112, 42, 162), (218, 128, 255), "membranous"),
        ("fairy_wings", 20005, 20105, 20205, "prismarine_crystals", (210, 108, 212), (255, 182, 245), (148, 248, 255), "butterfly"),
        ("phoenix_wings", 20006, 20106, 20206, "fire_charge", (185, 42, 10), (255, 132, 22), (255, 238, 96), "feathered"),
        ("dark_wings", 20007, 20107, 20207, "coal", (20, 20, 30), (50, 48, 72), (120, 96, 175), "membranous"),
        ("celestial_wings", 20008, 20108, 20208, "nether_star", (42, 96, 192), (115, 202, 255), (255, 250, 198), "astral"),
        ("dark_angel_wings", 20009, 20109, 20209, "obsidian", (22, 14, 32), (62, 34, 88), (210, 62, 108), "feathered"),
        ("void_wings", 20010, 20110, 20210, "echo_shard", (12, 20, 34), (24, 92, 118), (64, 238, 225), "void"),
        ("nature_wings", 20011, 20111, 20211, "spore_blossom", (28, 86, 36), (78, 182, 72), (255, 205, 230), "nature"),
    ]

    for wid, cmd_all, cmd_l, cmd_r, icon_mat, c1, c2, c3, style in wings:
        write_png(
            os.path.join(PACK_DIR, "assets", "cytricos", "textures", "item", "wings", f"{wid}.png"),
            32, 32, draw_wing_texture_32(c1, c2, c3, style)
        )
        write_json(
            os.path.join(PACK_DIR, "assets", "cytricos", "models", "item", "wings", f"{wid}.json"),
            wing_combined_blockbench_model(wid)
        )
        write_json(
            os.path.join(PACK_DIR, "assets", "cytricos", "models", "item", "wings", f"{wid}_left.json"),
            wing_half_blockbench_model(wid, "left")
        )
        write_json(
            os.path.join(PACK_DIR, "assets", "cytricos", "models", "item", "wings", f"{wid}_right.json"),
            wing_half_blockbench_model(wid, "right")
        )
        # Textura 64x32 + equipment/*.json para Élitros físicos en Paper 1.21.4+ / 26.2
        write_png(
            os.path.join(PACK_DIR, "assets", "cytricos", "textures", "entity", "equipment", "wings", f"{wid}.png"),
            64, 32, draw_elytra_skin_64x32(c1, c2, c3, style)
        )
        write_json(
            os.path.join(PACK_DIR, "assets", "cytricos", "equipment", f"{wid}.json"),
            {
                "layers": {
                    "wings": [
                        {
                            "texture": f"cytricos:{wid}",
                            "use_player_texture": False
                        }
                    ]
                }
            }
        )

    # 4. Todos los 15 Objetos Personalizados (texturas 32x32 + modelos JSON 2D o 3D reales)
    custom_items = [
        ("reincarnation_essence", "nether_star", 10099, "minecraft:item/generated", "coin", (180, 130, 15), (255, 215, 50), (140, 245, 255)),
        ("vampire_blood_chalice", "potion", 21001, "minecraft:item/generated", "chalice", (110, 15, 25), (200, 35, 50), (255, 190, 60)),
        ("abyssal_demon_blade", "netherite_sword", 21002, "minecraft:item/handheld", "sword", (45, 10, 15), (210, 50, 20), (255, 175, 40)),
        ("celestial_seraph_staff", "blaze_rod", 21003, "minecraft:item/handheld", "staff", (180, 145, 40), (245, 235, 180), (130, 240, 255)),
        ("dwarven_mithril_pickaxe", "diamond_pickaxe", 21004, "minecraft:item/handheld", "pickaxe", (75, 110, 135), (145, 215, 235), (240, 255, 255)),
        ("fairy_dew_amulet", "feather", 21005, "minecraft:item/generated", "potion", (165, 70, 175), (255, 170, 245), (180, 255, 235)),
        ("elf_sylvan_bow", "bow", 21006, "minecraft:item/handheld", "bow", (45, 95, 35), (105, 185, 70), (210, 255, 145)),
        ("celestial_crown", "carved_pumpkin", 21007, "3d_crown", "crown", (175, 130, 15), (255, 215, 55), (120, 230, 255)),
        ("cytricos_relic_coin", "gold_nugget", 21008, "minecraft:item/generated", "coin", (160, 110, 10), (245, 195, 45), (255, 245, 170)),
        ("void_reaper_scythe", "netherite_hoe", 21009, "minecraft:item/handheld", "scythe", (24, 18, 42), (72, 48, 128), (75, 245, 230)),
        ("nature_warden_talisman", "emerald", 21010, "minecraft:item/generated", "gem", (22, 92, 42), (72, 205, 102), (225, 255, 175)),
        ("phoenix_ember_ring", "blaze_powder", 21011, "minecraft:item/generated", "ring", (165, 55, 12), (255, 145, 30), (255, 245, 110)),
        ("shadow_cloak_brooch", "phantom_membrane", 21012, "minecraft:item/generated", "brooch", (26, 22, 38), (68, 62, 95), (175, 135, 255)),
        ("dragon_heart_gem", "amethyst_shard", 21013, "minecraft:item/generated", "gem", (88, 22, 115), (175, 65, 225), (255, 165, 255)),
        ("astralis_halo", "carved_pumpkin", 21014, "3d_halo", "halo", (95, 155, 225), (175, 235, 255), (255, 255, 215)),
    ]

    for cid, vanilla_mat, cmd, parent_model, icon_kind, c1, c2, c3 in custom_items:
        write_png(
            os.path.join(PACK_DIR, "assets", "cytricos", "textures", "item", "custom", f"{cid}.png"),
            32, 32, draw_icon_texture_32(icon_kind, c1, c2, c3)
        )
        out_path = os.path.join(PACK_DIR, "assets", "cytricos", "models", "item", "custom", f"{cid}.json")
        if parent_model == "3d_crown":
            write_json(out_path, crown_3d_model(cid))
        elif parent_model == "3d_halo":
            write_json(out_path, halo_3d_model(cid))
        else:
            write_json(out_path, {
                "parent": parent_model,
                "textures": {"layer0": f"cytricos:item/custom/{cid}"}
            })

    # 5. Mapeo dual para Vanilla Items (1.20.5-1.21.3 overrides + 1.21.4+/26.2 items/range_dispatch)
    vanilla_bindings: dict[str, list[tuple[int, str]]] = {}
    for wid, cmd_all, cmd_l, cmd_r, icon_mat, *_ in wings:
        vanilla_bindings.setdefault("feather", []).append((cmd_all, f"cytricos:item/wings/{wid}"))
        vanilla_bindings.setdefault("feather", []).append((cmd_l, f"cytricos:item/wings/{wid}_left"))
        vanilla_bindings.setdefault("feather", []).append((cmd_r, f"cytricos:item/wings/{wid}_right"))
        vanilla_bindings.setdefault("elytra", []).append((cmd_all, f"cytricos:item/wings/{wid}"))
        if icon_mat != "feather":
            vanilla_bindings.setdefault(icon_mat, []).append((cmd_all, f"cytricos:item/wings/{wid}"))

    for cid, vanilla_mat, cmd, *_ in custom_items:
        vanilla_bindings.setdefault(vanilla_mat, []).append((cmd, f"cytricos:item/custom/{cid}"))

    handheld_mats = {"netherite_sword", "blaze_rod", "diamond_pickaxe", "netherite_hoe"}
    for vmat, entries in vanilla_bindings.items():
        # Desduplicar por CMD y ordenar estrictamente ascendente
        dedup = {cmd: mod for cmd, mod in entries}
        entries_sorted = sorted(dedup.items(), key=lambda e: e[0])
        parent_base = "minecraft:item/handheld" if vmat in handheld_mats else "minecraft:item/generated"

        write_json(os.path.join(PACK_DIR, "assets", "minecraft", "models", "item", f"{vmat}.json"), {
            "parent": parent_base,
            "textures": {"layer0": f"minecraft:item/{vmat}"},
            "overrides": [
                {"predicate": {"custom_model_data": cmd}, "model": model_path}
                for cmd, model_path in entries_sorted
            ]
        })

        write_json(os.path.join(PACK_DIR, "assets", "minecraft", "items", f"{vmat}.json"), {
            "model": {
                "type": "minecraft:range_dispatch",
                "property": "minecraft:custom_model_data",
                "index": 0,
                "fallback": {
                    "type": "minecraft:model",
                    "model": f"minecraft:item/{vmat}"
                },
                "entries": [
                    {
                        "threshold": float(cmd),
                        "model": {
                            "type": "minecraft:model",
                            "model": model_path
                        }
                    }
                    for cmd, model_path in entries_sorted
                ]
            }
        })

    # 6. Empaquetar ZIPs
    for out_zip in (ZIP_OUT_PACK, ZIP_OUT_PUBLIC):
        with zipfile.ZipFile(out_zip, "w", compression=zipfile.ZIP_DEFLATED) as zf:
            for dirpath, _, filenames in os.walk(PACK_DIR):
                for fn in sorted(filenames):
                    full = os.path.join(dirpath, fn)
                    arc = os.path.relpath(full, PACK_DIR).replace(os.sep, "/")
                    zf.write(full, arc)

    # 7. Verificación estricta de integridad (JSON, equipment, modelos, texturas no vacías, PNG)
    with zipfile.ZipFile(ZIP_OUT_PUBLIC, "r") as zf:
        names = set(zf.namelist())
        assert "pack.mcmeta" in names, "Falta pack.mcmeta"
        assert "pack.png" in names, "Falta pack.png"
        json_count = 0
        png_count = 0
        equip_count = 0
        for name in sorted(names):
            if name.endswith(".json"):
                json_count += 1
                data = json.loads(zf.read(name).decode("utf-8"))
                textures = data.get("textures", {})
                for _, tref in textures.items():
                    if isinstance(tref, str) and tref.startswith("cytricos:"):
                        rel_tex = "assets/cytricos/textures/" + tref.split(":", 1)[1] + ".png"
                        assert rel_tex in names, f"Textura faltante: {rel_tex} en {name}"
                for ov in data.get("overrides", []):
                    mref = ov.get("model", "")
                    if mref.startswith("cytricos:"):
                        rel_mod = "assets/cytricos/models/" + mref.split(":", 1)[1] + ".json"
                        assert rel_mod in names, f"Modelo faltante: {rel_mod} en {name}"
                if name.startswith("assets/cytricos/equipment/"):
                    equip_count += 1
                    for layer_list in data.get("layers", {}).values():
                        for layer in layer_list:
                            tex = layer.get("texture", "")
                            if tex.startswith("cytricos:"):
                                rel_eq = "assets/cytricos/textures/entity/equipment/wings/" + tex.split(":", 1)[1] + ".png"
                                assert rel_eq in names, f"Textura de equipo faltante: {rel_eq}"
                for elem in data.get("elements", []):
                    for pt in elem.get("from", []) + elem.get("to", []):
                        assert -16.0 <= pt <= 32.0, f"Coordenada Blockbench fuera de rango [-16,32] en {name}: {pt}"
            elif name.endswith(".png"):
                png_count += 1
                raw = zf.read(name)
                assert raw[:8] == b"\x89PNG\r\n\x1a\n", f"PNG inválido: {name}"
                assert len(raw) > 80, f"PNG posiblemente vacío: {name}"

    with open(ZIP_OUT_PUBLIC, "rb") as f:
        sha1 = hashlib.sha1(f.read()).hexdigest()
        size = os.path.getsize(ZIP_OUT_PUBLIC)

    print(f"VERIFIED RESOURCE PACK: {ZIP_OUT_PUBLIC}")
    print(f"Total archivos en ZIP: {len(names)} | JSON validos: {json_count} | PNG validos: {png_count} | Equipment wings: {equip_count}")
    print(f"Tamano ZIP: {size} bytes | SHA-1: {sha1}")


if __name__ == "__main__":
    main()
