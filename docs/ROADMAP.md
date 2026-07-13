# Roadmap v0.4 — задачи для реализации

Статус ветки `codex-5.6`: реализовано для версии `0.4.0`. Блоки ниже сохранены как спецификация и критерии проверки.

| Блок | Статус |
|---|---|
| Войд: управление, прицел и полёт | Готово |
| Ямато: Player Animator и шкала заряда | Готово |
| Judgement Cut / Judgement Cut End | Готово |
| Опустошитель: ядерный заряд и лазер | Готово |
| Техники Годжо и Domain Expansion | Готово |
| Сборка, ресурсы и клиентская проверка | Готово |

Дизайн-база (модели, текстуры, звуки, анимации, новые предметы) уже в репозитории. Ниже — задачи механики, отсортированы по приоритету. Референсы игрока лежат в `/Users/maksimarbonen/Desktop/assets/` (копия смыслов ниже).

## 1. Войд: переработка управления (КРИТИЧНО, содержит багфикс)

Известный баг: убийство на F не работало. Причина: `key.overpowered.void_kill` привязан к GLFW_KEY_F, который конфликтует с ванильным "поменять предмет в руках" (swap offhand). В новой схеме кнопка F не нужна вообще.

Новая схема:
- Превращение: уже реализовано — ПКМ со Сферой Пустоты ([VoidOrbItem.java](../src/main/java/com/maxjonsi/overpowered/item/VoidOrbItem.java) вызывает `VoidAbility.toggle`). Удалить кейбинды VOID_KILL и VOID_TOGGLE из [ModKeyMappings.java](../src/main/java/com/maxjonsi/overpowered/client/ModKeyMappings.java), [OverpoweredClient.java](../src/main/java/com/maxjonsi/overpowered/client/OverpoweredClient.java) и обработку из ServerAbilityHandler (действия 3, 4 в payload оставить, шлёт их теперь другое место).
- Убийство по ЛКМ в форме Войда: в `ClientPreAttackCallback` (уже зарегистрирован в OverpoweredClient) — если игрок в форме Войда (`ClientVoidState.isActive(player.getId())`), отправить `AbilityActionPayload(VOID_KILL)` и вернуть `true` (отменить ванильную атаку). Серверная `VoidAbility.kill` уже готова и работает (спавнит тень, звук, частицы).
- Красный прицел: HUD-оверлей через `HudRenderCallback` (fabric-api). Каждый кадр: если форма Войда активна и клиентский рейкаст (32 блока, как в VoidAbility.kill) находит живую цель — рисовать текстуру `textures/gui/void_crosshair.png` (уже в репо, 15x15) поверх ванильного прицела по центру экрана.
- Полёт в форме Войда: в `VoidAbility.toggle` при включении: `player.getAbilities().mayfly = true; player.onUpdateAbilities();` при выключении вернуть (учесть креатив: не отбирать mayfly у креативщиков). Также иммунитет к урону от падения в форме (LivingFallEvent нет на Fabric — проще в toggle() давать бесконечный slow falling? Нет: правильный способ — в ServerTickEvents уже есть цикл по игрокам с активным войдом: сбрасывать `player.fallDistance = 0`).

## 2. Ямато

- Модель уже переделана: ножны (кость `saya`) + клинок с рукоятью (кость `blade_assembly`), клинок в покое в ножнах, в атаках выхватывается (position z -14.5 = обнажён). Анимации уже переписаны под выхватывание.
- Двуручный хват: подключить библиотеку [playerAnimator](https://modrinth.com/mod/playeranimator) (есть для Fabric 1.21.1) и проиграть позу двуручного хвата для рук игрока при удержании Ямато + анимации замаха рук в комбо. Держать в синхроне с GeckoLib-анимациями предмета (те же тайминги: slash 0.35s, judgement_cut 0.5s).
- Display transforms в [models/item/yamato.json](../src/main/resources/assets/overpowered/models/item/yamato.json) выставлены на глаз — подогнать в игре.
- Перезарядка приёмов: видимые кулдауны уже используют ванильный оверлей предмета; добавить для Judgement Cut End шкалу зарядки при удержании (HUD-полоска 1.5 сек, HudRenderCallback) — ТРЕБОВАНИЕ ПОЛЬЗОВАТЕЛЯ СФОРМУЛИРОВАНО РАСПЛЫВЧАТО ("речардж") — уточнить у него.

## 3. Judgement Cut: визуал по референсу

Референс: сфера из светящихся бело-фиолетовых дуг-разрезов (DMC5).
- Заменить NoopRenderer у `JUDGEMENT_CUT` на кастомный: рисовать 8-12 дуг (квад-ленты по дуге окружности, случайные плоскости, RenderType.lightning() или entityTranslucentEmissive с белой текстурой), цвет белый с фиолетовой кромкой (0.75, 0.55, 1.0), время жизни 24 тика, дуги появляются/исчезают по 3-4 за раз. Радиус 2.5.
- JCE (JUDGEMENT_CUT_END): поле прямых неоновых лучей на всю зону (радиус 24): 40-70 случайных бесконечно-тонких линий (квады 0.15 шириной, длина 30-50, случайные направления, RenderType.lightning()), сине-белые, каждые 5 тиков новый набор. Небо/атмосфера: у жертв уже есть DARKNESS.

## 4. Базука

- Ядерное разрушение 200x200: в [NukeEntity.java](../src/main/java/com/maxjonsi/overpowered/entity/NukeEntity.java) RADIUS 50 -> 100. ОБЯЗАТЕЛЬНО перейти со "слоя за тик" на бюджет блоков за тик (~20000, курсор с сохранением позиции между тиками), иначе верхние слои радиуса 100 (до 31 тыс. блоков на слой x2) начнут лагать сервер. Прогресс сверху вниз сохранить (выглядит как волна).
- Лазер: дальность = до края прогруженных чанков (клип до 256+ блоков циклом по загруженным чанкам, `level.hasChunkAt`), скорость разрушения 1 тик на блок (0.05 c): в [RocketLauncherItem.onUseTick](../src/main/java/com/maxjonsi/overpowered/item/RocketLauncherItem.java) `state.ticks >= 2` -> `>= 1`.
- Визуал лазера: сплошной луч (референс: яркий сине-фиолетовый лазер) вместо частиц END_ROD: рендер через `WorldRenderEvents.AFTER_ENTITIES` (fabric-api) — квад-биллборд от дула до точки попадания, белое ядро 0.08 + фиолетовое гало 0.25, светящийся (FULL_BRIGHT). Нужен синх состояния "кто стреляет лазером" на клиенты (payload или использовать `player.isUsingItem() && mode==LASER` у трекаемых игроков — предпочтительно второе, без новых пакетов).
- Держание за ручку: transforms в models/item/rocket_launcher.json подняты под хват за рукоять — доподогнать в игре.

## 5. Годжо

- Гейтинг: техники Six Eyes работают только если надета Маска Годжо: в [SixEyesItem.use/cycleTechnique](../src/main/java/com/maxjonsi/overpowered/item/SixEyesItem.java) проверка `player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof GojoMaskItem`, иначе actionbar-подсказка "Наденьте маску".
- Маска уже носится (GojoMaskItem = шлем, GeckoLib armor renderer). Проверить посадку в игре (кости armorHead), поправить размеры кубов при необходимости.
- Синий: перед спавном вихря 0.5 сек показывать синий шар между ладонями (частицы DUST синие + SOUL_FIRE_FLAME у рук кастера) — референс blue.jpeg.
- Domain Expansion черная дыра: в [DomainRenderer.java](../src/main/java/com/maxjonsi/overpowered/client/render/DomainRenderer.java) поверх сферы космоса добавить в центре: чёрный шар (r=4, RenderType.entitySolid с чёрной текстурой) + светящееся аккреционное кольцо (тор из квадов r=6, бело-голубое, вращается) — референс domain expansion ref.webp (чёрная дыра Интерстеллар).

## 6. Общее

- Все изменения только server-authoritative, как сейчас (клиент шлёт payload, сервер валидирует).
- После каждого блока задач: push -> дождаться зелёного CI -> в конце tag vX.Y.Z (релиз собирается сам).
- Тюнинг display transforms всех предметов делать В ИГРЕ (запустить клиент, F3+T для перезагрузки ресурсов).

## Звуки (уже подключены, менять не надо)

| Событие | Файл | Откуда |
|---|---|---|
| yamato.slice | weapon/judgement_cut.ogg | реф пользователя |
| yamato.bury_the_light | weapon/bury_the_light.ogg | реф пользователя (12.8s) |
| launcher.laser | weapon/laser_beam.ogg (10.3s) | реф пользователя |
| launcher.nuke | weapon/nuke_boom.ogg | реф пользователя |
| launcher.siren | weapon/nuke_siren.ogg (12.8s, играет при падении бомбы) | реф пользователя |
| gojo.blue / gojo.red | magic/gojo_blue.ogg / gojo_red.ogg | реф пользователя |
| gojo.purple | magic/gojo_purple.ogg | реф пользователя |
| gojo.domain | magic/domain_expand.ogg | реф пользователя |
| void.kill | magic/void_kill.ogg | реф пользователя |
