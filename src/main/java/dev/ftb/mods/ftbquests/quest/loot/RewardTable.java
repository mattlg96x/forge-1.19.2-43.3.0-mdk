package dev.ftb.mods.ftbquests.quest.loot;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.IconAnimation;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.icon.ItemIcon;
import dev.ftb.mods.ftblibrary.math.Bits;
import dev.ftb.mods.ftblibrary.snbt.SNBTCompoundTag;
import dev.ftb.mods.ftblibrary.ui.BaseScreen;
import dev.ftb.mods.ftblibrary.util.ClientUtils;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.gui.EditRewardTableScreen;
import dev.ftb.mods.ftbquests.gui.RewardTablesScreen;
import dev.ftb.mods.ftbquests.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.integration.RecipeModHelper;
import dev.ftb.mods.ftbquests.net.EditObjectMessage;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardType;
import dev.ftb.mods.ftbquests.quest.reward.RewardTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public final class RewardTable extends QuestObjectBase {
	public final QuestFile file;
	public final List<WeightedReward> rewards;
	public final Quest fakeQuest;
	public float emptyWeight;
	public int lootSize;
	public boolean hideTooltip;
	public boolean useTitle;
	public LootCrate lootCrate;
	public ResourceLocation lootTableId;
	public String filename;

	public RewardTable(QuestFile f) {
		file = f;
		rewards = new ArrayList<>();
		fakeQuest = new Quest(new Chapter(file, f.defaultChapterGroup));
		emptyWeight = 0f;
		lootSize = 1;
		hideTooltip = false;
		useTitle = false;
		lootCrate = null;
		lootTableId = null;
		filename = "";
	}

	@Override
	public QuestObjectType getObjectType() {
		return QuestObjectType.REWARD_TABLE;
	}

	@Override
	public QuestFile getQuestFile() {
		return file;
	}

	public float getTotalWeight(boolean includeEmpty) {
		float w = includeEmpty ? emptyWeight : 0f;

		for (WeightedReward r : rewards) {
			w += r.weight;
		}

		return w;
	}

	public Collection<WeightedReward> generateWeightedRandomRewards(RandomSource random, int nAttempts, boolean includeEmpty) {
		float total = getTotalWeight(includeEmpty);
		if (total <= 0f) return List.of();

		List<WeightedReward> res  = new ArrayList<>();

		for (WeightedReward reward : rewards) {
			// rewards with a weight of 0 are auto-granted
			if (reward.weight == 0f) {
				res.add(reward);
			}
		}

		for (int i = 0; i < nAttempts; i++) {
			float threshold = random.nextFloat() * total;
			float currentWeight = includeEmpty ? emptyWeight : 0f;

			if (currentWeight < threshold) {
				for (WeightedReward reward : rewards) {
					currentWeight += reward.weight;
					if (currentWeight >= threshold) {
						res.add(reward);
						break;
					}
				}
			}
		}
		return res;
	}

	@Override
	public void writeData(CompoundTag nbt) {
		super.writeData(nbt);

		if (emptyWeight > 0f) {
			nbt.putFloat("empty_weight", emptyWeight);
		}

		nbt.putInt("loot_size", lootSize);

		if (hideTooltip) {
			nbt.putBoolean("hide_tooltip", true);
		}

		if (useTitle) {
			nbt.putBoolean("use_title", true);
		}

		ListTag list = new ListTag();

		for (WeightedReward reward : rewards) {
			SNBTCompoundTag nbt1 = new SNBTCompoundTag();
			reward.reward.writeData(nbt1);

			if (reward.reward.getType() != RewardTypes.ITEM) {
				nbt1.putString("type", reward.reward.getType().getTypeForNBT());
			} else if (nbt1.getTagType("item") == Tag.TAG_STRING) {
				nbt1.singleLine();
			}

			if (reward.weight != 1f) {
				nbt1.putFloat("weight", reward.weight);
			}

			list.add(nbt1);
		}

		nbt.put("rewards", list);

		if (lootCrate != null) {
			CompoundTag nbt1 = new CompoundTag();
			lootCrate.writeData(nbt1);
			nbt.put("loot_crate", nbt1);
		}

		if (lootTableId != null) {
			nbt.putString("loot_table_id", lootTableId.toString());
		}
	}

	@Override
	public void readData(CompoundTag nbt) {
		super.readData(nbt);
		emptyWeight = nbt.getFloat("empty_weight");
		lootSize = nbt.getInt("loot_size");
		hideTooltip = nbt.getBoolean("hide_tooltip");
		useTitle = nbt.getBoolean("use_title");

		rewards.clear();
		ListTag list = nbt.getList("rewards", Tag.TAG_COMPOUND);

		for (int i = 0; i < list.size(); i++) {
			CompoundTag nbt1 = list.getCompound(i);
			Reward reward = RewardType.createReward(fakeQuest, nbt1.getString("type"));

			if (reward != null) {
				reward.readData(nbt1);
				rewards.add(new WeightedReward(reward, nbt1.contains("weight") ? nbt1.getFloat("weight") : 1));
			}
		}

		lootCrate = null;

		if (nbt.contains("loot_crate")) {
			lootCrate = new LootCrate(this);
			lootCrate.readData(nbt.getCompound("loot_crate"));
		}

		lootTableId = nbt.contains("loot_table_id") ? new ResourceLocation(nbt.getString("loot_table_id")) : null;
	}

	@Override
	public void writeNetData(FriendlyByteBuf buffer) {
		super.writeNetData(buffer);
		buffer.writeUtf(filename, Short.MAX_VALUE);
		buffer.writeFloat(emptyWeight);
		buffer.writeVarInt(lootSize);
		int flags = 0;
		flags = Bits.setFlag(flags, 1, hideTooltip);
		flags = Bits.setFlag(flags, 2, useTitle);
		flags = Bits.setFlag(flags, 4, lootCrate != null);
		flags = Bits.setFlag(flags, 8, lootTableId != null);
		buffer.writeVarInt(flags);
		buffer.writeVarInt(rewards.size());

		for (WeightedReward reward : rewards) {
			buffer.writeVarInt(reward.reward.getType().intId);
			reward.reward.writeNetData(buffer);
			buffer.writeFloat(reward.weight);
		}

		if (lootCrate != null) {
			lootCrate.writeNetData(buffer);
		}

		if (lootTableId != null) {
			buffer.writeResourceLocation(lootTableId);
		}
	}

	@Override
	public void readNetData(FriendlyByteBuf buffer) {
		super.readNetData(buffer);
		filename = buffer.readUtf(Short.MAX_VALUE);
		emptyWeight = buffer.readFloat();
		lootSize = buffer.readVarInt();
		int flags = buffer.readVarInt();
		hideTooltip = Bits.getFlag(flags, 1);
		useTitle = Bits.getFlag(flags, 2);
		boolean hasCrate = Bits.getFlag(flags, 4);
		boolean hasLootTableId = Bits.getFlag(flags, 8);
		rewards.clear();
		int s = buffer.readVarInt();

		for (int i = 0; i < s; i++) {
			RewardType type = file.rewardTypeIds.get(buffer.readVarInt());
			Reward reward = type.provider.create(fakeQuest);
			reward.readNetData(buffer);
			float weight = buffer.readFloat();
			rewards.add(new WeightedReward(reward, weight));
		}

		lootCrate = null;

		if (hasCrate) {
			lootCrate = new LootCrate(this);
			lootCrate.readNetData(buffer);
		}

		lootTableId = hasLootTableId ? buffer.readResourceLocation() : null;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void getConfig(ConfigGroup config) {
		super.getConfig(config);
		config.addDouble("empty_weight", emptyWeight, v -> emptyWeight = v.floatValue(), 0, 0, Integer.MAX_VALUE);
		config.addInt("loot_size", lootSize, v -> lootSize = v, 1, 1, Integer.MAX_VALUE);
		config.addBool("hide_tooltip", hideTooltip, v -> hideTooltip = v, false);
		config.addBool("use_title", useTitle, v -> useTitle = v, false);

		if (lootCrate != null) {
			lootCrate.getConfig(config.getGroup("loot_crate").setNameKey("item.ftbquests.lootcrate"));
		}

		// TODO: Implement this: config.addString("loot_table_id", lootTableId == null ? "" : lootTableId.toString(), v -> lootTableId = v.isEmpty() ? null : new ResourceLocation(v), "");
	}

	@Override
	public void clearCachedData() {
		super.clearCachedData();

		for (WeightedReward reward : rewards) {
			reward.reward.clearCachedData();
		}
	}

	@Override
	public void deleteSelf() {
		file.rewardTables.remove(this);
		super.deleteSelf();
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void editedFromGUI() {
		QuestScreen gui = ClientUtils.getCurrentGuiAs(QuestScreen.class);

		if (gui != null && gui.isViewingQuest()) {
			gui.viewQuestPanel.refreshWidgets();
		} else {
			RewardTablesScreen gui1 = ClientUtils.getCurrentGuiAs(RewardTablesScreen.class);

			if (gui1 != null) {
				gui1.refreshWidgets();
			}
		}

		file.updateLootCrates();
	}

	@Override
	public void editedFromGUIOnServer() {
		file.updateLootCrates();
	}

	@Override
	public void onCreated() {
		if (filename.isEmpty()) {
			String s = titleToID(title).orElse(toString());
			filename = s;

			Set<String> existingNames = file.rewardTables.stream().map(rt -> rt.filename).collect(Collectors.toSet());
			int i = 2;

			while (existingNames.contains(filename)) {
				filename = s + "_" + i;
				i++;
			}
		}

		file.rewardTables.add(this);
	}

	public String getFilename() {
		if (filename.isEmpty()) {
			filename = getCodeString(this);
		}

		return filename;
	}

	@Override
	public String getPath() {
		return "reward_tables/" + getFilename() + ".snbt";
	}

	@Override
	@Environment(EnvType.CLIENT)
	public Component getAltTitle() {
		if (rewards.size() == 1) {
			return rewards.get(0).reward.getTitle();
		}

		return Component.translatable("ftbquests.reward_table");
	}

	@Override
	@Environment(EnvType.CLIENT)
	public Icon getAltIcon() {
		if (lootCrate != null) {
			return ItemIcon.getItemIcon(lootCrate.createStack());
		}

		if (rewards.isEmpty()) {
			return Icons.DICE;
		}

		List<Icon> icons = new ArrayList<>();

		for (WeightedReward reward : rewards) {
			icons.add(reward.reward.getIcon());
		}

		return IconAnimation.fromList(icons, false);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void onEditButtonClicked(Runnable gui) {
		new EditRewardTableScreen(this, () -> {
			new EditObjectMessage(this).sendToServer();
			clearCachedData();
		}).openGui();
	}

	public void addMouseOverText(TooltipList list, boolean includeWeight, boolean includeEmpty) {
		if (!hideTooltip) {
			float totalWeight = getTotalWeight(includeEmpty);

			if (includeWeight && includeEmpty && emptyWeight > 0) {
				addItem(list, Component.translatable("ftbquests.reward_table.nothing"), emptyWeight, totalWeight);
			}

			List<WeightedReward> sortedRewards = rewards.stream().sorted().toList();

			BaseScreen gui = ClientUtils.getCurrentGuiAs(BaseScreen.class);
			int maxLines = gui == null ? 12 : (gui.height - 20) / (gui.getTheme().getFontHeight() + 2);
			int nRewards = sortedRewards.size();
			int start = nRewards > maxLines ?
					(int) ((FTBQuests.PROXY.getClientPlayer().getLevel().getGameTime() / 10) % nRewards) :
					0;

			int nLines = Math.min(maxLines, nRewards);
			for (int idx = 0; idx < nLines; idx++) {
				WeightedReward r = sortedRewards.get((start + idx) % nRewards);
				if (includeWeight) {
					addItem(list, r.reward.getTitle(), r.weight, totalWeight);
				} else {
					list.add(Component.literal("- ").withStyle(ChatFormatting.GRAY).append(r.reward.getTitle()));
				}
			}
		}
	}

	private static void addItem(TooltipList list, Component text, float weight, float totalWeight) {
		list.add(Component.literal("- ").withStyle(ChatFormatting.GRAY).append(text)
				.append(Component.literal(" [" + WeightedReward.chanceString(weight, totalWeight) + "]").withStyle(ChatFormatting.DARK_GRAY)));
	}

	@Override
	public Set<RecipeModHelper.Components> componentsToRefresh() {
		return EnumSet.of(RecipeModHelper.Components.LOOT_CRATES);
	}
}
