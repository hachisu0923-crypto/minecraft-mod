package com.example.saomod.entity.boss;

import com.example.saomod.entity.SaoFloorBoss;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.core.animation.Animation;

public class IllfangBoss extends SaoFloorBoss {

    private static final RawAnimation SLASH = RawAnimation.begin()
            .then("animation.boss.attack_slash", Animation.LoopType.PLAY_ONCE);
    private static final RawAnimation SMASH = RawAnimation.begin()
            .then("animation.boss.attack_smash", Animation.LoopType.PLAY_ONCE);

    public IllfangBoss(EntityType<? extends IllfangBoss> type, Level level) {
        super(type, level, 1);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 400.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.FOLLOW_RANGE, 48.0)
                .add(Attributes.ARMOR, 4.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected PlayState attackAnimController(AnimationState<SaoFloorBoss> state) {
        if (this.swinging) {
            return state.setAndContinue(phase >= 3 ? SMASH : SLASH);
        }
        return PlayState.STOP;
    }

    @Override
    protected void onPhaseChange(int newPhase) {
        if (this.level().isClientSide()) return;
        if (newPhase >= 3) {
            // Phase 3+: 移動速度上昇
            var speedAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null) speedAttr.setBaseValue(0.4);
        }
        if (newPhase == 4) {
            // Phase 4: 攻撃力強化
            var dmgAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (dmgAttr != null) dmgAttr.setBaseValue(20.0);
        }
    }
}
