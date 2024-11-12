package com.guiyomi;

import javafx.scene.image.Image;
import javafx.scene.media.Media;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class CacheHelper {
    private static final String CACHE_DIR = "cached_profiles";
    public static final String CACHE_DIR_MEDIA = "cached_media";

    public static void createCacheDirIfNotExists() {
        File directory = new File(CACHE_DIR);
        if (!directory.exists()) {
            directory.mkdir();
        }

        File mediaDirectory = new File(CACHE_DIR_MEDIA);
        if (!mediaDirectory.exists()) {
            mediaDirectory.mkdir();
        }
    }

    // Cache sent media files
    public static void cacheFile(File file) {
        File cachedFile = new File(CACHE_DIR_MEDIA + "/" + file.getName());
        if (!cachedFile.exists()) {
            try {
                java.nio.file.Files.copy(file.toPath(), cachedFile.toPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Image getCachedImage(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            // The filename is the last part of the path after "/o/"
            String fileName = path.substring(path.lastIndexOf('/') + 1);

            File cachedImage = new File(CACHE_DIR_MEDIA + "/" + fileName);
            if (cachedImage.exists()) {
                return new Image(cachedImage.toURI().toString());
            } else {
                downloadAndCacheImage(url, fileName);
                return new Image(cachedImage.toURI().toString());
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void downloadAndCacheImage(String url, String fileName) {
        if (url == null || url.isEmpty()) {
            System.out.println("URL is null or empty, cannot download image.");
            return;
        }

        try (InputStream in = new URI(url).toURL().openStream();
             FileOutputStream out = new FileOutputStream(CACHE_DIR_MEDIA + "/" + fileName)) {
    
            byte[] buffer = new byte[1024];
            int bytesRead;
    
            // Read image data from the URL and write it to the file
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
    
            System.out.println("Media file cached locally: " + fileName);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to download or cache media file.");
        }
    }

    public static Media getCachedVideo(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            // The filename is the last part of the path after "/o/"
            String fileName = path.substring(path.lastIndexOf('/') + 1);

            File cachedVideo = new File(CACHE_DIR_MEDIA + "/" + fileName);
            if (cachedVideo.exists()) {
                return new Media(cachedVideo.toURI().toString());
            } else {
                downloadAndCacheVideo(url, fileName);
                return new Media(cachedVideo.toURI().toString());
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void downloadAndCacheVideo(String url, String fileName) {
        if (url == null || url.isEmpty()) {
            System.out.println("URL is null or empty, cannot download video.");
            return;
        }

        try (InputStream in = new URI(url).toURL().openStream();
             FileOutputStream out = new FileOutputStream(CACHE_DIR_MEDIA + "/" + fileName)) {
    
            byte[] buffer = new byte[1024];
            int bytesRead;
    
            // Read video data from the URL and write it to the file
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
    
            System.out.println("Video file cached locally: " + fileName);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to download or cache video file.");
        }
    }

    public static File getCachedAttachment(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            // Extract the filename from the path
            String fileName = path.substring(path.lastIndexOf('/') + 1);

            File cachedAttachment = new File(CACHE_DIR_MEDIA + "/" + fileName);
            if (cachedAttachment.exists()) {
                System.out.println("Attachment loaded from cache: " + fileName);
                return cachedAttachment;
            } else {
                downloadAndCacheAttachment(url, fileName);
                return cachedAttachment;
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void downloadAndCacheAttachment(String url, String fileName) {
        if (url == null || url.isEmpty()) {
            System.out.println("URL is null or empty, cannot download attachment.");
            return;
        }

        try (InputStream in = new URI(url).toURL().openStream();
            FileOutputStream out = new FileOutputStream(CACHE_DIR_MEDIA + "/" + fileName)) {

            byte[] buffer = new byte[1024];
            int bytesRead;

            // Read data from the URL and write it to the file
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }

            System.out.println("Attachment file cached locally: " + fileName);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to download or cache attachment file.");
        }
    }

}
