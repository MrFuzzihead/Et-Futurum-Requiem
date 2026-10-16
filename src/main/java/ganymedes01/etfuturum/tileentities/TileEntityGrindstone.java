package ganymedes01.etfuturum.tileentities;

import net.minecraft.tileentity.TileEntity;

/**
 * The modern grindstone has no block entity, but 1.7.10 needs one as an anchor for the TESR that
 * renders the model. The actual inventory lives in the container.
 */
public class TileEntityGrindstone extends TileEntity {

	@Override
	public boolean canUpdate() {
		return false;
	}
}
