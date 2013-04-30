package eu.tomylobo.ccnoise.common;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.SoundPool;
import net.minecraft.client.audio.SoundPoolEntry;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ObfuscationReflectionHelper;
import cpw.mods.fml.common.network.Player;
import eu.tomylobo.ccnoise.common.PacketManager.PacketStream;

public class SoundSystemUtils implements PacketManager.PacketHandler {
	private static SoundSystemUtils instance = new SoundSystemUtils();

	private static final byte ID_REMOVE_SOUND = 0;
	static {
		PacketManager.registerPayloadPacket(ID_REMOVE_SOUND, instance);
	}

	@Override
	public void handlePacket(byte type, INetworkManager manager, DataInputStream dis, Player player) throws IOException, Exception {
		switch(type) {
		case ID_REMOVE_SOUND:
			if (FMLCommonHandler.instance().getSide().isServer())
				return;

			removeSound(Packet.readString(dis, 32767));
			return;
		}
	}

	/**
	 * Adds the given buffer as a sound to the sound system.
	 * It can then be played with Minecraft's regular sound playing functions. 
	 *
	 * Removes any previous sounds under the same name.
	 *
	 * @param soundName The sound to remove from the SoundSystem.
	 */
	public static void addSound(String soundName, byte[] data, AudioFormat format) {
		removeSound(soundName);

		final String fakeFileName = String.format("%s.ogg", soundName);
		final SoundPool soundPoolSounds = Minecraft.getMinecraft().sndManager.soundPoolSounds;

		SoundManager.sndSystem.loadSound(data, format, fakeFileName);

		soundPoolSounds.addSound(fakeFileName, (URL) null);
	}

	/**
	 * Removes the specified symbolic sound name from the sound system entirely.
	 *
	 * @param soundName The sound to remove from the SoundSystem.
	 */
	public static void removeSound(String soundName) {
		if (FMLCommonHandler.instance().getSide().isServer()) {
			PacketStream packetStream;
			try {
				packetStream = new PacketManager.PacketStream(ID_REMOVE_SOUND);
				packetStream.writeString(soundName);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		final String fakeFileName = String.format("%s.ogg", soundName);
		final SoundPool soundPoolSounds = Minecraft.getMinecraft().sndManager.soundPoolSounds;

		SoundManager.sndSystem.unloadSound(fakeFileName);

		final Map<String, ? extends List<SoundPoolEntry>> nameToSoundPoolEntriesMapping = ObfuscationReflectionHelper.getPrivateValue(SoundPool.class, soundPoolSounds, "nameToSoundPoolEntriesMapping", "d");

		final List<SoundPoolEntry> allSoundPoolEntries = ObfuscationReflectionHelper.getPrivateValue(SoundPool.class, soundPoolSounds, "allSoundPoolEntries", "e");

		final List<SoundPoolEntry> entries = nameToSoundPoolEntriesMapping.remove(soundName);
		if (entries == null)
			return;

		for (SoundPoolEntry entry : entries) {
			allSoundPoolEntries.remove(entry);
			--soundPoolSounds.numberOfSoundPoolEntries;
		}
	}
}
