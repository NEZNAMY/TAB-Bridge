package me.neznamy.tab.bridge.bukkit.nms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@Getter
public class DataWatcherItem {
	
	//type of value (position + data type (1.9+))
	@NonNull private final DataWatcherObject type;
	
	//actual data value
	@NonNull private final Object value;
}