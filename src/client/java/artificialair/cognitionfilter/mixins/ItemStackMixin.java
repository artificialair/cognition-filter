package artificialair.cognitionfilter.mixins;

import java.util.List;

import artificialair.cognitionfilter.CognitionFilterManager;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @ModifyReturnValue(
        method = "getTooltip(Lnet/minecraft/item/Item$TooltipContext;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/item/tooltip/TooltipType;)Ljava/util/List;",
        at = @At("RETURN")
    )
    private List<Text> getModifiedTooltip(List<Text> original) {
        return CognitionFilterManager.modifyTooltipLines(original);
    }
}