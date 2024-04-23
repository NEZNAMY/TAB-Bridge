package me.neznamy.tab.bridge.bukkit.nms;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import me.neznamy.tab.bridge.bukkit.BukkitBridge;
import me.neznamy.tab.bridge.shared.util.ReflectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

/**
 * A class representing the n.m.s.DataWatcher class to make work with it much easier
 */
public class DataWatcher {

    private static NMSStorage nms;

    /** 1.19.3+ */
    private static Constructor<?> newDataWatcher$Item;

    /** 1.19.2- */
    public static Class<?> DataWatcher;
    private static Constructor<?> newDataWatcher;
    private static Method DataWatcher_register;
    private static Constructor<?> newDataWatcherObject;

    private static Object DataWatcherSerializer_BYTE;
    private static Object DataWatcherSerializer_STRING;
    private static Object DataWatcherSerializer_OPTIONAL_COMPONENT;
    private static Object DataWatcherSerializer_BOOLEAN;

    private static int armorStandFlagsPosition;

    //DataWatcher data
    private final Map<Integer, Item> dataValues = new HashMap<>();

    public static void load(@NotNull NMSStorage nms0) throws ReflectiveOperationException {
        nms = nms0;
        armorStandFlagsPosition = getArmorStandFlagsPosition();
        int minorVersion = BukkitReflection.getMinorVersion();
        if (minorVersion >= 9) {
            loadSerializers();
        }
        if (BukkitReflection.is1_19_3Plus()) {
            Class<?> dataWatcher$Item = BukkitReflection.getClass("network.syncher.SynchedEntityData$DataValue", "network.syncher.DataWatcher$c", "network.syncher.DataWatcher$b");
            Class<?> dataWatcherSerializer = BukkitReflection.getClass("network.syncher.EntityDataSerializer",
                    "network.syncher.DataWatcherSerializer", "DataWatcherSerializer");
            newDataWatcher$Item = dataWatcher$Item.getConstructor(int.class, dataWatcherSerializer, Object.class);
        } else {
            DataWatcher = BukkitReflection.getClass("network.syncher.SynchedEntityData", "network.syncher.DataWatcher", "DataWatcher");
            if (minorVersion >= 7) {
                newDataWatcher = DataWatcher.getConstructor(BukkitReflection.getClass("world.entity.Entity", "Entity"));
            } else {
                newDataWatcher = DataWatcher.getConstructor();
            }
            if (minorVersion >= 9) {
                Class<?> dataWatcherObject = BukkitReflection.getClass("network.syncher.EntityDataAccessor",
                        "network.syncher.DataWatcherObject", "DataWatcherObject");
                Class<?> dataWatcherSerializer = BukkitReflection.getClass("network.syncher.EntityDataSerializer",
                        "network.syncher.DataWatcherSerializer", "DataWatcherSerializer");
                DataWatcher_register = ReflectionUtils.getMethod(
                        DataWatcher,
                        new String[]{"define", "register", "a", "m_135372_"}, // {Mojang, Bukkit, Bukkit 1.18+, Mohist 1.18.2}
                        dataWatcherObject, Object.class
                );
                newDataWatcherObject = dataWatcherObject.getConstructor(int.class, dataWatcherSerializer);
            } else {
                DataWatcher_register = ReflectionUtils.getMethod(
                        DataWatcher,
                        new String[]{"func_75682_a", "a"}, int.class, // {Thermos 1.7.10, Bukkit}
                        Object.class
                );
            }
        }
    }

    private static void loadSerializers() throws ReflectiveOperationException {
        Class<?> dataWatcherRegistry = BukkitReflection.getClass("network.syncher.EntityDataSerializers",
                "network.syncher.DataWatcherRegistry", "DataWatcherRegistry");
        DataWatcherSerializer_BYTE = ReflectionUtils.getField(dataWatcherRegistry, "BYTE", "a", "f_135027_").get(null); // Mohist 1.18.2
        DataWatcherSerializer_STRING = ReflectionUtils.getField(dataWatcherRegistry, "STRING", "d", "f_135030_").get(null); // Mohist 1.18.2
        if (BukkitReflection.is1_19_3Plus()) {
            DataWatcherSerializer_OPTIONAL_COMPONENT = ReflectionUtils.getField(dataWatcherRegistry, "OPTIONAL_COMPONENT", "g").get(null);
            if (BukkitReflection.is1_19_4Plus()) {
                DataWatcherSerializer_BOOLEAN = ReflectionUtils.getField(dataWatcherRegistry, "BOOLEAN", "k").get(null);
            } else {
                DataWatcherSerializer_BOOLEAN = ReflectionUtils.getField(dataWatcherRegistry, "BOOLEAN", "j").get(null);
            }
        } else {
            if (BukkitReflection.getMinorVersion() >= 13) {
                DataWatcherSerializer_OPTIONAL_COMPONENT = ReflectionUtils.getField(dataWatcherRegistry,
                        "OPTIONAL_COMPONENT", "f", "f_135032_").get(null); // Mohist 1.18.2
                DataWatcherSerializer_BOOLEAN = ReflectionUtils.getField(dataWatcherRegistry,
                        "BOOLEAN", "i", "f_135035_").get(null); // Mohist 1.18.2
            } else {
                DataWatcherSerializer_BOOLEAN = dataWatcherRegistry.getDeclaredField("h").get(null);
            }
        }
    }

    public void setValue(int position, Object serializer, Object value){
        dataValues.put(position, new Item(position, serializer, value));
    }

    public Object toNMS() throws ReflectiveOperationException {
        if (BukkitReflection.is1_19_3Plus()) {
            List<Object> items = new ArrayList<>();
            for (Item item : dataValues.values()) {
                items.add(newDataWatcher$Item.newInstance(item.position, item.serializer, item.value));
            }
            return items;
        } else {
            Object nmsWatcher;
            if (newDataWatcher.getParameterCount() == 1) { //1.7+
                nmsWatcher = newDataWatcher.newInstance(new Object[] {null});
            } else {
                nmsWatcher = newDataWatcher.newInstance();
            }
            for (Item item : dataValues.values()) {
                Object nmsObject = item.createObject();
                DataWatcher_register.invoke(nmsWatcher, nmsObject, item.getValue());
            }
            return nmsWatcher;
        }
    }

    /**
     * Writes entity byte flags
     *
     * @param   flags
     *          flags to write
     */
    public void setEntityFlags(byte flags) {
        setValue(0, DataWatcherSerializer_BYTE, flags);
    }

    @SneakyThrows
    public void setCustomName(String customName, String componentText) {
        if (BukkitBridge.getMinorVersion() >= 13) {
            setValue(2, DataWatcherSerializer_OPTIONAL_COMPONENT,
                    Optional.ofNullable(nms.deserialize.apply(componentText)));
        } else if (BukkitBridge.getMinorVersion() >= 8) {
            setValue(2, DataWatcherSerializer_STRING, customName);
        } else {
            //name length is limited to 64 characters on <1.8
            String cutName = (customName.length() > 64 ? customName.substring(0, 64) : customName);
            if (BukkitBridge.getMinorVersion() >= 6) {
                setValue(10, null, cutName);
            } else {
                setValue(5, null, cutName);
            }
        }
    }

    public void setCustomNameVisible(boolean visible) {
        if (BukkitBridge.getMinorVersion() >= 9) {
            setValue(3, DataWatcherSerializer_BOOLEAN, visible);
        } else {
            setValue(3, null, (byte)(visible?1:0));
        }
    }

    public void setArmorStandFlags(byte flags) {
        setValue(armorStandFlagsPosition, DataWatcherSerializer_BYTE, flags);
    }

    private static int getArmorStandFlagsPosition() {
        if (BukkitBridge.getMinorVersion() >= 17) {
            //1.17.x, 1.18.x, 1.19.x, 1.20.x
            return 15;
        } else if (BukkitBridge.getMinorVersion() >= 15) {
            //1.15.x, 1.16.x
            return 14;
        } else if (BukkitBridge.getMinorVersion() == 14) {
            //1.14.x
            return 13;
        } else if (BukkitBridge.getMinorVersion() >= 10) {
            //1.10.x - 1.13.x
            return 11;
        } else {
            //1.8.1 - 1.9.x
            return 10;
        }
    }

    @AllArgsConstructor
    @Getter
    public static class Item {

        private final int position;
        @Nullable private final Object serializer;
        @NonNull private final Object value;

        @SneakyThrows
        public Object createObject() {
            if (BukkitBridge.getMinorVersion() >= 9) {
                return newDataWatcherObject.newInstance(position, serializer);
            } else {
                return position;
            }
        }
    }
}