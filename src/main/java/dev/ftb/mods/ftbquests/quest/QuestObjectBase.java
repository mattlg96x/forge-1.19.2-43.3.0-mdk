package dev.ftb.mods.ftbquests.quest;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.config.Tristate;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.math.Bits;
import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.client.ConfigIconItemStack;
import dev.ftb.mods.ftbquests.integration.RecipeModHelper;
import dev.ftb.mods.ftbquests.item.CustomIconItem;
import dev.ftb.mods.ftbquests.net.EditObjectMessage;
import dev.ftb.mods.ftbquests.quest.theme.property.ThemeProperties;
import dev.ftb.mods.ftbquests.util.NBTUtils;
import dev.ftb.mods.ftbquests.util.NetUtils;
import dev.ftb.mods.ftbquests.util.ProgressChange;
import dev.ftb.mods.ftbquests.util.TextUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Pattern;

public abstract class QuestObjectBase {
	private static final Pattern TAG_PATTERN = Pattern.compile("^[a-z0-9_]*$");
	public static Tristate sendNotifications = Tristate.DEFAULT;

	public static boolean isNull(@Nullable QuestObjectBase object) {
		return object == null || object.invalid;
	}

	public static long getID(@Nullable QuestObjectBase object) {
		return isNull(object) ? 0L : object.id;
	}

	public static String getCodeString(long id) {
		return String.format("%016X", id);
	}

	public static String getCodeString(@Nullable QuestObjectBase object) {
		return getCodeString(getID(object));
	}

	public static long parseCodeString(String id) {
		if (id.isEmpty() || id.equals("-")) {
			return 0L;
		}

		try {
			return Long.parseLong(id.charAt(0) == '#' ? id.substring(1) : id, 16);
		} catch (Exception ex) {
			return 0L;
		}
	}

	public static Optional<String> titleToID(String s) {
		s = s.replace(' ', '_').replaceAll("\\W", "").toLowerCase().trim();

		while (s.startsWith("_")) {
			s = s.substring(1);
		}

		while (s.endsWith("_")) {
			s = s.substring(0, s.length() - 1);
		}

		return s.isEmpty() ? Optional.empty() : Optional.of(s);
	}

	public long id = 0L;
	public boolean invalid = false;
	public String title = "";
	public ItemStack icon = ItemStack.EMPTY;
	private List<String> tags = new ArrayList<>(0);

	private Icon cachedIcon = null;
	private Component cachedTitle = null;
	private Set<String> cachedTags = null;


	public final String getCodeString() {
		return getCodeString(id);
	}

	public final String toString() {
		return getCodeString();
	}

	public final boolean equals(Object object) {
		return object == this;
	}

	public final int hashCode() {
		return Long.hashCode(id);
	}

	public abstract QuestObjectType getObjectType();

	public abstract QuestFile getQuestFile();

	public Set<String> getTags() {
		if (tags.isEmpty()) {
			return Collections.emptySet();
		} else if (cachedTags == null) {
			cachedTags = new LinkedHashSet<>(tags);
		}

		return cachedTags;
	}

	public boolean hasTag(String tag) {
		return !tags.isEmpty() && getTags().contains(tag);
	}

	public void forceProgress(TeamData teamData, ProgressChange progressChange) {
	}

	public final void forceProgressRaw(TeamData teamData, ProgressChange progressChange) {
		if (teamData.isLocked()) {
			return;
		}

		teamData.clearCachedProgress();
		sendNotifications = progressChange.notifications ? Tristate.TRUE : Tristate.FALSE;
		forceProgress(teamData, progressChange);
		sendNotifications = Tristate.DEFAULT;
		teamData.clearCachedProgress();
		teamData.markDirty();
	}

	@Nullable
	public Chapter getQuestChapter() {
		return null;
	}

	public long getParentID() {
		return 1L;
	}

	public void writeData(CompoundTag nbt) {
		if (!title.isEmpty()) {
			nbt.putString("title", title);
		}

		NBTUtils.write(nbt, "icon", icon);

		if (!tags.isEmpty()) {
			ListTag tagList = new ListTag();

			for (String s : tags) {
				tagList.add(StringTag.valueOf(s));
			}

			nbt.put("tags", tagList);
		}
	}

	public void readData(CompoundTag nbt) {
		title = nbt.getString("title");
		icon = NBTUtils.read(nbt, "icon");

		ListTag tagsList = nbt.getList("tags", Tag.TAG_STRING);

		tags = new ArrayList<>(tagsList.size());

		for (int i = 0; i < tagsList.size(); i++) {
			tags.add(tagsList.getString(i));
		}

		if (nbt.contains("custom_id")) {
			tags.add(nbt.getString("custom_id"));
		}
	}

	public void writeNetData(FriendlyByteBuf buffer) {
		int flags = 0;
		flags = Bits.setFlag(flags, 1, !title.isEmpty());
		flags = Bits.setFlag(flags, 2, !icon.isEmpty());
		flags = Bits.setFlag(flags, 4, !tags.isEmpty());

		buffer.writeVarInt(flags);

		if (!title.isEmpty()) {
			buffer.writeUtf(title, Short.MAX_VALUE);
		}

		if (!icon.isEmpty()) {
			buffer.writeItem(icon);
		}

		if (!tags.isEmpty()) {
			NetUtils.writeStrings(buffer, tags);
		}
	}

	public void readNetData(FriendlyByteBuf buffer) {
		int flags = buffer.readVarInt();
		title = Bits.getFlag(flags, 1) ? buffer.readUtf(Short.MAX_VALUE) : "";
		icon = Bits.getFlag(flags, 2) ? buffer.readItem() : ItemStack.EMPTY;
		tags = new ArrayList<>(0);

		if (Bits.getFlag(flags, 4)) {
			NetUtils.readStrings(buffer, tags);
		}
	}

	protected boolean hasTitleConfig() {
		return true;
	}

	protected boolean hasIconConfig() {
		return true;
	}

	@Environment(EnvType.CLIENT)
	public void getConfig(ConfigGroup config) {
		if (hasTitleConfig()) {
			config.addString("title", title, v -> title = v, "").setNameKey("ftbquests.title").setOrder(-127);
		}

		if (hasIconConfig()) {
			config.add("icon", new ConfigIconItemStack(), icon, v -> icon = v, ItemStack.EMPTY).setNameKey("ftbquests.icon").setOrder(-126);
		}

		config.addList("tags", tags, new StringConfig(TAG_PATTERN), "").setNameKey("ftbquests.tags").setOrder(-125);
	}

	@Environment(EnvType.CLIENT)
	public abstract Component getAltTitle();

	@Environment(EnvType.CLIENT)
	public abstract Icon getAltIcon();

	@Environment(EnvType.CLIENT)
	public final Component getTitle() {
		if (cachedTitle != null) {
			return cachedTitle.copy();
		}

		if (!title.isEmpty()) {
			cachedTitle = TextUtils.parseRawText(title);
		} else {
			cachedTitle = getAltTitle();
		}

		return cachedTitle.copy();
	}

	@Environment(EnvType.CLIENT)
	public final MutableComponent getMutableTitle() {
		return Component.empty().append(getTitle());
	}

	@Environment(EnvType.CLIENT)
	public final Icon getIcon() {
		if (cachedIcon != null) {
			return cachedIcon;
		}

		if (!icon.isEmpty()) {
			cachedIcon = CustomIconItem.getIcon(icon);
		}

		if (cachedIcon == null || cachedIcon.isEmpty()) {
			cachedIcon = ThemeProperties.ICON.get(this);
		}

		if (cachedIcon.isEmpty()) {
			cachedIcon = getAltIcon();
		}

		return cachedIcon;
	}

	public void deleteSelf() {
		getQuestFile().remove(id);
	}

	public void deleteChildren() {
	}

	@Environment(EnvType.CLIENT)
	public void editedFromGUI() {
		ClientQuestFile.INSTANCE.refreshGui();
	}

	public void editedFromGUIOnServer() {
	}

	public void onCreated() {
	}

	@Nullable
	public String getPath() {
		return null;
	}

	public void clearCachedData() {
		cachedIcon = null;
		cachedTitle = null;
		cachedTags = null;
	}

	public ConfigGroup createSubGroup(ConfigGroup group) {
		return group.getGroup(getObjectType().id);
	}

	@Environment(EnvType.CLIENT)
	public void onEditButtonClicked(Runnable gui) {
		ConfigGroup group = new ConfigGroup(FTBQuests.MOD_ID);
		getConfig(createSubGroup(group));

		group.savedCallback = accepted -> {
			gui.run();
			if (accepted && validateEditedConfig()) {
				new EditObjectMessage(this).sendToServer();
			}
		};

		new EditConfigScreen(group).openGui();
	}

	protected boolean validateEditedConfig() {
		return true;
	}

	public Set<RecipeModHelper.Components> componentsToRefresh() {
		return EnumSet.noneOf(RecipeModHelper.Components.class);
	}
}
