package dev.spotifylyrics;

import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;

public class Config {
    public String clientId = "";
    public boolean enabled = true;
    public String theme = "neon";
    /** Himmelsfarbe als Hex (#RRGGBB) oder leer = Vanilla */
    public String skyColor = "";

    public transient Theme activeTheme = new Theme();

    static Path dir() { return FabricLoader.getInstance().getConfigDir().resolve("spotifylyrics"); }
    static Path file() { return dir().resolve("config.json"); }
    static Path themes() { return dir().resolve("themes"); }

    public static Config load() {
        Config c = new Config();
        try {
            Files.createDirectories(themes());
            writeDefault("neon.json", "{\"color\":\"#B388FF\",\"activeColor\":\"#00FFE5\",\"background\":\"#99000000\",\"scale\":1.6,\"offsetX\":0,\"offsetY\":40,\"contextLines\":2,\"shadow\":true,\"showTitle\":true}");
            writeDefault("minimal.json", "{\"color\":\"#AAAAAA\",\"activeColor\":\"#FFFFFF\",\"background\":\"#00000000\",\"scale\":1.0,\"offsetX\":0,\"offsetY\":20,\"contextLines\":0,\"shadow\":true,\"showTitle\":false}");
            if (Files.exists(file())) c = LyricsMod.GSON.fromJson(Files.readString(file()), Config.class);
            else Files.writeString(file(), LyricsMod.GSON.toJson(c));
        } catch (Exception e) { e.printStackTrace(); }
        c.loadTheme(c.theme);
        return c;
    }

    static void writeDefault(String name, String json) throws Exception {
        Path p = themes().resolve(name);
        if (!Files.exists(p)) Files.writeString(p, json);
    }

    public boolean loadTheme(String name) {
        try {
            Path p = themes().resolve(name + ".json");
            if (!Files.exists(p)) return false;
            activeTheme = LyricsMod.GSON.fromJson(Files.readString(p), Theme.class);
            theme = name;
            save();
            return true;
        } catch (Exception e) { e.printStackTrace(); return false; }
    }

    public void save() {
        try { Files.writeString(file(), LyricsMod.GSON.toJson(this)); } catch (Exception e) { e.printStackTrace(); }
    }
}
