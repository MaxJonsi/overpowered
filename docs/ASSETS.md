# Ассеты

Все ассеты лежат в `src/main/resources/assets/overpowered/`.

## Структура

| Папка | Что лежит | Формат |
|---|---|---|
| `geo/item/`, `geo/entity/` | 3D-модели (Blockbench, bedrock geometry) | `*.geo.json` |
| `animations/item/`, `animations/entity/` | Keyframe-анимации | `*.animation.json` |
| `textures/item/`, `textures/entity/`, `textures/block/` | Текстуры | `*.png` |
| `sounds/weapon/`, `sounds/magic/`, `sounds/mob/` | Звуки, моно ogg vorbis | `*.ogg` |
| `sounds.json` | Реестр звуковых событий | JSON |
| `lang/` | Локализация en_us / ru_ru | JSON |
| `models/item/` | Ванильные item-модели (для GeckoLib-предметов) | JSON |

## Пайплайн моделей

Модели и анимации в формате Blockbench (тип проекта Geckolib Animated Model, плагин GeckoLib Animator). Файлы редактируются либо в [Blockbench](https://www.blockbench.net) вручную, либо правкой JSON. Идентификаторы:

- геометрия: `geometry.overpowered.<имя>`
- анимации: `animation.overpowered.<имя>.<действие>` (idle / shoot / reload и т.д.)

Анимации могут содержать `sound_effects` keyframes — GeckoLib дергает их в нужный кадр, имя эффекта маппится на звуковое событие в коде.

## Звуки

Minecraft требует ogg vorbis; для позиционного 3D-звука файл обязан быть моно. Конвертация: `ffmpeg -i in.ogg -ac 1 tmp.wav && oggenc -q 4 tmp.wav -o out.ogg`.

Каждое событие в `sounds.json` может иметь несколько файлов-вариантов, игра выбирает случайный (плюс небольшой случайный pitch задаётся в коде).

## Готовые ассеты

### plasma_blaster (оружие)

- `geo/item/plasma_blaster.geo.json` — модель: корпус, ствол с дулом, энергоячейка (отдельная кость для анимации), прицел, рукоятка
- `animations/item/plasma_blaster.animation.json` — idle (вращение ячейки, покачивание), shoot (отдача с easeOutElastic, вспышка ячейки), reload (наклон, извлечение и раскрутка ячейки)
- `textures/item/plasma_blaster.png` — 64x64, UV-атлас
- звуковые события: `plasma_blaster.shoot` (3 варианта), `plasma_blaster.reload_start`, `plasma_blaster.reload_end`

### Звуковая библиотека (CC0, Kenney Sci-Fi Sounds)

- `heavy_blaster.shoot` — 2 варианта, для будущего тяжёлого оружия
- `impact.metal` — 2 варианта, попадания снарядов
- `magic.forcefield` — 2 варианта, щиты и ауры
- `magic.explosion` — магические взрывы
