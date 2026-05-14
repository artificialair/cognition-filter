package artificialair.cognitionfilter.commands;
 
import com.mojang.brigadier.context.CommandContext;

import artificialair.cognitionfilter.CognitionFilterConfig;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.text.Text;
 
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
 
public class CognitionFilterCommand {
 
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(
                literal("cognitionfilter")
                    .then(literal("reload")
                        .executes(CognitionFilterCommand::executeReload)
                    )
            )
        );
    }
 
    private static int executeReload(CommandContext<FabricClientCommandSource> ctx) {
        CognitionFilterConfig.load();
        int count = CognitionFilterConfig.getRules().size();
        ctx.getSource().sendFeedback(
            Text.literal("§c[§eCognitionFilter§c] §freloaded §b" + count + "§f rule(s) from config.")
        );
        return 1;
    }
}
 