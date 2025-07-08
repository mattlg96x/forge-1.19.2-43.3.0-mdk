package dev.ftb.mods.ftbquests.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TimeUtils;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.net.GetEmergencyItemsMessage;
import dev.ftb.mods.ftbquests.quest.QuestShape;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EmergencyItemsScreen extends BaseScreen {
	private final long endTime = System.currentTimeMillis() + ClientQuestFile.INSTANCE.emergencyItemsCooldown * 1000L;
	private boolean done = false;

	private static class EmergencyItem extends Widget {
		private final ItemStack stack;

		public EmergencyItem(Panel p, ItemStack is) {
			super(p);
			setY(3);
			stack = is;
			setSize(16, 16);
		}

		@Override
		public void addMouseOverText(TooltipList list) {
			List<Component> list1 = new ArrayList<>();
			GuiHelper.addStackTooltip(stack, list1);

			for (Component t : list1) {
				list.add(t);
			}
		}

		@Override
		public void draw(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
			GuiHelper.setupDrawing();
			QuestShape.get("rsquare").outline.draw(matrixStack, x - 3, y - 3, w + 6, h + 6);
			matrixStack.pushPose();
			matrixStack.translate(x + w / 2D, y + h / 2D, 100);
			GuiHelper.drawItem(matrixStack, stack, 0, true, null);
			matrixStack.popPose();
		}

		@Override
		@Nullable
		public Object getIngredientUnderMouse() {
			return stack;
		}
	}

	private final SimpleTextButton cancelButton = new SimpleTextButton(this, Component.translatable("gui.cancel"), Color4I.EMPTY) {
		@Override
		public void onClicked(MouseButton button) {
			playClickSound();
			getGui().closeGui();
		}
	};

	private final Panel itemPanel = new Panel(this) {
		@Override
		public void addWidgets() {
			for (ItemStack stack : ClientQuestFile.INSTANCE.emergencyItems) {
				add(new EmergencyItem(this, stack));
			}
		}

		@Override
		public void alignWidgets() {
			setWidth(align(new WidgetLayout.Horizontal(3, 7, 3)));
			setHeight(22);
			setPos((EmergencyItemsScreen.this.width - itemPanel.width) / 2, EmergencyItemsScreen.this.height * 2 / 3 - 10);
		}
	};

	@Override
	public void addWidgets() {
		add(itemPanel);
		add(cancelButton);
		cancelButton.setPos((width - cancelButton.width) / 2, height * 2 / 3 + 16);
	}

	@Override
	public boolean onInit() {
		return setFullscreen();
	}

	@Override
	public void drawBackground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		long left = endTime - System.currentTimeMillis();

		if (left <= 0L) {
			if (!done) {
				done = true;
				cancelButton.setTitle(Component.translatable("gui.close"));
				new GetEmergencyItemsMessage().sendToServer();
			}

			left = 0L;
		}

		matrixStack.pushPose();
		matrixStack.translate((int) (w / 2D), (int) (h / 5D), 0);
		matrixStack.scale(2F, 2F, 1F);
		String s = I18n.get("ftbquests.file.emergency_items");
		theme.drawString(matrixStack, s, -theme.getStringWidth(s) / 2F, 0, Color4I.WHITE, 0);
		matrixStack.popPose();

		matrixStack.pushPose();
		matrixStack.translate((int) (w / 2D), (int) (h / 2.5D), 0);
		matrixStack.scale(4F, 4F, 1F);
		s = left <= 0L ? "00:00" : TimeUtils.getTimeString(left / 1000L * 1000L + 1000L);
		int x1 = -theme.getStringWidth(s) / 2;
		theme.drawString(matrixStack, s, x1 - 1, 0, Color4I.BLACK, 0);
		theme.drawString(matrixStack, s, x1 + 1, 0, Color4I.BLACK, 0);
		theme.drawString(matrixStack, s, x1, 1, Color4I.BLACK, 0);
		theme.drawString(matrixStack, s, x1, -1, Color4I.BLACK, 0);
		theme.drawString(matrixStack, s, x1, 0, Color4I.WHITE, 0);
		matrixStack.popPose();
	}

	@Override
	public Theme getTheme() {
		return FTBQuestsTheme.INSTANCE;
	}
}
