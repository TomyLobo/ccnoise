package eu.tomylobo.ccnoise.common;

import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;
import net.minecraft.tileentity.TileEntity;

public class TileEntitySpeaker extends TileEntity implements IPeripheral {
	private static Object[] wrap(Object... args) {
		return args;
	}

	@Override
	public String getType() {
		return "speaker";
	}

	private static final String[] methodNames = {
		"playSound",
	};

	@Override
	public String[] getMethodNames() {
		return methodNames;
	}

	@Override
	public Object[] callMethod(IComputerAccess computer, int methodIndex, Object[] args) throws Exception {
		switch (methodIndex) {
		case 0:
			String soundName = args[0].toString();
			float volume = ((Number)args[1]).floatValue();
			float pitch = ((Number)args[2]).floatValue();

			this.worldObj.playSoundEffect(this.xCoord + 0.5, this.yCoord + 0.5, this.zCoord + 0.5, soundName, volume, pitch);

			return wrap();
		}
		return wrap();
	}

	@Override
	public boolean canAttachToSide(int paramInt) {
		return true;
	}

	@Override
	public void attach(IComputerAccess paramIComputerAccess) {
	}

	@Override
	public void detach(IComputerAccess paramIComputerAccess) {
	}
}
