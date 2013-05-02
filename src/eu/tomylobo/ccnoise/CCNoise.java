package eu.tomylobo.ccnoise;

import eu.tomylobo.ccnoise.common.CommonProxy;
import eu.tomylobo.ccnoise.common.BlockSpeaker;
import eu.tomylobo.ccnoise.common.PacketManager;
import eu.tomylobo.ccnoise.common.SoundSystemUtils;
import eu.tomylobo.ccnoise.common.TileEntitySpeaker;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.Configuration;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import dan200.computer.api.ComputerCraftAPI;

@Mod(
	modid = "CCNoise",
	name = "CCNoise",
	version = "0.0.4b",
	dependencies = "required-after:ComputerCraft;after:CCTurtle"
)
@NetworkMod(
	clientSideRequired = true,
	serverSideRequired = false,
	channels = PacketManager.CHANNEL_ID,
	packetHandler = PacketManager.class
)
public class CCNoise {
	public static class Blocks {
		public static BlockSpeaker speakerBlock;
	}

	public static class Config {
		public static int speakerBlockID = 1310;
		public static boolean allowPlayRegularSounds = true;
		public static boolean allowGenerateSounds = true;
		public static boolean allowGenerateGlobalSounds = true;
	}

	@Mod.Instance("CCNoise")
	public static CCNoise instance;

	@SidedProxy(
		clientSide = "eu.tomylobo.ccnoise.client.ClientProxy",
		serverSide = "eu.tomylobo.ccnoise.common.CommonProxy"
	)
	public static CommonProxy proxy;

	public static CreativeTabs creativeTab;

	@Mod.Init
	public void init(FMLInitializationEvent evt) {
		/*OCSLog.init();

		OCSLog.info( "CCNoise version %s starting", FMLCommonHandler.instance().findContainerFor(instance).getVersion() );*/

		creativeTab = ComputerCraftAPI.getCreativeTab();

		Blocks.speakerBlock = new BlockSpeaker(Config.speakerBlockID);

		GameRegistry.registerBlock(Blocks.speakerBlock, "ccnoise.speaker");

		LanguageRegistry.addName(Blocks.speakerBlock, "Speaker");

		GameRegistry.registerTileEntity(TileEntitySpeaker.class, "ccnoise.tile.speaker");

		GameRegistry.addRecipe(new ItemStack(Blocks.speakerBlock), 
				new String[] { "WNW", "WIW", "SRS" }, 
				'W', Block.planks,
				'N', Block.music,
				'I', Item.ingotIron,
				'S', Item.stick,
				'R', Item.redstone
		);

		proxy.init();
	}

	@Mod.PreInit
	public void preInit( FMLPreInitializationEvent evt ) {
		Configuration configFile = new Configuration(evt.getSuggestedConfigurationFile());

		Config.speakerBlockID = configFile.getBlock("speakerBlockID", Config.speakerBlockID, "The block ID for the speaker block").getInt();
		Config.allowPlayRegularSounds = configFile.get("speaker", "allowPlayRegularSounds", Config.allowPlayRegularSounds, "Allow speaker.playSound to play regular sounds.").getBoolean(Config.allowPlayRegularSounds);
		Config.allowGenerateSounds = configFile.get("speaker", "allowGenerateSounds", Config.allowGenerateSounds, "Enable the speaker.generate* functions.").getBoolean(Config.allowGenerateSounds);
		Config.allowGenerateGlobalSounds = configFile.get("speaker", "allowGenerateGlobalSounds", Config.allowGenerateGlobalSounds, "Allow the speaker.generate* functions to generate globally accessible sounds and potentially override existing sounds.").getBoolean(Config.allowGenerateGlobalSounds);

		configFile.save();
	}

	public static SoundSystemUtils ignore1 = new SoundSystemUtils();
}
