package me.neznamy.tab.bridge.bukkit.nms;

import java.util.Optional;

/**
 * A class to help to assign DataWatcher items as positions often change per-version
 */
public class DataWatcherHelper {

	//position of armor stand flags
	private final int armorStandFlagsPosition = getArmorStandFlagsPosition();

	//original DataWatcher to write to
	private final DataWatcher data;
	
	//data watcher registry
	private final DataWatcherRegistry registry;
	
	/**
	 * Constructs new instance of this class with given parent
	 * @param data - data to write to
	 */
	public DataWatcherHelper(DataWatcher data) {
		this.data = data;
		this.registry = NMSStorage.getInstance().getDataWatcherRegistry();
	}
	
	/**
	 * Returns armor stand flags position based on server version
	 * @return armor stand flags position based on server version
	 */
	private int getArmorStandFlagsPosition() {
		if (NMSStorage.getInstance().getMinorVersion() >= 17) {
			//1.17.x, 1.18.x
			return 15;
		} else if (NMSStorage.getInstance().getMinorVersion() >= 15) {
			//1.15.x, 1.16.x
			return 14;
		} else if (NMSStorage.getInstance().getMinorVersion() >= 14) {
			//1.14.x
			return 13;
		} else if (NMSStorage.getInstance().getMinorVersion() >= 10) {
			//1.10.x - 1.13.x
			return 11;
		} else {
			//1.8.1 - 1.9.x
			return 10;
		}
	}
	
	/**
	 * Writes entity byte flags
	 * @param flags - flags to write
	 */
	public void setEntityFlags(byte flags) {
		data.setValue(new DataWatcherObject(0, registry.getByte()), flags);
	}
	

	public void setCustomName(String text, String componentText) {
		if (NMSStorage.getInstance().getMinorVersion() >= 13) {
			try {
				data.setValue(new DataWatcherObject(2, registry.getOptionalComponent()), Optional.ofNullable(NMSStorage.getInstance().DESERIALIZE.invoke(null, componentText)));
			} catch (ReflectiveOperationException e) {
				e.printStackTrace();
			}
		} else if (NMSStorage.getInstance().getMinorVersion() >= 8){
			data.setValue(new DataWatcherObject(2, registry.getString()), text);
		} else {
			//name length is limited to 64 characters on <1.8
			String cutName = (text.length() > 64 ? text.substring(0, 64) : text);
			if (NMSStorage.getInstance().getMinorVersion() >= 6){
				data.setValue(new DataWatcherObject(10, registry.getString()), cutName);
			} else {
				data.setValue(new DataWatcherObject(5, registry.getString()), cutName);
			}
		}
			
	}
	
	/**
	 * Writes custom name visibility boolean
	 * @param visible - if visible or not
	 */
	public void setCustomNameVisible(boolean visible) {
		if (NMSStorage.getInstance().getMinorVersion() >= 9) {
			data.setValue(new DataWatcherObject(3, registry.getBoolean()), visible);
		} else {
			data.setValue(new DataWatcherObject(3, registry.getByte()), (byte)(visible?1:0));
		}
	}

	/**
	 * Writes armor stand flags
	 * @param flags - flags to write
	 */
	public void setArmorStandFlags(byte flags) {
		data.setValue(new DataWatcherObject(armorStandFlagsPosition, registry.getByte()), flags);
	}
}
