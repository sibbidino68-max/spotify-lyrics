package dev.spotifylyrics;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.util.Util;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

public class SpotifyClient {
    public record Line(long timeMs, String text) {}

    static final String REDIRECT = "http://127.0.0.1:8888/callback";
    static final String SCOPE = "user-read-currently-playing user-read-playback-state";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ScheduledExecutorService ex = Executors.newScheduledThreadPool(2, r -> { Thread t = new Thread(r, "spotifylyrics"); t.setDaemon(true); return t; });

    private volatile String accessToken, refreshToken, verifier;
    private volatile long expiresAt;

    public volatile String title = "", artist = "", trackId = "";
    public volatile boolean playing;
    public volatile List<Line> lyrics = List.of();
    public volatile String status = "Nicht verbunden. Nutze /lyrics login";
    private volatile long progressMs, fetchedAt;

    public long progress() { return playing ? progressMs + (System.currentTimeMillis() - fetchedAt) : progressMs; }

    public void start() {
        try {
            Path p = Config.dir().resolve("token.json");
            if (Files.exists(p)) refreshToken = JsonParser.parseString(Files.readString(p)).getAsJsonObject().get("refresh").getAsString();
        } catch (Exception ignored) {}
        ex.scheduleWithFixedDelay(this::poll, 2, 2, TimeUnit.SECONDS);
    }

    // ---------- Login (PKCE) ----------
    public void login() throws Exception {
        String clientId = LyricsMod.config.clientId;
        if (clientId.isBlank()) throw new IllegalStateException("Setze zuerst /lyrics clientid <ID>");
        byte[] rnd = new byte[48]; new SecureRandom().nextBytes(rnd);
        verifier = Base64.getUrlEncoder().withoutPadding().encodeToString(rnd);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8888), 0);
        server.createContext("/callback", h -> {
            String q = h.getRequestURI().getQuery();
            String code = null;
            if (q != null) for (String kv : q.split("&")) if (kv.startsWith("code=")) code = URLDecoder.decode(kv.substring(5), StandardCharsets.UTF_8);
            byte[] msg = "Fertig! Du kannst dieses Fenster schliessen und zu Minecraft zurueck.".getBytes(StandardCharsets.UTF_8);
            h.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            h.sendResponseHeaders(200, msg.length);
            h.getResponseBody().write(msg); h.close();
            final String c = code;
            if (c != null) ex.execute(() -> exchange(c));
            server.stop(1);
        });
        server.start();
        String url = "https://accounts.spotify.com/authorize?response_type=code&client_id=" + enc(clientId)
                + "&scope=" + enc(SCOPE) + "&redirect_uri=" + enc(REDIRECT)
                + "&code_challenge_method=S256&code_challenge=" + challenge;
        Util.getOperatingSystem().open(URI.create(url));
        status = "Warte auf Spotify-Login im Browser...";
    }

    private void exchange(String code) {
        tokenRequest("grant_type=authorization_code&code=" + enc(code) + "&redirect_uri=" + enc(REDIRECT)
                + "&client_id=" + enc(LyricsMod.config.clientId) + "&code_verifier=" + enc(verifier));
    }

    private void refresh() {
        if (refreshToken == null) return;
        tokenRequest("grant_type=refresh_token&refresh_token=" + enc(refreshToken) + "&client_id=" + enc(LyricsMod.config.clientId));
    }

    private void tokenRequest(String body) {
        try {
            HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create("https://accounts.spotify.com/api/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
            JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
            if (!o.has("access_token")) { status = "Login fehlgeschlagen: " + r.body(); return; }
            accessToken = o.get("access_token").getAsString();
            expiresAt = System.currentTimeMillis() + o.get("expires_in").getAsLong() * 1000 - 30_000;
            if (o.has("refresh_token")) {
                refreshToken = o.get("refresh_token").getAsString();
                JsonObject s = new JsonObject(); s.addProperty("refresh", refreshToken);
                Files.createDirectories(Config.dir());
                Files.writeString(Config.dir().resolve("token.json"), s.toString());
            }
            status = "Verbunden";
        } catch (Exception e) { status = "Fehler: " + e.getMessage(); }
    }

    // ---------- Polling ----------
    private void poll() {
        try {
            if (!LyricsMod.config.enabled) return;
            if (accessToken == null || System.currentTimeMillis() > expiresAt) { refresh(); if (accessToken == null) return; }
            HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create("https://api.spotify.com/v1/me/player/currently-playing"))
                    .header("Authorization", "Bearer " + accessToken).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() == 401) { expiresAt = 0; return; }
            if (r.statusCode() != 200) { playing = false; return; }
            JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
            if (!o.has("item") || o.get("item").isJsonNull()) { playing = false; return; }
            JsonObject item = o.getAsJsonObject("item");
            progressMs = o.get("progress_ms").getAsLong();
            fetchedAt = System.currentTimeMillis();
            playing = o.get("is_playing").getAsBoolean();
            String id = item.get("id").getAsString();
            if (!id.equals(trackId)) {
                trackId = id;
                title = item.get("name").getAsString();
                artist = item.getAsJsonArray("artists").get(0).getAsJsonObject().get("name").getAsString();
                lyrics = List.of();
                long dur = item.get("duration_ms").getAsLong() / 1000;
                ex.execute(() -> fetchLyrics(id, title, artist, dur));
            }
        } catch (Exception e) { status = "Fehler: " + e.getMessage(); }
    }

    // ---------- Lyrics (LRCLIB) ----------
    private void fetchLyrics(String id, String t, String a, long dur) {
        try {
            String url = "https://lrclib.net/api/get?track_name=" + enc(t) + "&artist_name=" + enc(a) + "&duration=" + dur;
            HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(url)).header("User-Agent", "SpotifyLyricsMod/1.0").GET().build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) return;
            JsonObject o = JsonParser.parseString(r.body()).getAsJsonObject();
            if (!o.has("syncedLyrics") || o.get("syncedLyrics").isJsonNull()) return;
            List<Line> out = parseLrc(o.get("syncedLyrics").getAsString());
            if (id.equals(trackId)) lyrics = out;
        } catch (Exception ignored) {}
    }

    static final Pattern TS = Pattern.compile("\\[(\\d+):(\\d+)(?:[.:](\\d+))?]");

    static List<Line> parseLrc(String lrc) {
        List<Line> out = new ArrayList<>();
        for (String line : lrc.split("\n")) {
            int end = line.lastIndexOf(']');
            if (end < 0) continue;
            String text = line.substring(end + 1).trim();
            Matcher m = TS.matcher(line);
            while (m.find()) {
                long ms = Long.parseLong(m.group(1)) * 60_000 + Long.parseLong(m.group(2)) * 1000;
                if (m.group(3) != null) { String f = (m.group(3) + "00").substring(0, 3); ms += Long.parseLong(f); }
                out.add(new Line(ms, text));
            }
        }
        out.sort(Comparator.comparingLong(Line::timeMs));
        return out;
    }

    static String enc(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8); }
}
