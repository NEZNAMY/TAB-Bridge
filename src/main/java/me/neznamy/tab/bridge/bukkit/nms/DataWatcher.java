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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A class representing the n.m.s.DataWatcher class to make work with it much easier
 */
public class DataWatcher {

    private static NMSStorage nms;

    public static Class<?> DataWatcherClass;
    private static Constructor<?> newDataWatcher;
    private static Method DataWatcher_register;
    private static Method DataWatcher_markDirty;
    public static Method DataWatcher_packDirty;

    private static Class<?> DataWatcherObject;
    private static Constructor<?> newDataWatcherObject;

    private static Class<?> DataWatcherRegistry;
    private static Class<?> DataWatcherSerializer;
    private static Object DataWatcherSerializer_BYTE;
    private static Object DataWatcherSerializer_STRING;
    private static Object DataWatcherSerializer_OPTIONAL_COMPONENT;
    private static Object DataWatcherSerializer_BOOLEAN;

    private static int armorStandFlagsPosition;

    //DataWatcher data
    private final Map<Integer, Item> dataValues = new HashMap<>();

    public static void load(@NotNull NMSStorage nms) throws ReflectiveOperationException {
        DataWatcher.nms = nms;
        armorStandFlagsPosition = getArmorStandFlagsPosition();
        if (BukkitBridge.getMinorVersion() >= 17) {
            DataWatcherClass = Class.forName("net.minecraft.network.syncher.DataWatcher");
            DataWatcherObject = Class.forName("net.minecraft.network.syncher.DataWatcherObject");
            DataWatcherRegistry = Class.forName("net.minecraft.network.syncher.DataWatcherRegistry");
            DataWatcherSerializer = Class.forName("net.minecraft.network.syncher.DataWatcherSerializer");
        } else {
            DataWatcherClass = nms.getLegacyClass("DataWatcher");
            if (BukkitBridge.getMinorVersion() >= 9) {
                DataWatcherObject = nms.getLegacyClass("DataWatcherObject");
                DataWatcherRegistry = nms.getLegacyClass("DataWatcherRegistry");
                DataWatcherSerializer = nms.getLegacyClass("DataWatcherSerializer");
            }
        }
        newDataWatcher = ReflectionUtils.getOnlyConstructor(DataWatcherClass);
        if (BukkitBridge.getMinorVersion() >= 9) {
            DataWatcher_register = ReflectionUtils.getMethod(
                    DataWatcherClass,
                    new String[]{"define", "register", "a", "m_135372_"}, // {Bukkit 1.20.2+, Bukkit, Bukkit 1.18+, Mohist 1.18.2}
                    DataWatcherObject, Object.class
            );
        } else {
            DataWatcher_register = ReflectionUtils.getMethod(DataWatcherClass, new String[]{"func_75682_a", "a"}, int.class, Object.class); // {Thermos 1.7.10, Bukkit}
        }
        if (BukkitBridge.getMinorVersion() >= 19) {
            DataWatcher_packDirty = ReflectionUtils.getMethod(DataWatcherClass, new String[]{"packDirty", "b"}); // {Mojang | 1.20.2+, 1.20.2-}
        }
        if (BukkitBridge.is1_19_3Plus()) {
            DataWatcher_markDirty = ReflectionUtils.getMethods(DataWatcherClass, void.class, DataWatcherObject).get(0);
        }
        if (BukkitBridge.getMinorVersion() >= 9) {
            newDataWatcherObject = DataWatcherObject.getConstructor(int.class, DataWatcherSerializer);
            DataWatcherSerializer_BYTE = ReflectionUtils.getField(DataWatcherRegistry, "BYTE", "a", "f_135027_").get(null); // Mohist 1.18.2
            DataWatcherSerializer_STRING = ReflectionUtils.getField(DataWatcherRegistry, "STRING", "d", "f_135030_").get(null); // Mohist 1.18.2
            if (BukkitBridge.is1_19_3Plus()) {
                DataWatcherSerializer_OPTIONAL_COMPONENT = ReflectionUtils.getField(DataWatcherRegistry, "OPTIONAL_COMPONENT", "g").get(null);
                if (BukkitBridge.is1_19_4Plus()) {
                    DataWatcherSerializer_BOOLEAN = ReflectionUtils.getField(DataWatcherRegistry, "BOOLEAN", "k").get(null);
                } else {
                    DataWatcherSerializer_BOOLEAN = ReflectionUtils.getField(DataWatcherRegistry, "BOOLEAN", "j").get(null);
                }
            } else {
                if (BukkitBridge.getMinorVersion() >= 13) {
                    DataWatcherSerializer_OPTIONAL_COMPONENT = ReflectionUtils.getField(DataWatcherRegistry,
                            "OPTIONAL_COMPONENT", "f", "f_135032_").get(null); // Mohist 1.18.2
                    DataWatcherSerializer_BOOLEAN = ReflectionUtils.getField(DataWatcherRegistry,
                            "BOOLEAN", "i", "f_135035_").get(null); // Mohist 1.18.2
                } else {
                    DataWatcherSerializer_BOOLEAN = DataWatcherRegistry.getDeclaredField("h").get(null);
                }
            }
        }
    }

    public void setValue(int position, Object serializer, Object value){
        dataValues.put(position, new Item(position, serializer, value));
    }

    public Object toNMS() throws ReflectiveOperationException {
        Object nmsWatcher;
        if (newDataWatcher.getParameterCount() == 1) { //1.7+
            Object[] args = new Object[] {null};
            nmsWatcher = newDataWatcher.newInstance(args);
        } else {
            nmsWatcher = newDataWatcher.newInstance();
        }
        for (Item item : dataValues.values()) {
            Object nmsObject = item.createObject();
            DataWatcher_register.invoke(nmsWatcher, nmsObject, item.getValue());
            if (BukkitBridge.is1_19_3Plus()) DataWatcher_markDirty.invoke(nmsWatcher, nmsObject);
        }
        return nmsWatcher;
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
                    Optional.ofNullable(nms.DESERIALIZE.invoke(null, componentText)));
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
        } else if (BukkitBridge.getMinorVersion() >= 14) {
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