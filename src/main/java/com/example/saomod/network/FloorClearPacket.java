package com.example.saomod.network;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class FloorClearPacket {

    public final int floorId;
    public final String bossName;

    public FloorClearPacket(int floorId, String bossName) {
        this.floorId = floorId;
        this.bossName = bossName;
    }

    public static void encode(FloorClearPacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.floorId);
        buf.writeUtf(pkt.bossName);
    }

    public static FloorClearPacket decode(FriendlyByteBuf buf) {
        return new FloorClearPacket(buf.readInt(), buf.readUtf());
    }

    public static void handle(FloorClearPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            mc.gui.setTitle(
                    Component.literal("Floor " + pkt.floorId + " - Cleared!")
                            .withStyle(ChatFormatting.GOLD)
            );
            mc.gui.setSubtitle(
                    Component.literal(pkt.bossName)
                            .withStyle(ChatFormatting.YELLOW)
            );
            mc.gui.resetTitleTimes();
            if (mc.player != null) {
                mc.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
