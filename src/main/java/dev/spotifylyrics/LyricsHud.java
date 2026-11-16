package dev.spotifylyrics;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.List;

public class LyricsHud {
    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        Config cfg = LyricsMod.config;
        if (!cfg.enabled || mc.player == null || mc.options.hudHidden) return;
        SpotifyClient sp = LyricsMod.SPOTIFY;
        if (!sp.playing) return;
        Theme th = cfg.activeTheme;
        TextRenderer tr = mc.textRenderer;

        List<SpotifyClient.Line> lines = sp.lyrics;
        long pos = sp.progress();
        int cur = -1;
        for (int i = 0; i < lines.size(); i++) { if (lines.get(i).timeMs() <= pos) cur = i; else break; }

        int cx = ctx.getScaledWindowWidth() / 2 + th.offsetX;   // Fadenkreuz = dorthin, wo du schaust
        int cy = ctx.getScaledWindowHeight() / 2 + th.offsetY;
        int normal = Theme.parse(th.color, 0xFFFFFFFF);
        int active = Theme.parse(th.activeColor, 0xFFFFFF55);
        int bg = Theme.parse(th.background, 0x80000000);

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(cx, cy);
        ctx.getMatrices().scale(th.scale, th.scale);

        int y = 0;
        if (th.showTitle) {
            drawCentered(ctx, tr, sp.artist + " - " + sp.title, y, 0xFF888888, 0, th.shadow);
            y += 12;
        }
        if (lines.isEmpty()) {
            drawCentered(ctx, tr, "(keine synchronisierten Lyrics)", y, normal, bg, th.shadow);
        } else {
            for (int i = cur - th.contextLines; i <= cur + th.contextLines; i++) {
                if (i < 0 || i >= lines.size()) continue;
                String t = lines.get(i).text();
                if (t.isEmpty()) t = "...";
                drawCentered(ctx, tr, t, y, i == cur ? active : normal, bg, th.shadow);
                y += 11;
            }
        }
        ctx.getMatrices().popMatrix();
    }

    private static void drawCentered(DrawContext ctx, TextRenderer tr, String s, int y, int color, int bg, boolean shadow) {
        int w = tr.getWidth(s);
        int x = -w / 2;
        if ((bg >>> 24) != 0) ctx.fill(x - 2, y - 1, x + w + 2, y + 9, bg);
        ctx.drawText(tr, s, x, y, color, shadow);
    }
}
