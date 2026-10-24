"""
Builds the default loadouts that every player sees in the Community browser.
They live under users/default like any published loadout, so every plugin
version lists them; names end in "(Default)", and 1.2.1 and later also show the
author "Default". Writes firebase/defaults.json, which an admin uploads with:

    firebase database:set /users/default firebase/defaults.json

No player can write users/default (Firebase ids are 28 characters), and the
rules refuse the word "default" in any player's loadout name.
"""
import json
import os
import time

TABS = ["inv", "prayer", "spell"]
TAB_SECTIONS = {"inv": "Inventory", "prayer": "Prayer", "spell": "Spellbook"}


def color(r, g, b, a=255):
    """A color as RuneLite stores it: a signed 32-bit ARGB int, as text."""
    value = (a << 24) | (r << 16) | (g << 8) | b
    if value >= 1 << 31:
        value -= 1 << 32
    return str(value)


def text(values):
    return {k: str(v).lower() if isinstance(v, bool) else str(v) for k, v in values.items()}


def hover(style, outline, fill, width=1, radius=4, fade=300, background=None):
    """The same hover settings for every tab."""
    sections = {}
    for tab in TABS:
        values = {
            "Style": style, "OutlineColor": outline, "FillColor": fill, "OutlineWidth": width,
            "CornerRadius": radius, "HoverFade": fade, "HoverDelay": 0, "HideWhenMenuOpen": True,
            "DiscoMode": False, "KeepIconColors": True,
            "SlotBackground": background is not None,
        }
        if background is not None:
            values["SlotBackgroundColor"] = background
        sections[TAB_SECTIONS[tab] + ": hover"] = text({tab + k: v for k, v in values.items()})
    return sections


def missed(color_value, ring="GROW", ring_ms=450):
    return {"Missed clicks": text({
        "deadClickMarker": True, "rejectedClickMarker": True, "missOutline": True,
        "misclickColor": color_value, "rejectedClickColor": color(255, 160, 0, 200),
        "ringAnimation": ring, "ringDuration": ring_ms,
        "missOutlineColor": color_value, "missOutlineDuration": 1200, "missOutlineWidth": 2,
    })}


def loadouts():
    red = color(255, 60, 60, 210)
    clean = {
        "Areas": text({"prayers": True, "spells": True, "bankInventory": True, "onlyItems": True,
                       "grid": "OFF", "roundedAreas": True}),
        "Click feedback": text({"clickFlash": True, "clickFlashColor": color(255, 255, 255, 70),
                                "clickFlashDuration": 150, "clickMarker": False}),
        **missed(red),
        "Dead areas": text({"deadAreas": "OFF"}),
        **hover("OUTLINE", color(255, 255, 255, 150), color(255, 255, 255, 0), fade=250),
    }

    pvp = {
        "Areas": text({"prayers": True, "spells": True, "bankInventory": True, "onlyItems": True,
                       "grid": "OFF", "roundedAreas": True}),
        "Click feedback": text({"clickFlash": True, "clickFlashColor": color(120, 255, 140, 110),
                                "clickFlashDuration": 120, "clickMarker": True,
                                "clickMarkerColor": color(120, 255, 140, 220),
                                "doubleClickColor": color(255, 220, 60, 230)}),
        **missed(red, ring="CROSS", ring_ms=400),
        "Dead areas": text({"deadAreas": "IN_PANEL", "deadInventory": True,
                            "deadInventoryColor": color(255, 40, 40, 45), "deadPrayers": True,
                            "deadPrayersColor": color(255, 40, 40, 45), "deadSpells": True,
                            "deadSpellsColor": color(255, 40, 40, 45)}),
        "Eat and drink delay": text({"cooldowns": True, "cooldownStyle": "DRAIN",
                                     "cooldownColor": color(0, 0, 0, 130)}),
        **hover("BOTH", color(120, 255, 140, 220), color(120, 255, 140, 45), width=2, fade=0),
    }

    flick = {
        "Areas": text({"prayers": True, "spells": True, "onlyItems": True, "grid": "OFF",
                       "roundedAreas": True}),
        "Prayers and spells": text({"activePrayers": True, "pendingPrayers": True, "selectedSpell": True,
                                    "autocastMarker": True, "activePrayerColor": color(255, 200, 60, 230),
                                    "activePrayerFill": color(255, 200, 60, 60)}),
        "Tick cue": text({"tickCue": "PULSE", "tickCueAllPrayers": False,
                          "cuePrayers": "Protect from Magic, Protect from Missiles, Protect from Melee",
                          "pulseColor": color(170, 110, 255, 120), "tickLatency": 20, "pulsePosition": 40}),
        **missed(red),
        **hover("BOTH", color(255, 255, 255, 170), color(170, 110, 255, 40), fade=150),
    }

    contrast = {
        "Areas": text({"prayers": True, "spells": True, "bankInventory": True, "onlyItems": False,
                       "grid": "IN_PANEL", "gridColor": color(255, 255, 255, 40), "roundedAreas": False}),
        "Click feedback": text({"clickFlash": True, "clickFlashColor": color(255, 235, 0, 120),
                                "clickFlashDuration": 250, "clickMarker": True,
                                "clickMarkerColor": color(255, 235, 0, 255)}),
        **missed(color(255, 0, 200, 230), ring="CROSS", ring_ms=700),
        "Dead areas": text({"deadAreas": "ALWAYS", "deadInventory": True,
                            "deadInventoryColor": color(255, 0, 0, 90), "deadBank": True,
                            "deadBankColor": color(255, 0, 0, 90), "deadPrayers": True,
                            "deadPrayersColor": color(255, 0, 0, 90), "deadSpells": True,
                            "deadSpellsColor": color(255, 0, 0, 90)}),
        **hover("BOTH", color(255, 235, 0, 255), color(255, 235, 0, 70), width=3, radius=0, fade=0),
    }

    glow = {
        "Click feedback": text({"clickFlash": False, "clickMarker": False}),
        **missed(red),
        **hover("OUTLINE", color(255, 255, 255, 120), color(255, 255, 255, 0), fade=400),
        "Inventory: spotlight": text({
            "invSpotlight": True, "invSpotlightRadius": 140, "invSpotlightDim": color(0, 0, 0, 90),
            "invSpotlightGlow": color(255, 230, 170, 50), "invSpotlightGlowSize": 40,
            "invSpotlightShape": "SMOOTH", "invSpotlightType": "CIRCLE", "invSpotlightMotion": "NONE",
            "invSpotlightEffect": "SPARKLES", "invSpotlightEffectColor": "OVERLAY",
            "invSpotlightEffectOnClick": True, "invSpotlightEffectDetach": True,
            "invSpotlightEffectReach": 200, "invSpotlightEffectSpread": 150,
            "invSpotlightEffectLength": 900, "invSpotlightEffectSpeed": 100, "invSpotlightEffectSize": 100,
            "invSpotlightClickResult": True, "invSpotlightClickAnywhere": True,
            "invSpotlightClickAnywhereDelay": 0, "invSpotlightAlways": False,
            "invSpotlightDeadOnly": False, "invSpotlightFalloff": False,
        }),
    }

    return [
        ("Clean Outline", clean),
        ("PvP Switcher", pvp),
        ("Prayer Flicker", flick),
        ("High Contrast", contrast),
        ("Spotlight Sparkle", glow),
    ]


def main():
    now = int(time.time() * 1000)
    out = {}
    for slot, (name, settings) in enumerate(loadouts()):
        export = {"plugin": "click-helpers", "version": 1, "settings": settings}
        # Older first, so the browser's newest-first sort keeps this order
        out[str(slot)] = {"name": name + " (Default)", "created": now - slot * 1000, "settings": json.dumps(export)}
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "defaults.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(out, f, indent=2)
    print("Wrote", path)


if __name__ == "__main__":
    main()
