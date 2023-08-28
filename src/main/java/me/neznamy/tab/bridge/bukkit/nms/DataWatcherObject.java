package me.neznamy.tab.bridge.bukkit.nms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * A class representing the n.m.s.DataWatcherObject class to make work with it much easier
 */
@AllArgsConstructor
@Getter
public class DataWatcherObject {

    //position in DataWatcher
    private final int position;
    
    //value class type used since 1.9
    @Nullable private final Object classType;
}