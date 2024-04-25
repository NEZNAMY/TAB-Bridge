package me.neznamy.tab.bridge.shared.util;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Component cache to save resources when converting the same
 * values over and over.
 *
 * @param   <K>
 *          Source component
 * @param   <V>
 *          Target component
 */
@AllArgsConstructor
public class ComponentCache<K, V> {

    private final int cacheSize;
    private final Function<K, V> function;
    private final Map<K, V> cache = new HashMap<>();

    /**
     * Gets value from cache. If not present, it is created using given function, inserted
     * into the cache and then returned.
     *
     * @param   key
     *          Source
     * @return  Converted component
     */
    @SneakyThrows
    @NotNull
    public V get(@NonNull K key) {
        if (cache.size() > cacheSize) cache.clear();
        return cache.computeIfAbsent(key, function);
    }
}