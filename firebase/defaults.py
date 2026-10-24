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
                            "deadInventoryColor": color(0, 0, 0, 120), "deadBank": True,
                            "deadBankColor": color(0, 0, 0, 120), "deadPrayers": True,
                            "deadPrayersColor": color(0, 0, 0, 120), "deadSpells": True,
                            "deadSpellsColor": color(0, 0, 0, 120)}),
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


def spotlight(tab, values):
    """One tab's spotlight section, keyed without the tab prefix: "" is the on switch, "Radius" the radius."""
    return {TAB_SECTIONS[tab] + ": spotlight": text({tab + "Spotlight" + k: v for k, v in values.items()})}


def miss_dots(tab, dot_color, shape, size=4, hollow=False, seconds=2):
    return {TAB_SECTIONS[tab] + ": miss dots": text({
        tab + "MissDots": True, tab + "MissDotColor": dot_color, tab + "MissDotShape": shape,
        tab + "MissDotHollow": hollow, tab + "MissDotSize": size, tab + "MissDotDuration": seconds,
    })}


def burst(effect, effect_color, reach, spread, amount, length, speed=100, size=100):
    """Spotlight settings for an effect that bursts on each click and plays out where it started."""
    return {"Effect": effect, "EffectCustom": True, "EffectOverride": effect_color, "EffectOnClick": True,
            "EffectDetach": True, "EffectReach": reach, "EffectSpread": spread, "EffectAmount": amount,
            "EffectLength": length, "EffectSpeed": speed, "EffectSize": size}


def involved():
    """Five showier loadouts that combine the spotlight, effects and per-tab extras."""
    forge = {
        **missed(color(255, 90, 20, 220), ring="PULSE", ring_ms=500),
        "Click feedback": text({"clickFlash": True, "clickFlashColor": color(255, 140, 40, 90),
                                "clickFlashDuration": 160, "clickMarker": False}),
        **hover("BOTH", color(255, 150, 60, 220), color(255, 110, 20, 40), width=2, fade=200),
    }
    for tab in TABS:
        forge.update(spotlight(tab, {
            "": True, "Radius": 160, "Dim": color(20, 5, 0, 120), "Glow": color(255, 120, 30, 70),
            "GlowSize": 45, "Shape": "SOFT", "Type": "HEXAGON", "Motion": "NONE", "EffectColor": "GRADIENT",
            **burst("EMBERS", color(255, 140, 40, 230), reach=300, spread=120, amount=150, length=1200,
                    speed=90, size=110),
            "ClickResult": True, "ClickSuccessColor": color(255, 200, 80, 230),
            "ClickMissColor": color(255, 40, 20, 230), "ClickRefusedColor": color(160, 160, 160, 200),
            "Always": False, "DeadOnly": True,
            "Falloff": True, "FalloffDistance": 200, "FalloffEnd": 520, "FalloffColor": color(90, 20, 0, 60),
        }))
        forge.update(miss_dots(tab, color(255, 120, 30), "TRIANGLE", size=5))
    forge["Inventory: spotlight"].update(text({"invSpotlightClickAnywhere": True,
                                              "invSpotlightClickAnywherePanels": True,
                                              "invSpotlightClickAnywhereDelay": 350}))

    frost = {
        **missed(color(120, 200, 255, 220), ring="SHRINK", ring_ms=600),
        "Click feedback": text({"clickFlash": True, "clickFlashColor": color(200, 240, 255, 100),
                                "clickFlashDuration": 200, "clickMarker": True,
                                "clickMarkerColor": color(200, 240, 255, 230)}),
        **hover("OUTLINE", color(170, 230, 255, 220), color(170, 230, 255, 0), width=2, fade=350),
    }
    for tab in TABS:
        frost.update(spotlight(tab, {
            "": True, "Radius": 200, "Dim": color(0, 20, 40, 100), "Glow": color(170, 230, 255, 60),
            "GlowSize": 50, "Shape": "SMOOTH", "Type": "ELLIPSE", "Motion": "WOBBLE",
            "Effect": "SNOW", "EffectColor": "WHOLE", "EffectCustom": True,
            "EffectOverride": color(235, 248, 255, 220), "EffectOnClick": False, "EffectDetach": False,
            "EffectAmount": 180, "EffectSpeed": 60, "EffectSize": 120,
            "Always": True, "DeadOnly": False, "Falloff": False,
        }))
        frost.update(miss_dots(tab, color(150, 220, 255), "RING", size=5, hollow=True))

    arcane = {
        "Areas": text({"prayers": True, "spells": True, "onlyItems": True, "grid": "OFF", "roundedAreas": True}),
        "Prayers and spells": text({"activePrayers": True, "pendingPrayers": True, "selectedSpell": True,
                                    "autocastMarker": True, "activePrayerColor": color(190, 120, 255, 230),
                                    "activePrayerFill": color(150, 80, 255, 60),
                                    "autocastColor": color(220, 170, 255, 230)}),
        "Tick cue": text({"tickCue": "COLOR", "tickCueAllPrayers": False,
                          "cuePrayers": "Protect from Magic, Protect from Missiles, Protect from Melee",
                          "tickLatency": 20, "pulsePosition": 40}),
        **missed(color(255, 80, 200, 220), ring="GROW", ring_ms=450),
        **hover("BOTH", color(200, 150, 255, 220), color(150, 80, 255, 45), width=2, fade=150),
    }
    looks = {"inv": ("RADAR", "SPIN", "ORBIT"), "prayer": ("STAR", "SPIN", "RIPPLES"),
             "spell": ("RINGS", "NONE", "SPARKLES")}
    for tab, (kind, motion, effect) in looks.items():
        arcane.update(spotlight(tab, {
            "": True, "Radius": 170, "Dim": color(15, 0, 30, 110), "Glow": color(180, 100, 255, 60),
            "GlowSize": 40, "Shape": "BANDED", "Type": kind, "Motion": motion, "EffectColor": "GLOW",
            **burst(effect, color(210, 160, 255, 220), reach=150, spread=100, amount=120, length=1000, speed=120),
            "ClickResult": False, "Always": False, "DeadOnly": True, "Falloff": False,
        }))
        arcane.update(miss_dots(tab, color(180, 100, 255), "DIAMOND"))

    pink = color(255, 80, 190)
    disco = {
        **missed(pink, ring="PULSE", ring_ms=500),
        "Click feedback": text({"clickFlash": True, "clickFlashColor": color(255, 120, 220, 110),
                                "clickFlashDuration": 180, "clickMarker": True,
                                "clickMarkerColor": color(120, 255, 240, 230),
                                "doubleClickColor": color(255, 240, 80, 230)}),
    }
    for tab in TABS:
        disco[TAB_SECTIONS[tab] + ": hover"] = text({
            tab + "Style": "BOTH", tab + "OutlineColor": pink, tab + "FillColor": color(255, 80, 190, 50),
            tab + "OutlineWidth": 2, tab + "CornerRadius": 6, tab + "HoverFade": 100, tab + "HoverDelay": 0,
            tab + "HideWhenMenuOpen": True, tab + "DiscoMode": True, tab + "DiscoInterval": 300,
            tab + "KeepIconColors": True, tab + "SlotBackground": False})
        disco.update(spotlight(tab, {
            "": True, "Radius": 150, "Dim": color(10, 0, 20, 90), "Glow": color(255, 100, 220, 60),
            "GlowSize": 35, "Shape": "SHARP", "Type": "FLOWER", "Motion": "SPIN", "EffectColor": "OVERLAY",
            **burst("SPARKLES", pink, reach=250, spread=80, amount=250, length=900, speed=150, size=90),
            "EffectCustom": False, "ClickResult": False, "Always": False, "DeadOnly": False, "Falloff": False,
        }))
        disco.update(miss_dots(tab, pink, "HEART", size=6))
    disco["Inventory: spotlight"].update(text({"invSpotlightClickAnywhere": True,
                                              "invSpotlightClickAnywherePanels": True,
                                              "invSpotlightClickAnywhereDelay": 250}))

    gold = color(255, 200, 60)
    pvm = {
        "Areas": text({"prayers": True, "spells": True, "bankInventory": True, "onlyItems": True,
                       "grid": "OFF", "roundedAreas": True}),
        "Prayers and spells": text({"activePrayers": True, "pendingPrayers": True, "selectedSpell": True,
                                    "autocastMarker": True, "activePrayerColor": color(255, 200, 60, 240),
                                    "activePrayerFill": color(255, 200, 60, 55)}),
        "Tick cue": text({"tickCue": "DRAIN", "tickCueAllPrayers": False,
                          "cuePrayers": "Protect from Magic, Protect from Missiles, Protect from Melee",
                          "tickLatency": 20, "pulsePosition": 40}),
        "Eat and drink delay": text({"cooldowns": True, "cooldownStyle": "DRAIN", "cooldownColor": color(0, 0, 0, 140),
                                     "cooldownOutline": True, "cooldownOutlineStyle": "TRACE",
                                     "cooldownOutlineColor": gold, "cooldownOutlineWidth": 2}),
        "Dead areas": text({"deadAreas": "IN_PANEL", "deadInventory": True,
                            "deadInventoryColor": color(255, 40, 40, 40), "deadPrayers": True,
                            "deadPrayersColor": color(255, 40, 40, 40)}),
        **missed(color(255, 60, 60, 220), ring="CROSS", ring_ms=400),
        **hover("BOTH", color(255, 220, 120, 220), color(255, 200, 60, 35), width=2, fade=0),
    }
    pvm.update(spotlight("prayer", {
        "": True, "Radius": 120, "Dim": color(0, 0, 0, 80), "Glow": color(255, 210, 90, 60), "GlowSize": 40,
        "Shape": "SMOOTH", "Type": "CIRCLE", "Motion": "NONE", "EffectColor": "OVERLAY",
        **burst("RIPPLES", color(255, 220, 120, 220), reach=120, spread=100, amount=100, length=600, speed=140),
        "ClickResult": True, "ClickSuccessColor": color(120, 255, 140, 230),
        "ClickMissColor": color(255, 60, 60, 230), "ClickRefusedColor": color(160, 160, 160, 200),
        "Always": False, "DeadOnly": True, "Falloff": False,
    }))
    pvm.update(miss_dots("prayer", gold, "CROSS", size=5))

    return [
        ("Ember Forge", forge),
        ("Frost Bite", frost),
        ("Arcane Radar", arcane),
        ("Neon Disco", disco),
        ("Tick Perfect PvM", pvm),
    ]


def main():
    now = int(time.time() * 1000)
    out = {}
    for slot, (name, settings) in enumerate(loadouts() + involved()):
        export = {"plugin": "click-helpers", "version": 1, "settings": settings}
        # Older first, so the browser's newest-first sort keeps this order
        out[str(slot)] = {"name": name + " (Default)", "created": now - slot * 1000, "settings": json.dumps(export)}
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "defaults.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(out, f, indent=2)
    print("Wrote", path)


if __name__ == "__main__":
    main()
