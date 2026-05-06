$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$PackDir = Join-Path $Root "resourcepacks\networkstorage-test-gui"
$ReferenceDir = Join-Path $Root "reference"
$TextureDir = Join-Path $PackDir "assets\networkstorage\textures\font"
$ToolTextureDir = Join-Path $PackDir "assets\networkstorage\textures\item"
$ButtonTextureDir = Join-Path $PackDir "assets\networkstorage\textures\item\gui\terminal"
$ButtonModelDir = Join-Path $PackDir "assets\networkstorage\models\item\gui\terminal"
$MinecraftItemsDir = Join-Path $PackDir "assets\minecraft\items"
$MinecraftBlockstatesDir = Join-Path $PackDir "assets\minecraft\blockstates"
$TerminalBlockTextureDir = Join-Path $PackDir "assets\networkstorage\textures\block"
$TerminalBlockModelDir = Join-Path $PackDir "assets\networkstorage\models\block"
$TerminalItemModelDir = Join-Path $PackDir "assets\networkstorage\models\item"
$ZipPath = Join-Path $Root "NetworkStorage.zip"
$MinecraftResourcePacks = Join-Path $env:APPDATA ".minecraft\resourcepacks"

if (-not (Test-Path $TextureDir)) {
    New-Item -ItemType Directory -Path $TextureDir | Out-Null
}
foreach ($dir in @($ReferenceDir, $ToolTextureDir, $ButtonTextureDir, $ButtonModelDir, $MinecraftItemsDir, $MinecraftBlockstatesDir, $TerminalBlockTextureDir, $TerminalBlockModelDir, $TerminalItemModelDir)) {
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir | Out-Null
    }
}

$SourceTexture = Join-Path $TextureDir "menu_container.png"

$ShouldGenerateMenuTexture = -not (Test-Path $SourceTexture)
if ($ShouldGenerateMenuTexture) {
    Invoke-WebRequest `
        -Uri "https://raw.githubusercontent.com/MitchGB/CustomInventoryUI/refs/heads/main/TestResourcePack/assets/minecraft/textures/custom/ui/menu_container.png" `
        -OutFile $SourceTexture

    $env:NS_GUI_SOURCE = $SourceTexture
    @'
import os
from PIL import Image

path = os.environ["NS_GUI_SOURCE"]
image = Image.open(path).convert("RGBA")

# Keep the reference texture's full 256x256 canvas for alignment, but make
# only the 9x5 item storage area transparent. The sixth row is the terminal
# control row, so it stays covered by the custom panel.
slot_origin_x = 7
slot_origin_y = 23
slot_size = 18

for row in range(5):
    for col in range(9):
        x0 = slot_origin_x + col * slot_size
        y0 = slot_origin_y + row * slot_size
        for y in range(y0, y0 + slot_size):
            for x in range(x0, x0 + slot_size):
                image.putpixel((x, y), (0, 0, 0, 0))

image.save(path)
print("Cut transparent 9x5 storage slot area into menu_container.png.")
'@ | python -
}

$env:NS_GUI_DIGITS = (Join-Path $TextureDir "page_digits.png")
$env:NS_GUI_ASCII = (Join-Path $TextureDir "ascii.png")
if (-not (Test-Path $env:NS_GUI_DIGITS)) {
    Invoke-WebRequest `
        -Uri "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/1.21.11/assets/minecraft/textures/font/ascii.png" `
        -OutFile $env:NS_GUI_ASCII
    @'
import os
from PIL import Image

target = os.environ["NS_GUI_DIGITS"]
ascii_path = os.environ["NS_GUI_ASCII"]
chars = "0123456789/"
cell_width = 8
height = 8
image = Image.new("RGBA", (cell_width * len(chars), height), (0, 0, 0, 0))
atlas = Image.open(ascii_path).convert("RGBA")

for index, char in enumerate(chars):
    codepoint = ord(char)
    source_x = (codepoint % 16) * cell_width
    source_y = (codepoint // 16) * height
    glyph = atlas.crop((source_x, source_y, source_x + cell_width, source_y + height))
    image.paste(glyph, (index * cell_width, 0), glyph)

image.save(target)
print("Generated page_digits.png from Minecraft ascii.png for the terminal page indicator.")
'@ | python -
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
}

$env:NS_GUI_BUTTON_TEXTURES = $ButtonTextureDir
$env:NS_GUI_BUTTON_MODELS = $ButtonModelDir
$env:NS_GUI_ITEM_MODELS = $TerminalItemModelDir
$env:NS_GUI_MINECRAFT_ITEMS = $MinecraftItemsDir
@'
import json
import math
import os
from pathlib import Path
from PIL import Image, ImageDraw

texture_dir = Path(os.environ["NS_GUI_BUTTON_TEXTURES"])
model_dir = Path(os.environ["NS_GUI_BUTTON_MODELS"])
item_model_dir = Path(os.environ["NS_GUI_ITEM_MODELS"])
items_dir = Path(os.environ["NS_GUI_MINECRAFT_ITEMS"])

button_specs = {
    "prev_page": {"material": "arrow", "cmd": 10101, "kind": "arrow_left"},
    "next_page": {"material": "arrow", "cmd": 10102, "kind": "arrow_right"},
    "search": {"material": "spyglass", "cmd": 10103, "kind": "search"},
    "sort": {"material": "comparator", "cmd": 10104, "kind": "sort"},
    "info": {"material": "book", "cmd": 10105, "kind": "info"},
    "stats": {"material": "emerald", "cmd": 10106, "kind": "stats"},
    "refresh": {"material": "clock", "cmd": 10107, "kind": "refresh"},
    "page_indicator": {"material": "light_gray_stained_glass_pane", "cmd": 10108, "kind": "invisible"},
}

def draw_arrow(draw, direction, fill):
    if direction == "left":
        draw.polygon([(4, 8), (10, 3), (10, 6), (13, 6), (13, 10), (10, 10), (10, 13)], fill=fill)
    else:
        draw.polygon([(12, 8), (6, 3), (6, 6), (3, 6), (3, 10), (6, 10), (6, 13)], fill=fill)

def draw_icon(kind, path):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    outline = (35, 35, 35, 255)
    light = (235, 235, 235, 255)
    muted = (150, 150, 150, 255)
    teal = (50, 150, 160, 255)
    green = (80, 190, 80, 255)
    gold = (225, 170, 45, 255)
    red = (190, 75, 65, 255)

    if kind == "invisible":
        image.save(path)
        return

    draw.rectangle((1, 1, 14, 14), fill=light, outline=outline)

    if kind == "arrow_left":
        draw_arrow(draw, "left", muted)
    elif kind == "arrow_right":
        draw_arrow(draw, "right", muted)
    elif kind == "search":
        draw.ellipse((3, 3, 9, 9), outline=teal, width=2)
        draw.line((9, 9, 13, 13), fill=teal, width=2)
    elif kind == "sort":
        draw.line((4, 4, 12, 4), fill=gold, width=2)
        draw.line((4, 8, 10, 8), fill=gold, width=2)
        draw.line((4, 12, 8, 12), fill=gold, width=2)
    elif kind == "refresh":
        draw.arc((3, 3, 13, 13), 35, 310, fill=green, width=2)
        draw.polygon([(12, 4), (12, 8), (15, 6)], fill=green)
    elif kind == "info":
        draw.ellipse((7, 3, 9, 5), fill=teal)
        draw.rectangle((7, 7, 9, 12), fill=teal)
    elif kind == "stats":
        draw.rectangle((4, 9, 6, 12), fill=green)
        draw.rectangle((7, 6, 9, 12), fill=green)
        draw.rectangle((10, 3, 12, 12), fill=green)

    image.save(path)

for name, spec in button_specs.items():
    texture_path = texture_dir / f"{name}.png"
    if not texture_path.exists():
        draw_icon(spec["kind"], texture_path)

    with open(model_dir / f"{name}.json", "w", encoding="utf-8") as f:
        json.dump({
            "parent": "minecraft:item/generated",
            "textures": {
                "layer0": f"networkstorage:item/gui/terminal/{name}"
            }
        }, f, indent=2)

by_material = {}
for name, spec in button_specs.items():
    by_material.setdefault(spec["material"], []).append((spec["cmd"], name))

for material, entries in by_material.items():
    entries = sorted(entries)
    with open(items_dir / f"{material}.json", "w", encoding="utf-8") as f:
        json.dump({
            "model": {
                "type": "minecraft:range_dispatch",
                "property": "minecraft:custom_model_data",
                "entries": [
                    {
                        "threshold": cmd,
                        "model": {
                            "type": "minecraft:model",
                            "model": f"networkstorage:item/gui/terminal/{name}"
                        }
                    }
                    for cmd, name in entries
                ],
                "fallback": {
                    "type": "minecraft:model",
                    "model": f"minecraft:item/{material}"
                }
            }
        }, f, indent=2)

tool_specs = {
    "compass": {"cmd": 10001, "name": "wireless_terminal"},
    "stick": {"cmd": 10002, "name": "storage_wand"},
}

def vanilla_compass_model(target):
    entries = []
    models = ["16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31",
              "00", "01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13", "14", "15", "16"]
    for index, suffix in enumerate(models):
        threshold = 0.0 if index == 0 else index - 0.5
        entries.append({
            "threshold": threshold,
            "model": {
                "type": "minecraft:model",
                "model": f"minecraft:item/compass_{suffix}"
            }
        })
    return {
        "type": "minecraft:range_dispatch",
        "property": "minecraft:compass",
        "scale": 32.0,
        "target": target,
        "entries": entries
    }

def fallback_model(material):
    if material == "compass":
        return {
            "type": "minecraft:condition",
            "property": "minecraft:has_component",
            "component": "minecraft:lodestone_tracker",
            "on_false": vanilla_compass_model("spawn"),
            "on_true": vanilla_compass_model("lodestone")
        }
    return {
        "type": "minecraft:model",
        "model": f"minecraft:item/{material}"
    }

for material, spec in tool_specs.items():
    with open(item_model_dir / f"{spec['name']}.json", "w", encoding="utf-8") as f:
        json.dump({
            "parent": "minecraft:item/handheld" if spec["name"] == "storage_wand" else "minecraft:item/generated",
            "textures": {
                "layer0": f"networkstorage:item/{spec['name']}"
            }
        }, f, indent=2)

    with open(items_dir / f"{material}.json", "w", encoding="utf-8") as f:
        json.dump({
            "model": {
                "type": "minecraft:range_dispatch",
                "property": "minecraft:custom_model_data",
                "entries": [
                    {
                        "threshold": spec["cmd"],
                        "model": {
                            "type": "minecraft:model",
                            "model": f"networkstorage:item/{spec['name']}"
                        }
                    }
                ],
                "fallback": fallback_model(material)
            }
        }, f, indent=2)

print("Generated terminal button textures and item model definitions.")
'@ | python -
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$env:NS_GUI_BLOCKSTATES = $MinecraftBlockstatesDir
$env:NS_GUI_BLOCK_TEXTURES = $TerminalBlockTextureDir
$env:NS_GUI_BLOCK_MODELS = $TerminalBlockModelDir
$env:NS_GUI_ITEM_MODELS = $TerminalItemModelDir
$env:NS_GUI_TOMS_MODEL = (Join-Path $ReferenceDir "storage_terminal.tom.json")
if (-not (Test-Path $env:NS_GUI_TOMS_MODEL)) {
    Invoke-WebRequest `
        -Uri "https://raw.githubusercontent.com/tom5454/Toms-Storage/refs/heads/master/NeoForge/src/platform-shared/resources/assets/toms_storage/models/block/storage_terminal.json" `
        -OutFile $env:NS_GUI_TOMS_MODEL
}
$TerminalFrontTexture = Join-Path $TerminalBlockTextureDir "storage_terminal_front.png"
if (-not (Test-Path $TerminalFrontTexture)) {
    Invoke-WebRequest `
        -Uri "https://github.com/tom5454/Toms-Storage/blob/master/NeoForge/src/platform-shared/resources/assets/toms_storage/textures/block/terminal_front.png?raw=true" `
        -OutFile $TerminalFrontTexture
}
$TerminalSideTexture = Join-Path $TerminalBlockTextureDir "storage_terminal_side.png"
if (-not (Test-Path $TerminalSideTexture)) {
    Invoke-WebRequest `
        -Uri "https://github.com/tom5454/Toms-Storage/blob/master/NeoForge/src/platform-shared/resources/assets/toms_storage/textures/block/terminal_side.png?raw=true" `
        -OutFile $TerminalSideTexture
}
$TerminalInventoryTexture = Join-Path $TerminalBlockTextureDir "storage_terminal_inventory.png"
if (-not (Test-Path $TerminalInventoryTexture)) {
    Invoke-WebRequest `
        -Uri "https://github.com/tom5454/Toms-Storage/blob/master/NeoForge/src/platform-shared/resources/assets/toms_storage/textures/block/inventory_block.png?raw=true" `
        -OutFile $TerminalInventoryTexture
}
@'
import json
import os
from pathlib import Path

blockstates_dir = Path(os.environ["NS_GUI_BLOCKSTATES"])
items_dir = Path(os.environ["NS_GUI_MINECRAFT_ITEMS"])
texture_dir = Path(os.environ["NS_GUI_BLOCK_TEXTURES"])
model_dir = Path(os.environ["NS_GUI_BLOCK_MODELS"])
item_model_dir = Path(os.environ["NS_GUI_ITEM_MODELS"])
source_model = Path(os.environ["NS_GUI_TOMS_MODEL"])
old_source_model = model_dir / "storage_terminal.tom.json"
if old_source_model.exists():
    old_source_model.unlink()

with open(source_model, "r", encoding="utf-8") as f:
    terminal_model = json.load(f)

terminal_model.pop("__comment", None)
terminal_model["textures"] = {
    "0": "networkstorage:block/storage_terminal_inventory",
    "1": "networkstorage:block/storage_terminal_side",
    "2": "networkstorage:block/storage_terminal_front",
    "particle": "networkstorage:block/storage_terminal_inventory",
}

with open(model_dir / "storage_terminal.json", "w", encoding="utf-8") as f:
    json.dump(terminal_model, f, indent=2)

terminal_item_model = json.loads(json.dumps(terminal_model))
terminal_item_model["display"] = {
    "gui": {
        "rotation": [30, 45, 0],
        "translation": [2.5, -1.5, 0],
        "scale": [0.625, 0.625, 0.625],
    },
    "fixed": {
        "rotation": [0, 180, 0],
        "translation": [0, 0, -4],
        "scale": [0.5, 0.5, 0.5],
    },
    "on_shelf": {
        "rotation": [0, 0, 0],
        "translation": [0, 0, 4],
        "scale": [1, 1, 1],
    },
    "firstperson_righthand": {
        "rotation": [0, 315, 0],
        "translation": [0, -2, 0],
        "scale": [0.5, 0.5, 0.5],
    },
    "thirdperson_righthand": {
        "rotation": [75, 315, 0],
        "translation": [-3, 1, 0],
        "scale": [0.4, 0.4, 0.4],
    },
}
with open(item_model_dir / "storage_terminal_inventory.json", "w", encoding="utf-8") as f:
    json.dump(terminal_item_model, f, indent=2)

vanilla_facing_rotations = {
    "north": None,
    "east": 90,
    "south": 180,
    "west": 270,
}

terminal_facing_rotations = {
    "north": 180,
    "east": 270,
    "south": None,
    "west": 90,
}

multipart = []

def condition(*parts):
    return {"AND": list(parts)}

for facing, rotation in vanilla_facing_rotations.items():
    apply = {"model": "minecraft:block/oak_shelf"}
    if rotation is not None:
        apply["y"] = rotation
    multipart.append({
        "apply": apply,
        "when": condition(
            {"facing": facing},
            {"OR": [{"powered": "true"}, {"side_chain": "unconnected|left|right"}]},
        )
    })

for facing, rotation in vanilla_facing_rotations.items():
    apply = {"model": "minecraft:block/oak_shelf_unpowered"}
    if rotation is not None:
        apply["y"] = rotation
    multipart.append({
        "apply": apply,
        "when": condition(
            {"facing": facing},
            {"powered": "false"},
            {"side_chain": "unconnected|left|right"},
        )
    })

for side_chain in ("unconnected", "left", "center", "right"):
    model = "minecraft:block/oak_shelf_unconnected" if side_chain == "unconnected" else f"minecraft:block/oak_shelf_{side_chain}"
    for facing, rotation in vanilla_facing_rotations.items():
        apply = {"model": model}
        if rotation is not None:
            apply["y"] = rotation
        multipart.append({
            "apply": apply,
            "when": condition(
                {"facing": facing},
                {"powered": "true"},
                {"side_chain": side_chain},
            )
        })

for facing, rotation in terminal_facing_rotations.items():
    apply = {"model": "networkstorage:block/storage_terminal"}
    if rotation is not None:
        apply["y"] = rotation
    multipart.append({
        "apply": apply,
        "when": condition(
            {"facing": facing},
            {"powered": "false"},
            {"side_chain": "center"},
        )
    })

with open(blockstates_dir / "oak_shelf.json", "w", encoding="utf-8") as f:
    json.dump({"multipart": multipart}, f, indent=2)

with open(items_dir / "oak_shelf.json", "w", encoding="utf-8") as f:
    json.dump({
        "model": {
            "type": "minecraft:range_dispatch",
            "property": "minecraft:custom_model_data",
            "entries": [
                {
                    "threshold": 10003,
                    "model": {
                        "type": "minecraft:model",
                        "model": "networkstorage:item/storage_terminal_inventory"
                    }
                }
            ],
            "fallback": {
                "type": "minecraft:model",
                "model": "minecraft:block/oak_shelf_inventory"
            }
        }
    }, f, indent=2)

print("Generated custom oak_shelf terminal blockstate, model, and textures.")
'@ | python -
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$TempZipPath = Join-Path $Root "NetworkStorage.tmp.zip"
if (Test-Path $TempZipPath) {
    Remove-Item $TempZipPath -Force
}

Compress-Archive -Path (Join-Path $PackDir "*") -DestinationPath $TempZipPath -Force

if (-not (Test-Path $MinecraftResourcePacks)) {
    New-Item -ItemType Directory -Path $MinecraftResourcePacks | Out-Null
}

try {
    Copy-Item $TempZipPath $ZipPath -Force
}
catch {
    Write-Warning "Could not update $ZipPath because it is in use. Close it and rerun this script to update the workspace copy."
}

Copy-Item $TempZipPath (Join-Path $MinecraftResourcePacks "NetworkStorage.zip") -Force
Remove-Item $TempZipPath -Force
Write-Host "Built $ZipPath"
Write-Host "Copied to $MinecraftResourcePacks"
