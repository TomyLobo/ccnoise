package eu.tomylobo.ccnoise.client;

import net.minecraftforge.client.MinecraftForgeClient;
import eu.tomylobo.ccnoise.common.CommonProxy;

public class ClientProxy extends CommonProxy {
	@Override
	public void init() {
		MinecraftForgeClient.preloadTexture(TEX_BLOCKS);
	}
}
