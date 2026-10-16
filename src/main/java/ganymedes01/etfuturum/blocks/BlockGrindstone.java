package ganymedes01.etfuturum.blocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.EtFuturum;
import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.lib.GUIIDs;
import ganymedes01.etfuturum.lib.RenderIDs;
import ganymedes01.etfuturum.tileentities.TileEntityGrindstone;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.Random;

/**
 * A backport of the modern grindstone. It has no inventory of its own: like vanilla, the two input
 * slots live in the container and are dropped when it is closed, so a tile entity is only used as a
 * render anchor.
 *
 * Metadata layout:
 * bits 0-1: orientation (0 = north/-Z, 1 = east/+X, 2 = south/+Z, 3 = west/-X)
 * bits 2-3: attachment face (0 = floor, 1 = wall, 2 = ceiling)
 */
public class BlockGrindstone extends BlockContainer {

	public static final int FACE_FLOOR = 0;
	public static final int FACE_WALL = 1;
	public static final int FACE_CEILING = 2;

	private IIcon sideIcon;
	private IIcon roundIcon;
	private IIcon pivotIcon;

	public BlockGrindstone() {
		super(Material.rock);
		setStepSound(soundTypeStone);
		setHardness(2.0F);
		setResistance(6.0F);
		setHarvestLevel("pickaxe", 0);
		setBlockName(Utils.getUnlocalisedName("grindstone"));
		setCreativeTab(EtFuturum.creativeTabBlocks);
		setLightOpacity(0);
		useNeighborBrightness = true;
	}

	public static int getFace(int meta) {
		return (meta >> 2) & 3;
	}

	public static int getOrientation(int meta) {
		return meta & 3;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityGrindstone();
	}

	@Override
	public int getRenderType() {
		return RenderIDs.GRINDSTONE;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public Item getItemDropped(int meta, Random random, int fortune) {
		return Item.getItemFromBlock(this);
	}

	@Override
	public int damageDropped(int meta) {
		return 0;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister reg) {
		this.blockIcon = this.sideIcon = reg.registerIcon("minecraft:grindstone_side");
		this.roundIcon = reg.registerIcon("minecraft:grindstone_round");
		this.pivotIcon = reg.registerIcon("minecraft:grindstone_pivot");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIcon(int side, int meta) {
		return sideIcon;
	}

	@SideOnly(Side.CLIENT)
	public IIcon getSideIcon() {
		return sideIcon;
	}

	@SideOnly(Side.CLIENT)
	public IIcon getRoundIcon() {
		return roundIcon;
	}

	@SideOnly(Side.CLIENT)
	public IIcon getPivotIcon() {
		return pivotIcon;
	}

	@Override
	public int onBlockPlaced(World world, int x, int y, int z, int side, float hitX, float hitY, float hitZ, int meta) {
		int face;
		int orientation = 0;
		switch (side) {
			case 0:
				face = FACE_CEILING;
				break;
			case 1:
				face = FACE_FLOOR;
				break;
			default:
				face = FACE_WALL;
				orientation = orientationFromSide(side);
				break;
		}
		return face * 4 + orientation;
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
		int meta = world.getBlockMetadata(x, y, z);
		int face = getFace(meta);
		// Floor and ceiling placements are free to spin around the vertical axis, so face the player.
		if (face == FACE_FLOOR || face == FACE_CEILING) {
			world.setBlockMetadataWithNotify(x, y, z, face * 4 + orientationFromEntity(placer, x, y, z), 2);
		}
	}

	private static int orientationFromSide(int side) {
		switch (side) {
			case 2:
				return 0; // north (-Z)
			case 3:
				return 2; // south (+Z)
			case 4:
				return 3; // west (-X)
			case 5:
				return 1; // east (+X)
			default:
				return 0;
		}
	}

	private static int orientationFromEntity(EntityLivingBase placer, int x, int y, int z) {
		double dx = placer.posX - (x + 0.5D);
		double dz = placer.posZ - (z + 0.5D);
		if (Math.abs(dx) > Math.abs(dz)) {
			return dx > 0 ? 1 : 3;
		}
		return dz > 0 ? 2 : 0;
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float subX, float subY, float subZ) {
		if (!world.isRemote) {
			player.openGui(EtFuturum.instance, GUIIDs.GRINDSTONE, world, x, y, z);
		}
		return true;
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
		if (getFace(world.getBlockMetadata(x, y, z)) == FACE_WALL) {
			setBlockBounds(2 / 16F, 2 / 16F, 0F, 14 / 16F, 14 / 16F, 1F);
		} else {
			setBlockBounds(2 / 16F, 0F, 2 / 16F, 14 / 16F, 1F, 14 / 16F);
		}
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		setBlockBoundsBasedOnState(world, x, y, z);
		return super.getCollisionBoundingBoxFromPool(world, x, y, z);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getSelectedBoundingBoxFromPool(World world, int x, int y, int z) {
		setBlockBoundsBasedOnState(world, x, y, z);
		return super.getSelectedBoundingBoxFromPool(world, x, y, z);
	}
}
