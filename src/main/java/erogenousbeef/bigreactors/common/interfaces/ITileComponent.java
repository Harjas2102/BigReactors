package erogenousbeef.bigreactors.common.interfaces;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;

public interface ITileComponent
{
    public void tick();

    public void read(NBTTagCompound nbtTags);

    public void read(ByteBuf dataStream);

    public void write(NBTTagCompound nbtTags);

    public void write(ArrayList data);
}