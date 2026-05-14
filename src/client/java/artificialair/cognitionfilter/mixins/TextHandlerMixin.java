package artificialair.cognitionfilter.mixins;

import java.util.List;

import artificialair.cognitionfilter.CognitionFilterManager;
import net.minecraft.client.font.TextHandler;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextHandler.class)
public class TextHandlerMixin {
    @ModifyVariable(
        method = "wrapLines(Lnet/minecraft/text/StringVisitable;ILnet/minecraft/text/Style;Ljava/util/function/BiConsumer;)V",
        at = @At("HEAD"), index = 1, argsOnly = true
    )
    private StringVisitable modifyBeforeWrap(StringVisitable visitable) {
        return CognitionFilterManager.modifyText(visitable);
    }

    @Inject(
        method = "wrapLines(Lnet/minecraft/text/StringVisitable;ILnet/minecraft/text/Style;)Ljava/util/List;",
        at = @At("HEAD")
    )
    private void disableBeforeRewrap(StringVisitable text, int maxWidth, Style style, CallbackInfoReturnable<List<StringVisitable>> cir) {
        CognitionFilterManager.changeWords = false;
    }

    @Inject(
        method = "wrapLines(Lnet/minecraft/text/StringVisitable;ILnet/minecraft/text/Style;)Ljava/util/List;",
        at = @At("RETURN")
    )
    private void enableAfterRewrap(StringVisitable text, int maxWidth, Style style, CallbackInfoReturnable<List<StringVisitable>> cir) {
        CognitionFilterManager.changeWords = true;
    }
}
