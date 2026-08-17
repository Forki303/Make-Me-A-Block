package com.makemeablock.client.screen;

import com.makemeablock.client.BlockSkinState;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public class BlockPickerScreen extends Screen {
	private static final int SLOT = 22;

	private final List<Block> blocks = new ArrayList<>();
	private final List<BlockSlot> slots = new ArrayList<>();
	private int cols;
	private int rows;
	private int scroll;
	private int gridX;
	private int gridY;
	private Button paintButton;
	private Button resetButton;
	private Button closeButton;
	private Block selected;

	public BlockPickerScreen() {
		super(Component.translatable("screen.makemeablock.picker.title"));
	}

	@Override
	protected void init() {
		this.blocks.clear();
		for (Block block : BuiltInRegistries.BLOCK) {
			if (block.asItem() != Items.AIR) {
				this.blocks.add(block);
			}
		}

		this.cols = Math.max(1, (this.width - 20) / SLOT);
		this.rows = Math.max(1, (this.height - 62) / SLOT);
		this.selected = BlockSkinState.getBlock();
		for (int i = 0; i < this.cols * this.rows; i++) {
			BlockSlot slot = new BlockSlot(i);
			this.slots.add(slot);
			this.addRenderableWidget(slot);
		}

		this.paintButton = this.addRenderableWidget(
			Button.builder(Component.translatable("button.makemeablock.paint"), button -> this.openPainter()).size(100, 20).build()
		);
		this.resetButton = this.addRenderableWidget(
			Button.builder(
				Component.translatable("button.makemeablock.reset"),
				button -> {
					BlockSkinState.reset();
					this.selected = null;
				}
			).size(100, 20).build()
		);
		this.closeButton = this.addRenderableWidget(
			Button.builder(Component.translatable("button.makemeablock.close"), button -> this.onClose()).size(100, 20).build()
		);
		this.repositionElements();
	}

	@Override
	protected void repositionElements() {
		if (this.paintButton == null) {
			return;
		}
		int bottom = this.height - 28;
		this.paintButton.setPosition(this.width / 2 - 156, bottom);
		this.resetButton.setPosition(this.width / 2 - 50, bottom);
		this.closeButton.setPosition(this.width / 2 + 56, bottom);

		this.scroll = Math.min(this.scroll, Math.max(0, (this.blocks.size() + this.cols - 1) / this.cols - this.rows));
		this.gridX = this.width / 2 - this.cols * SLOT / 2;
		this.gridY = 28;
		this.refreshSlots();
	}

	private void refreshSlots() {
		for (BlockSlot slot : this.slots) {
			int index = this.scroll * this.cols + slot.slotIndex;
			slot.blockIndex = index < this.blocks.size() ? index : -1;
			slot.setPosition(this.gridX + slot.slotIndex % this.cols * SLOT, this.gridY + slot.slotIndex / this.cols * SLOT);
		}
	}

	@Override
	public boolean mouseScrolled(final double x, final double y, final double scrollX, final double scrollY) {
		int maxScroll = Math.max(0, (this.blocks.size() + this.cols - 1) / this.cols - this.rows);
		int newScroll = (int) Math.floor(this.scroll - scrollY);
		this.scroll = Math.max(0, Math.min(maxScroll, newScroll));
		this.refreshSlots();
		return true;
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);
		graphics.centeredText(this.font, this.title, this.width / 2, 8, 0xFFFFFFFF);

		Component status;
		if (BlockSkinState.isActive() && BlockSkinState.isCustom()) {
			status = Component.translatable("screen.makemeablock.status.custom");
		} else {
			Block current = BlockSkinState.getBlock();
			if (current != null) {
				status = Component.translatable("screen.makemeablock.status.block", new ItemStack(current).getHoverName());
			} else {
				status = Component.translatable("screen.makemeablock.status.none");
			}
		}
		graphics.centeredText(this.font, status, this.width / 2, this.height - 38, 0xA0FFFFFF);
	}

	private void select(final Block block) {
		BlockSkinState.selectBlock(block);
		this.selected = block;
	}

	private void openPainter() {
		this.minecraft.gui.setScreen(new SkinPainterScreen());
	}

	private class BlockSlot extends AbstractWidget {
		private final int slotIndex;
		private int blockIndex = -1;

		private BlockSlot(final int slotIndex) {
			super(0, 0, SLOT, SLOT, Component.empty());
			this.slotIndex = slotIndex;
		}

		private Block getBlock() {
			return this.blockIndex >= 0 && this.blockIndex < BlockPickerScreen.this.blocks.size()
				? BlockPickerScreen.this.blocks.get(this.blockIndex)
				: null;
		}

		@Override
		protected void extractWidgetRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
			Block block = this.getBlock();
			if (block == null) {
				return;
			}
			boolean selected = block == BlockPickerScreen.this.selected;
			int background = selected ? 0xA033AAFF : this.isHovered() ? 0x60FFFFFF : 0x40808080;
			graphics.fill(this.getX(), this.getY(), this.getX() + SLOT, this.getY() + SLOT, background);
			graphics.item(new ItemStack(block), this.getX() + 3, this.getY() + 3);
		}

		@Override
		public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
			Block block = this.getBlock();
			if (block == null) {
				return false;
			}
			if (event.x() >= this.getX() && event.x() < this.getX() + SLOT && event.y() >= this.getY() && event.y() < this.getY() + SLOT) {
				BlockPickerScreen.this.select(block);
				return true;
			}
			return false;
		}

		@Override
		public void updateWidgetNarration(final NarrationElementOutput narrationElementOutput) {
		}
	}
}
