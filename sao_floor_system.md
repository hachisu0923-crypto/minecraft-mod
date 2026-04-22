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

### 基底クラス

```java
// SaoFloorBoss.java
public abstract class SaoFloorBoss extends Monster {

    protected int phase = 1;
    protected final int floorId;
    protected ServerBossEvent bossBar;

    public SaoFloorBoss(EntityType<? extends Monster> type,
                        Level level, int floorId) {
        super(type, level);
        this.floorId = floorId;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        // BossBar 表示
        bossBar.addPlayer(player);
    }

    @Override
    public void tick() {
        super.tick();
        updatePhase();        // HPに応じてフェーズ移行
        updateBossBar();      // BossBar 更新
    }

    protected void updatePhase() {
        float ratio = this.getHealth() / this.getMaxHealth();
        int newPhase = (ratio > 0.75f) ? 1
                     : (ratio > 0.50f) ? 2
                     : (ratio > 0.25f) ? 3 : 4;
        if (newPhase != phase) {
            phase = newPhase;
            onPhaseChange(phase); // サブクラスで攻撃パターン変更
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
  ✅ Floor 1〜3 構造体NBT作成
  ✅ SaoFloorBoss 基底クラス
  ✅ Floor 1 ボス実装 (イルファング)
  ✅ FloorGateBlock 実装
  ✅ クリアパケット + 簡易演出

[Phase 2]
  ☐ Floor 4〜5 構造体・ボス
  ☐ jigsaw モジュール生成
  ☐ エネミー種ごとのカスタムAI
  ☐ フロアマップUI

[Phase 3]
  ☐ Floor 6〜10
  ☐ Floor 75 中間ボス
  ☐ Floor 100 最終ボス
```

---

> **関連ドキュメント**:
> - スキルシステム設計（未作成）
> - ステータス設計（未作成）
> - HUD設計（未作成）
