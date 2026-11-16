package dev.spotifylyrics;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;

import java.nio.file.*;
import java.util.stream.Stream;

public class Commands {
    static void msg(FabricClientCommandSource s, String m) { s.sendFeedback(Text.literal("[Lyrics] " + m)); }

    public static void register(CommandDispatcher<FabricClientCommandSource> d, CommandRegistryAccess reg) {
        d.register(ClientCommandManager.literal("lyrics")
            .then(ClientCommandManager.literal("login").executes(c -> {
                try { LyricsMod.SPOTIFY.login(); msg(c.getSource(), "Browser oeffnet sich, bitte bei Spotify anmelden."); }
                catch (Exception e) { msg(c.getSource(), "Fehler: " + e.getMessage()); }
                return 1; }))
            .then(ClientCommandManager.literal("clientid")
                .then(ClientCommandManager.argument("id", StringArgumentType.word()).executes(c -> {
                    LyricsMod.config.clientId = StringArgumentType.getString(c, "id");
                    LyricsMod.config.save(); msg(c.getSource(), "Client-ID gespeichert."); return 1; })))
            .then(ClientCommandManager.literal("toggle").executes(c -> {
                LyricsMod.config.enabled = !LyricsMod.config.enabled; LyricsMod.config.save();
                msg(c.getSource(), LyricsMod.config.enabled ? "an" : "aus"); return 1; }))
            .then(ClientCommandManager.literal("status").executes(c -> { msg(c.getSource(), LyricsMod.SPOTIFY.status); return 1; }))
            .then(ClientCommandManager.literal("themes").executes(c -> {
                try (Stream<Path> s = Files.list(Config.themes())) {
                    msg(c.getSource(), String.join(", ", s.map(p -> p.getFileName().toString().replace(".json", "")).toList()));
                } catch (Exception e) { msg(c.getSource(), "Fehler: " + e.getMessage()); }
                return 1; }))
            .then(ClientCommandManager.literal("theme")
                .then(ClientCommandManager.argument("name", StringArgumentType.word()).executes(c -> {
                    String n = StringArgumentType.getString(c, "name");
                    msg(c.getSource(), LyricsMod.config.loadTheme(n) ? "Theme '" + n + "' geladen." : "Theme nicht gefunden."); return 1; })))
            .then(ClientCommandManager.literal("sky")
                .then(ClientCommandManager.argument("hex", StringArgumentType.word()).executes(c -> {
                    String h = StringArgumentType.getString(c, "hex");
                    LyricsMod.config.skyColor = h.equalsIgnoreCase("off") ? "" : h;
                    LyricsMod.config.save();
                    msg(c.getSource(), h.equalsIgnoreCase("off") ? "Himmel: Vanilla" : "Himmel: " + h); return 1; })))
        );
    }
}
