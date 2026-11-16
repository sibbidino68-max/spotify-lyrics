package dev.spotifylyrics;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** EXPERIMENTELL: ueberschreibt die Himmelsfarbe. Falls das Spiel hier abstuerzt: SkyMixin aus spotifylyrics.mixins.json entfernen. */
@Mixin(ClientWorld.class)
public class SkyMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void spotifylyrics$sky(Vec3d cameraPos, float tickDelta, CallbackInfoReturnable<Integer> cir) {
        String c = LyricsMod.config == null ? "" : LyricsMod.config.skyColor;
        if (c != null && !c.isBlank()) cir.setReturnValue(Theme.parse(c, 0xFF78A7FF) & 0xFFFFFF);
    }
}
