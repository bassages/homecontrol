package nl.homeserver.cache;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    private final CacheManager cacheManager;

    public CacheService(final CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void clearAll() {
        cacheManager.getCacheNames().forEach(this::clear);
    }

    public void clear(final String nameOfCacheToClear) {
        cacheManager.getCache(nameOfCacheToClear).clear();
    }
}