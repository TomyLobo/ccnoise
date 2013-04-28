package dan200.computer.api;

import net.minecraft.item.ItemStack;

public abstract interface IMedia
{
	public abstract String getLabel(ItemStack paramItemStack);

	public abstract boolean setLabel(ItemStack paramItemStack, String paramString);

	public abstract String getAudioTitle(ItemStack paramItemStack);

	public abstract String getAudioRecordName(ItemStack paramItemStack);

	public abstract String mountData(ItemStack paramItemStack, IComputerAccess paramIComputerAccess);
}
