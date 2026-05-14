package artificialair.cognitionfilter;

import artificialair.cognitionfilter.commands.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class CognitionFilterClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		CognitionFilterConfig.init(FabricLoader.getInstance().getConfigDir());
		CognitionFilterCommand.register();
	}
}