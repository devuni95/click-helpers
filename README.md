# Click Helpers

RuneLite plugin that shows exactly where you can click in the inventory, the
bank-side inventory, the prayer tab and the spellbook, and gives feedback when
you click.

- Highlights the exact clickable area under the mouse, with an optional grid of
  every clickable area and shading over dead gaps
- Click flash, a queued click marker until the next game tick, a red ring when a
  click does nothing, and a yellow ring when the game refuses a click
- Gold highlights for active prayers (from the server-confirmed state, so they do
  not flicker) and for a spell selected for manual casting
- Optional mid-tick cue on prayers to help with prayer flicking
- Miss markers, a floating click chart, and settings you can share
- Per-tab settings for the inventory, prayer tab and spellbook, with an option to
  keep all three in sync
- Spotlight at the mouse in many shapes and patterns, with animated effects such
  as sparkles, embers, snow and ripples that can play on every click
- Gear switch practice with progress tracking, named settings loadouts, and
  community loadouts that players publish and share
- Optional hiding of items you never click in a fight, and eat and drink delay
  shading and outlines
- Every setting in a searchable side panel

Fixed and resizable modes are both supported because bounds come straight from
the game's widgets. The plugin only draws overlays and never sends input. Item
hiding only changes how icons are drawn; hidden items still work when clicked.

## Settings

All settings are in the **Settings** tab of the Click Helpers side panel, in
collapsible sections with a search box that filters them as you type, and
Expand all / Collapse all buttons. Right-click a setting to reset it. RuneLite's
own config panel only holds import/export, the side panel toggle and the
community loadouts switch. Hover, spotlight and miss dot settings are set per
tab; see
[Inventory, prayer and spellbook tabs](#inventory-prayer-and-spellbook-tabs).

### Areas

- **Only slots with items**: skip empty inventory slots
- **Bank inventory / Prayers / Spells**: also highlight these panels
- **Clickable grid**: outlines every area the hover highlight can fill, either in
  the panel under the mouse or in every visible panel
- **Grid on empty slots**: also outline inventory slots with no item
- **Round grid and dead areas to highlight radius**: rounds the grid and dead area
  shading with each tab's corner radius (on by default). The game's clickable
  areas are square, so slot corners can look dead even though clicks there still work

### Click feedback

- **Click flash**: the clicked slot flashes and fades out (color and duration configurable)
- **Queued click marker**: a dot on the clicked slot until the next game tick.
  It turns red when the same slot is clicked twice in one tick, and yellow when
  you eat or drink during the delay, since the game ignores that click

### Missed clicks

- **Dead click marker**: a red ring where you click an empty slot or a gap with no interaction
- **Rejected click marker**: a yellow ring where a click reached the game but was
  refused, like equipping an item with no inventory space for the swap. Detected
  from the game's refusal message within two ticks of the click
- **Ring duration / animation**: how long both markers last, and whether they
  grow, shrink, pulse or show as an X
- **Miss outline**: outlines the slot nearest a dead click, the one you most likely
  meant, in place of the normal outline until it fades. **Miss outline width**
  sets its line width (default 2); 0 makes it one thicker than the hover outline
- Miss dots are set per tab

### Dead areas

- **Dead areas**: faint dark gray shading over the gaps where a click does nothing, in
  every visible panel (default) or only in the panel under the mouse
- Each panel (inventory, bank inventory, prayers, spells) has its own on/off and color.
  The bank inventory shades its whole frame around the slots
- **Cover whole prayer and spell tab**: also shades the rest of the tab, like the
  space a filter leaves empty, and counts clicks there as misses

### Prayers and spells

- **Active prayer highlight**: replaces the game's active prayer glow with a gold
  highlight that follows the server-confirmed prayer state, so it does not
  flicker when clicking fast
- **Pending prayer state**: a dashed outline from your click until the server
  confirms the change (half fill while turning off)
- **Selected spell highlight**: a spell selected for manual casting gets the same
  highlight as an active prayer
- **Autocast marker**: outlines the spell set to autocast; hovering it shows the
  hover highlight instead

### Tick cue

- **Mid-tick cue**: shows progress through the current click window on the hovered
  prayer. A click reaches the server about one round trip after the client sees a
  tick, so the window closes one round trip *before* each tick the client sees.
  - Pulse: brightens at the cue position
  - Drain: the outline refills at the cue position, then shrinks
  - Color: the outline turns green at the cue position, then goes to red
- **Pulse color**: see-through light purple by default
- **Mid-tick cue latency**: your round-trip ping to the world (shown in the World Hopper)
- **Cue position**: where in the window every cue mode kicks off, 40% by default.
  Slightly before the middle leaves room for reaction time, while clicks that
  anticipate the beat a little early still land in the same tick
- **Cue on listed prayers / Cue prayers**: also show the cue on the listed prayers
  when they are not hovered. Protection prayers by default; `*` means every prayer

### Click chart

- **Click chart**: a floating box (Alt-drag to move it) with your clean click rate
  and stacked bars of good clicks (green), double clicks (orange), refused clicks
  (yellow) and missed clicks (red), with a legend of the count of each
- **Click chart hotkey**: shows or hides the chart
- **Time unit**: seconds or game ticks
- **Bar interval / bars**: how many seconds or ticks each bar covers, and how many
  bars to show; the chart covers bars times the interval (5 s x 24 bars by default)
- **Moving average**: a line of the average clicks per bar over the last few bars
- **Width / bar height**: size of the chart, for small screens. Text shortens to
  fit narrow charts
- **Legend / time labels**: hide either to make the chart smaller

### Hide items

- **Hide non-fight items**: makes the icon of items with no fight action
  transparent, such as items with only Use, Open or Bury, and noted items.
  Clicks on them still work
- **Hide toggle hotkey**: turns the hiding on and off; when unset, hiding stays on
- **Fight actions**: inventory actions that keep an item visible (Eat, Drink,
  Wield, Wear, Equip and more by default)
- **Always show**: item names that are never hidden; `*` is a wildcard
- **Show hidden item outline / backfill**: a faint icon outline and slot shading
  where a hidden item is, each with its own toggle and color. Turn both off to make
  hidden slots look empty

### Eat and drink delay

- Shades food or potions while eating or drinking another one would be ignored.
  Items share a delay by their action: all food shares one, all potions share
  one, and karambwans have their own. Fill, drain or dim style
- Follows the combo order: food, then a potion, then a karambwan can go in one
  tick. A potion blocks food for the rest of its tick; a karambwan blocks food
  for 3 ticks and potions for its tick
- Fast foods use their own delay: pizzas and pies 1 tick (2 for the second half of
  pizzas and apple, meat and redberry pies), cakes and giant crab meat 2 ticks
- **Delay outline**: outlines those items too, with or without the shading. Solid,
  fading out as the delay runs down, or tracing around the slot like a timer;
  its own color and line width

### Switch practice

- Lights up random empty inventory slots to click as fast as you can, like a gear
  switch. Clicks off the targets count as misses and show the usual miss markers
- Each round records reaction time, switch time (first to last click), the ticks
  the clicks spread over, and misses. Stats show in a box you can move with
  Alt+drag: the last round, the session (average and best time, 1-tick rate,
  accuracy, timeouts), or both
- Start and stop from the side panel or the **Practice hotkey**. Clicking an item
  still uses it, so practice with free slots
- Settings are on the side panel's Practice tab: slots per switch (1-28), pause
  between rounds with an optional random extra pause, a round time limit in
  seconds with a timer bar, rounds per session (the session stops and saves when
  done), click in numbered order, and target and hit colors
- The Practice tab also shows a chart of switch time and accuracy across your
  last 50 sessions

### Import/export settings

- **Export** and **Import** on the side panel's Loadouts tab copy every setting to
  the clipboard as readable JSON grouped by section, and apply shared JSON from it
- RuneLite's own config panel holds just two switches: **Side panel**, which turns
  the side panel on or off (it holds every other setting), and **Community
  loadouts**, off by default, which turns on the community features below

### Community loadouts

The Community part of the side panel's Loadouts tab publishes your saved
loadouts for others, and **Browse** opens a window of what other players
published. You never log in: the plugin makes up an author name for you, like
"Swift Goblin 42", and each install can publish up to 10 loadouts. Publishing
again with the same name replaces that loadout. You must be logged in to the
game to publish, and names with bad words are refused and cleared.

- **Publish as**: fills in with the picked loadout's name; change it to publish
  under another name
- **Anonymous**: on by default, showing the made-up author name. Untick it to show
  your game name as the author of what you publish next
- **Sections to publish**: publish only some settings sections, so others can take
  just your click feedback or your spotlight
- **Browser**: a table sortable by name, author, date, sections or downloads, a
  search box, a section filter (ticked sections, with **Only these** for exact
  matches), your own loadouts only, and a preview of every setting a loadout changes
- **Downloads**: how many other players loaded a loadout, counted once per install.
  Your own loads are not counted. Publishing again under the same name keeps the
  count; a new loadout starts at 0
- **Small x**: shows on your own loadouts in the browser and deletes that one
- **Defaults**: ten starter loadouts marked **(Default)** by the author
  **Default** (in green), always listed first: Clean Outline, PvP Switcher,
  Prayer Flicker, High Contrast, Spotlight Sparkle, Ember Forge, Frost Bite,
  Arcane Radar, Neon Disco and Tick Perfect PvM. No player can publish
  as Default or use the word in a loadout name
- **Load** asks which of its sections to take, then adds it as a new loadout or
  applies it over your current settings. Tick **Clear other settings** to put
  everything it does not set back to its default

Loading a community loadout only changes Click Helpers settings, and never
turns network features on.

**What leaves your client.** Nothing until Community loadouts is on and you
press a button. Then:

- **Google Firebase** (firebaseio.com, googleapis.com): an anonymous sign-in,
  and the loadouts you publish or delete (name, time and settings). Listing
  loadouts downloads the published ones and their download counts. Loading
  someone else's loadout records your anonymous id under it, once, to count
  the download.
- **jsDelivr** (cdn.jsdelivr.net): downloads a public list of blocked words,
  once per session, to check loadout names. Nothing about you is sent.

Both see your IP address, as with any web request. Your account is never sent.
Your game name is sent only when you untick **Anonymous** and publish. Setup and admin notes are in [`firebase/README.md`](firebase/README.md).

### Side panel

- **Settings tab**: every setting, in collapsible sections, with a search box that
  filters as you type and Expand all / Collapse all buttons. Values update the
  moment they change, tab sync included. Right-click a setting to reset it
- **Loadouts tab**: Export and Import buttons; named loadouts that save every
  current setting and load or delete them from a dropdown, with **Clear other
  settings** to put anything a loadout does not set back to its default; the
  Community section;
  and **Randomize all settings**, which randomizes every setting except the click
  chart, hotkeys, text lists, tab sync and practice, each tab on its own.
  Loadouts are kept in RuneLite's config, not in files
- **Practice tab**: switch practice start button, session stats, progress chart
  and practice settings
- **Off with empty inventory** (Areas): draw nothing while your inventory is
  empty, as if the plugin were off

### Inventory, prayer and spellbook tabs

The inventory (and bank inventory), prayer tab and spellbook each have three
sections of their own:

- **Hover**: style (outline, fill or both), outline and fill colors, outline
  width, corner radius, hover fade-out, hover delay, hide when a right-click menu
  is open, disco mode with an optional color change interval, keep icon colors
  on hover, and a slot background color with icons kept on top
- **Spotlight**: a glow at the mouse that dims the panel outward. Radius (up to
  2000 px), dim and glow colors, glow size (0 for dimming only), spotlight the
  panel even when not hovered, only dim dead areas, and falloff: past the
  falloff start the gradient runs in reverse, fading the dim out and a falloff
  color in by the falloff end. **Falloff color end** fades that color back out
  the same way it came in, or into a **Fade end color**
  - **Gradient**: smooth, soft, sharp or banded fades
  - **Gradient type**: circle, ellipse, square, diamond, star, hexagon, cross,
    flower, vertical or horizontal band, or the patterns rings, spiral and radar.
    **Gradient motion** spins or wobbles the shape. All of them carry on through
    the falloff
  - **Effect**: pulse, ripples, sparkles, embers, orbit or snow. **Effect on**
    keeps it on top (overlay) or to the glow, the gradient or the whole spotlight,
    and **Replace with effect** draws that part only as the effect. Particles take
    the colors drawn under them unless you set a **Custom effect color**
  - **Effect on click** plays it as a burst on each click, with an **Effect length**,
    optional **Click result colors** for good, bad and refused clicks, and **Detach
    from mouse** to let each burst play out where it started. **Detached reach**
    sets how far a detached burst reaches (up to 1000%), and **Detached spread**
    sets how far apart its particles are (up to 1000%). **Effect speed** and
    **Effect size** scale it, and **Effect amount** sets how many particles it
    draws
  - **Click effects anywhere** (inventory only): clicks outside the panels, such as
    on the game world, play the effect too, at most once per **Click anywhere delay**,
    which inventory clicks share
  - **Include inventory clicks**: with Click effects anywhere, inventory clicks play
    that effect too, free to spread past the inventory, in place of the
    inventory's own. **Only dead areas** still keeps it off the slots
- **Miss dots**: a dot at every dead click, with color, shape (circle, ring,
  square, diamond, triangle, cross, plus or heart), hollow, size and duration

**Sync tab settings** keeps all three tabs the same: changing a setting in one
tab changes it in the others, and turning it on copies the inventory settings to
the others.

## Run locally

```
./gradlew run
```

Launches RuneLite in developer mode with the plugin loaded.
