PUNCH SNOWBALL + INFINITE TNT v1.2 | Paper 1.21.11

Source project: compile on GitHub Actions. NOT a prebuilt .jar.

FAST UPDATE TO YOUR EXISTING GITHUB REPOSITORY:
1. Open Code -> src/main/java/com/example/punchsnowball/PunchSnowballPlugin.java
2. Click pencil/Edit, replace ALL code with the updated PunchSnowballPlugin.java, Commit changes.
3. Edit src/main/resources/plugin.yml by replacing its content with plugin.yml from this ZIP.
4. Add a NEW file src/main/resources/config.yml with the provided config.yml content.
5. Edit build.gradle.kts to update version to 1.2.0 (optional, affects jar filename).
6. Actions -> Build plugin -> Run workflow; download the green run's artifact.
7. Stop your Paper server, replace the old .jar with the new one, restart.

COMMANDS (OP):
/givesnowball [player]    custom infinite snowball
/givetnt [player]         custom infinite enchanted-glint TNT

FEATURES:
- Snowball: unlimited ammo, 7-second cooldown, stronger velocity-based knockback,
  Slowness II for 2 seconds on players hit.
- TNT: right-click a block; creates primed TNT without placing a block or consuming
  the item. 3-second custom explosion, 2.75 block radius, 4 health points base
  magic damage (~2 hearts, armor bypass, subject to enchantments/effects).
- TNT explosion uses visuals and sound instead of vanilla terrain damage. No
  destruction, fire, chain reactions, or vanilla explosion knockback.
- Item glows using enchantment glint override, not an actual enchantment.
- Both items are identified by their hidden PersistentDataContainer tags.
- Settings in plugins/PunchSnowball/config.yml; restart after editing.
- The snowball material cooldown affects normal snowballs for that player too.
- Knockback distance is approximate; test in-game and edit config if needed.

IMPORTANT:
Only use these custom items for the special behavior; ordinary TNT and
snowballs stay vanilla. This project was not compiled or tested in-game here.
