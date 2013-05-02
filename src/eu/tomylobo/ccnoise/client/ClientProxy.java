package eu.tomylobo.ccnoise.client;

import cpw.mods.fml.client.FMLClientHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.MinecraftForgeClient;
import eu.tomylobo.ccnoise.common.CommonProxy;

public class ClientProxy extends CommonProxy {
	@Override
	public void init() {
		MinecraftForgeClient.preloadTexture(TEX_BLOCKS);
	}

	@Override
	public void debugPrint(String format, Object... args) {
		super.debugPrint(format, args);

		Minecraft mc = FMLClientHandler.instance().getClient();
		if (mc.ingameGUI != null)
		{
			mc.ingameGUI.getChatGUI().printChatMessage(String.format(format, args));
		}
	}
}
