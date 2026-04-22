package com.example.saomod.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public class FloorData extends SavedData {

    private static final String DATA_KEY = "sao_floors";

    private final Map<Integer, Boolean> clearedFloors = new HashMap<>();
    private int highestFloor = 1;

    public FloorData() {
        // Floor 0 は常にクリア済み扱い → Floor 1 ゲートが最初から開く
        clearedFloors.put(0, true);
    }

    public static FloorData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                FloorData::load, FloorData::new, DATA_KEY
        );
    }

    public boolean isCleared(int floor) {
        return clearedFloors.getOrDefault(floor, false);
    }

    public void setCleared(int floor) {
        clearedFloors.put(floor, true);
        if (floor >= highestFloor) highestFloor = floor + 1;
        setDirty();
    }

    public int getHighestFloor() {
        return highestFloor;
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
