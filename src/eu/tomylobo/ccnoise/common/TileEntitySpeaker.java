package eu.tomylobo.ccnoise.common;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFormat;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.Player;
import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;
import eu.tomylobo.ccnoise.CCNoise;
import eu.tomylobo.ccnoise.common.PacketManager.TileEntityPacketStream;
import eu.tomylobo.expression.Expression;
import eu.tomylobo.expression.ExpressionException;
import eu.tomylobo.expression.runtime.EvaluationException;
import eu.tomylobo.expression.runtime.LValue;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.tileentity.TileEntity;

public class TileEntitySpeaker extends TileEntity implements IPeripheral, PacketManager.PacketHandler {
	public static class SoundDescriptor {
		public final String soundName;
		public final String expression;
		public final double length;

		public SoundDescriptor(String soundName, String expression, double length) {
			this.soundName = soundName;
			this.expression = expression;
			this.length = length;
		}

		public PacketManager.PacketStream toPacketStream(TileEntity tileEntity) throws IOException {
			final PacketManager.PacketStream ps = new PacketManager.TileEntityPacketStream(ID_GENERATE_FUNCTIONAL, tileEntity);

			writeToStream(ps);

			return ps;
		}

		public void writeToStream(final PacketManager.PacketStream ps) throws IOException {
			ps.writeDouble(length);
			ps.writeString(soundName);
			ps.writeString(expression);
		}
	}

	private static int lastId;
	private final int id;

	public TileEntitySpeaker() {
		this.id = ++lastId;
	}


	@Override
	public String getType() {
		return "speaker";
	}

	private static final String[] methodNames = {
		"playSound",
		"eval",
		"generateFunctional",
	};
	public static final double SAMPLE_RATE = 44100;
	private static final byte ID_GENERATE_FUNCTIONAL = (byte) -1;
	private static final byte ID_MULTIPACKET = (byte) -2;

	@Override
	public String[] getMethodNames() {
		return methodNames;
	}

	@Override
	public Object[] callMethod(IComputerAccess computer, int methodIndex, Object[] args) throws Exception {
		switch (methodIndex) {
		case 0: { // playSound
			final String soundName = mapSoundName(args[0].toString(), computer.getID(), CCNoise.Config.allowPlayRegularSounds);
			final float volume = ((Number) args[1]).floatValue();
			final float pitch = ((Number) args[2]).floatValue();

			this.worldObj.playSoundEffect(this.xCoord + 0.5, this.yCoord + 0.5, this.zCoord + 0.5, soundName, volume, pitch);

			return wrap();
		}

		case 1: { // eval
			final String expression = args[0].toString();

			final Expression compiled = Expression.compile(expression);

			return wrap(compiled.evaluate());
		}

		case 2: { // generateFunctional
			if (!CCNoise.Config.allowGenerateSounds)
				throw new Exception("Generating sounds is disabled.");

			final String soundName = mapSoundName(args[0].toString(), computer.getID(), CCNoise.Config.allowGenerateGlobalSounds);
			final String expression = args[1].toString();
			final double length = ((Number) args[2]).doubleValue();

			// Make sure the expression compiles
			compileFunctional(expression);

			// Register sound for cleanup in detach/chunkUnload
			final SoundDescriptor soundDescriptor = new SoundDescriptor(soundName, expression, length);
			computers.get(computer.getID()).put(soundName, soundDescriptor);

			// Send it to the clients
			final PacketManager.PacketStream ps = soundDescriptor.toPacketStream(this);
			final int dimension = this.worldObj.getWorldInfo().getDimension();
			ps.sendToAllInDimension(dimension); // TODO: restrict range

			return wrap();
		}
		}

		return wrap();
	}

	@Override
	public boolean canAttachToSide(int paramInt) {
		return true;
	}

	private final Map<Integer, Map<String, SoundDescriptor>> computers = new HashMap<Integer, Map<String, SoundDescriptor>>();
	@Override
	public void attach(IComputerAccess computer) {
		computers.put(computer.getID(), new HashMap<String, SoundDescriptor>());
	}

	@Override
	public void detach(IComputerAccess computer) {
		//TODO: In SMP, this isn't working. Sound continues to be loaded after a computer reboot
		for (String soundName : computers.remove(computer.getID()).keySet()) {
			SoundSystemUtils.removeSound(soundName);
		}
	}

	@Override
	public void onChunkUnload() {
		for (Map<String, SoundDescriptor> entry : computers.values()) {
			for (String soundName : entry.keySet()) {
				SoundSystemUtils.removeSound(soundName);
			}
		}
	}

	public void sendToPlayer(EntityPlayer player) throws IOException {
		for (Map<String, SoundDescriptor> entry : computers.values()) {
			for (SoundDescriptor soundDescriptor : entry.values()) {
				soundDescriptor.toPacketStream(this).sendTo(player);
			}
		}
	}

	@Override
	public Packet getDescriptionPacket() {
		try {
			final TileEntityPacketStream ps = new PacketManager.TileEntityPacketStream(ID_MULTIPACKET, this);
			for (Map<String, SoundDescriptor> entry : computers.values()) {
				for (SoundDescriptor soundDescriptor : entry.values()) {
					ps.writeBoolean(true);
					ps.writeByte(ID_GENERATE_FUNCTIONAL);
					soundDescriptor.writeToStream(ps);
				}
			}
			ps.writeBoolean(false);
			return ps.toPacket();
		}
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void handlePacket(byte type, INetworkManager manager, DataInputStream dis, Player player) throws Exception {
		switch (type) {
		case ID_GENERATE_FUNCTIONAL:
			if (FMLCommonHandler.instance().getSide().isServer())
				return;

			final double length = dis.readDouble();
			final String soundName = Packet.readString(dis, 32767);
			final String expression = Packet.readString(dis, 32767);

			generateFunctional(soundName, expression, length);
			return;

		case ID_MULTIPACKET:
			while (dis.readBoolean()) {
				handlePacket(dis.readByte(), manager, dis, player);
			}
			return;
		}
	}

	public void generateFunctional(String soundName, String expression, double length) throws Exception {
		final Expression compiled = compileFunctional(expression);
		((LValue) compiled.getVariable("length", true)).assign(length);

		final int samples = (int) (SAMPLE_RATE * length);

		final byte[] data = new byte[samples*2];
		for (int i = 0; i < samples; ++i) {
			final double t = i / SAMPLE_RATE;
			final double y = Math.max(-1, Math.min(1, compiled.evaluate(t)));
			final short sample = (short) (y * Short.MAX_VALUE);

			// Write sample in little-endian order, as that's the only order SoundSystem supports
			data[i * 2    ] = (byte) ((sample >>> 0) & 0xFF);
			data[i * 2 + 1] = (byte) ((sample >>> 8) & 0xFF);
		}

		final AudioFormat format = new AudioFormat((float) SAMPLE_RATE, 16, 1, true, false);
		SoundSystemUtils.addSound(soundName, data, format);


		/*SoundManager.sndSystem.quickPlay(
				true, identifier, false,
				this.xCoord, this.yCoord, this.zCoord,
				paulscode.sound.SoundSystemConfig.ATTENUATION_NONE, 0
		);*/
	}


	public Expression compileFunctional(String expression) throws ExpressionException, EvaluationException {
		final Expression compiled = Expression.compile(expression, "t", "length");
		compiled.optimize();
		return compiled;
	}

	public String mapSoundName(String soundName, int computerId, boolean allowGlobalSounds) throws Exception {
		if (soundName.charAt(0) == '#') {
			return String.format("%d_%d_%s", computerId, id, soundName.substring(1));
		}

		if (!allowGlobalSounds)
			throw new Exception("Global scope sounds not allowed. Prefix # for local sounds.");

		return soundName;
	}

	private static Object[] wrap(Object... args) {
		return args;
	}
}
