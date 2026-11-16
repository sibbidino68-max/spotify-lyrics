package dev.spotifylyrics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class LyricsMod implements ClientModInitializer {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Config config;
    public static final SpotifyClient SPOTIFY = new SpotifyClient();

    @Override
    public void onInitializeClient() {
        config = Config.load();
        SPOTIFY.start();
        HudRenderCallback.EVENT.register(LyricsHud::render);
        ClientCommandRegistrationCallback.EVENT.register(Commands::register);
    }
}
