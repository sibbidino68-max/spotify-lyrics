# Spotify Lyrics (Fabric, Minecraft 1.21.11)

## Bauen
1. JDK 21 installieren.
2. Auf https://fabricmc.net/develop die Versionen fuer 1.21.11 pruefen und in gradle.properties eintragen.
3. Gradle-Wrapper erzeugen (einmalig, Gradle 8.14+ installiert): `gradle wrapper`
4. `./gradlew build`  (Windows: `gradlew.bat build`)
5. Fertige Datei: build/libs/spotify-lyrics-1.0.0.jar -> in den mods-Ordner (plus Fabric API).

## Spotify einrichten
1. https://developer.spotify.com/dashboard -> App erstellen, Redirect URI exakt: http://127.0.0.1:8888/callback
2. In Minecraft: /lyrics clientid <DEINE_CLIENT_ID>, dann /lyrics login

## Befehle
/lyrics login | clientid <id> | toggle | status | themes | theme <name> | sky <#RRGGBB|off>

## Eigene Designs
config/spotifylyrics/themes/<name>.json kopieren/bearbeiten (Farben, Groesse, Position, Hintergrund, Zeilenanzahl), dann /lyrics theme <name>.

## JAR ohne Installation bauen (GitHub)
1. Kostenloses GitHub-Konto, neues Repository erstellen, den ganzen Projektordner hochladen (inkl. .github).
2. Reiter "Actions" -> "Build JAR" laeuft automatisch (oder "Run workflow").
3. Nach ca. 2-3 Minuten unter "Artifacts" -> spotify-lyrics-jar herunterladen, entpacken, JAR + Fabric API in den mods-Ordner.
Falls der Build wegen der Gradle-Version scheitert: Version in .github/workflows/build.yml an fabricmc.net/develop anpassen.
