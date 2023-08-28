package me.neznamy.tab.bridge.bukkit.nms;

import java.util.HashMap;
import java.util.Map;

/**
 * A class representing the n.m.s.DataWatcher class to make work with it much easier
 */
public class DataWatcher {

    //DataWatcher data
    private final Map<Integer, DataWatcherItem> dataValues = new HashMap<>();
    
    //a helper for easier data write
    private final DataWatcherHelper helper = new DataWatcherHelper(this);

    /**
     * Sets value into data values
     * @param type - type of value
     * @param value - value
     */
    public void setValue(DataWatcherObject type, Object value){
        dataValues.put(type.getPosition(), new DataWatcherItem(type, value));
    }

    /**
     * Returns helper created by this instance
     * @return data write helper
     */
    public DataWatcherHelper helper() {
        return helper;
    }

    /**
     * Converts the class into an instance of NMS.DataWatcher
     * @return an instance of NMS.DataWatcher with same data
     * @throws    ReflectiveOperationException
     *             if thrown by reflective operation
     */
    public Object toNMS() throws ReflectiveOperationException {
        NMSStorage nms = NMSStorage.getInstance();
        Object nmsWatcher;
        if (nms.newDataWatcher.getParameterCount() == 1) { //1.7+
            Object[] args = new Object[] {null};
            nmsWatcher = nms.newDataWatcher.newInstance(args);
        } else {
            nmsWatcher = nms.newDataWatcher.newInstance();
        }
        for (DataWatcherItem item : dataValues.values()) {
            Object position;
            if (nms.getMinorVersion() >= 9) {
                position = nms.newDataWatcherObject.newInstance(item.getType().getPosition(), item.getType().getClassType());
            } else {
                position = item.getType().getPosition();
            }
            nms.DataWatcher_REGISTER.invoke(nmsWatcher, position, item.getValue());
            if (nms.is1_19_3Plus()) nms.DataWatcher_markDirty.invoke(nmsWatcher, position);
        }
        return nmsWatcher;
    }
}