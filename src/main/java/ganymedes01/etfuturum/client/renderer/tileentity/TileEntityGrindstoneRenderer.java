package ganymedes01.etfuturum.client.renderer.tileentity;

import ganymedes01.etfuturum.blocks.BlockGrindstone;
import ganymedes01.etfuturum.client.renderer.block.BlockGrindstoneRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

public class TileEntityGrindstoneRenderer extends TileEntitySpecialRenderer {

	private final RenderBlocks renderer = new RenderBlocks();

	@Override
	public void renderTileEntityAt(TileEntity tile, double x, double y, double z, float partialTicks) {
		World world = tile.getWorldObj();
		if (world == null) {
			return;
		}
		int blockX = tile.xCoord;
		int blockY = tile.yCoord;
		int blockZ = tile.zCoord;
		if (!(world.getBlock(blockX, blockY, blockZ) instanceof BlockGrindstone block)) {
			return;
		}
		int meta = world.getBlockMetadata(blockX, blockY, blockZ);
		int brightness = block.getMixedBrightnessForBlock(world, blockX, blockY, blockZ);

		bindTexture(TextureMap.locationBlocksTexture);
		renderer.blockAccess = world;
		renderer.renderAllFaces = true;

		GL11.glPushMatrix();
		GL11.glTranslated(x + 0.5D, y + 0.5D, z + 0.5D);
		applyOrientation(meta);
		GL11.glTranslated(-0.5D, -0.5D, -0.5D);

		Tessellator tessellator = Tessellator.instance;
		tessellator.startDrawingQuads();
		BlockGrindstoneRenderer.renderModel(tessellator, renderer, block, 0, 0, 0, brightness);
		tessellator.draw();
		tessellator.setColorOpaque_F(1F, 1F, 1F);

		GL11.glPopMatrix();
	}

	/**
	 * Rotates the un-rotated (floor, facing north) model into the orientation stored in the block metadata.
	 */
	public static void applyOrientation(int meta) {
		int face = BlockGrindstone.getFace(meta);
		int orientation = BlockGrindstone.getOrientation(meta);
		switch (face) {
			case BlockGrindstone.FACE_WALL:
				switch (orientation) {
					case 0:
						GL11.glRotatef(-90F, 1F, 0F, 0F); // attached to the north (-Z) side
						break;
					case 1:
						GL11.glRotatef(-90F, 0F, 0F, 1F); // attached to the east (+X) side
						break;
					case 2:
						GL11.glRotatef(90F, 1F, 0F, 0F); // attached to the south (+Z) side
						break;
					case 3:
						GL11.glRotatef(90F, 0F, 0F, 1F); // attached to the west (-X) side
						break;
				}
				break;
			case BlockGrindstone.FACE_CEILING:
				GL11.glRotatef((2 - orientation) * 90F, 0F, 1F, 0F);
				GL11.glRotatef(180F, 1F, 0F, 0F);
				break;
			case BlockGrindstone.FACE_FLOOR:
			default:
				GL11.glRotatef((2 - orientation) * 90F, 0F, 1F, 0F);
				break;
		}
	}
}
