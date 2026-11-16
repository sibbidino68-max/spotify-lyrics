package dev.spotifylyrics;

public class Theme {
    public String color = "#FFFFFF";
    public String activeColor = "#FFFF55";
    public String background = "#80000000";
    public float scale = 1.5f;
    public int offsetX = 0;
    public int offsetY = 30;
    public int contextLines = 1;
    public boolean shadow = true;
    public boolean showTitle = true;

    public static int parse(String hex, int fallback) {
        try {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            long v = Long.parseLong(h, 16);
            if (h.length() <= 6) v |= 0xFF000000L;
            return (int) v;
        } catch (Exception e) { return fallback; }
    }
}
