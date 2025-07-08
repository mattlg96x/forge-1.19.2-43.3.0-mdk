package dev.ftb.mods.ftbquests.gui.quests;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Panel;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.gui.RewardNotificationsScreen;
import dev.ftb.mods.ftbquests.net.ClaimAllRewardsMessage;
import dev.ftb.mods.ftbquests.quest.theme.property.ThemeProperties;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class CollectRewardsButton extends TabButton {
	public CollectRewardsButton(Panel panel) {
		super(panel, Component.empty(), ThemeProperties.COLLECT_REWARDS_ICON.get());
		title = questScreen.file.getTitle();
	}

	@Override
	public void onClicked(MouseButton button) {
		if (questScreen.file.self.hasUnclaimedRewards(Minecraft.getInstance().player.getUUID(), questScreen.file)) {
			playClickSound();
			new RewardNotificationsScreen().openGui();
			new ClaimAllRewardsMessage().sendToServer();
		}
	}

	@Override
	public void addMouseOverText(TooltipList list) {
		list.translate("ftbquests.gui.collect_rewards");
	}

	@Override
	public void draw(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		super.draw(matrixStack, theme, x, y, w, h);

		if (questScreen.file.self.hasUnclaimedRewards(Minecraft.getInstance().player.getUUID(), questScreen.file)) {
			GuiHelper.setupDrawing();
			int s = w / 2;//(int) (treeGui.getZoom() / 2 * quest.size);
			matrixStack.pushPose();
			matrixStack.translate(x + w - s, y, 200);
			ThemeProperties.ALERT_ICON.get(questScreen.file).draw(matrixStack, 0, 0, s, s);
			matrixStack.popPose();
		}
	}
}
