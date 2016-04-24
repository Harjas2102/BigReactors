package erogenousbeef.bigreactors.common.block.base;

import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import erogenousbeef.bigreactors.ModObject;
import erogenousbeef.bigreactors.client.ClientProxy;
import erogenousbeef.bigreactors.client.gui.IResourceTooltipProvider;
import erogenousbeef.bigreactors.common.BRLoader;
import erogenousbeef.bigreactors.common.machine.IoMode;
import erogenousbeef.bigreactors.common.machine.PacketIoMode;
import erogenousbeef.bigreactors.common.machine.PacketItemBuffer;
import erogenousbeef.bigreactors.common.machine.PacketPowerStorage;
import erogenousbeef.bigreactors.common.tileentity.base.TileEntityBase;
import erogenousbeef.bigreactors.common.tileentity.base.TileEntityBasicMachine;
import erogenousbeef.bigreactors.net.PacketHandler;
import erogenousbeef.bigreactors.waila.IWailaInfoProvider;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.List;
import java.util.Random;

public abstract class BlockMachine<T extends TileEntityBasicMachine> extends BlockBR implements IGuiHandler, IResourceTooltipProvider,
        IWailaInfoProvider {

    public static int renderId;

    public IIcon overlayIconPull;
    public IIcon overlayIconPush;
    public IIcon overlayIconPushPull;
    public IIcon overlayIconDisabled;
    public IIcon overlayIconNone;
    public IIcon overlayIconDirty;

    public IIcon selectedFaceIcon;

    @SideOnly(Side.CLIENT)
    protected IIcon[][] iconBuffer;

    protected final Random random;

    protected final ModObject modObject;

    static {
        PacketHandler.INSTANCE.registerMessage(PacketIoMode.class, PacketIoMode.class, PacketHandler.nextID(), Side.SERVER);
        PacketHandler.INSTANCE.registerMessage(PacketItemBuffer.class, PacketItemBuffer.class, PacketHandler.nextID(), Side.SERVER);
        PacketHandler.INSTANCE.registerMessage(PacketPowerStorage.class, PacketPowerStorage.class, PacketHandler.nextID(), Side.CLIENT);
    }

    protected BlockMachine(ModObject mo, Class<T> teClass, Material mat) {
        super(mo.unlocalisedName, teClass, mat);
        modObject = mo;
        setHardness(2.0F);
        setStepSound(soundTypeMetal);
        setHarvestLevel("pickaxe", 0);
        random = new Random();
    }

    protected BlockMachine(ModObject mo, Class<T> teClass) {
        this(mo, teClass, new Material(MapColor.ironColor));
    }

    @Override
    protected void init() {
        GameRegistry.registerBlock(this, modObject.unlocalisedName);
        GameRegistry.registerTileEntity(teClass, modObject.unlocalisedName + "TileEntity");
        BRLoader.guiHandler.registerGuiHandler(getGuiId(), this);
    }

    @Override
    public int getRenderType() {
        return renderId;
    }

    @Override
    public boolean openGui(World world, int x, int y, int z, EntityPlayer entityPlayer, int side) {
        if(!world.isRemote) {
            entityPlayer.openGui(BRLoader.instance, getGuiId(), world, x, y, z);
        }
        return true;
    }

    @Override
    public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int metadata) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iIconRegister) {

        iconBuffer = new IIcon[2][12];
        String side = getSideIconKey(false);
        // first the 6 sides in OFF state
        iconBuffer[0][0] = iIconRegister.registerIcon(getBottomIconKey(false));
        iconBuffer[0][1] = iIconRegister.registerIcon(getTopIconKey(false));
        iconBuffer[0][2] = iIconRegister.registerIcon(getBackIconKey(false));
        iconBuffer[0][3] = iIconRegister.registerIcon(getMachineFrontIconKey(false));
        iconBuffer[0][4] = iIconRegister.registerIcon(side);
        iconBuffer[0][5] = iIconRegister.registerIcon(side);

        side = getSideIconKey(true);
        iconBuffer[0][6] = iIconRegister.registerIcon(getBottomIconKey(true));
        iconBuffer[0][7] = iIconRegister.registerIcon(getTopIconKey(true));
        iconBuffer[0][8] = iIconRegister.registerIcon(getBackIconKey(true));
        iconBuffer[0][9] = iIconRegister.registerIcon(getMachineFrontIconKey(true));
        iconBuffer[0][10] = iIconRegister.registerIcon(side);
        iconBuffer[0][11] = iIconRegister.registerIcon(side);

        iconBuffer[1][0] = iIconRegister.registerIcon(getModelIconKey(false));
        iconBuffer[1][1] = iIconRegister.registerIcon(getModelIconKey(true));

        registerOverlayIcons(iIconRegister);

    }

    @SideOnly(Side.CLIENT)
    protected void registerOverlayIcons(IIconRegister iIconRegister) {
        overlayIconPull = iIconRegister.registerIcon("bigreactors:overlays/pull");
        overlayIconPush = iIconRegister.registerIcon("bigreactors:overlays/push");
        overlayIconPushPull = iIconRegister.registerIcon("bigreactors:overlays/pushPull");
        overlayIconDisabled = iIconRegister.registerIcon("bigreactors:overlays/disabled");
        overlayIconNone = iIconRegister.registerIcon("bigreactors:overlays/none");
        selectedFaceIcon = iIconRegister.registerIcon("bigreactors:overlays/selectedFace");
        overlayIconDirty = iIconRegister.registerIcon("bigreactors:overlays/dirt");
    }

    @SideOnly(Side.CLIENT)
    public IIcon getOverlayIconForMode(T tile, ForgeDirection face, IoMode mode) {
        if(mode == null) {
            return null;
        }
        switch (mode) {
            case DISABLED:
                return overlayIconDisabled;
            case PULL:
                return overlayIconPull;
            case PUSH:
                return overlayIconPush;
            case PUSH_PULL:
                return overlayIconPushPull;
            default:
                return tile.isDirty ? overlayIconDirty : null;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int blockSide) {

        // used to render the block in the world
        TileEntity te = world.getTileEntity(x, y, z);
        int facing = 3;
        if(te instanceof TileEntityBasicMachine) {
            TileEntityBasicMachine me = (TileEntityBasicMachine) te;
            facing = me.facing;
        }
        if(isActive(world, x, y, z)) {
            return iconBuffer[0][ClientProxy.sideAndFacingToSpriteOffset[blockSide][facing] + 6];
        } else {
            return iconBuffer[0][ClientProxy.sideAndFacingToSpriteOffset[blockSide][facing]];
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int blockSide, int blockMeta) {
        // This is used to render the block as an item
        return iconBuffer[0][blockSide];
    }

    public IIcon getModelIcon(IBlockAccess world, int x, int y, int z) {
        return getModelIcon(((TileEntityBasicMachine) world.getTileEntity(x, y, z)).isActive());
    }

    public IIcon getModelIcon() {
        return getModelIcon(false);
    }

    private IIcon getModelIcon(boolean active) {
        return active ? iconBuffer[1][1] : iconBuffer[1][0];
    }

    @Override
    public boolean doNormalDrops(World world, int x, int y, int z) {
        return false;
    }

    @Override
    protected void processDrop(World world, int x, int y, int z, TileEntityBase te, ItemStack stack) {
        if(te != null) {
            ((TileEntityBasicMachine) te).writeToItemStack(stack);
        }
    }

    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack stack) {
        super.onBlockPlacedBy(world, x, y, z, player, stack);
        int heading = MathHelper.floor_double(player.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        TileEntityBasicMachine te = (TileEntityBasicMachine) world.getTileEntity(x, y, z);
            te.setFacing(getFacingForHeading(heading));
            te.readFromItemStack(stack);

        if(world.isRemote) {
            return;
        }
        world.markBlockForUpdate(x, y, z);
    }

    protected short getFacingForHeading(int heading) {
        switch (heading) {
            case 0:
                return 2;
            case 1:
                return 5;
            case 2:
                return 3;
            case 3:
            default:
                return 4;
        }
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        super.onBlockAdded(world, x, y, z);
        world.markBlockForUpdate(x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block blockId) {
        TileEntity ent = world.getTileEntity(x, y, z);
        if(ent instanceof TileEntityBasicMachine) {
            TileEntityBasicMachine te = (TileEntityBasicMachine) ent;
            te.onNeighborBlockChange(blockId);
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(World world, int x, int y, int z, Random rand) {
        // If active, randomly throw some smoke around
        if(isActive(world, x, y, z)) {
            float startX = x + 1.0F;
            float startY = y + 1.0F;
            float startZ = z + 1.0F;
            for (int i = 0; i < 4; i++) {
                float xOffset = -0.2F - rand.nextFloat() * 0.6F;
                float yOffset = -0.1F + rand.nextFloat() * 0.2F;
                float zOffset = -0.2F - rand.nextFloat() * 0.6F;
                world.spawnParticle("smoke", startX + xOffset, startY + yOffset, startZ + zOffset, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    protected abstract int getGuiId();

    protected abstract String getMachineFrontIconKey(boolean active);

    protected String getSideIconKey(boolean active) {
        return "bigreactors:machineSide";
    }

    protected String getBackIconKey(boolean active) {
        return "bigreactors:machineBack";
    }

    protected String getTopIconKey(boolean active) {
        return "bigreactors:machineTop";
    }

    protected String getBottomIconKey(boolean active) {
        return "bigreactors:machineBottom";
    }

    protected String getModelIconKey(boolean active) {
        return getSideIconKey(active);
    }

    protected boolean isActive(IBlockAccess blockAccess, int x, int y, int z) {
        TileEntity te = blockAccess.getTileEntity(x, y, z);
        if(te instanceof TileEntityBasicMachine) {
            return ((TileEntityBasicMachine) te).isActive();
        }
        return false;
    }

    @Override
    public String getUnlocalizedNameForTooltip(ItemStack stack) {
        return getUnlocalizedName();
    }

    @Override
    public void getWailaInfo(List<String> tooltip, EntityPlayer player, World world, int x, int y, int z) {
    }

    @Override
    public int getDefaultDisplayMask(World world, int x, int y, int z) {
        return IWailaInfoProvider.ALL_BITS;
    }
}
