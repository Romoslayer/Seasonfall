# Seasonfall

A server-side seasons mod for Minecraft **26.2 and 26.3**, on **Fabric and NeoForge**. Players join with a completely
unmodified game: no client mod, resource pack or client networking is needed. Installing Seasonfall on a client is
optional and only makes colour changes show up live instead of on rejoin.

The year is one continuous cycle (0 = start of spring, 0.25 summer, 0.5 autumn, 0.75 winter). Nothing flips over when a
season starts; temperatures, colours, crop growth and day length all move smoothly through the year, and every biome
responds according to its own climate.

## What it does

| | |
|---|---|
| **Seasonal temperature** | Each biome follows a yearly temperature curve scaled to its climate. Temperate plains swing strongly, jungles barely change, deserts get cooler but stay dry, snowy biomes stay below freezing all year. |
| **Snow and rain** | Where the season makes a biome cold enough, rain falls as snow and settles, using the game's own weather. Nothing is placed when winter starts. |
| **Freezing** | Still water freezes the same way it does in snowy biomes: gradually, from the edges in. |
| **Thaw** | Once a biome warms past the thaw temperature, snow open to the sky melts a layer at a time and exposed ice turns back into water, on the same random ticks the game uses to melt them next to a torch. Roofed/indoor snow and ice are left alone. |
| **Foliage and grass** | Seasonal tints per biome style: light yellow-green spring leaves, deep summer green, autumn yellow-orange-brown leaves over olive-tawny grass, grey winter leaves and grass. Evergreen, tropical and arid biomes change much less. |
| **Crops and plants** | Growth speed multipliers by crop group (default, warm-season, cool-season, general vegetation), scaled by how seasonal the local biome is. Crops under glass grow at least at normal speed all year. |
| **Day length** | Longer days in summer, longer nights in winter. A full day still takes 24000 ticks. |
| **Empty server** | The year waits while nobody is online and carries on when someone joins; the server itself keeps running. On by default (`general.pauseWhenEmpty`). |
| **Vanilla weather** | Without Stormcell, rain and thunderstorms become more or less frequent through the year. |
| **Stormcell API** | Seasonal climate modifiers (temperature offset, humidity, precipitation, storm probability) for the Stormcell weather mod to use. Seasonfall never simulates weather itself. |

There are no new items, blocks, screens or player-facing messages; players notice the seasons by the world changing.

## What players see

- **Without Seasonfall on their game:** biome colours and the rain-or-snow look are sent while joining, so they see the
  current season each time they connect. Snow and ice still form and melt live, because those are real blocks. Until
  they rejoin, their game may draw rain where the server is already settling snow (or the other way round).
- **With Seasonfall on their game:** the server also sends live updates, so colours and snowfall change while playing.
  Each step redraws the world around them once (96 steps a year by default). Switching Seasonfall (or its visuals) off,
  or taking a dimension or biome out of the seasons, sends them the normal look straight away. Switching off only
  `visuals.liveUpdatesForModdedClients` stops further updates; they keep what they have until they rejoin.

What changes colour:

- **Grass colour:** grass blocks (top and sides), short and tall grass, ferns, bushes, sugar cane, and the stems of pink
  petals and wildflowers. Grass goes bright in spring, deep green in summer, olive to tawny in autumn and a dormant
  grey in winter. Swamp grass takes its two colours from the game itself, so it only follows the seasons for
  players with Seasonfall installed.
- **Leaf colour:** oak, dark oak, jungle, acacia and mangrove leaves, and vines.
- **Birch, spruce, cherry, azalea, flowering azalea and pale oak leaves:** only for players with Seasonfall installed. An
  unmodified game gives birch and spruce one fixed colour and takes the others' colour straight from their textures,
  which the server cannot change. With the mod, birch follows the biome's palette, spruce only shifts slightly (it keeps
  its needles), and azalea and pale oak keep their own colours in spring and summer, then take on the biome's autumn and
  winter colours. Cherry and flowering azalea have their flowers in the same texture, so they only warm towards gold in
  autumn (cherry goes peach, the azalea flowers stay pink) and fade a little in winter.
- **Unchanged:** leaf litter, lily pads and stems.

Biome colours are per biome, not per dimension: if a dimension without seasons uses a biome that also appears in one
with seasons (plains, say), players see that biome's seasonal colours there too. Snow, ice and crops in that dimension
are not affected.

Modded biomes get seasons like vanilla ones, as long as a dimension with seasons generates them (biome mods add theirs to
the overworld). A biome that no seasonal dimension generates, for example one only placed with `/fillbiome`, keeps its
normal look. Large biome lists are fine: tested with about 1,470 biomes on Fabric and NeoForge, with and without the mod
on the player's side.

## Installing

Put the jar for your loader and game version in the server's `mods` folder (Fabric also needs Fabric API). Optionally
put the same jar in players' `mods` folders for live updates.

| Loader | 26.3 | 26.2 |
|---|---|---|
| Fabric | `seasonfall-fabric-1.0.0+26.3.jar` | `seasonfall-fabric-1.0.0+26.2.jar` |
| NeoForge | `seasonfall-neoforge-1.0.0+26.3.jar` | `seasonfall-neoforge-1.0.0+26.2.jar` |

The year is saved in `seasonfall.json` in the world folder and survives restarts.

## Configuration

`config/seasonfall.json` is written on first start with a comment above every option. `/seasonfall reload` applies
changes. Main sections:

- `general`: on/off, whether the year advances, whether it waits while nobody is online (`pauseWhenEmpty`, on by default), starting season
- `seasonLength`: days per season (24 each by default)
- `dimensions`: which dimensions have seasons (overworld only by default)
- `temperature`: the yearly temperature curve
- `visuals`: colours, colour steps, live updates for clients with the mod
- `crops`: growth multipliers per group (0 to 4), per-crop overrides (vanilla or modded block ids), greenhouses
- `freezing`, `snow`: seasonal freezing and snow, and the thaw temperature
- `dayLength`: summer and winter daytime length
- `weather`, `stormcell`: seasonal weather tendencies and what is shared with Stormcell
- `performance`: update intervals
- `biomeOverrides`: per-biome changes by id, for vanilla or modded biomes
- `palettes`: the seasonal tints for each biome style

Values that are not usable (not a number, out of range) are replaced and a warning naming the option is logged. A season
left out of a crop override grows at normal speed; one left out of a built-in group keeps that group's default.

### How biomes are classified

Every biome gets a style worked out from its own temperature, downfall, precipitation and tags (including the shared
`c:` tags most modded biomes use), so modded biomes get sensible seasons automatically:

`frozen`, `ocean`, `tropical`, `savanna`, `arid`, `evergreen`, `wetland`, `deciduous`, `temperate`

Each style sets how strongly temperature, foliage, grass, crops and weather change, and whether seasonal snow is
allowed. `biomeOverrides` can change any of it per biome, for example:

```json
"biomeOverrides": {
  "minecraft:jungle": { "seasonStrength": 0.25, "foliageChangeStrength": 0.15, "winterCropMultiplier": 0.85 },
  "minecraft:plains": { "allowSeasonalSnow": true }
}
```

### Tags

- Blocks: `seasonfall:crops/warm_season`, `seasonfall:crops/cool_season`, `seasonfall:crops/default`,
  `seasonfall:vegetation`, `seasonfall:greenhouse_glass`

Change them with a data pack like any other tag. The whole random tick of a block in a crop tag is sped up or slowed down, not only
its growth, so only add plants whose random tick is about growing. Growth up to 4 times normal is supported: above 1, a
plant gets extra random ticks (re-checked each time, so a fully grown or replaced plant is left alone).

## Commands

All need operator permission (level 2).

| Command | |
|---|---|
| `/seasonfall season` | Date, year progress, day length, and the climate where you stand |
| `/seasonfall setseason <spring\|summer\|autumn\|winter> [percent]` | Jump to a point in a season |
| `/seasonfall setday <day>` | Jump to a day of the year |
| `/seasonfall pause` / `resume` | Stop or restart the year |
| `/seasonfall reload` | Re-read the config |

## For Stormcell (and other mods)

`dev.romoslayer.seasonfall.api.SeasonfallApi`, safe to call from the server thread:

```java
ClimateModifiers climate = SeasonfallApi.climate(level, pos);
climate.temperatureOffset();          // added to the biome's normal temperature
climate.humidityMultiplier();
climate.precipitationMultiplier();
climate.stormProbabilityMultiplier();

SeasonfallApi.yearProgress();         // 0..1
SeasonfallApi.season();               // SPRING, SUMMER, AUTUMN, WINTER
SeasonfallApi.seasonProgress();       // 0..1 through the current season
```

When no world is running, Seasonfall is off, or the place has no seasons, it reports `ClimateModifiers.NEUTRAL`. While
Stormcell is installed, Seasonfall stops adjusting vanilla weather.

Plant growth, for mods that grow plants themselves:

```java
SeasonfallApi.cropGrowthMultiplier(level, pos, state);                       // right now
SeasonfallApi.averageCropGrowthMultiplier(level, pos, state, elapsedTicks);  // averaged over the past elapsedTicks
```

Both use the same rules as live growth: the plant's crop group, the biome's profile and greenhouses, and both report the
speed actually applied (0 to 4). The average follows what the year really did during the elapsed game ticks: Seasonfall
keeps a short history of the year against game time (in `seasonfall.json`), so pauses, `/seasonfall setseason` and
`setday` are accounted for. It uses the current season lengths and settings throughout. Time before that history was
recorded is assumed to have passed at the normal pace.

### Elapsed

[Elapsed](../Elapsed) (offline progression) finds Seasonfall on its own, with no extra setup on either side. A crop left
unloaded grows by the seasons the year went through while it was unloaded, pauses included (also the time the year
waited with nobody online): a field left alone through
winter comes back with about a third of the growth of one left alone through summer. If Elapsed is set to count real
time while the server was off, that time is treated as game time before the absence (the year does not move while the
server is off), which is an approximation. Elapsed's `[seasonfall] integrationEnabled` option turns the link off.

## Things to know

- The year only moves while someone is online (`general.pauseWhenEmpty`, on by default). Most servers stop ticking
  anyway once empty (`pause-when-empty-seconds` in `server.properties`); this also holds the year on servers that keep
  running. Turn it off to let the seasons pass on an empty server that keeps running.

- Seasonal day length takes over the overworld clock's speed (`/time rate`). Turn off `dayLength.seasonalDayLength` if
  you use `/time rate` yourself.
- Exposed snow layers and ice in seasonal biomes melt when it is warm, including player-placed ones. Use packed or blue
  ice, or put a roof over them.
- New chunks generate with each biome's normal climate; seasonal snow and ice only come from weather afterwards.
- The seasons only apply on the server thread. Turning off `freezing.seasonalFreezing` or `snow.seasonalSnowPersistence`
  makes water freeze, or snow settle, as in vanilla; with seasonal snow off, players are also sent each biome's normal
  temperature, so they see the rain that actually falls.
- If `seasonfall.json` cannot be written (a read-only world folder, say), one error is logged and saving is retried until
  it works. A file that cannot be read is renamed to `seasonfall.json.unreadable-<time>` rather than overwritten.

## Building

Java 25. One source tree builds every version:

```bash
gradlew build
```

```bash
gradlew build -Pmc=26.2
```

Jars end up in `Fabric/build/libs` and `NeoForge/build/libs`.
`build` also runs the unit tests (calendar, season history, growth maths, config checks, saving).

Testing helpers (add `-Pmc=26.2` for 26.2):

- `gradlew :Fabric:runServer` or `:NeoForge:runServer`: a test server in `runs/server-<version>`; add `-Prun=<name>` for a fresh
  folder `runs/<name>-<version>` so existing test worlds are left alone
- `gradlew runVanillaClient`: the official, unmodified game from your launcher, joining `localhost:25565`
- `gradlew :Fabric:runModdedClient` or `:NeoForge:runModdedClient`: a client with Seasonfall, joining `localhost:25565`

Seasonfall contains no code from Serene Seasons or any other seasons mod.

## Licence

MIT. See `LICENSE`.
