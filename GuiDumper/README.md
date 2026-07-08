# GuiDumper

A Paper plugin that exports whatever custom GUI a player currently has open
into `plugins/GuiDumper/dumps.json`, so the server's own menus can be backed
up, version-controlled, or rebuilt later.

Built for Paper `1.21.11-R0.1-SNAPSHOT` (matches this server's exact
version). Should also work unmodified on other 1.21.x Paper builds.

## Build

Requires JDK 21.

```
./gradlew build
```

The output jar is `build/libs/GuiDumper-1.0.0.jar`. Drop it into the
server's `plugins/` folder and restart (or `/reload`, if that's allowed).

> This project was scaffolded without a local JDK 21 available, so it
> hasn't been build-verified end to end. If `./gradlew build` reports a
> compile error, paste it back and it can be fixed directly.

## Use

In-game, with op or the `guidumper.dump` permission:

```
/guidump            captures whatever GUI you currently have open
/guidump <player>   captures whatever GUI that online player currently has open
```

Each run appends one entry to `plugins/GuiDumper/dumps.json`: the GUI's
title, inventory type/size, and every non-empty slot (material, amount,
display name, lore, custom model data, unbreakable flag, enchantments).
Running it with no custom GUI open (e.g. just your own inventory) reports
an error instead of writing anything.

## Notes / things to adjust for your use case

- `dumps.json` accumulates every capture as a growing array — nothing is
  ever overwritten.
- Only non-empty slots are recorded; empty slots are just omitted rather
  than written out as `null`, on the assumption you only care about what's
  actually in the menu.
- Click behavior (what happens when a slot is clicked) lives in your
  plugin's Java code and isn't captured here — this only records what's
  visually/structurally in the GUI, not the logic behind it.
