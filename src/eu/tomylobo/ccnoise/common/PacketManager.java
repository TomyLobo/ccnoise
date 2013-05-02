package eu.tomylobo.ccnoise.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;

public class PacketManager implements IPacketHandler {
	public static interface PacketHandler {
		void handlePacket(byte type, INetworkManager manager, DataInputStream dis, Player player) throws IOException, Exception;
	}

	public static final String CHANNEL_ID = "CCNoise";

	public static class PacketStream extends DataOutputStream {
		public PacketStream(byte type) throws IOException {
			super(new ByteArrayOutputStream());

			writeByte(type);
		}


		public void writeString(String string) throws IOException {
			Packet.writeString(string, this);
		}

		public void writeItemStack(ItemStack stack) throws IOException {
			Packet.writeItemStack(stack, this);
		}


		public byte[] toByteArray() {
			return ((ByteArrayOutputStream)out).toByteArray();
		}

		public Packet250CustomPayload toPacket() {
			return new Packet250CustomPayload(CHANNEL_ID, toByteArray());
		}


		public void sendToServer() {
			PacketDispatcher.sendPacketToServer(toPacket());
		}

		public void sendToAllPlayers() {
			PacketDispatcher.sendPacketToAllPlayers(toPacket());
		}

		public void sendTo(EntityPlayer player) {
			PacketDispatcher.sendPacketToPlayer( toPacket(), (Player) player );
		}

		public void sendToAllInDimension(int dimensionId) {
			//PacketDispatcher.sendPacketToAllInDimension(toPacket(), dimensionId);

			MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
			if (server == null) {
				FMLLog.fine("Attempt to send packet to all in dimension without a server instance available");
				return;
			}

			ServerConfigurationManager r = server.getConfigurationManager();

			@SuppressWarnings("unchecked")
			final List<? extends EntityPlayerMP> playerEntityList = r.playerEntityList;

			for (EntityPlayerMP player : playerEntityList) {
				if (player.worldObj.getWorldInfo().getDimension() != dimensionId)
					continue;

				player.playerNetServerHandler.sendPacketToPlayer(toPacket());
			}
		}

		public void sendToAllAround(double x, double y, double z, double range, int dimensionId) {
			PacketDispatcher.sendPacketToAllAround(x, y, z, range, dimensionId, toPacket());
		}
	}

	public static class TileEntityPacketStream extends PacketStream {
		public TileEntityPacketStream(byte type, TileEntity tileEntity) throws IOException {
			super(type);

			writeInt(tileEntity.worldObj.getWorldInfo().getDimension()); // Not really needed, but let's stick it here for good measure
			writeInt(tileEntity.xCoord);
			writeShort(tileEntity.yCoord);
			writeInt(tileEntity.zCoord);
		}
	}

	@Override
	public void onPacketData(INetworkManager manager, Packet250CustomPayload packet, Player player) {
		final EntityPlayer notchPlayer = (EntityPlayer) player;

		final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(packet.data));

		try {
			final byte type = dis.readByte();
			if (type < 0) {
				final int dimension = dis.readInt(); // Not really needed, but let's stick it here for good measure
				final World world = notchPlayer.worldObj;

				if (world.getWorldInfo().getDimension() != dimension)
					return; // This shouldnt be possible, but there is some bug in mc/forge/mystcraft/whatever

				assert(world.getWorldInfo().getDimension() == dimension);
				final int x = dis.readInt();
				final int y = dis.readShort();
				final int z = dis.readInt();

				final TileEntity te = world.getBlockTileEntity(x, y, z);
				if (te == null)
					return;

				if (!(te instanceof PacketHandler)) {
					System.err.println("CCNoise TileEntity payload package sent to non-PacketHandler TileEntity!");
					return;
				}

				((PacketHandler) te).handlePacket(type, manager, dis, player);
			}
			else {
				final PacketHandler packetHandler = packetHandlers.get(type);
				if (packetHandler == null)
					throw new RuntimeException("Unhandled CCNoise plain payload package!");

				packetHandler.handlePacket(type, manager, dis, player);
			}
		}
		catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private static final Map<Byte, SoundSystemUtils> packetHandlers = new HashMap<Byte, SoundSystemUtils>();
	public static byte registerPayloadPacket(int type, SoundSystemUtils instance) {
		packetHandlers.put((byte) type, instance);

		return (byte) type;
	}
}
