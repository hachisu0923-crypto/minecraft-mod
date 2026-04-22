package com.example.saomod;

import com.example.saomod.data.FloorData;
import com.example.saomod.entity.SaoFloorBoss;
import com.example.saomod.network.FloorClearPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = SaoMod.MOD_ID)
public class CombatEventHandler {

    @SubscribeEvent
    public static void onBossKilled(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof SaoFloorBoss boss)) return;
        if (!(boss.level() instanceof ServerLevel serverLevel)) return;

        FloorData data = FloorData.get(serverLevel);
        data.setCleared(boss.getFloorId());

        FloorClearPacket packet = new FloorClearPacket(
                boss.getFloorId(),
                boss.getName().getString()
        );
        serverLevel.players().forEach(player ->
                ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player), packet)
        );
    }
}
