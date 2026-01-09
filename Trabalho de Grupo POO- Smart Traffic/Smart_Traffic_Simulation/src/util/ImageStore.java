package util;

import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple image cache/load helper. Loads from /assets/ in resources.
 */
public class ImageStore {
    private static final Map<String, Image> cache = new HashMap<>();

    public static Image load(String name) {
        if (name == null) return null;
        return cache.computeIfAbsent(name, k -> {
            try {
                String path = "/assets/" + k;
                InputStream is = ImageStore.class.getResourceAsStream(path);
                if (is == null) {
                    System.err.println("Image not found: " + path);
                    return null;
                }
                return new Image(is);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });
    }
}