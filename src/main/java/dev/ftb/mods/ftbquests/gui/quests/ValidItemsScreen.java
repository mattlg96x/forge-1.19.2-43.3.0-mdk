package dev.ftb.mods.ftbquests.gui.quests;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.ui.misc.CompactGridLayout;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftblibrary.util.WrappedIngredient;
import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.gui.FTBQuestsTheme;
import dev.ftb.mods.ftbquests.net.SubmitTaskMessage;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ValidItemsScreen extends BaseScreen {
	public static class ValidItemButton extends Button {
		public final ItemStack stack;

		public ValidItemButton(Panel panel, ItemStack is) {
			super(panel, Component.empty(), ItemIcon.getItemIcon(is));
			stack = is;
		}

		@Override
		public void onClicked(MouseButton button) {
			if (FTBQuests.getRecipeModHelper().isRecipeModAvailable()) {
				FTBQuests.getRecipeModHelper().showRecipes(stack);
			}
		}

		@Nullable
		@Override
		public Object getIngredientUnderMouse() {
			return new WrappedIngredient(stack).tooltip();
		}

		@Override
		public void draw(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
			if (isMouseOver()) {
				Color4I.WHITE.withAlpha(33).draw(matrixStack, x, y, w, h);
			}

			matrixStack.pushPose();
			matrixStack.translate(x + w / 2D, y + h / 2D, 10);
			matrixStack.scale(2F, 2F, 2F);
			GuiHelper.drawItem(matrixStack, stack, 0, true, null);
			matrixStack.popPose();
		}
	}

	public final ItemTask task;
	public final List<ItemStack> validItems;
	public String title = "";
	public final boolean canClick;
	public final Panel itemPanel;
	public final Button backButton, submitButton;

	public ValidItemsScreen(ItemTask t, List<ItemStack> v, boolean c) {
		task = t;
		validItems = v;
		canClick = c;

		itemPanel = new Panel(this) {
			@Override
			public void addWidgets() {
				for (ItemStack validItem : validItems) {
					add(new ValidItemButton(this, validItem));
				}
			}

			@Override
			public void alignWidgets() {
				align(new CompactGridLayout(36));
				setHeight(Math.min(160, getContentHeight()));
				parent.setHeight(height + 53);
				int off = (width - getContentWidth()) / 2;

				for (Widget widget : widgets) {
					widget.setX(widget.posX + off);
				}

				itemPanel.setX((parent.width - width) / 2);
				backButton.setPosAndSize(itemPanel.posX - 1, height + 28, 70, 20);
				submitButton.setPosAndSize(itemPanel.posX + 75, height + 28, 70, 20);
			}

			@Override
			public void drawBackground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
				theme.drawButton(matrixStack, x - 1, y - 1, w + 2, h + 2, WidgetType.NORMAL);
			}
		};

		itemPanel.setPosAndSize(0, 22, 144, 0);

		backButton = new SimpleTextButton(this, Component.translatable("gui.back"), Color4I.EMPTY) {
			@Override
			public void onClicked(MouseButton button) {
				playClickSound();
				onBack();
			}

			@Override
			public boolean renderTitleInCenter() {
				return true;
			}
		};

		submitButton = new SimpleTextButton(this, Component.literal("Submit"), Color4I.EMPTY) {
			@Override
			public void onClicked(MouseButton button) {
				playClickSound();
				new SubmitTaskMessage(task.id).sendToServer();
				onBack();
			}

			@Override
			public void addMouseOverText(TooltipList list) {
				if (canClick && !task.consumesResources() && !task.isTaskScreenOnly()) {
					list.translate("ftbquests.task.auto_detected");
				}
			}

			@Override
			public WidgetType getWidgetType() {
				return canClick && task.consumesResources() && !task.isTaskScreenOnly() ? super.getWidgetType() : WidgetType.DISABLED;
			}

			@Override
			public boolean renderTitleInCenter() {
				return true;
			}
		};
	}

	@Override
	public void addWidgets() {
		title = Component.translatable("ftbquests.task.ftbquests.item.valid_for", task.getTitle()).getString();
		setWidth(Math.max(156, getTheme().getStringWidth(title) + 12));
		add(itemPanel);
		add(backButton);
		add(submitButton);
	}

	@Override
	public Theme getTheme() {
		return FTBQuestsTheme.INSTANCE;
	}

	@Override
	public void drawBackground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		super.drawBackground(matrixStack, theme, x, y, w, h);
		theme.drawString(matrixStack, title, x + w / 2F, y + 6, Color4I.WHITE, Theme.CENTERED);
	}

	@Override
	public boolean keyPressed(Key key) {
		if (super.keyPressed(key)) return true;
		if (key.esc()) {
			onBack();
			return true;
		}
		return false;
	}

	@Override
	public boolean onClosedByKey(Key key) {
		if (super.onClosedByKey(key)) {
			onBack();
		}

		return false;
	}
}
