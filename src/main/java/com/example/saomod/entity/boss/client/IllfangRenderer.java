package com.example.saomod.entity.boss.client;

import com.example.saomod.SaoMod;
import com.example.saomod.entity.boss.IllfangBoss;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class IllfangRenderer extends GeoEntityRenderer<IllfangBoss> {

    public IllfangRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(
                new ResourceLocation(SaoMod.MOD_ID, "entity/boss_floor1")
        ));
    }
}
