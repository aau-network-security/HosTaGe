package de.tudarmstadt.informatik.hostage.commons;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Custom LinkedHashMap in order to get any value using an index.
 *
 * @param <K> key
 * @param <V> value
 */
public class MyLinkedMap<K, V> extends LinkedHashMap<K, V>{

    public V getValue(int i) {
        Map.Entry<K, V>entry = this.getEntry(i);
        if(entry == null) return null;

        return entry.getValue();
    }

    public Map.Entry<K, V> getEntry(int i) {
        Set<Entry<K,V>> entries = entrySet();
        int j = 0;

        for(Map.Entry<K, V>entry : entries)
            if(j++ == i)return entry;

        return null;
    }

}