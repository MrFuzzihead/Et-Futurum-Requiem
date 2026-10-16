package ganymedes01.etfuturum.client.renderer.block;

import com.gtnewhorizons.angelica.api.ThreadSafeISBRH;
import ganymedes01.etfuturum.blocks.BlockGrindstone;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

/**
 * Geometry is taken 1:1 from the modern {@code block/grindstone} model. The world model is drawn by
 * {@link ganymedes01.etfuturum.client.renderer.tileentity.TileEntityGrindstoneRenderer} so that all
 * 12 orientations are simple GL rotations; this renderer only exists for the inventory/held form.
 */
@ThreadSafeISBRH(perThread = false)
public class BlockGrindstoneRenderer extends BlockModelBase {

	// x1, y1, z1, x2, y2, z2 in 1/16 of a block, matching the vanilla model elements.
	private static final double[][] BOXES = {
			{12, 0, 6, 14, 7, 10},   // leg (right)
			{2, 0, 6, 4, 7, 10},     // leg (left)
			{12, 7, 5, 14, 13, 11},  // pivot (right)
			{2, 7, 5, 4, 13, 11},    // pivot (left)
			{4, 4, 2, 12, 16, 14},   // wheel
	};

	// box, face (0 down, 1 up, 2 north, 3 south, 4 west, 5 east), icon (0 leg, 1 round, 2 side, 3 pivot), u1, v1, u2, v2
	private static final int[][] FACES = {
			{0, 2, 0, 2, 9, 4, 16},
			{0, 5, 0, 10, 16, 6, 9},
			{0, 3, 0, 12, 9, 14, 16},
			{0, 4, 0, 6, 9, 10, 16},
			{0, 0, 0, 12, 6, 14, 10},

			{1, 2, 0, 12, 9, 14, 16},
			{1, 5, 0, 10, 16, 6, 9},
			{1, 3, 0, 2, 9, 4, 16},
			{1, 4, 0, 6, 9, 10, 16},
			{1, 0, 0, 2, 6, 4, 10},

			{2, 2, 3, 6, 0, 8, 6},
			{2, 5, 3, 0, 0, 6, 6},
			{2, 3, 3, 6, 0, 8, 6},
			{2, 1, 3, 8, 0, 10, 6},
			{2, 0, 3, 8, 0, 10, 6},

			{3, 2, 3, 6, 0, 8, 6},
			{3, 3, 3, 6, 0, 8, 6},
			{3, 4, 3, 0, 0, 6, 6},
			{3, 1, 3, 8, 0, 10, 6},
			{3, 0, 3, 8, 0, 10, 6},

			{4, 2, 1, 0, 0, 8, 12},
			{4, 5, 2, 0, 0, 12, 12},
			{4, 3, 1, 0, 0, 8, 12},
			{4, 4, 2, 0, 0, 12, 12},
			{4, 1, 1, 0, 0, 8, 12},
			{4, 0, 1, 0, 0, 8, 12},
	};

	private static final float[] FACE_SHADE = {0.5F, 1.0F, 0.8F, 0.8F, 0.6F, 0.6F};

	public BlockGrindstoneRenderer(int modelID) {
		super(modelID);
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
		// Drawn by the tile entity renderer so it can be rotated into every orientation.
		return true;
	}

	@Override
	protected void renderInventoryModel(Block block, int meta, int modelId, RenderBlocks renderer, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		Tessellator tessellator = Tessellator.instance;
		renderModel(tessellator, renderer, block, 0, 0, 0, 0xF000F0);
		tessellator.setColorOpaque_F(1F, 1F, 1F);
	}

	/**
	 * Draws the model in block-local coordinates (0-1). The caller is responsible for positioning it
	 * and for passing the light value of the block it is drawn at.
	 */
	public static void renderModel(Tessellator tessellator, RenderBlocks renderer, Block block, double x, double y, double z, int brightness) {
		BlockGrindstone grindstone = (BlockGrindstone) block;
		IIcon[] icons = {
				Blocks.log2.getIcon(2, 1), // dark oak log, used by the vanilla model for the legs
				grindstone.getRoundIcon(),
				grindstone.getSideIcon(),
				grindstone.getPivotIcon(),
		};

		for (int[] face : FACES) {
			double[] box = BOXES[face[0]];
			renderer.setRenderBounds(box[0] / 16.0D, box[1] / 16.0D, box[2] / 16.0D, box[3] / 16.0D, box[4] / 16.0D, box[5] / 16.0D);
			tessellator.setBrightness(brightness);
			float shade = FACE_SHADE[face[1]];
			tessellator.setColorOpaque_F(shade, shade, shade);

			IIcon icon = new SubIcon(icons[face[2]], face[3], face[4], face[5], face[6]);
			// RenderBlocks' face methods do not emit normals themselves; without one the fixed-function
			// lighting pipeline falls back to whatever normal was last left in GL state (for example by
			// the item rendered in the player's hand). That makes the inventory icon arbitrarily dark
			// depending on what is being held, so mirror renderStandardInventoryCube and set it here.
			switch (face[1]) {
				case 0:
					tessellator.setNormal(0F, -1F, 0F);
					renderer.renderFaceYNeg(block, x, y, z, icon);
					break;
				case 1:
					tessellator.setNormal(0F, 1F, 0F);
					renderer.renderFaceYPos(block, x, y, z, icon);
					break;
				case 2:
					tessellator.setNormal(0F, 0F, -1F);
					renderer.renderFaceZNeg(block, x, y, z, icon);
					break;
				case 3:
					tessellator.setNormal(0F, 0F, 1F);
					renderer.renderFaceZPos(block, x, y, z, icon);
					break;
				case 4:
					tessellator.setNormal(-1F, 0F, 0F);
					renderer.renderFaceXNeg(block, x, y, z, icon);
					break;
				case 5:
					tessellator.setNormal(1F, 0F, 0F);
					renderer.renderFaceXPos(block, x, y, z, icon);
					break;
			}
		}
	}

	/**
	 * Re-maps a sub-rectangle of an atlas icon onto the full 0-16 range the block face renderers use,
	 * so the model can use the same partial UVs as its modern JSON counterpart.
	 */
	private static class SubIcon implements IIcon {

		private final IIcon base;
		private final float minU, maxU, minV, maxV;

		SubIcon(IIcon base, float u1, float v1, float u2, float v2) {
			this.base = base;
			this.minU = base.getInterpolatedU(u1);
			this.maxU = base.getInterpolatedU(u2);
			this.minV = base.getInterpolatedV(v1);
			this.maxV = base.getInterpolatedV(v2);
		}

		@Override
		public int getIconWidth() {
			return base.getIconWidth();
		}

		@Override
		public int getIconHeight() {
			return base.getIconHeight();
		}

		@Override
		public float getMinU() {
			return minU;
		}

		@Override
		public float getMaxU() {
			return maxU;
		}

		@Override
		public float getInterpolatedU(double u) {
			return minU + (maxU - minU) * (float) u / 16F;
		}

		@Override
		public float getMinV() {
			return minV;
		}

		@Override
		public float getMaxV() {
			return maxV;
		}

		@Override
		public float getInterpolatedV(double v) {
			return minV + (maxV - minV) * (float) v / 16F;
		}

		@Override
		public String getIconName() {
			return base.getIconName();
		}
	}
}
