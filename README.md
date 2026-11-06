# EaglerCraftX Server

## Credits
Original Project: Lax1Dude
<br>
1.12 Project: PeytonPlayz595
<br>
Original Server Fork: EcoliEater87
<br> 
Adapted to Canary Craft (ADSCRAFT): QuizzityMC
<br>

## To make an eaglercraft 1.12 server, follow the instructions below:
Here is how you can setup a connection:
<br>
<br>
First, go to the top of the repo and click on code > codespaces > create codespace
<br>
now you have your own free server instance to host eaglercraft. Next you need to run the setup commands:
<br>
<br>
Create a terminal tab and paste the following:<br>
<br>
enter the following: `startserver`
<br>
This starts both the game server and the EaglerCraft gateway. Press `Ctrl+C`
to stop them; the game server will save and shut down cleanly.
<br>
To start either server separately instead, use `startserver 1` for the gateway
`startserver 2` for the game server, or `startserver 4` for the Lston Packs site/pack server (port 8082, also started automatically by plain `startserver`).
<br>
Now go to the ports area and forward (and make public) port `8081`
The port's homepage serves the EaglerCraft 26.2 client (`web/EaglerCraft-26.2.html`);
the previous 1.12 client page remains at `/index-1.12.html` (it can no longer join).
The stack is Paper 26.2 (Java 25, port 25566) behind Velocity 4.3.1 with
EaglerXServer 1.1.1 (port 25565). `bin/web-gateway.js` serves `web/` on 8081 and
tunnels WebSocket connections to port 25565. Old 1.12 plugins are in
`server/legacy-plugins/` and the old Eagler gateway jars in `bungee/old-plugins/`;
they don't load on 26.2.
<br>
Load up the client! When you are in, copy the link, paste it in the add server section, replace https:// with wss:// and join!
Your eaglercraft server is setup!

PS: You can use this as a normal server too (like properly hosted) just clone this repo and run it in much the same way.

## Eaglercraft voice chat

Eaglercraft voice chat is enabled on the WebSocket listener and EaglerXServer
voice service. Players need a compatible Eaglercraft client with microphone
permission. Voice uses browser WebRTC and the configured public STUN/TURN
servers; network/firewall policies may prevent some players from connecting.
The EaglerXServer configuration warns that its voice protocol is easy to stress
and abuse, so consider the risks before enabling it on a large public server.

## Saving and loading world backups

Stop the Minecraft server before saving or restoring so its world files are
not being changed while copied.

### Save a backup

1. Stop the Minecraft server.
2. From the repository's top-level directory, create a backup. You can give
   it a name:

   ```sh
   backup before-update
   ```

   This creates `backups/before-update.tar.gz`. Or omit the name to generate
   one automatically:

   ```sh
   backup
   ```

   Automatic names use the `world-YYYY-MM-DD-HHMMSS.tar.gz` format. Backups
   contain the configured Overworld, Nether, and End. Explicit names can
   contain letters, numbers, dots, underscores, and hyphens. Existing names
   are not overwritten. The `backup`, `backups`, `delbackup`,
   `loadbackup`, and `restorebackup` commands are added to the workspace terminal PATH. Rebuild
   or reopen the dev container after changing its configuration, then open a
   new terminal. The backup command validates region data for the Overworld,
   Nether, and End in either the legacy separate-folder layout or the modern
   `dimensions/minecraft/` layout.
3. Check that the archive was created:

   ```sh
   backups
   ```

   Backup archives are listed newest to oldest by `backups`, `delbackup`, and
   `loadbackup`. `backups` hides the automatic `pre-restore-*` folders; see
   [Recover a pre-restore folder](#recover-a-pre-restore-folder).

4. To remove backup archives, run `delbackup`. Choose one or more menu codes
   (`1`-`9`, then `A`-`M`) separated by commas, then type `Y` to confirm or
   `N` to cancel. Enter `Q` at the selection prompt to quit. This command
   only deletes archive files; it does not delete `pre-restore-*` folders.
5. Start the server again as usual. Keep a copy of the archive somewhere
   outside this repository too, so it remains safe if the server files are
   lost.

### Load a backup

1. Stop the Minecraft server.
2. Put the `.zip`, `.tar`, `.tar.gz`, or `.tgz` backup in the repository's
   `backups/` folder. The archive must contain a world folder with `level.dat`
   and Overworld region data (`region/` in the legacy layout or
   `dimensions/minecraft/overworld/region/` in the modern layout). Include
   sibling `<world>_nether` and `<world>_the_end` folders in the legacy layout;
   modern dimension data is stored inside the world folder.
3. From the repository's top-level directory, run the loader and choose the
   listed backup (newest first):

   ```sh
   loadbackup
   ```

   Or provide the archive path directly:

   ```sh
   loadbackup backups/world-YYYY-MM-DD-HHMMSS.tar.gz
   ```

4. Confirm the success message. The loader moves the current world folders
   into a timestamped `backups/pre-restore-*` folder before replacing them.
   Server configuration and plugins are not replaced. These folders are
   not shown by `backups` or `loadbackup`.
5. Start the server again as usual and check that the restored world loads.

### Recover a pre-restore folder

To make a saved `pre-restore-*` folder loadable, run:

```sh
restorebackup
```

The folder list is newest first. Choose a folder from the list (or pass one, e.g.
`restorebackup backups/pre-restore-YYYYMMDD-HHMMSS-ffffff`). This creates
`backups/restored-YYYYMMDD-HHMMSS-ffffff.tar.gz` and leaves the original folder
untouched. The archive then shows up in `backups` and can be loaded with
`loadbackup`.

The loader prefers the world matching `level-name` in
`server/server.properties`. If an archive contains multiple worlds and none
matches, it stops instead of guessing. Do not extract an archive over the
live server folders manually.

## Plugins

All custom plugins (CustomAbility, CustomGun, CustomItems, CustomTNT,
Money, SpawnMobs, UsernameEffects) are ported to the Paper 26.2
API and built with `gradle -p <name>-plugin build`; copy the jar from
`build/libs/` into `server/plugins/`. WorldEdit 7.4.6 and WorldGuard 7.0.19 are
used instead of the 6.x versions. Verity is not ported.

### Basic plugins

#### WorldEdit

WorldEdit (`worldedit-bukkit-6.1.9.jar`) is an in-game building tool for
operators. Select two corners with the wooden axe (`//wand`, left/right click)
or `//pos1` and `//pos2`, then edit the selection: `//set <block>` fills it,
`//replace <from> <to>` swaps blocks, `//copy`, `//paste`, `//undo`, and
`//redo` move or revert builds, and `//sphere` / `//cyl` / `//fill` make shapes.
Settings are in `server/plugins/WorldEdit/config.yml`.

#### WorldGuard

WorldGuard (`worldguard-bukkit-6.2.2.jar`) protects areas and requires
WorldEdit. Select an area with the WorldEdit wand, then
`/region define <name>` creates a region, `/region addowner <name> <player>`
and `/region addmember <name> <player>` choose who may build there, and
`/region flag <name> <flag> <allow|deny>` controls behavior (for example
`pvp`, `tnt`, `creeper-explosion`, `mob-spawning`, `entry`). Other commands:
`/region list`, `/region info <name>`, `/region remove <name>`. Global settings
are in `server/plugins/WorldGuard/config.yml` and per-world settings in
`server/plugins/WorldGuard/worlds/`. Note that region flags (such as `tnt` and
`other-explosion`) affect the Custom TNT plugin's explosions.

#### Spawn Mobs

Adds `/spawn <entity> <x y z> [amount] [radius]` (`/spawnmob` alias, requires
`spawnmobs.use`, default op) to spawn many entities in an area, for example
`/spawn pig -197 64 229 10` or `/spawn wither_skeleton ~ ~ ~ 10 5`. Entities
are spread randomly within the radius (default 3 when amount is more than 1,
max 50). Coordinates accept `~` relative values, amount is 1-100, and entity
names use Bukkit names such as `pig`, `wither_skeleton`, or `zombie`. It works
from players and command blocks, and `/spawn <entity>` spawns one at your feet.

Build with `gradle -p spawn-plugin build`, copy
`spawn-plugin/build/libs/SpawnMobs-1.0.0.jar` to `server/plugins/`, then
restart the server.

### Custom plugins

Custom item- and money-giving commands accept player names or native Minecraft entity
selectors: `@a`, `@e`, `@n`, `@p`, `@r`, `@s`, and selector filters such as
`@e[type=player]` or `@a[distance=..10]`. Only players selected by a selector
receive items. Tab completion suggests common selectors; full selector filters
can be entered directly. This applies to `/customtnt`, `/customability`,
`/customsword`, `/pass`, `/passbutton`, `/cgun`, `/verity <targets>`,
and `/pay`. The Verity plugin is kept in `server/legacy-plugins/`, so it
isn't loaded by the current Paper server; on a modern server it uses native
selectors, while its 1.12 fallback supports the common selector forms. Money
can be sent between players with `/pay <player|selector> <amount>`. Players
pay from their balance; admins and the server console grant money. Use
`/balance [player]` to check a balance and `/bankrupt <player>` to clear one.
`/shop [1-5]` opens the individual-item shop (including supplies such as
Ender Pearls, planks, and Firework Rockets), `/cshop [1-5]` opens custom items,
and `/kit [1-5]` opens kits. Staff can edit a custom shop with `/cshop <n>`.
`/sell [amount]` sells items from the main hand (one by default). Shop items
sell for more by rarity: common `$5`, uncommon `$10`, rare `$50`, epic `$100`,
legendary `$500`, and mythic `$1,000` per item. Item listings and individual
sales are capped at `$1,000,000`. Run `mreload` in the server console to reload
the money configuration.

#### Custom TNT

Adds nuke, mining, fire, slime, x-ray, sun, black-hole, rdm, mob, boss, poop, and
firework TNT. Give variants with `/customtnt <player|selector> <type>
[amount]` (`/ctnt` alias); custom variants are not separate items in the
creative menu. Place most variants normally; right-click a block with mining,
fire, slime, x-ray, sun, or black-hole TNT to prime it. Items are consumed
except in Creative.

Firework TNT (`firework`, a firework star icon) launches about 30 random
fireworks in all directions when it explodes; the count is
`explosions.firework.size` (max 60).

Boss TNT summons the TNT Overlord, a giant-sized (6x scale) 250-health zombie with a TNT-block helmet,
red TNT-core chestplate, and green zombie body, plus a live boss bar, heavy
melee knockback, ranged wither-skull attacks, and a close-range ground slam.
Its melee swings, blast charge, and roaring shockwave each have a visible
windup, particles, and sound cues. Its boss bar is a winged red/black/gray TNT
frame with a TNT block in the middle; it comes from the resource pack (restyled
`white` boss bar sprites plus the `lston:tnt_boss` font glyph, so other bosses are
unchanged), and the attack state and HP show in the action bar. Players need the
pack loaded to see the custom frame.
The custom look is boss-only and does not change ordinary zombie skins. It also
summons two Wither bosses.

The 1.8-compatible resource pack is `web/custom-tnt-pack.zip`. It retextures
reused vanilla items, which may also change their normal appearance. For browser
clients hosted on another origin, run
`python3 custom-tnt-resource-pack/serve_pack.py` and publicly forward port 8082. Opening that port in a browser shows the animated "Lston Packs" site
(`custom-tnt-resource-pack/site/index.html`, item gallery built from the pack); the pack itself is still
at `/custom-tnt-pack.zip`, so the in-game download is unchanged.
Whenever the pack changes, keep the site in sync: the gallery, stats and hash come live from the pack,
but new item categories or hero icons need `site/index.html` updated (restart `serve_pack.py` if it changed).

On 26.2 every custom item (TNT, swords, guns, abilities, redstone pass/button) has its own
item model `lston:<id>` set by its plugin with `ItemMeta#setItemModel`, so nothing restyles a vanilla
item. `custom-tnt-resource-pack/build_items.py` (called by `build_pack.py`) generates them.

Item textures replace that vanilla item's icon for all players. Give custom items their own
unused material where possible, then register each texture in
`custom-tnt-resource-pack/check_pack.py` and run it after changes.

Verity's 3D body is a voxel smiley sphere worn on her armor stand. Each facial expression
(happy, joy, sad, angry, surprised, neutral, evil) is its own model on an item players cannot
obtain: Barrier and the six silverfish-block variants. `python3
custom-tnt-resource-pack/build_verity.py` regenerates those models and textures; then run
`build_pack.py`. Keep `EXPRESSIONS` there in the same order as the item list in `VerityManager`.
Her face changes with what happens: questions, requests, compliments, insults, pats, gifts,
her owner getting hurt, dying, killing mobs, or rejoining, and her phase twists the faces
(phase 3 turns smiles flat, phase 4 turns them evil). Owners can also tell Verity "turn around",
"face me", "spin", "nod", "shake your head", "tilt your head", "look up" or "look down".
A blank face (Mob Spawner item) is shown while Verity can still be calmed.

Phase 4's Demon is a gaunt, T-posed skeleton model (`build_demon.py`, Command Block item) worn
by a marker armor stand that follows an invisible Wither Skeleton hitbox. Behaviours (toggles in
`server/plugins/Verity/config.yml`, `demon:` and `world-decay:`): a calm window where "I came
back for you" (or "I'm sorry" x3, looking at Verity within 10 blocks) ends it; otherwise it
hunts, targets villagers first, ignores water, climbs walls, smashes glass/doors/iron bars,
grabs and hurls the owner, eats kills (cobwebs), blocks sleeping within 100 blocks and kicks the
owner with "Farewell" on death. Killing it restores Verity (karma 100, phase 1). In phase 3 new
animals/villagers die and halfway through, nearby creatures vanish and time freezes.
After rebuilding the pack, update `resource-pack-sha1` and `?v=` in
`server/server.properties`.
Placing a custom TNT starts a per-player cooldown that depends on the TNT (mining 0s up to nuke 60s;
set in `cooldowns:` of the same config). Primed TNT also plays the fuse sizzle with smoke and
colored effect particles.
Explosion settings are in `server/plugins/CustomTNT/config.yml`; apply changes
with `/rldcustomtnt`.

Build with `gradle -p custom-tnt-plugin build`, copy
`custom-tnt-plugin/build/libs/CustomTNT-1.0.0.jar` to `server/plugins/`, then
restart the server.

#### Custom Items

Adds custom items. Give them with `/customsword <player|selector> <item|all>
[amount]` (`/cweapon` alias, requires `customitems.admin`, default op).

- `lightning`: Lightning Sword (gold sword, yellow and white). Hitting a living
  entity strikes lightning on it.
- `explosion`: Explosion Sword (iron sword, red and orange). Hitting a living
  entity causes a power-3 explosion (no fire, breaks blocks unless protected,
  and can hurt you if you are close).
- `pearl`: Pearl Sword (diamond sword, purple and cyan). Hitting a living entity
  teleports you to it.

Amount is 1-64. The swords are unbreakable and glow. The resource pack
retextures the vanilla gold, iron, and diamond swords, so every sword of those
types looks like the custom ones.

Build with `gradle -p custom-items-plugin build`, copy
`custom-items-plugin/build/libs/CustomItems-1.0.0.jar` to `server/plugins/`, then
restart the server.

Admins and ops can give enchanted paper redstone passes with
`/pass <player|selector> [amount]` (amount is 1-64).
Admins and ops give special enchanted stone buttons with
`/passbutton <player|selector> [amount]` (amount is 1-64).
Place a special button to make it require a pass; ordinary buttons keep their
normal behavior. Right-clicking a special button without a pass is blocked and
shows an error message. Holding a pass and right-clicking consumes one and
powers the special button for 3 seconds. Passes do nothing on ordinary buttons
(the click is blocked with a message). Special button locations are saved
across restarts and cleared when the button is broken.

#### Custom guns

Build with `gradle -p custom-gun-plugin build`, copy
`custom-gun-plugin/build/libs/CustomGun-1.0.0.jar` to `server/plugins/`, then restart.
Admins and ops use `/cgun <player|selector> <gun|all> (27 guns; run /cgun with a bad name to list them) [amount]`
(alias `/customgun`). All guns are golden hoes with a gun texture from the resource pack. Right-click to shoot; shots are instant, cost no ammo, and have a short cooldown.

#### Orbitalstrike Cannon

`server/plugins/Orbitalstrike-1.7.0.jar` is the third-party
[Orbitalstrike Cannon](https://modrinth.com/plugin/orbitalstrike-cannon) plugin
(Modrinth release `ERZ2k6VZ`, SHA-512
`b1ccb6208c64e933fd08e290d1a6dbb2a95218a66a36bad431da32cf2351f6ea4e8bca9a26aceaae68d1efbc8b65c10a86eaaf0b7812df89d8242a8746a1f3aa`).
It was selected as the most-downloaded Paper-compatible orbital strike plugin
found (over 127,000 Modrinth downloads) and explicitly supports Paper 26.2.
Restart the server to load it. Operators and signed-in admins (after `/login`)
can use `/orbital` and its
`chunkeater`, `nuke`, `stab`, `dogs`, `stasis`, `wither`, `totem`, and `give`
options. Admin permissions are applied by the Money plugin at login; its
`orbital.use` and `orbital.give` permissions default to operators.
Admins and ops are protected from damage and targeted kill, clear, and
teleport commands; incoming teleports are also blocked until they sign in.
The plugin creates `server/plugins/OrbitalStrike/config.yml` on first start;
review its settings before use, since the default nuke and stab strikes can
break blocks.
