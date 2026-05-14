package artificialair.cognitionfilter.mixins;

import artificialair.cognitionfilter.CognitionFilterManager;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(TextRenderer.class)
public class TextRendererMixin {
    @ModifyVariable(
        method = "prepare(Lnet/minecraft/text/OrderedText;FFIZZI)Lnet/minecraft/client/font/TextRenderer$GlyphDrawable;",
        index = 1, at = @At("HEAD"), argsOnly = true
    )
    private OrderedText modifyOrderedText(OrderedText value) {
        return CognitionFilterManager.modifyText(value);
    }

    @ModifyVariable(
        method = "prepare(Ljava/lang/String;FFIZI)Lnet/minecraft/client/font/TextRenderer$GlyphDrawable;",
        index = 1, at = @At("HEAD"), argsOnly = true
    )
    private String modifyString(String value) {
        return CognitionFilterManager.modifyText(value);
    }

    @ModifyVariable(
        method = "getWidth(Lnet/minecraft/text/OrderedText;)I",
        index = 1, at = @At("HEAD"), argsOnly = true
    )
    private OrderedText modifyWidthOrderedText(OrderedText text) {
        return CognitionFilterManager.modifyText(text);
    }

    @ModifyVariable(
        method = "getWidth(Ljava/lang/String;)I",
        index = 1, at = @At("HEAD"), argsOnly = true
    )
    private String modifyWidthString(String text) {
        return CognitionFilterManager.modifyText(text);
    }

    @ModifyVariable(
        method = "getWidth(Lnet/minecraft/text/StringVisitable;)I",
        index = 1, at = @At("HEAD"), argsOnly = true
    )
    private StringVisitable modifyWidthFormattedText(StringVisitable text) {
        return CognitionFilterManager.modifyText(text);
    }
}