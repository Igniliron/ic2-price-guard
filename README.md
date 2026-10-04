# IC2 Price Guard

An independent addon for the Villager-O-Mat trading station in IC2 Classic. It helps prevent overpaying during automatic villager trading.

**Early experimental release.** Code, build scripts and documentation were generated with ChatGPT/Codex. The project author defined the requirements and tested the interface and trading behavior in Minecraft. Testing is limited; bugs may remain. The addon does not use an online AI service during gameplay.

## Requirements

- Minecraft Java Edition **1.19.2**
- Forge **43.x**
- IC2 Classic **1.19.2-2.1.2.1**
- In multiplayer, install the addon on **both client and server**.

Other Minecraft and IC2 versions are not supported by this build. IC2 and Minecraft code are not bundled.

## How it works

Set the maximum number of items the station may give to a villager for one trade. This can be emeralds, carrots or another resource.

**If a trade requires two types of payment items, the limit applies only to the first one.** The second payment is still required. The quantity received from the villager is not limited.

Examples with a limit of **1**:

- **1 emerald → 6 bread:** allowed.
- **2 emeralds → 6 bread:** skipped.
- **32 carrots → 1 emerald:** skipped.
- **1 emerald + 1 book → enchanted book:** allowed.

The addon does not change prices or prevent price increases. It skips trades whose current first payment exceeds your limit.

## Features

- Default payment limit: **1**.
- Set a limit from **1 to 64**, or disable the limit.
- Configure all villagers, a profession or an individual villager.
- Set exceptions for individual trades; different enchanted books are identified separately.
- Settings are saved separately for each station.
- Overpriced trades are skipped before payment items are consumed.

## Installation

1. Close Minecraft and, if applicable, the server.
2. Download `ic2-price-guard-2.0.0-mc1.19.2-fixed.jar` from [Releases](https://github.com/Igniliron/ic2-price-guard/releases).
3. Remove the previous Price Guard JAR from `mods`.
4. Put the new JAR in `mods`, alongside IC2 Classic.
5. Restart the game/server.

Do not put the source ZIP or publication kit in `mods`.

## Controls

Use the station's selector to choose all villagers, a profession or one villager.

- **Top row:** default limit for all trades in the selected group or villager.
- **Buttons beside a trade:** limit for that specific trade.
- **− / +:** decrease or increase the limit. Hold **Shift** for steps of 8.
- **Left-click the button between − and +:** switch between unlimited payment and a limit of 1.
- **Shift + left-click that button:** reset the exception and inherit the broader rule.
- **\*:** the displayed value is inherited.

These controls use the **left mouse button**, not the mouse wheel button.

Rules apply in this order, highest priority first:

1. Individual villager's trade rule.
2. Individual villager's default.
3. Profession's trade rule.
4. Profession's default.
5. Trade rule for all villagers.
6. Station default.

Changing a rule for all villagers does not remove profession or individual exceptions. Reset those exceptions if you want everyone to follow the general rule.

## Limitations and testing

- The interface and inherited rules may take some getting used to.
- Interface refresh performs repeated work; performance has not been benchmarked.
- Only the first payment is checked.
- Settings are not guaranteed to survive breaking and replacing a station.
- Isolated automated tests were run against the target IC2 transaction code. Full Minecraft/Forge and real multiplayer testing were not performed in the development environment.
- The project author tested the addon in Minecraft and reports that it works in their setup. Compatibility with other modpacks is not guaranteed.
- The JAR includes `pack.mcmeta` to fix the missing resource metadata warning.

Back up your world before testing.

Report bugs or suggest improvements in [Issues](https://github.com/Igniliron/ic2-price-guard/issues). For bugs, include your Minecraft, Forge and IC2 versions, steps to reproduce the problem and relevant logs. Remove personal information from logs before posting.

## Source and build

Sources are available in this repository and in the JAR's `development/` directory.

Requires **JDK 17** and **Python 3**. Build with:

```text
python build.py
```

Test instructions and automated test coverage are described in `VALIDATION.txt`.

**License:** MIT — see `LICENSE`.

---

## Русский

**IC2 Price Guard** — независимый аддон для торговой станции IC2 Classic. Он помогает не переплачивать при автоматической торговле с жителями.

**Ранняя экспериментальная версия.** Код, скрипты сборки и документация созданы с помощью ChatGPT/Codex. Автор проекта определил требования и проверил интерфейс и торговлю в Minecraft. Проверка ограничена, возможны ошибки. Во время игры аддон не использует онлайн-сервисы ИИ.

### Требования и установка

Нужны **Minecraft 1.19.2**, **Forge 43.x** и **IC2 Classic 2.1.2.1**. В сетевой игре одинаковый JAR требуется на клиенте и сервере.

Закройте игру, удалите старый Price Guard из `mods` и положите туда новый JAR из [Releases](https://github.com/Igniliron/ic2-price-guard/releases). Архивы исходников устанавливать не нужно.

### Что ограничивает лимит

Лимит задаёт максимальное количество предметов, которые станция отдаёт жителю за одну сделку: изумрудов, моркови или другого ресурса.

**Если нужны два вида предметов, ограничение применяется только к первому.** Второй платёж тоже требуется. Количество получаемого товара не ограничивается.

При лимите **1**:

- **1 изумруд → 6 хлеба:** сделка разрешена.
- **2 изумруда → 6 хлеба:** сделка пропускается.
- **32 моркови → 1 изумруд:** сделка пропускается.
- **1 изумруд + 1 книга → зачарованная книга:** сделка разрешена.

Аддон не меняет цены. Если цена превышает лимит, станция пропускает сделку.

### Настройки и кнопки

По умолчанию лимит равен **1**. Можно выбрать значение от **1 до 64** или безлимит.

Настройки доступны для всех жителей, отдельной профессии, конкретного жителя и сделки. Они сохраняются отдельно для каждой станции.

- **Верхняя строка:** общий лимит сделок выбранной группы или жителя.
- **Кнопки возле сделки:** лимит этой сделки.
- **− / +:** изменить лимит; с **Shift** — на 8.
- **Левая кнопка мыши по кнопке между − и +:** переключить безлимит / лимит 1.
- **Shift + левая кнопка по этой кнопке:** сбросить исключение.
- **\*:** значение унаследовано от более общего правила.

Приоритет: **конкретный житель → профессия → все жители**. На каждом уровне правило отдельной сделки важнее общего лимита этого уровня.

Изменение правила для всех жителей не удаляет исключения профессий и отдельных жителей. Чтобы они следовали общему правилу, сбросьте их исключения.

### Ошибки и предложения

Перед проверкой сделайте резервную копию мира. Работа на других сборках и в сетевой игре не гарантируется.

Идеи и ошибки можно оставлять в [Issues](https://github.com/Igniliron/ic2-price-guard/issues). Укажите версии игры и модов, опишите проблему и приложите нужные логи, предварительно удалив личные данные.

Дополнительная инструкция: `README_RU.txt`.

**Лицензия:** MIT, см. `LICENSE`.
