package io.github.sammers21.tg.cleaner;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class CleanConfig {

    private Set<Long> ignoredPacks = new ConcurrentSkipListSet<>();
    private Map<Long, Set<String>> ignoredSTickers = new ConcurrentHashMap<>();

    public void ignoreSticker(Long packId, String emoji) {
        ignoredSTickers.compute(packId, (pack, strings) -> {
            if (strings == null) {
                HashSet<String> set = new HashSet<>();
                set.add(emoji);
                return set;
            } else {
                strings.add(emoji);
                return strings;
            }
        });
    }

    public void ignorePack(Long packId) {
        ignoredPacks.add(packId);
    }

    public boolean isStickerIgnored(long packId, String emoji) {
        if (ignoredPacks.contains(packId)) {
            return true;
        }
        if (ignoredSTickers.containsKey(packId)) {
            return ignoredSTickers.get(packId).contains(emoji);
        } else {
            return false;
        }
    }
}
