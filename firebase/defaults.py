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


def themed():
    """Five more loadouts that set every section, each with its own look per tab."""
    def theme(accent, second, glow, dim, miss, hit, flash, fill, slot, grid, dead, falloff, particle,
              ring, cue, dot, shape, radius, reach, spread, amount, speed, looks):
        return complete({
            "accent": accent, "second": second, "glow": glow, "dim": dim, "miss": miss,
            "refused": color(160, 160, 170, 210), "hit": hit, "flash": flash, "fill": fill, "slot": slot,
            "grid": grid, "dead": dead, "falloff": falloff, "particle": particle, "ring": ring, "cue": cue,
            "dot": dot, "shape": shape, "radius": radius, "reach": reach, "spread": spread, "amount": amount,
            "speed": speed, "looks": looks})

    toxic = theme(color(150, 255, 60, 230), color(230, 255, 80, 230), color(90, 255, 40, 70), color(5, 25, 0, 125),
                  color(255, 120, 200, 230), color(200, 255, 120, 230), color(150, 255, 60, 90),
                  color(120, 255, 40, 40), color(10, 40, 0, 70), color(150, 255, 60, 30), color(5, 30, 0, 70),
                  color(30, 90, 0, 60), color(180, 255, 90, 230), "PULSE", "DRAIN", "CIRCLE", "SOFT", 160, 280,
                  130, 180, 70,
                  {"inv": ("CROSS", "WOBBLE", "EMBERS", "GRADIENT"), "prayer": ("VERTICAL_BAND", "NONE", "SNOW", "WHOLE"),
                   "spell": ("SQUARE", "SPIN", "SPARKLES", "GLOW")})
    ocean = theme(color(80, 220, 255, 230), color(40, 140, 255, 230), color(30, 160, 255, 70), color(0, 15, 35, 125),
                  color(255, 150, 90, 230), color(140, 255, 230, 230), color(90, 200, 255, 90),
                  color(40, 150, 255, 40), color(0, 25, 50, 70), color(80, 200, 255, 30), color(0, 20, 45, 70),
                  color(0, 50, 110, 60), color(200, 240, 255, 220), "SHRINK", "PULSE", "RING", "SMOOTH", 180, 320,
                  140, 150, 60,
                  {"inv": ("ELLIPSE", "WOBBLE", "RIPPLES", "GRADIENT"),
                   "prayer": ("HORIZONTAL_BAND", "WOBBLE", "SNOW", "WHOLE"), "spell": ("CIRCLE", "NONE", "RIPPLES", "GLOW")})
    aurora = theme(color(110, 255, 190, 230), color(255, 120, 230, 230), color(120, 255, 200, 70),
                   color(0, 15, 25, 120), color(255, 90, 140, 230), color(170, 255, 220, 230),
                   color(180, 140, 255, 90), color(140, 120, 255, 40), color(10, 20, 40, 70),
                   color(160, 255, 220, 30), color(5, 15, 35, 70), color(80, 30, 120, 60), color(200, 255, 230, 230),
                   "GROW", "COLOR", "DIAMOND", "BANDED", 160, 400, 180, 170, 90,
                   {"inv": ("RADAR", "SPIN", "SPARKLES", "GRADIENT"), "prayer": ("SPIRAL", "SPIN", "ORBIT", "GLOW"),
                    "spell": ("FLOWER", "WOBBLE", "SPARKLES", "GRADIENT")})
    moon = theme(color(225, 230, 240, 230), color(200, 40, 60, 230), color(170, 20, 40, 70), color(15, 0, 5, 135),
                 color(255, 60, 80, 230), color(240, 240, 255, 230), color(220, 220, 235, 80),
                 color(180, 30, 50, 35), color(30, 5, 10, 75), color(220, 220, 235, 25), color(20, 0, 5, 70),
                 color(70, 0, 15, 60), color(255, 120, 130, 220), "CROSS", "DRAIN", "TRIANGLE", "SHARP", 140, 260,
                 120, 140, 100,
                 {"inv": ("CIRCLE", "NONE", "EMBERS", "GRADIENT"), "prayer": ("STAR", "SPIN", "RIPPLES", "GLOW"),
                  "spell": ("HEXAGON", "WOBBLE", "EMBERS", "GRADIENT")})
    noir = theme(color(245, 245, 245, 235), color(160, 160, 160, 230), color(255, 255, 255, 55), color(0, 0, 0, 140),
                 color(255, 255, 255, 230), color(255, 255, 255, 230), color(255, 255, 255, 80),
                 color(255, 255, 255, 30), color(0, 0, 0, 80), color(255, 255, 255, 25), color(0, 0, 0, 80),
                 color(40, 40, 40, 60), color(240, 240, 240, 220), "CROSS", "PULSE", "SQUARE", "SHARP", 150, 300,
                 200, 140, 75,
                 {"inv": ("SQUARE", "NONE", "SNOW", "WHOLE"), "prayer": ("CROSS", "NONE", "RIPPLES", "GLOW"),
                  "spell": ("DIAMOND", "SPIN", "SPARKLES", "GRADIENT")})
    return [("Toxic Swamp", toxic), ("Ocean Depths", ocean), ("Aurora", aurora), ("Blood Moon", moon),
            ("Monochrome Noir", noir)]


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


def complete(theme):
    """
    A loadout that sets every section a player would style: all but the click chart,
    switch practice and import/export, which are personal. The theme gives the
    colors, plus each tab's spotlight look as (type, motion, effect, effect area).
    """
    accent, glow, dim, miss = theme["accent"], theme["glow"], theme["dim"], theme["miss"]
    settings = {
        "Areas": text({"roundedAreas": True, "prayers": True, "spells": True, "bankInventory": True,
                       "hideWhenEmpty": False, "onlyItems": True, "grid": "IN_PANEL", "gridShowEmpty": False,
                       "gridColor": theme["grid"]}),
        "Click feedback": text({"clickFlash": True, "clickMarker": True, "clickFlashColor": theme["flash"],
                                "clickFlashDuration": 170, "clickMarkerColor": accent,
                                "doubleClickColor": theme["second"]}),
        "Missed clicks": text({"deadClickMarker": True, "rejectedClickMarker": True, "missOutline": True,
                               "misclickColor": miss, "rejectedClickColor": theme["refused"],
                               "ringAnimation": theme["ring"], "ringDuration": 550, "missOutlineColor": miss,
                               "missOutlineDuration": 1300, "missOutlineWidth": 2}),
        "Dead areas": text({"deadAreas": "IN_PANEL", "deadInventory": True, "deadInventoryColor": theme["dead"],
                            "deadBank": True, "deadBankColor": theme["dead"], "deadPrayers": True,
                            "deadPrayersColor": theme["dead"], "deadSpells": True, "deadSpellsColor": theme["dead"],
                            "deadOuterAreas": False}),
        "Prayers and spells": text({"activePrayers": True, "pendingPrayers": True, "selectedSpell": True,
                                    "autocastMarker": True, "activePrayerColor": accent,
                                    "activePrayerFill": theme["fill"], "autocastColor": theme["second"]}),
        "Tick cue": text({"tickCue": theme["cue"], "tickCueAllPrayers": False,
                          "cuePrayers": "Protect from Magic, Protect from Missiles, Protect from Melee",
                          "pulseColor": theme["second"], "tickLatency": 20, "pulsePosition": 40}),
        "Hide items": text({"hideItems": False, "hiddenItemShowOutline": True, "hiddenItemShowFill": True,
                            "hiddenItemOutline": accent, "hiddenItemFill": theme["fill"]}),
        "Eat and drink delay": text({"cooldowns": True, "cooldownStyle": "DIM", "cooldownColor": dim,
                                     "cooldownOutline": True, "cooldownOutlineStyle": "TRACE",
                                     "cooldownOutlineColor": theme["second"], "cooldownOutlineWidth": 2}),
    }
    for tab in TABS:
        kind, motion, effect, area = theme["looks"][tab]
        settings[TAB_SECTIONS[tab] + ": hover"] = text({
            tab + "Style": "BOTH", tab + "OutlineColor": accent, tab + "FillColor": theme["fill"],
            tab + "OutlineWidth": 2, tab + "CornerRadius": 5, tab + "HoverFade": 250, tab + "HoverDelay": 0,
            tab + "HideWhenMenuOpen": True, tab + "DiscoMode": False, tab + "DiscoInterval": 0,
            tab + "KeepIconColors": True, tab + "SlotBackground": True, tab + "SlotBackgroundColor": theme["slot"]})
        settings.update(spotlight(tab, {
            "": True, "Radius": theme["radius"], "Dim": dim, "Glow": glow, "GlowSize": 45, "Shape": theme["shape"],
            "Type": kind, "Motion": motion, "EffectColor": area, "EffectReplace": False,
            **burst(effect, theme["particle"], reach=theme["reach"], spread=theme["spread"], amount=theme["amount"],
                    length=1300, speed=theme["speed"], size=110),
            "ClickResult": True, "ClickSuccessColor": theme["hit"], "ClickMissColor": miss,
            "ClickRefusedColor": theme["refused"], "Always": False, "DeadOnly": True,
            "Falloff": True, "FalloffDistance": theme["radius"] + 40, "FalloffEnd": theme["radius"] * 3,
            "FalloffColor": theme["falloff"], "FalloffColorEnd": 0, "FadeEndColor": color(0, 0, 0, 0),
        }))
        settings.update(miss_dots(tab, miss, theme["dot"], size=5, hollow=tab != "inv"))
    settings["Inventory: spotlight"].update(text({"invSpotlightClickAnywhere": True,
                                                 "invSpotlightClickAnywherePanels": True,
                                                 "invSpotlightClickAnywhereDelay": 300}))
    return settings


def showcase():
    """Two loadouts that set every section, to show what the plugin can do together."""
    void = complete({
        "accent": color(120, 230, 255, 230), "second": color(200, 120, 255, 230), "glow": color(110, 60, 255, 70),
        "dim": color(5, 0, 25, 130), "miss": color(255, 70, 160, 230), "refused": color(150, 150, 170, 210),
        "hit": color(120, 255, 220, 230), "flash": color(150, 120, 255, 90), "fill": color(110, 80, 255, 40),
        "slot": color(20, 10, 50, 70), "grid": color(140, 120, 255, 35), "dead": color(10, 0, 30, 70),
        "falloff": color(30, 10, 80, 60), "particle": color(200, 230, 255, 230), "ring": "SHRINK",
        "cue": "COLOR", "dot": "DIAMOND", "shape": "BANDED", "radius": 150, "reach": 350, "spread": 160,
        "amount": 160, "speed": 80,
        "looks": {"inv": ("SPIRAL", "SPIN", "SPARKLES", "GRADIENT"), "prayer": ("STAR", "SPIN", "ORBIT", "GLOW"),
                  "spell": ("RINGS", "WOBBLE", "RIPPLES", "GRADIENT")},
    })
    solar = complete({
        "accent": color(255, 200, 70, 235), "second": color(255, 110, 40, 230), "glow": color(255, 150, 40, 75),
        "dim": color(30, 10, 0, 120), "miss": color(255, 50, 30, 230), "refused": color(170, 160, 150, 210),
        "hit": color(255, 240, 140, 230), "flash": color(255, 190, 80, 100), "fill": color(255, 170, 40, 40),
        "slot": color(50, 25, 0, 70), "grid": color(255, 190, 90, 35), "dead": color(40, 15, 0, 70),
        "falloff": color(120, 40, 0, 60), "particle": color(255, 190, 90, 230), "ring": "PULSE",
        "cue": "PULSE", "dot": "PLUS", "shape": "SOFT", "radius": 170, "reach": 300, "spread": 110,
        "amount": 200, "speed": 110,
        "looks": {"inv": ("FLOWER", "SPIN", "EMBERS", "GRADIENT"), "prayer": ("HEXAGON", "NONE", "RIPPLES", "GLOW"),
                  "spell": ("DIAMOND", "WOBBLE", "EMBERS", "WHOLE")},
    })
    return [("Cosmic Void", void), ("Solar Flare", solar)]


def main():
    now = int(time.time() * 1000)
    out = {}
    for slot, (name, settings) in enumerate(themed() + involved() + showcase()):
        export = {"plugin": "click-helpers", "version": 1, "settings": settings}
        # Older first, so the browser's newest-first sort keeps this order
        out[str(slot)] = {"name": name + " (Default)", "created": now - slot * 1000, "settings": json.dumps(export)}
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "defaults.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(out, f, indent=2)
    print("Wrote", path)


if __name__ == "__main__":
    main()
