# SAO Mod — 階層（フロア）システム設計書

> Forge 1.20.1 対応 / 中規模構成

---

## 1. 概要

アインクラッド100層を模した階層ダンジョンシステム。  
各フロアは**単一のディメンション内**に構造体として配置され、  
「通常エリア（探索・雑魚戦）」と「ボスエリア（専用部屋）」が同一空間に並存する。

ボスエリアへの入口には `FloorGateBlock` を設置し、前フロアクリア済みでないと通過できない。  
ボス討伐でフロアクリアフラグが立ち、次フロアへの昇降口が開放される。

### エリア分離方針

```text
同一ディメンション内のフロア構成イメージ

  ┌─────────────────────────────────────────┐
  │  Floor N ディメンション                  │
  │                                         │
  │  ┌──────────────────┐                  │
  │  │  通常エリア        │                  │
  │  │  ・探索ルート      │                  │
  │  │  ・雑魚スポーン    │                  │
  │  │  ・宝箱・NPC      │                  │
  │  └────────┬─────────┘                  │
  │           │ FloorGateBlock（ロック）      │
  │  ┌────────▼─────────┐                  │
  │  │  ボスエリア        │                  │
  │  │  ・ボス専用部屋    │                  │
  │  │  ・ボススポーン    │                  │
  │  │  ・クリア後：出口  │                  │
  │  └──────────────────┘                  │
  └─────────────────────────────────────────┘
```

---

## 2. フロアデータ管理

### SavedData によるサーバー永続化

```java
// FloorData.java — world.getDataStorage() で取得
public class FloorData extends SavedData {
    private Map<Integer, Boolean> clearedFloors = new HashMap<>();
    private int highestFloor = 1;

    public boolean isCleared(int floor) {
        return clearedFloors.getOrDefault(floor, false);
    }

    public void setCleared(int floor) {
        clearedFloors.put(floor, true);
        if (floor >= highestFloor) highestFloor = floor + 1;
        setDirty(); // NBTへ自動保存マーク
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        clearedFloors.forEach((floor, cleared) -> {
            CompoundTag entry = new CompoundTag();
            entry.putInt("floor", floor);
            entry.putBoolean("cleared", cleared);
            list.add(entry);
        });
        tag.put("clearedFloors", list);
        tag.putInt("highestFloor", highestFloor);
        return tag;
    }

    public static FloorData load(CompoundTag tag) {
        FloorData data = new FloorData();
        ListTag list = tag.getList("clearedFloors", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            data.clearedFloors.put(entry.getInt("floor"), entry.getBoolean("cleared"));
        }
        data.highestFloor = tag.getInt("highestFloor");
        return data;
    }
}
```

### 取得方法

```java
// サーバーサイドで取得
FloorData data = level.getDataStorage()
    .computeIfAbsent(FloorData::load, FloorData::new, "sao_floors");
```

---

## 3. フロア構成

### Phase 1 実装範囲（Floor 1〜5）

| Floor | エリア名称       | 難易度 | 主要ギミック             | ボス名          |
|-------|-----------------|--------|--------------------------|----------------|
| 1     | 始まりの草原     | ★☆☆   | 基本戦闘・チュートリアル  | イルファング     |
| 2     | 地下迷宮         | ★★☆   | 暗闇・松明管理            | コロバス         |
| 3     | 溶岩洞窟         | ★★☆   | 溶岩床・耐火属性有利      | ゴル・ベイン     |
| 4     | 氷結回廊         | ★★★   | 凍結デバフ・滑り床        | フロストジャイアント |
| 5     | 天空城塞         | ★★★   | 高所落下・飛行エネミー    | ザ・コロッサス   |

### Phase 2 以降（Floor 6〜）

- Floor 6〜10: 水中・砂漠・森林など多様バイオーム対応
- Floor 75: 中間ボス（SAO原作再現）
- Floor 100: 最終ボス「ヒースクリフ」

---

## 4. 構造体（Structure）実装

### 方針

- 1フロア = **通常エリア群** + **ボスエリア1つ** が同一ディメンション内に配置
- 通常エリアは `jigsaw` ブロックによるモジュール式ランダム生成
- ボスエリアは固定NBT構造体（毎回同じレイアウト）
- 通常エリアの最奥に `FloorGateBlock` → その先にボス部屋

### エリアの役割分担

> 空間レイアウトは [Section 1 のエリア構成図](#エリア分離方針) も参照。

| エリア種別 | 生成方式 | 内容 |
|-----------|---------|------|
| 通常エリア | jigsaw モジュール式 | 探索・雑魚戦・宝箱・ショップNPC |
| ボスエリア | 固定NBT | ボス戦専用・演出・クリア後出口 |

### ファイル構成

```text
resources/
└── data/
    └── saomod/
        └── structures/
            ├── floor_1/
            │   ├── normal/               ← 通常エリア（jigsaw モジュール）
            │   │   ├── entrance.nbt      　 入口
            │   │   ├── corridor_a.nbt    　 通路バリエーション
            │   │   ├── corridor_b.nbt
            │   │   ├── room_small.nbt    　 小部屋（雑魚スポーン）
            │   │   ├── room_large.nbt    　 大部屋（宝箱・NPC）
            │   │   └── gate_room.nbt     　 FloorGateBlock 設置部屋（最奥）
            │   └── boss/                 ← ボスエリア（固定）
            │       ├── boss_room.nbt     　 ボス戦フロア
            │       └── boss_exit.nbt     　 クリア後の昇降口
            ├── floor_2/
            │   └── ...
            └── common/
                ├── chest_room.nbt
                └── shop_room.nbt
```

### Structurize による構造体作成ワークフロー

手動で NBT を編集するより、Structurize のインゲームスキャンツールを使うと
部屋の作成・修正サイクルが大幅に短縮できる。

**build.gradle への追加（開発時ランタイムのみ）:**

```gradle
repositories {
    maven { url = 'https://www.cursemaven.com' }
}
dependencies {
    // CurseForge のファイルページから File ID を取得して差し替えること
    runtimeOnly fg.deobf("curse.maven:structurize-298744:<FILE_ID>")
}
```

**作業手順:**

```text
1. ./gradlew runClient で開発クライアントを起動
2. クリエイティブモードで部屋を建築
   （スポーナー・宝箱・jigsaw ブロックを配置する）
3. Structurize のスキャンツールで範囲を選択 → .blueprint として保存
4. Structurize の変換機能で .blueprint → .nbt に変換
5. 出力した .nbt を resources/data/saomod/structures/floor_N/ に配置
```

> **注意**: Structurize のネイティブ保存形式は `.blueprint` だが、  
> Minecraft の jigsaw システムが要求するのは `.nbt` 形式。  
> スキャン後は必ず変換ステップを挟むこと。

### 通常エリア → ボスエリアの接続

```text
jigsaw 生成チェーン:
  entrance.nbt
    → corridor_*.nbt (ランダム複数)
      → room_*.nbt   (ランダム複数)
        → gate_room.nbt  ← 必ずここで終端 (FloorGateBlock 設置)
          → boss_room.nbt (ゲート通過後)
            → boss_exit.nbt (クリア後開放)
```

> **ポイント**: `gate_room.nbt` を jigsaw チェーンの **終端ピース** として定義することで、  
> 通常エリアの最奥に必ずボスエリアへの入口が生成されることを保証する。

### ボス部屋の自動ロック（FloorGateBlock）

```java
// ボス部屋入口に設置するブロック
// FloorGateBlock.java
public class FloorGateBlock extends Block {

    @Override
    public InteractionResult use(BlockState state, Level level,
            BlockPos pos, Player player, ...) {

        FloorData data = FloorData.get(level);
        int requiredFloor = state.getValue(REQUIRED_FLOOR);

        if (!data.isCleared(requiredFloor - 1)) {
            // 前フロア未クリア → 入場不可
            player.displayClientMessage(
                Component.literal("前フロアをクリアしてください"), true);
            return InteractionResult.FAIL;
        }
        // ゲート開放
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        return InteractionResult.SUCCESS;
    }
}
```

---

## 5. フロアクリアフロー

```text
プレイヤーがボス部屋に入場
        ↓
ボスエンティティがスポーン（BossBarEvent 登録）
        ↓
ボスHP = 0 → LivingDeathEvent 発火
        ↓
CombatEventHandler.onBossKilled()
  ├── FloorData.setCleared(floorId)  // SavedData に保存
  ├── FloorClearPacket を全プレイヤーへブロードキャスト
  ├── クリア演出（パーティクル・音・テキスト）
  └── 次フロアゲートのフラグ解除
```

### FloorClearPacket（S→C）

```java
public class FloorClearPacket {
    public final int floorId;
    public final String bossName;

    // クライアント側でクリア演出を再生
    public static void handle(FloorClearPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            // 画面中央に「Floor X — Cleared!」を数秒間オーバーレイ表示
            mc.gui.setTitle(
                Component.literal("Floor " + pkt.floorId + " — Cleared!"),
                Component.literal(pkt.bossName)
            );
            // クリア音を再生
            mc.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        });
        ctx.get().setPacketHandled(true);
    }
}
```

---

## 6. フロア内エネミー設定

### スポーン管理

- `SpawnPlacementRegisterEvent` でフロア専用モブのスポーン条件を登録
- フロアごとにカスタムバイオームタグを付与してスポーンを分離
- 各フロアのエネミーは `MobCategory.MONSTER` 継承

### エネミー強度テーブル

| Floor | HP 倍率 | ダメージ倍率 | 特殊能力           |
|-------|---------|-------------|--------------------|
| 1     | ×1.0    | ×1.0        | なし               |
| 2     | ×1.5    | ×1.2        | 暗闇付与           |
| 3     | ×2.0    | ×1.5        | 炎上付与           |
| 4     | ×2.5    | ×1.8        | 凍結スロウ         |
| 5     | ×3.0    | ×2.0        | 飛行・遠距離攻撃   |

---

## 7. ボスエンティティ設計

### 依存ライブラリ：GeckoLib

バニラ Forge のエンティティアニメーションはフレームベースで表現力が低い。
GeckoLib を使うと Blockbench で作成した 3D アニメーションを JSON で管理でき、
フェーズ移行時の攻撃モーション切り替えがシンプルに実装できる。

**build.gradle への追加:**

```gradle
repositories {
    maven { url = 'https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/' }
}
dependencies {
    implementation fg.deobf("software.bernie.geckolib:geckolib-forge-1.20.1:4.4.4")
}
```

**GeckoLib アセットファイル構成:**

```text
assets/saomod/
├── geo/entity/
│   └── boss_floor1.json           ← Blockbench で出力する 3D モデル
├── animations/entity/
│   └── boss_floor1.animation.json ← アニメーション定義
└── textures/entity/boss/
    └── floor1_boss.png
```

**フェーズ × アニメーション対応表:**

| フェーズ | アニメーション名 | 内容 |
|----------|----------------|------|
| Phase 1  | `animation.boss.idle_phase1`  | 通常待機・歩行 |
| Phase 2  | `animation.boss.idle_phase2`  | 速度上昇後の待機 |
| Phase 3  | `animation.boss.idle_phase3`  | スマッシュ構え |
| Phase 4  | `animation.boss.rage`         | 全力モード待機 |
| 攻撃     | `animation.boss.attack_slash` | 斬撃（Phase 1〜2）|
| 攻撃     | `animation.boss.attack_smash` | スマッシュ（Phase 3〜4）|

### 基底クラス

`Monster` の代わりに GeckoLib の `GeoEntity` を継承し、アニメーションコントローラーを登録する。

```java
// SaoFloorBoss.java
public abstract class SaoFloorBoss extends Monster implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected int phase = 1;
    protected final int floorId;
    protected ServerBossEvent bossBar;

    public SaoFloorBoss(EntityType<? extends Monster> type,
                        Level level, int floorId) {
        super(type, level);
        this.floorId = floorId;
    }

    // --- GeckoLib 必須実装 ---

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "base", 5, this::baseAnimController));
        registrar.add(new AnimationController<>(this, "attack", 0, this::attackAnimController));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    protected PlayState baseAnimController(AnimationState<SaoFloorBoss> state) {
        return switch (phase) {
            case 1 -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.boss.idle_phase1"));
            case 2 -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.boss.idle_phase2"));
            case 3 -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.boss.idle_phase3"));
            default -> state.setAndContinue(RawAnimation.begin().thenLoop("animation.boss.rage"));
        };
    }

    // サブクラスで攻撃アニメーションを定義する
    protected abstract PlayState attackAnimController(AnimationState<SaoFloorBoss> state);

    // --- 既存ロジック ---

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void tick() {
        super.tick();
        updatePhase();
        updateBossBar();
    }

    protected void updatePhase() {
        float ratio = this.getHealth() / this.getMaxHealth();
        int newPhase = (ratio > 0.75f) ? 1
                     : (ratio > 0.50f) ? 2
                     : (ratio > 0.25f) ? 3 : 4;
        if (newPhase != phase) {
            phase = newPhase;
            onPhaseChange(phase);
        }
    }

    protected abstract void onPhaseChange(int newPhase);
}
```

### フェーズ変化パターン例（Floor 1 ボス）

| フェーズ | HP残量     | 変化内容                         |
|----------|-----------|----------------------------------|
| Phase 1  | 100〜75%  | 通常攻撃のみ                     |
| Phase 2  | 75〜50%   | 範囲攻撃追加・移動速度上昇       |
| Phase 3  | 50〜25%   | 広範囲スマッシュ追加             |
| Phase 4  | 25〜0%    | 全攻撃強化・無敵フレーム追加     |

---

## 8. フロアUI・表示

### ミニHUDへの情報表示

- 現在フロア番号を画面右上に常時表示
- ボス部屋では `ServerBossEvent` によるボスHPバーを上部に表示
- フロアクリア時: 画面中央に「Floor X — Cleared!」をアニメーション表示

### フロアマップ（Phase 2 実装予定 → [Section 9](#9-開発優先順位) 参照）

- `Patchouli` 連携でフロア攻略メモ帳
- クリアフロアは金色でマーク

---

## 9. 開発優先順位

```text
[Phase 1]
  ✅ FloorData (SavedData) 実装
  ✅ Floor 1〜3 構造体NBT作成（Structurize でスキャン → .nbt 変換）
  ✅ GeckoLib 依存追加 + build.gradle 設定
  ✅ SaoFloorBoss 基底クラス（GeckoLib GeoEntity 継承）
  ✅ Floor 1 ボス実装 (イルファング) + GeckoLib アニメーション JSON
  ✅ FloorGateBlock 実装
  ✅ クリアパケット + 簡易演出

[Phase 2]
  ☐ Floor 4〜5 構造体（Structurize スキャン済み NBT を使用）・ボス
  ☐ YUNG's API 導入 + jigsaw を Enhanced Jigsaw Manager に移行
  ☐ Structure Gel API 導入 + 既存構造体 NBT に Gel ブロックを追加
  ☐ jigsaw モジュール生成（分岐・優先度ルール設計）
  ☐ エネミー種ごとのカスタムAI
  ☐ フロアマップUI
  ☐ GeckoLib アニメーション拡充（Phase 2 ボス分）

[Phase 3]
  ☐ TerraBlender 導入 + Floor 6〜10 カスタムバイオーム登録
  ☐ Floor 6〜10（Structurize で構造体を量産・バイオームテーマ反映）
  ☐ Floor 75 中間ボス
  ☐ Floor 100 最終ボス「ヒースクリフ」
```

---

## 10. 地形生成補助ライブラリ

現在の設計は手作り NBT + jigsaw モジュールで成り立っているが、
Floor 6〜100 に向けてスケールする際に以下の限界がある。

- jigsaw はあくまで事前ビルドしたモジュールを繋ぐだけで、真の手続き生成ではない
- フロアごとのカスタムバイオーム（水中・砂漠・森林）は標準 API だけでは煩雑
- モジュール接続の多様性（分岐・ループ・高さ変化）が vanilla jigsaw では難しい

以下の 3 ライブラリを導入することでこれらを解決する。

### ライブラリ比較

| ライブラリ | 主な用途 | Forge 1.20.1 対応 | 導入種別 |
|-----------|---------|:-----------------:|---------|
| **YUNG's API** | jigsaw 強化・構造体プール管理 | ✅ | `implementation`（必須依存） |
| **TerraBlender** | フロアディメンションへのカスタムバイオーム注入 | ✅ | `implementation`（必須依存） |
| **Structure Gel API** | 構造体境界定義・構造体内エア変換 | ✅ | `implementation`（任意強化） |

---

### YUNG's API — jigsaw 強化ライブラリ

vanilla の jigsaw は単純なチェーンしか組めないが、YUNG's API の **Enhanced Jigsaw Manager** を
使うと分岐・ループ・接続優先度などの高度なレイアウトが可能になる。

**影響箇所:** Section 4「通常エリア → ボスエリアの接続」

**build.gradle への追加:**

```gradle
repositories {
    maven { url = 'https://repo1.maven.org/maven2/' }
}
dependencies {
    implementation fg.deobf("com.yungnickyoung.minecraft.yungsapi:YungsApi-1.20-Forge:4.0.2")
}
```

**jigsaw チェーンの改善例:**

```text
[vanilla jigsaw]
  entrance → corridor → ... → gate_room  (直列のみ)

[YUNG's API Enhanced Jigsaw]
  entrance
    ├── corridor_main  (必須経路)
    │     ├── room_small (重み 3)
    │     ├── room_large (重み 1)
    │     └── corridor_branch (分岐あり)
    └── shortcut_path  (優先度低・ランダム出現)
          └── gate_room (終端ピース)
```

**コード変更箇所:** `SaoFloorBoss` の構造体プール登録で `YungsJigsawManager` に切り替える。

```java
// 既存: vanilla JigsawPlacement
JigsawPlacement.addPieces(context, poolHolder, ...);

// YUNG's API に切り替え
YungsJigsawManager.addPieces(context, poolHolder, ...);
```

---

### TerraBlender — フロアバイオーム注入 API

各フロアディメンションに専用バイオームを付与することで、
Floor 6〜10 の「水中・砂漠・森林」テーマを地形レベルで実現する。
TerraBlender はバイオーム間の干渉を避けるための **uniqueness** パラメータを提供する。

**影響箇所:** Section 3「Phase 2 以降（Floor 6〜）」

**build.gradle への追加:**

```gradle
repositories {
    maven { url = 'https://maven.blamejared.com' }
}
dependencies {
    implementation fg.deobf("curse.maven:terrablender-563928:4509605")
}
```

**フロアバイオーム設計:**

| Floor | テーマ | TerraBlender バイオーム設定 |
|-------|--------|---------------------------|
| 6     | 水中迷宮 | `temperature=-0.5, humidity=1.0` の水中バイオーム |
| 7     | 砂漠遺跡 | `temperature=2.0, humidity=-1.0` の砂漠バイオーム |
| 8     | 深森林 | `temperature=0.5, humidity=0.8` の密林バイオーム |
| 9     | 氷河洞窟 | `temperature=-2.0, humidity=0.0` の氷結バイオーム |
| 10    | 火山地帯 | `temperature=2.0, humidity=-0.5` の溶岩バイオーム |

**バイオーム登録例:**

```java
// SaoModBiomes.java
public class SaoModBiomes {
    public static void registerBiomes(BiomeProviders providers) {
        providers.register(new SaoFloorBiomeProvider(
            SaoMod.MOD_ID, 4  // uniqueness = 4 (他 Mod との干渉を避ける)
        ));
    }
}
```

---

### Structure Gel API — 構造体境界ヘルパー

Gel ブロックを構造体に埋め込むことで、生成時に周囲の地形を自動整地したり
構造体内部のエア化を自動処理できる。jigsaw モジュールの配置精度が上がる。

**影響箇所:** Section 4「Structurize による構造体作成ワークフロー」

**build.gradle への追加:**

```gradle
repositories {
    maven { url = 'https://maven.moddinglegacy.com/artifactory/modding-legacy/' }
}
dependencies {
    implementation fg.deobf("com.legacy:structure-gel:2.16.2:forge")
}
```

**Gel ブロックの種類と用途:**

| Gel ブロック | 用途 |
|-------------|------|
| `GelBlocks.BLUE_GEL` | 構造体の外壁・床に埋め込み → 生成時に周囲を整地 |
| `GelBlocks.RED_GEL` | 構造体内部に埋め込み → 生成時に空気に置換（洞窟など） |
| `GelBlocks.GREEN_GEL` | 液体（水・溶岩）除去マーカー |

> **ワークフロー変更**: Structurize でスキャンした部屋の外周に Blue Gel を
> 手動配置してから `.nbt` に変換することで、
> jigsaw 生成時に隣接する地形との境界が自動整地される。

---

### 3 ライブラリ導入後の恩恵まとめ

| 課題 | 導入前 | 導入後 |
|------|-------|-------|
| 通路の分岐・多様性 | 直列チェーンのみ | YUNG's API で分岐・優先度制御が可能 |
| Floor 6〜10 のバイオーム | 手動タグ付けのみ | TerraBlender で地形レベルのバイオーム生成 |
| 構造体と地形の境界 | 手作業でフラット地形を前提 | Structure Gel API で自動整地 |
| 100 フロアへのスケール | 手作り NBT が必要 | 生成ルールの設定追加のみで対応可能 |

---

> **関連ドキュメント**:
> - スキルシステム設計（未作成）
> - ステータス設計（未作成）
> - HUD設計（未作成）
