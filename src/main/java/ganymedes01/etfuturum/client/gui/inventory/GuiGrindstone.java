package ganymedes01.etfuturum.client.gui.inventory;

import ganymedes01.etfuturum.core.utils.Utils;
import ganymedes01.etfuturum.inventory.ContainerGrindstone;
import ganymedes01.etfuturum.lib.Reference;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class GuiGrindstone extends GuiContainer {

	private static final ResourceLocation TEXTURE = Utils.getResource(Reference.MOD_ID + ":textures/gui/container/grindstone.png");
	private final ContainerGrindstone container;

	public GuiGrindstone(ContainerGrindstone container) {
		super(container);
		this.container = container;
		this.xSize = 176;
		this.ySize = 166;
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(TEXTURE);
		int offsetX = (width - xSize) / 2;
		int offsetY = (height - ySize) / 2;
		drawTexturedModalRect(offsetX, offsetY, 0, 0, xSize, ySize);
		if (container.hasInput() && !container.hasResult()) {
			drawTexturedModalRect(offsetX + 92, offsetY + 31, xSize, 0, 28, 21);
		}
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		fontRendererObj.drawString(I18n.format("container.grindstone_title"), 8, 6, 0x404040);
	}
}
