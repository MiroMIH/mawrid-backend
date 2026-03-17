package com.mawrid.common.util;

import java.text.Normalizer;

public final class SlugUtils {

    private SlugUtils() {}

    /**
     * Converts a human-readable name into a URL-safe slug.
     * Strips accents, lowercases, replaces non-alphanumeric sequences with hyphens.
     * Examples: "Machines-outils" → "machines-outils", "Pièces Mécaniques" → "pieces-mecaniques"
     */
    public static String toSlug(String name) {
        // Decompose accented characters (é → e + combining acute)
        String decomposed = Normalizer.normalize(name, Normalizer.Form.NFD);
        // Drop combining diacritics (non-ASCII after decomposition)
        String ascii = decomposed.replaceAll("[^\\p{ASCII}]", "");
        // Lowercase and replace any non-alphanumeric run with a single hyphen
        String slug = ascii.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", ""); // trim leading/trailing hyphens
        return slug.isEmpty() ? "category" : slug;
    }

    /**
     * Ensures the generated slug is unique by appending "-2", "-3", etc.
     * The existence check is provided by the caller via a Predicate.
     */
    public static String toUniqueSlug(String name, java.util.function.Predicate<String> existsCheck) {
        String base = toSlug(name);
        if (!existsCheck.test(base)) return base;
        int suffix = 2;
        while (existsCheck.test(base + "-" + suffix)) {
            suffix++;
        }
        return base + "-" + suffix;
    }
}
