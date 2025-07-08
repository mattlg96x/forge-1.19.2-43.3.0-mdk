package dev.ftb.mods.ftbquests.gui;

import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.ui.misc.SimpleToast;
import dev.ftb.mods.ftbquests.quest.Chapter;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class ToastQuestObject extends SimpleToast {
	private final QuestObject object;

	public ToastQuestObject(QuestObject q) {
		object = q;
	}

	@Override
	public Component getTitle() {
		return Component.translatable(object.getObjectType().translationKey + ".completed");
	}

	@Override
	public Component getSubtitle() {
		return object.getTitle();
	}

	@Override
	public boolean isImportant() {
		return object instanceof Chapter;
	}

	@Override
	public Icon getIcon() {
		return object.getIcon();
	}

	@Override
	public void playSound(SoundManager handler) {
		if (object instanceof Chapter) {
			handler.play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1F, 1F));
		}
	}
}
