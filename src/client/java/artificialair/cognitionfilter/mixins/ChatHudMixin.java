package artificialair.cognitionfilter.mixins;
 
import artificialair.cognitionfilter.CognitionFilterManager;
import net.minecraft.client.gui.hud.ChatHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
 
@Mixin(ChatHud.class)
public class ChatHudMixin {
    @Inject(
        method = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V", 
        at = @At("HEAD")
    )
    private void cognitionfilter_disableDuringRender(CallbackInfo ci) {
        CognitionFilterManager.changeWords = false;
    }
 
    @Inject(
        method = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V", 
        at = @At("RETURN")
    )
    private void cognitionfilter_enableAfterRender(CallbackInfo ci) {
        CognitionFilterManager.changeWords = true;
    }
}
