package com.makemeablock.client.screen;

import com.makemeablock.client.BlockSkinState;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

public class SkinPainterScreen extends Screen {
	private static final Identifier PREVIEW_ID = Identifier.fromNamespaceAndPath("makemeablock", "textures/skin/paint_preview");
	private static final int GRID = 64;

	private static final int[] PALETTE = {
		ARGB.color(31, 31, 35),
		ARGB.color(70, 70, 80),
		ARGB.color(148, 148, 158),
		ARGB.color(255, 255, 255),
		ARGB.color(226, 68, 68),
		ARGB.color(236, 154, 60),
		ARGB.color(244, 208, 63),
		ARGB.color(140, 204, 66),
		ARGB.color(58, 175, 90),
		ARGB.color(40, 160, 160),
		ARGB.color(88, 186, 232),
		ARGB.color(62, 110, 235),
		ARGB.color(155, 89, 214),
		ARGB.color(220, 80, 160),
		ARGB.color(255, 140, 180),
		ARGB.color(133, 105, 84),
	};

	private NativeImage work;
	private NativeImage preview;
	private DynamicTexture previewTexture;
	private int brushColor = PALETTE[0];
	private int brushSize = 1;
	private boolean eraser;
	private int scale = 3;
	private int canvasX;
	private int canvasY;
	private CanvasWidget canvas;
	private Button saveButton;
	private Button cancelButton;
	private Button eraserButton;
	private final PaletteButton[] paletteButtons = new PaletteButton[PALETTE.length];

	public SkinPainterScreen() {
		super(Component.translatable("screen.makemeablock.painter.title"));
		this.work = BlockSkinState.currentImageCopy();
		if (this.work == null) {
			this.work = new NativeImage(GRID, GRID, true);
			int base = ARGB.color(214, 210, 206);
			for (int y = 0; y < GRID; y++) {
				for (int x = 0; x < GRID; x++) {
					this.work.setPixel(x, y, base);
				}
			}
		}
		this.preview = new NativeImage(GRID * 6, GRID * 6, true);
		this.previewTexture = new DynamicTexture(() -> "makemeablock paint preview", this.preview);
		Minecraft.getInstance().getTextureManager().register(PREVIEW_ID, this.previewTexture);
	}

	@Override
	protected void init() {
		this.canvas = this.addRenderableWidget(new CanvasWidget(0, 0, 64 * this.scale, 64 * this.scale));
		for (int i = 0; i < PALETTE.length; i++) {
			PaletteButton button = new PaletteButton(i, PALETTE[i]);
			this.paletteButtons[i] = button;
			this.addRenderableWidget(button);
		}
		this.saveButton = this.addRenderableWidget(
			Button.builder(Component.translatable("button.makemeablock.save"), button -> this.save()).size(100, 20).build()
		);
		this.cancelButton = this.addRenderableWidget(
			Button.builder(Component.translatable("button.makemeablock.cancel"), button -> this.discard()).size(100, 20).build()
		);
		this.eraserButton = this.addRenderableWidget(
			Button.builder(Component.translatable("button.makemeablock.eraser"), button -> {
				this.eraser = !this.eraser;
				this.updateEraserButton();
			}).size(100, 20).build()
		);
		this.repositionElements();
		this.updatePreview();
	}

	@Override
	protected void repositionElements() {
		if (this.canvas == null) {
			return;
		}
		int bottom = this.height - 28;
		int available = bottom - 34 - 28;
		this.scale = Math.max(2, Math.min(6, available / GRID));
		this.canvas.setSize(GRID * this.scale, GRID * this.scale);
		this.canvasX = this.width / 2 - this.canvas.getWidth() / 2;
		this.canvasY = 34 + Math.max(0, (available - this.canvas.getHeight()) / 2);
		this.canvas.setPosition(this.canvasX, this.canvasY);
		if (this.work != null) {
			this.updatePreview();
		}

		int paletteWidth = PALETTE.length * 18 - 2;
		int paletteX = this.width / 2 - paletteWidth / 2;
		for (int i = 0; i < this.paletteButtons.length; i++) {
			this.paletteButtons[i].setPosition(paletteX + i % 8 * 18, 8 + i / 8 * 18);
		}

		this.saveButton.setPosition(this.width / 2 - 156, bottom);
		this.cancelButton.setPosition(this.width / 2 - 50, bottom);
		this.eraserButton.setPosition(this.width / 2 + 56, bottom);
		this.updateEraserButton();
	}

	private void updateEraserButton() {
		if (this.eraserButton != null) {
			this.eraserButton.setMessage(Component.translatable(this.eraser ? "button.makemeablock.eraser.on" : "button.makemeablock.eraser.off"));
		}
	}

	private void updatePreview() {
		int size = GRID * this.scale;
		if (this.preview.getWidth() != size) {
			this.previewTexture.close();
			Minecraft.getInstance().getTextureManager().release(PREVIEW_ID);
			this.preview = new NativeImage(size, size, true);
			this.previewTexture = new DynamicTexture(() -> "makemeablock paint preview", this.preview);
			Minecraft.getInstance().getTextureManager().register(PREVIEW_ID, this.previewTexture);
		}
		int s = this.scale;
		for (int y = 0; y < GRID; y++) {
			for (int x = 0; x < GRID; x++) {
				int pixel = this.work.getPixel(x, y);
				for (int dy = 0; dy < s; dy++) {
					for (int dx = 0; dx < s; dx++) {
						this.preview.setPixel(x * s + dx, y * s + dy, pixel);
					}
				}
			}
		}
		this.previewTexture.upload();
	}

	private void paintAt(final double mouseX, final double mouseY, final boolean erase) {
		if (mouseX < this.canvasX || mouseX >= this.canvasX + this.canvas.getWidth() || mouseY < this.canvasY || mouseY >= this.canvasY + this.canvas.getHeight()) {
			return;
		}
		int px = (int) ((mouseX - this.canvasX) / this.scale);
		int py = (int) ((mouseY - this.canvasY) / this.scale);
		int color = erase || this.eraser ? 0 : this.brushColor;
		int radius = this.brushSize - 1;
		for (int dy = -radius; dy <= radius; dy++) {
			for (int dx = -radius; dx <= radius; dx++) {
				int x = px + dx;
				int y = py + dy;
				if (x >= 0 && x < GRID && y >= 0 && y < GRID) {
					this.work.setPixel(x, y, color);
				}
			}
		}
		this.updatePreview();
	}

	private void save() {
		BlockSkinState.selectCustom(this.work);
		this.work = null;
		this.closeResources();
		this.onClose();
	}

	private void discard() {
		this.closeResources();
		this.onClose();
	}

	private void closeResources() {
		if (this.work != null) {
			this.work.close();
			this.work = null;
		}
		this.previewTexture.close();
		Minecraft.getInstance().getTextureManager().release(PREVIEW_ID);
	}

	private boolean inCanvas(final double x, final double y) {
		return this.canvas != null && x >= this.canvasX && x < this.canvasX + this.canvas.getWidth() && y >= this.canvasY && y < this.canvasY + this.canvas.getHeight();
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		if (this.inCanvas(event.x(), event.y())) {
			this.paintAt(event.x(), event.y(), event.buttonInfo().button() == 1);
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(final MouseButtonEvent event, final double dx, final double dy) {
		if (this.inCanvas(event.x(), event.y())) {
			this.paintAt(event.x(), event.y(), event.buttonInfo().button() == 1);
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
		if (this.inCanvas(x, y)) {
			this.brushSize = Math.max(1, Math.min(4, this.brushSize + (scrollY < 0 ? 1 : -1)));
			return true;
		}
		return false;
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(this.font, this.title, this.width / 2, this.canvasY - 12, 0xFFFFFFFF);
		graphics.centeredText(this.font, Component.translatable("screen.makemeablock.painter.brush", this.brushSize), this.width / 2, this.canvasY + this.canvas.getHeight() + 4, 0xA0FFFFFF);
	}

	private class CanvasWidget extends AbstractWidget {
		private CanvasWidget(final int x, final int y, final int width, final int height) {
			super(x, y, width, height, Component.empty());
		}

		@Override
		protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
			graphics.blit(PREVIEW_ID, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0.0F, 1.0F, 0.0F, 1.0F);
			graphics.outline(this.getX(), this.getY(), this.getWidth(), this.getHeight(), 0xFFFFFFFF);
		}

		@Override
		public void updateWidgetNarration(final NarrationElementOutput narrationElementOutput) {
		}
	}

	private class PaletteButton extends AbstractWidget {
		private final int colorIndex;

		private PaletteButton(final int colorIndex, final int color) {
			super(0, 0, 16, 16, Component.empty());
			this.colorIndex = colorIndex;
		}

		@Override
		protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
			int color = PALETTE[this.colorIndex];
			graphics.fill(this.getX(), this.getY(), this.getX() + 16, this.getY() + 16, color);
			if (this.colorIndex == SkinPainterScreen.this.selectedPaletteIndex()) {
				graphics.outline(this.getX() - 1, this.getY() - 1, 18, 18, 0xFFFFFFFF);
			}
		}

		@Override
		public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
			if (event.x() >= this.getX() && event.x() < this.getX() + 16 && event.y() >= this.getY() && event.y() < this.getY() + 16) {
				SkinPainterScreen.this.brushColor = PALETTE[this.colorIndex];
				SkinPainterScreen.this.eraser = false;
				SkinPainterScreen.this.updateEraserButton();
				return true;
			}
			return false;
		}

		@Override
		public void updateWidgetNarration(final NarrationElementOutput narrationElementOutput) {
		}
	}

	private int selectedPaletteIndex() {
		if (this.eraser) {
			return -1;
		}
		for (int i = 0; i < PALETTE.length; i++) {
			if (PALETTE[i] == this.brushColor) {
				return i;
			}
		}
		return -1;
	}
}
