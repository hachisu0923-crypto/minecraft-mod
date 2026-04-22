package com.example.saomod.entity;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public abstract class SaoFloorBoss extends Monster implements GeoEntity {

    private static final RawAnimation IDLE_P1 = RawAnimation.begin().thenLoop("animation.boss.idle_phase1");
    private static final RawAnimation IDLE_P2 = RawAnimation.begin().thenLoop("animation.boss.idle_phase2");
    private static final RawAnimation IDLE_P3 = RawAnimation.begin().thenLoop("animation.boss.idle_phase3");
    private static final RawAnimation RAGE    = RawAnimation.begin().thenLoop("animation.boss.rage");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected int phase = 1;
    private final int floorId;
    protected ServerBossEvent bossBar;

    protected SaoFloorBoss(EntityType<? extends SaoFloorBoss> type, Level level, int floorId) {
        super(type, level);
        this.floorId = floorId;
        this.bossBar = new ServerBossEvent(
                this.getDisplayName(),
                BossEvent.BossBarColor.PURPLE,
                BossEvent.BossBarOverlay.PROGRESS
        );
    }

    public int getFloorId() {
        return floorId;
    }

    // --- GeckoLib ---

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "base", 5, this::baseAnimController));
        registrar.add(new AnimationController<>(this, "attack", 0, this::attackAnimController));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    protected PlayState baseAnimController(AnimationState<SaoFloorBoss> state) {
        return state.setAndContinue(switch (phase) {
            case 1 -> IDLE_P1;
            case 2 -> IDLE_P2;
            case 3 -> IDLE_P3;
            default -> RAGE;
        });
    }

    protected abstract PlayState attackAnimController(AnimationState<SaoFloorBoss> state);

    // --- BossBar ---

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    // --- Phase system ---

    @Override
    public void tick() {
        super.tick();
        updatePhase();
        if (!this.level().isClientSide()) updateBossBar();
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

    private void updateBossBar() {
        bossBar.setProgress(this.getHealth() / this.getMaxHealth());
    }

    protected abstract void onPhaseChange(int newPhase);
}
