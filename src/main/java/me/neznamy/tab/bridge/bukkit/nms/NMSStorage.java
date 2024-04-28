package me.neznamy.tab.bridge.bukkit.nms;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import me.neznamy.tab.bridge.bukkit.BukkitScoreboard;
import me.neznamy.tab.bridge.shared.util.ComponentCache;
import me.neznamy.tab.bridge.shared.util.FunctionWithException;
import me.neznamy.tab.bridge.shared.util.ReflectionUtils;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

public class NMSStorage {

    //instance of this class
    @Nullable @Getter @Setter private static NMSStorage instance;

    @Getter private final PacketSender packetSender = new PacketSender();
    private final FunctionWithException<String, Object> deserialize = getDeserializeFunction();
    @Getter private ChannelInjection channelInjection;
    private final ComponentCache<String, Object> componentCache = new ComponentCache<>(1000, json -> {
        try {
            return deserialize.apply(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    });

    /**
     * Creates new instance, initializes required NMS classes and fields
     * @throws    ReflectiveOperationException
     *             If any class, field or method fails to load
     */
    public NMSStorage() throws ReflectiveOperationException {
        DataWatcher.load(this);
        PacketEntityView.load();
        if (BukkitReflection.getMinorVersion() >= 8) {
            channelInjection = new ChannelInjection();
        }
        if (!BukkitScoreboard.isAvailable())
            Bukkit.getConsoleSender().sendMessage("\u00a7c[TAB-Bridge] Failed to initialize Scoreboard fields due to " +
                    "a compatibility issue, plugin functionality will be limited.");
    }

    private FunctionWithException<String, Object> getDeserializeFunction() throws ReflectiveOperationException {
        Class<?> ChatSerializer = BukkitReflection.getClass("network.chat.Component$Serializer",
                "network.chat.IChatBaseComponent$ChatSerializer", "IChatBaseComponent$ChatSerializer", "ChatSerializer");
        try {
            // 1.20.5+
            Class<?> HolderLookup$Provider = BukkitReflection.getClass("core.HolderLookup$Provider", "core.HolderLookup$a");
            Method fromJson = first(ReflectionUtils.getMethods(ChatSerializer, Object.class, String.class, HolderLookup$Provider));
            Object emptyProvider = ReflectionUtils.getOnlyMethod(HolderLookup$Provider, HolderLookup$Provider, Stream.class).invoke(null, Stream.empty());
            return string -> fromJson.invoke(null, string, emptyProvider);
        } catch (ReflectiveOperationException e) {
            // 1.20.4-
            Method fromJson = first(ReflectionUtils.getMethods(ChatSerializer, Object.class, String.class));
            return string -> fromJson.invoke(null, string);
        }
    }

    public Object deserialize(@NonNull String json) {
        return componentCache.get(json);
    }

    @NotNull
    private Method first(@NotNull List<Method> methods) throws NoSuchMethodException {
        if (methods.isEmpty()) throw new NoSuchMethodException("Json deserialize method not found");
        return methods.get(0);
    }
}