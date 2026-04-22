package com.example.saomod;

import com.example.saomod.entity.boss.IllfangBoss;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SaoMod.MOD_ID);

    public static final RegistryObject<EntityType<IllfangBoss>> ILLFANG =
            ENTITIES.register("illfang", () -> EntityType.Builder
                    .<IllfangBoss>of(IllfangBoss::new, MobCategory.MONSTER)
                    .sized(1.5f, 2.5f)
                    .clientTrackingRange(80)
                    .build("saomod:illfang"));

    @Mod.EventBusSubscriber(modid = SaoMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class AttributeRegistrar {
        @SubscribeEvent
        public static void onAttributeCreate(EntityAttributeCreationEvent event) {
            event.put(ILLFANG.get(), IllfangBoss.createAttributes().build());
        }
    }
}
