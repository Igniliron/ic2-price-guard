# IC2 Price Guard

Early experimental release for the Villager-O-Mat in IC2 Classic.

Developed with assistance from ChatGPT/Codex and tested by the author in-game.

## Requirements

- Minecraft Java Edition **1.19.2**
- Forge **43.x**
- IC2 Classic **1.19.2-2.1.2.1**
- Install on **both client and server** when playing multiplayer.

Other Minecraft/IC2 versions are not supported by this build. IC2 and Minecraft code are not bundled.

## Features

- Default maximum payment: **1 item paid per trade**.
- Set limits from 1 to 64, or allow unlimited prices.
- Configure all villagers, a profession, or an individual villager in the existing station selector.
- Set exceptions for specific trades, with NBT-aware identification of enchanted books.
- Settings are saved separately for each station.
- Offers above the cap are skipped before items are consumed.

**The addon does not stop Minecraft demand or price increases.** It prevents automatic execution of trades whose current price exceeds the configured cap.

## Install

1. Close Minecraft/server.
2. Download `ic2-price-guard-2.0.0-mc1.19.2-fixed.jar` from Releases.
3. Remove any previous Price Guard jar from `mods`.
4. Put the downloaded jar in `mods`, alongside the required IC2 Classic version.
5. Restart the game/server.

Do not put the source ZIP or the whole publication kit in `mods`.

## Controls

Use the normal selector to choose all villagers, a profession, or one villager.

- Top row: default for all trades in the selected scope.
- Buttons next to a trade: rule for that trade in the selected scope.
- `−` / `+`: change cap; hold Shift for steps of 8.
- **Left-click the center on-screen button:** toggle unlimited / cap 1.
- **Shift + left-click the center on-screen button:** remove an exception and inherit a broader rule. This does not mean the middle mouse button.
- `*`: inherited value.

Priority, highest first:
1. Individual villager's trade.
2. Individual villager's default.
3. Profession's trade.
4. Profession's default.
5. Trade rule for all villagers.
6. Station default.

Changing an all-villagers rule preserves narrower exceptions. Reset those exceptions if you want every villager to inherit the station setting.

The cap limits the quantity you give to the villager, whether it is emeralds or another item. If a trade requires two payment types, only the first payment slot is capped.

For `carrots → emerald`, the cap applies to the number of carrots. For `emeralds + book → enchanted book`, it applies to emeralds; the second payment is still required. The output quantity is not restricted to one.

## Known limitations and testing

- Early release: graphical layout and rule inheritance can be confusing.
- GUI refresh currently performs repeated work; no performance benchmark is available.
- Limits apply to the first payment only.
- Station settings are not guaranteed to transfer when breaking and replacing a station.
- Automated isolated tests were run against the target IC2 transaction code. The development environment did not run a full Minecraft/Forge instance or real multiplayer tests.
- The human tester reports that the interface and trading behavior work in their setup; this is not a guarantee for other modpacks.
- This file fixes the missing resource metadata warning by placing `pack.mcmeta` at the jar root.

Try a backed-up world first. For bugs, include Minecraft/Forge/IC2 versions, a short reproduction and relevant logs; redact personal/server details from logs before publishing them.

## Source and build

Sources are included in this repository and also in `development/` inside the jar.
Use JDK 17 and Python 3; run `python build.py`. Test instructions and the exact automated-test scope are in `VALIDATION.txt`.

Author: Igniliron. License: MIT; see `LICENSE`.

## Русский

Ранняя экспериментальная версия дополнения к торговой станции IC2 Classic.
Разработано с помощью ChatGPT/Codex и проверено автором в игре.
Автор: Igniliron.

Требуются Minecraft 1.19.2, Forge 43.x и IC2 Classic 2.1.2.1.
Для сервера нужен одинаковый JAR на клиенте и сервере.
Положите JAR из Releases в mods, предварительно удалив старый Price Guard.

Лимит задаёт максимальное количество отдаваемых жителю предметов:
изумрудов, моркови или другого предмета. Для сделки с двумя видами оплаты
лимит действует только на первый слот; второй по-прежнему требуется.
Количество получаемого товара не ограничивается. Мод не убирает рост цен,
а пропускает слишком дорогие сделки. Профессии и отдельные жители имеют
приоритет над общими правилами; их исключения можно сбросить.

Кнопки −/+ меняют предел. ЛЕВОЙ кнопкой по центральной кнопке в интерфейсе:
безлимит / вернуть 1. Shift + ЛЕВАЯ кнопка по этой кнопке: сброс исключения.
Это не нажатие колёсика мыши. Подробности — README_RU.txt.
