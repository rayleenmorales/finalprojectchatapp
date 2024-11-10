package com.guiyomi;

import java.io.File;

public class CacheHelper {
    private static final String CACHE_DIR = "cached_profiles";

    public static void createCacheDirIfNotExists() {
        File directory = new File(CACHE_DIR);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }
}
