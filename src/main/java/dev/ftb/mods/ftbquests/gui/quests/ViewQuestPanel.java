package dev.ftb.mods.ftbquests.gui.quests;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.ImageConfig;
import dev.ftb.mods.ftblibrary.config.ListConfig;
import dev.ftb.mods.ftblibrary.config.StringConfig;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigFromStringScreen;
import dev.ftb.mods.ftblibrary.config.ui.EditConfigScreen;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.*;
import dev.ftb.mods.ftblibrary.ui.input.Key;
import dev.ftb.mods.ftblibrary.ui.input.MouseButton;
import dev.ftb.mods.ftblibrary.ui.misc.CompactGridLayout;
import dev.ftb.mods.ftblibrary.util.ImageComponent;
import dev.ftb.mods.ftblibrary.util.TooltipList;
import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.client.ClientQuestFile;
import dev.ftb.mods.ftbquests.gui.ImageComponentWidget;
import dev.ftb.mods.ftbquests.gui.MultilineTextEditorScreen;
import dev.ftb.mods.ftbquests.net.EditObjectMessage;
import dev.ftb.mods.ftbquests.net.TogglePinnedMessage;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.QuestLink;
import dev.ftb.mods.ftbquests.quest.QuestObject;
import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.reward.RewardAutoClaim;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.quest.theme.QuestTheme;
import dev.ftb.mods.ftbquests.quest.theme.property.ThemeProperties;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class ViewQuestPanel extends Panel {
	public static final String PAGEBREAK_CODE = "{@pagebreak}";
	public static final Icon PAGEBREAK_ICON = Icon.getIcon(new ResourceLocation(FTBQuests.MOD_ID, "textures/gui/pagebreak.png"));

	public final QuestScreen questScreen;
	private Quest quest = null;
	public boolean hidePanel = false;
	private Icon icon = Color4I.EMPTY;
	public Button buttonClose;
	public Button buttonPin;
	public Button buttonOpenDependencies;
	public Button buttonOpenDependants;
	public BlankPanel panelContent;
	public BlankPanel panelTasks;
	public BlankPanel panelRewards;
	public BlankPanel panelText;
	private TextField titleField;
	private final List<Pair<Integer,Integer>> pageIndices = new ArrayList<>();
	private final Long2IntMap currentPages = new Long2IntOpenHashMap();
	private long lastScrollTime = 0L;

	public ViewQuestPanel(QuestScreen g) {
		super(g);
		questScreen = g;
		setPosAndSize(-1, -1, 0, 0);
		setOnlyRenderWidgetsInside(true);
		setOnlyInteractWithWidgetsInside(true);
	}

	public boolean viewingQuest() {
		return quest != null;
	}

	public boolean viewingQuest(Quest quest) {
		return this.quest == quest;
	}

	public Quest getViewedQuest() {
		return quest;
	}

	public boolean setViewedQuest(Quest newQuest) {
		if (quest != newQuest) {
			quest = newQuest;
			refreshWidgets();
			return true;
		}
		return false;
	}

	public boolean canEdit() {
		return quest.getQuestFile().canEdit();
	}

	private void buildPageIndices() {
		pageIndices.clear();

		if (quest != null) {
			int l1 = 0;
			for (int l2 = l1; l2 < quest.description.size(); l2++) {
				if (quest.description.get(l2).equals(PAGEBREAK_CODE)) {
					pageIndices.add(Pair.of(l1, l2 - 1));
					l1 = l2 + 1;
				}
			}
			if (l1 < quest.description.size()) {
				pageIndices.add(Pair.of(l1, quest.description.size() - 1));
			}
		}
	}

	private int getCurrentPage() {
		if (quest == null) {
			return 0;
		}
		int page = currentPages.getOrDefault(quest.id, 0);
		if (page < 0 || page >= pageIndices.size()) {
			page = 0;
			currentPages.put(quest.id, 0);
		}
		return page;
	}

	private void setCurrentPage(int page) {
		if (quest != null) {
			currentPages.put(quest.id, page);
		}
	}

	@Override
	public void addWidgets() {
		setPosAndSize(-1, -1, 1, 1);

		if (quest == null || hidePanel) {
			return;
		}

		QuestObjectBase prev = QuestTheme.currentObject;
		QuestTheme.currentObject = quest;

		setScrollX(0);
		setScrollY(0);

		icon = quest.getIcon();

		boolean canEdit = questScreen.file.canEdit();

		titleField = new QuestDescriptionField(this, canEdit, b -> editTitle())
				.addFlags(Theme.CENTERED)
				.setMinWidth(150).setMaxWidth(500).setSpacing(9)
				.setText(quest.getTitle().copy().withStyle(Style.EMPTY.withColor(TextColor.fromRgb(ThemeProperties.QUEST_VIEW_TITLE.get().rgb()))));
		int w = Math.max(200, titleField.width + 54);

		if (quest.minWidth > 0) {
			w = Math.max(quest.minWidth, w);
		} else if (questScreen.selectedChapter.defaultMinWidth > 0) {
			w = Math.max(questScreen.selectedChapter.defaultMinWidth, w);
		}

		titleField.setPosAndSize(27, 4, w - 54, titleField.height);
		add(titleField);

		add(panelContent = new BlankPanel(this, "ContentPanel"));
		panelContent.add(panelTasks = new BlankPanel(panelContent, "TasksPanel"));
		panelContent.add(panelRewards = new BlankPanel(panelContent, "RewardsPanel"));
		panelContent.add(panelText = new BlankPanel(panelContent, "TextPanel"));

		int bsize = 18;

		for (Task task : quest.tasks) {
			TaskButton b = new TaskButton(panelTasks, task);
			panelTasks.add(b);
			b.setSize(bsize, bsize);
		}

		if (!canEdit && panelTasks.widgets.isEmpty()) {
			DisabledButtonTextField noTasks = new DisabledButtonTextField(panelTasks, Component.translatable("ftbquests.gui.no_tasks"));
			noTasks.setSize(noTasks.width + 8, bsize);
			noTasks.setColor(ThemeProperties.DISABLED_TEXT_COLOR.get(quest));
			panelTasks.add(noTasks);
		}

		for (Reward reward : quest.rewards) {
			if (canEdit || !questScreen.file.self.isRewardBlocked(reward) && reward.getAutoClaimType() != RewardAutoClaim.INVISIBLE) {
				RewardButton b = new RewardButton(panelRewards, reward);
				panelRewards.add(b);
				b.setSize(bsize, bsize);
			}
		}

		if (!canEdit && panelRewards.widgets.isEmpty()) {
			DisabledButtonTextField noRewards = new DisabledButtonTextField(panelRewards, Component.translatable("ftbquests.gui.no_rewards"));
			noRewards.setSize(noRewards.width + 8, bsize);
			noRewards.setColor(ThemeProperties.DISABLED_TEXT_COLOR.get(quest));
			panelRewards.add(noRewards);
		}

		if (questScreen.file.canEdit()) {
			panelTasks.add(new AddTaskButton(panelTasks, quest));
			panelRewards.add(new AddRewardButton(panelRewards, quest));
		}

		int ww = 0;

		for (Widget widget : panelTasks.widgets) {
			ww = Math.max(ww, widget.width);
		}

		for (Widget widget : panelRewards.widgets) {
			ww = Math.max(ww, widget.width);
		}

		Color4I borderColor = ThemeProperties.WIDGET_BORDER.get(questScreen.selectedChapter);

		ww = Mth.clamp(ww, 70, 140);
		w = Math.max(w, ww * 2 + 10);

		if (ThemeProperties.FULL_SCREEN_QUEST.get(quest) == 1) {
			w = questScreen.width - 1;
		}

		if (w % 2 == 0) {
			w++;
		}

		setWidth(w);
		panelContent.setPosAndSize(0, Math.max(16, titleField.height + 8), w, 0);

		int iconSize = Math.min(16, titleField.height + 2);

		add(buttonClose = new CloseViewQuestButton());
		buttonClose.setPosAndSize(w - iconSize - 2, 4, iconSize, iconSize);

		add(buttonPin = new PinViewQuestButton());
		buttonPin.setPosAndSize(w - iconSize * 2 - 4, 4, iconSize, iconSize);

		if (questScreen.selectedChapter.id != quest.chapter.id) {
			GotoLinkedQuestButton b = new GotoLinkedQuestButton();
			add(b);
			b.setPosAndSize(iconSize + 4, 0, iconSize, iconSize);
		}

		List<QuestLink> links = new ArrayList<>();
		questScreen.file.chapterGroups.forEach(group -> group.chapters
				.forEach(chapter -> chapter.getQuestLinks().stream()
						.filter(link -> chapter != questScreen.selectedChapter && link.linksTo(quest))
						.forEach(links::add)
				)
		);
		var linksButton = new ViewQuestLinksButton(links);
		add(linksButton);
		linksButton.setPosAndSize(w - iconSize * 3 - 4, 0, iconSize, iconSize);

		if (!quest.hasDependencies()) {
			add(buttonOpenDependencies = new SimpleButton(this, Component.translatable("ftbquests.gui.no_dependencies"), Icon.getIcon(FTBQuests.MOD_ID + ":textures/gui/arrow_left.png").withTint(borderColor), (widget, button) -> {
			}));
		} else {
			add(buttonOpenDependencies = new SimpleButton(this, Component.translatable("ftbquests.gui.view_dependencies"), Icon.getIcon(FTBQuests.MOD_ID + ":textures/gui/arrow_left.png").withTint(ThemeProperties.QUEST_VIEW_TITLE.get()), (widget, button) -> showList(quest.getDependencies().toList(), true)));
		}

		if (quest.getDependants().isEmpty()) {
			add(buttonOpenDependants = new SimpleButton(this, Component.translatable("ftbquests.gui.no_dependants"), Icon.getIcon(FTBQuests.MOD_ID + ":textures/gui/arrow_right.png").withTint(borderColor), (widget, button) -> {
			}));
		} else {
			add(buttonOpenDependants = new SimpleButton(this, Component.translatable("ftbquests.gui.view_dependants"), Icon.getIcon(FTBQuests.MOD_ID + ":textures/gui/arrow_right.png").withTint(ThemeProperties.QUEST_VIEW_TITLE.get()), (widget, button) -> showList(quest.getDependants(), false)));
		}

		buttonOpenDependencies.setPosAndSize(0, panelContent.posY + 2, 13, 13);
		buttonOpenDependants.setPosAndSize(w - 13, panelContent.posY + 2, 13, 13);

		TextField textFieldTasks = new TextField(panelContent) {
			@Override
			public TextField resize(Theme theme) {
				return this;
			}
		};

		int w2 = w / 2;

		textFieldTasks.setPosAndSize(2, 2, w2 - 3, 13);
		textFieldTasks.setMaxWidth(w);
		textFieldTasks.addFlags(Theme.CENTERED | Theme.CENTERED_V);
		textFieldTasks.setText(Component.translatable("ftbquests.tasks"));
		textFieldTasks.setColor(ThemeProperties.TASKS_TEXT_COLOR.get(quest));
		panelContent.add(textFieldTasks);

		TextField textFieldRewards = new TextField(panelContent) {
			@Override
			public TextField resize(Theme theme) {
				return this;
			}
		};

		textFieldRewards.setPosAndSize(w2 + 2, 2, w2 - 3, 13);
		textFieldRewards.setMaxWidth(w);
		textFieldRewards.addFlags(Theme.CENTERED | Theme.CENTERED_V);
		textFieldRewards.setText(Component.translatable("ftbquests.rewards"));
		textFieldRewards.setColor(ThemeProperties.REWARDS_TEXT_COLOR.get(quest));
		panelContent.add(textFieldRewards);

		panelTasks.setPosAndSize(2, 16, w2 - 3, 0);
		panelRewards.setPosAndSize(w2 + 2, 16, w2 - 3, 0);

		int at = panelTasks.align(new CompactGridLayout(bsize + 2));
		int ar = panelRewards.align(new CompactGridLayout(bsize + 2));

		int h = Math.max(at, ar);
		panelTasks.setHeight(h);
		panelRewards.setHeight(h);

		int tox = (panelTasks.width - panelTasks.getContentWidth()) / 2;
		int rox = (panelRewards.width - panelRewards.getContentWidth()) / 2;
		int toy = (panelTasks.height - panelTasks.getContentHeight()) / 2;
		int roy = (panelRewards.height - panelRewards.getContentHeight()) / 2;

		for (Widget widget : panelTasks.widgets) {
			widget.setX(widget.posX + tox);
			widget.setY(widget.posY + toy);
		}

		for (Widget widget : panelRewards.widgets) {
			widget.setX(widget.posX + rox);
			widget.setY(widget.posY + roy);
		}

		panelText.setPosAndSize(3, 16 + h + 12, w - 6, 0);

		Component subtitle = quest.getSubtitle();

		if (subtitle.getContents() == ComponentContents.EMPTY && canEdit) {
			subtitle = Component.literal("[No Subtitle]");
		}

		if (!subtitle.equals(Component.empty())) {
			panelText.add(new QuestDescriptionField(panelText, canEdit, b -> editSubtitle())
					.addFlags(Theme.CENTERED)
					.setMinWidth(panelText.width).setMaxWidth(panelText.width)
					.setSpacing(9)
					.setText(Component.literal("").append(subtitle).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY)));
		}

		boolean showText = !quest.hideTextUntilComplete.get(false) || questScreen.file.self != null && questScreen.file.self.isCompleted(quest);

		buildPageIndices();

		if (showText && !pageIndices.isEmpty()) {
			addDescriptionText(canEdit, subtitle);
		}

		if (showText && !quest.guidePage.isEmpty()) {
			if (subtitle.getContents() != ComponentContents.EMPTY) {
				panelText.add(new VerticalSpaceWidget(panelText, 7));
			}

			panelText.add(new OpenInGuideButton(panelText, quest));

			panelText.add(new OpenInGuideButton(panelText, quest));
		}

		if (pageIndices.size() > 1 || canEdit) {
			addButtonBar(canEdit);
		}

		if (panelText.widgets.isEmpty()) {
			panelContent.add(new ColorWidget(panelContent, borderColor, null).setPosAndSize(w2, 0, 1, h + 40));
			panelText.setHeight(0);
			setHeight(Math.min(panelContent.getContentHeight(), parent.height - 10));
		} else {
			panelContent.add(new ColorWidget(panelContent, borderColor, null).setPosAndSize(w2, 0, 1, 16 + h + 6));
			panelContent.add(new ColorWidget(panelContent, borderColor, null).setPosAndSize(1, 16 + h + 6, w - 2, 1));
			panelText.setHeight(panelText.align(new WidgetLayout.Vertical(0, 1, 2)));
			setHeight(Math.min(panelContent.getContentHeight() + titleField.height + 12, parent.height - 10));
		}

		if (ThemeProperties.FULL_SCREEN_QUEST.get(quest) == 1) {
			height = questScreen.height;
		}

		setPos((parent.width - width) / 2, (parent.height - height) / 2);
		panelContent.setHeight(height - 17);

		QuestTheme.currentObject = prev;
	}

	private void addDescriptionText(boolean canEdit, Component subtitle) {
		Pair<Integer,Integer> pageSpan = pageIndices.get(getCurrentPage());
		if (subtitle.getContents() != ComponentContents.EMPTY) {
			panelText.add(new VerticalSpaceWidget(panelText, 7));
		}

		for (int i = pageSpan.getFirst(); i <= pageSpan.getSecond() && i < quest.getDescription().length; i++) {
			Component component = quest.getDescription()[i];

			ImageComponent img = findImageComponent(component);
			if (img != null) {
				ImageComponentWidget c = new ImageComponentWidget(this, panelText, img, i);

				if (c.component.fit) {
					double scale = panelText.width / (double) c.width;
					c.setSize((int) (c.width * scale), (int) (c.height * scale));
				} else if (c.component.align == 1) {
					c.setX((panelText.width - c.width) / 2);
				} else if (c.component.align == 2) {
					c.setX(panelText.width - c.width);
				} else {
					c.setX(0);
				}

				panelText.add(c);
			} else {
				final int line = i;
				TextField field = new QuestDescriptionField(panelText, canEdit, context -> editDescLine(line, context, null))
						.setMaxWidth(panelText.width).setSpacing(9).setText(component);
				field.setWidth(panelText.width);
				panelText.add(field);
			}
		}
	}

	private void addButtonBar(boolean canEdit) {
		// button bar has page navigation buttons for multi-page text, and the Add button in edit mode

		panelText.add(new VerticalSpaceWidget(panelText, 3));

		Panel buttonPanel = new BlankPanel(panelText);
		buttonPanel.setSize(panelText.width, 14);
		panelText.add(buttonPanel);

		int currentPage = getCurrentPage();

		Component page = Component.literal((currentPage + 1) + "/" + pageIndices.size()).withStyle(ChatFormatting.GRAY);
		int labelWidth = questScreen.getTheme().getStringWidth(page);

		if (currentPage > 0) {
			SimpleTextButton prevPage = new SimpleTextButton(buttonPanel, Component.empty(), ThemeProperties.LEFT_ARROW.get()) {
				@Override
				public void onClicked(MouseButton mouseButton) {
					setCurrentPage(Math.max(0, currentPage - 1));
					refreshWidgets();
				}

				@Override
				public void addMouseOverText(TooltipList list) {
					list.add(Component.literal("[Page Up]").withStyle(ChatFormatting.DARK_GRAY));
					list.add(Component.literal("[Mousewheel Up]").withStyle(ChatFormatting.DARK_GRAY));
				}
			};
			prevPage.setX(panelText.width - 43 - labelWidth);
			prevPage.setSize(16, 14);
			buttonPanel.add(prevPage);
		}
		if (pageIndices.size() > 1) {
			TextField pageLabel = new TextField(buttonPanel);
			pageLabel.setText(page);
			pageLabel.setPosAndSize(panelText.width - 24 - labelWidth, 3, 20, 14);
			buttonPanel.add(pageLabel);
		}
		if (currentPage < pageIndices.size() - 1) {
			SimpleTextButton nextPage = new SimpleTextButton(buttonPanel, Component.empty(), ThemeProperties.RIGHT_ARROW.get()) {
				@Override
				public void onClicked(MouseButton mouseButton) {
					setCurrentPage(Math.min(pageIndices.size() + 1, currentPage + 1));
					refreshWidgets();
				}

				@Override
				public void addMouseOverText(TooltipList list) {
					list.add(Component.literal("[Page Down]").withStyle(ChatFormatting.DARK_GRAY));
					list.add(Component.literal("[Mousewheel Down]").withStyle(ChatFormatting.DARK_GRAY));
				}
			};
			nextPage.setSize(16, 14);
			nextPage.setX(panelText.width - 5 - nextPage.width);
			buttonPanel.add(nextPage);
		}

		if (canEdit) {
			SimpleTextButton edit = new SimpleTextButton(buttonPanel, Component.translatable("ftbquests.gui.edit").append(" ▼"), ThemeProperties.EDIT_ICON.get()) {
				@Override
				public void onClicked(MouseButton mouseButton) {
					openEditButtonContextMenu();
				}
			};

			edit.setX((panelText.width - edit.width) / 2);
			edit.setHeight(14);
			buttonPanel.add(edit);
		}
	}

	private ImageComponent findImageComponent(Component c) {
		// FIXME: this isn't ideal and needs a proper fix in ftb library, but works for now
		for (Component c1 : c.getSiblings()) {
			if (c1.getContents() instanceof ImageComponent img) {
				return img;
			} else {
				return findImageComponent(c1);
			}
		}
		return null;
	}

	@Override
	public void alignWidgets() {
	}

	@Override
	public void tick() {
		super.tick();

		if (quest != null && quest.hasDependencies() && !questScreen.file.self.canStartTasks(quest) && buttonOpenDependencies != null) {
			float red = Mth.sin((System.currentTimeMillis() % 1200) * (3.1415927f / 1200f));
			Color4I col = Color4I.rgb((int) (red * 127 + 63), 0, 0);
			buttonOpenDependencies.setIcon(Icon.getIcon(FTBQuests.MOD_ID + ":textures/gui/arrow_left.png").withTint(col));
		}
	}

	private void showList(Collection<QuestObject> c, boolean dependencies) {
		int hidden = 0;
		List<ContextMenuItem> contextMenu = new ArrayList<>();

		if (dependencies && quest.minRequiredDependencies > 0) {
			contextMenu.add(new ContextMenuItem(
					Component.translatable("ftbquests.quest.min_required_header", quest.minRequiredDependencies)
							.withStyle(ChatFormatting.UNDERLINE), Color4I.EMPTY, null).setEnabled(false)
			);
		}

		for (QuestObject object : c) {
			if (questScreen.file.canEdit() || object.isSearchable(questScreen.file.self)) {
				MutableComponent title = object.getMutableTitle();
				if (object.getQuestChapter() != null && object.getQuestChapter() != quest.getQuestChapter()) {
					Component suffix = Component.literal(" [").append(object.getQuestChapter().getTitle()).append("]").withStyle(ChatFormatting.GRAY);
					title.append(suffix);
				}
				contextMenu.add(new ContextMenuItem(title, Color4I.EMPTY, () -> questScreen.open(object, true)));
			} else {
				hidden++;
			}
		}

		if (hidden > 0) {
			MutableComponent prefix = hidden == c.size() ? Component.empty() : Component.literal("+ ");
			contextMenu.add(new ContextMenuItem(
					prefix.append(Component.translatable("ftbquests.quest.hidden_quests_footer", hidden)), Color4I.EMPTY, null).setEnabled(false)
			);
		}

		getGui().openContextMenu(contextMenu);
	}

	@Override
	public void keyReleased(Key key) {
		// released rather than pressed; if we used pressed, keypress would be picked up by the next screen

		if (hidePanel || quest == null) return;

		if (questScreen.file.canEdit()) {
			if (key.is(GLFW.GLFW_KEY_S)) {
				editSubtitle();
			} else if (key.is(GLFW.GLFW_KEY_T)) {
				editTitle();
			} else if (key.is(GLFW.GLFW_KEY_D)) {
				editDescription();
			} else if (key.is(GLFW.GLFW_KEY_P)) {
				addPageBreak();
			} else if (key.is(GLFW.GLFW_KEY_L)) {
				editDescLine0(-1, null);
			} else if (key.is(GLFW.GLFW_KEY_I)) {
				editDescLine0(-1, new ImageComponent());
			}
		}

		if (key.is(GLFW.GLFW_KEY_PAGE_UP) || key.is(GLFW.GLFW_KEY_LEFT)) {
			setCurrentPage(Math.max(0, getCurrentPage() - 1));
			refreshWidgets();
		} else if (key.is(GLFW.GLFW_KEY_PAGE_DOWN) || key.is(GLFW.GLFW_KEY_RIGHT)) {
			setCurrentPage(Math.min(pageIndices.size() - 1, getCurrentPage() + 1));
			refreshWidgets();
		}
	}

	private void editTitle() {
		StringConfig c = new StringConfig(null);

		// pressing T while mousing over a task button allows editing the task title
		QuestObject qo = quest;
		String titleKey = "ftbquests.title";
		for (Widget w : panelTasks.widgets) {
			if (w instanceof TaskButton b && b.isMouseOver()) {
				qo = b.task;
				titleKey = "ftbquests.task_title";
				break;
			}
		}

		final var qo1 = qo;
		EditConfigFromStringScreen.open(c, qo1.title, "", Component.translatable(titleKey), accepted -> {
			if (accepted) {
				qo1.title = c.value;
				new EditObjectMessage(qo1).sendToServer();
			}

			openGui();
		});
	}

	private void editSubtitle() {
		StringConfig c = new StringConfig(null);

		EditConfigFromStringScreen.open(c, quest.subtitle, "", Component.translatable("ftbquests.quest.subtitle"), accepted -> {
			if (accepted) {
				quest.subtitle = c.value;
				new EditObjectMessage(quest).sendToServer();
			}

			openGui();
		});
	}

	private void editDescription() {
		ListConfig<String, StringConfig> lc = new ListConfig<>(new StringConfig());
		lc.value = quest.description;
		new MultilineTextEditorScreen(Component.translatable("ftbquests.gui.edit_description"), lc, accepted -> {
			if (accepted) {
				new EditObjectMessage(quest).sendToServer();
				refreshWidgets();
			}
			openGui();
		}).openGui();
	}

	private void openEditButtonContextMenu() {
		List<ContextMenuItem> contextMenu = new ArrayList<>();

		contextMenu.add(new ContextMenuItem(Component.translatable("ftbquests.title").append(hotkey("T")),
				Icons.NOTES,
				this::editTitle));
		contextMenu.add(new ContextMenuItem(Component.translatable("ftbquests.quest.subtitle").append(hotkey("S")),
				Icons.NOTES,
				this::editSubtitle));
		contextMenu.add(new ContextMenuItem(Component.translatable("ftbquests.quest.description").append(hotkey("D")),
				Icons.NOTES,
				this::editDescription));

		contextMenu.add(ContextMenuItem.SEPARATOR);

		contextMenu.add(new ContextMenuItem(Component.translatable("ftbquests.gui.line").append(hotkey("L")),
				Icons.NOTES,
				() -> editDescLine0(-1, null)));
		contextMenu.add(new ContextMenuItem(Component.translatable("ftbquests.gui.page_break").append(hotkey("P")),
				PAGEBREAK_ICON,
				this::addPageBreak));
		contextMenu.add(new ContextMenuItem(Component.translatable("ftbquests.gui.image").append(hotkey("I")),
				Icons.ART,
				() -> editDescLine0(-1, new ImageComponent())));

		getGui().openContextMenu(contextMenu);
	}

	private static Component hotkey(String key) {
		return Component.literal(" [" + key + "]").withStyle(ChatFormatting.DARK_GRAY);
	}

	private void addPageBreak() {
		appendToPage(quest.description, List.of(PAGEBREAK_CODE, "(new page placeholder text)"), getCurrentPage());
		new EditObjectMessage(quest).sendToServer();
		setCurrentPage(Math.min(pageIndices.size() - 1, getCurrentPage() + 1));
		refreshWidgets();
	}

	private void editDescLine0(int line, @Nullable Object type) {
		if (type instanceof ImageComponent) {
			editImage(line, (ImageComponent) type);
			return;
		}

		StringConfig c = new StringConfig(null);

		int l = line + 1;
		int s = quest.description.size();
		if (l == 0) {
			// adding a new line
			l = quest.description.size() + 1;
			s++;
		}
		Component title = Component.translatable("ftbquests.quest.description").append(String.format(": %d/%d", l, s));

		EditConfigFromStringScreen.open(c, line == -1 ? "" : quest.description.get(line), "", title, accepted -> {
			if (accepted) {
				if (line == -1) {
					appendToPage(quest.description, List.of(c.value), getCurrentPage());
				} else {
					quest.description.set(line, c.value);
				}

				new EditObjectMessage(quest).sendToServer();
				refreshWidgets();
			}

			openGui();
		});
	}

	private void editImage(int line, ImageComponent component) {
		ConfigGroup group = new ConfigGroup(FTBQuests.MOD_ID);
		//task.getConfig(task.createSubGroup(group));

		group.add("image", new ImageConfig(), component.image.toString(), v -> component.image = Icon.getIcon(v), "");
		group.addInt("width", component.width, v -> component.width = v, 0, 1, 1000);
		group.addInt("height", component.height, v -> component.height = v, 0, 1, 1000);
		group.addInt("align", component.align, v -> component.align = v, 0, 1, 2);
		group.addBool("fit", component.fit, v -> component.fit = v, false);

		group.savedCallback = accepted -> {
			openGui();
			if (accepted) {
				if (line == -1) {
					appendToPage(quest.description, List.of(component.toString()), getCurrentPage());
				} else {
					quest.description.set(line, component.toString());
				}
				new EditObjectMessage(quest).sendToServer();

				refreshWidgets();
			}
		};

		new EditConfigScreen(group).openGui();
	}

	private void appendToPage(List<String> list, List<String> toAdd, int pageNumber) {
		if (pageIndices.isEmpty()) {
			list.addAll(toAdd);
			buildPageIndices();
		} else {
			int idx = pageIndices.get(pageNumber).getSecond() + 1;

			for (String line : toAdd) {
				list.add(idx, line);
				idx++;
			}
		}
	}

	public void editDescLine(int line, boolean context, @Nullable Object type) {
		if (context) {
			List<ContextMenuItem> contextMenu = new ArrayList<>();
			contextMenu.add(new ContextMenuItem(Component.translatable("selectServer.edit"), ThemeProperties.EDIT_ICON.get(), () -> editDescLine0(line, type)));
			contextMenu.add(new ContextMenuItem(Component.translatable("selectServer.delete"), ThemeProperties.DELETE_ICON.get(), () -> {
				quest.description.remove(line);
				new EditObjectMessage(quest).sendToServer();
				refreshWidgets();
			}));

			getGui().openContextMenu(contextMenu);
		} else {
			editDescLine0(line, type);
		}
	}

	@Override
	public void draw(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		if (quest != null && !hidePanel) {
			QuestObjectBase prev = QuestTheme.currentObject;
			QuestTheme.currentObject = quest;
			matrixStack.pushPose();
			matrixStack.translate(0, 0, 500);
			super.draw(matrixStack, theme, x, y, w, h);
			matrixStack.popPose();
			QuestTheme.currentObject = prev;
		}
	}

	@Override
	public void drawBackground(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
		Color4I borderColor = ThemeProperties.QUEST_VIEW_BORDER.get();
		Color4I.DARK_GRAY.withAlpha(120).draw(matrixStack, questScreen.getX(), questScreen.getY(), questScreen.width, questScreen.height);
		Icon background = ThemeProperties.QUEST_VIEW_BACKGROUND.get();
		background.draw(matrixStack, x, y, w, h);
		if (titleField != null && panelContent != null) {
			int iconSize = Math.min(16, titleField.height + 2);
			icon.draw(matrixStack, x + 4, y + 4, iconSize, iconSize);
			borderColor.draw(matrixStack, x + 1, panelContent.getY(), w - 2, 1);
		}
	}

	@Override
	public boolean mousePressed(MouseButton button) {
		return super.mousePressed(button) || isMouseOver();
	}

	@Override
	public boolean mouseScrolled(double scroll) {
		long now = System.currentTimeMillis();

		if (super.mouseScrolled(scroll)) {
			lastScrollTime = now;
			return true;
		}

		if (now - lastScrollTime > 500L) {
			if (scroll < 0 && getCurrentPage() < pageIndices.size() - 1) {
				setCurrentPage(getCurrentPage() + 1);
				refreshWidgets();
				lastScrollTime = now;
				return true;
			} else if (scroll > 0 && getCurrentPage() > 0) {
				setCurrentPage(getCurrentPage() - 1);
				refreshWidgets();
				lastScrollTime = now;
				return true;
			}
		}

		return false;
	}

	private class QuestDescriptionField extends TextField {
		private final boolean canEdit;
		private final Consumer<Boolean> editCallback;

		QuestDescriptionField(Panel panel, boolean canEdit, Consumer<Boolean> editCallback) {
			super(panel);
			this.canEdit = canEdit;
			this.editCallback = editCallback;
		}

		@Override
		public boolean mousePressed(MouseButton button) {
			if (isMouseOver()) {
				if (canEdit && button.isRight()) {
					editCallback.accept(true);
					return true;
				} else if (button.isLeft() && Minecraft.getInstance().screen != null) {
					Style style = getComponentStyleAt(questScreen.getTheme(), getMouseX(), getMouseY());
					if (style != null) {
						return handleCustomClickEvent(style) || Minecraft.getInstance().screen.handleComponentClicked(style);
					}
				}
			}

			return super.mousePressed(button);
		}

		private boolean handleCustomClickEvent(Style style) {
			if (style == null) return false;

			ClickEvent clickEvent = style.getClickEvent();
			if (clickEvent == null) return false;

			if (clickEvent.getAction() == ClickEvent.Action.CHANGE_PAGE) {
				try {
					long questId = Long.valueOf(clickEvent.getValue(), 16);
					QuestObject qo = FTBQuests.PROXY.getQuestFile(true).get(questId);
					if (qo != null) {
						questScreen.open(qo, false);
					} else {
						errorToPlayer("Unknown quest object id: %s", clickEvent.getValue());
					}
				} catch (NumberFormatException e) {
					errorToPlayer("Invalid quest object id: %s (%s)",clickEvent.getValue(), e.getMessage());
				}
				return true;
			} else if (clickEvent.getAction() == ClickEvent.Action.OPEN_URL) {
				try {
					URI uri = new URI(clickEvent.getValue());
					String scheme = uri.getScheme();
					if (scheme == null) {
						throw new URISyntaxException(clickEvent.getValue(), "Missing protocol");
					}
					if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
						throw new URISyntaxException(clickEvent.getValue(), "Unsupported protocol: " + scheme.toLowerCase(Locale.ROOT));
					}

					final Screen curScreen = Minecraft.getInstance().screen;
					Minecraft.getInstance().setScreen(new ConfirmLinkScreen(accepted -> {
						if (accepted) {
							Util.getPlatform().openUri(uri);
						}
						Minecraft.getInstance().setScreen(curScreen);
					}, clickEvent.getValue(), false));
					return true;
				} catch (URISyntaxException e) {
					errorToPlayer("Can't open url for %s (%s)", clickEvent.getValue(), e.getMessage());
				}
				return true;
			}
			return false;
		}

		private void errorToPlayer(String msg, Object... args) {
			QuestScreen.displayError(Component.literal(String.format(msg, args)).withStyle(ChatFormatting.RED));
		}

		@Override
		public boolean mouseDoubleClicked(MouseButton button) {
			if (isMouseOver() && canEdit) {
				editCallback.accept(false);
				return true;
			}

			return false;
		}

		@Override
		@Nullable
		public CursorType getCursor() {
			return canEdit ? CursorType.IBEAM : null;
		}

		@Override
		public void addMouseOverText(TooltipList list) {
			if (!isMouseOver()) return;

			super.addMouseOverText(list);

			Style style = getComponentStyleAt(questScreen.getTheme(), getMouseX(), getMouseY());
			if (style != null && style.getHoverEvent() != null) {
				HoverEvent hoverevent = style.getHoverEvent();
				HoverEvent.ItemStackInfo stackInfo = hoverevent.getValue(HoverEvent.Action.SHOW_ITEM);
				Minecraft mc = Minecraft.getInstance();
				TooltipFlag flag = mc.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL;
				if (stackInfo != null) {
					stackInfo.getItemStack().getTooltipLines(mc.player, flag).forEach(list::add);
				} else {
					HoverEvent.EntityTooltipInfo entityInfo = hoverevent.getValue(HoverEvent.Action.SHOW_ENTITY);
					if (entityInfo != null) {
						if (flag.isAdvanced()) {
							entityInfo.getTooltipLines().forEach(list::add);
						}
					} else {
						Component component = hoverevent.getValue(HoverEvent.Action.SHOW_TEXT);
						if (component != null) {
							list.add(component);
						}
					}
				}
			}
		}
	}

	private abstract class AbstractPanelButton extends SimpleTextButton {
		public AbstractPanelButton(Component txt, Icon icon) {
			super(ViewQuestPanel.this, txt, icon);
		}

		@Override
		public void draw(PoseStack matrixStack, Theme theme, int x, int y, int w, int h) {
			drawIcon(matrixStack, theme, x + 1, y + 1, w - 2, h - 2);
		}
	}

	private class GotoLinkedQuestButton extends AbstractPanelButton {
		public GotoLinkedQuestButton() {
			super(Component.translatable("ftbquests.gui.goto_linked_quest", quest.chapter.getTitle().copy().withStyle(ChatFormatting.YELLOW)), ThemeProperties.LINK_ICON.get());
		}

		@Override
		public void onClicked(MouseButton button) {
			double qx = quest.x + 0.5D;
			double qy = quest.y + 0.5D;
			questScreen.selectChapter(quest.chapter);
			questScreen.questPanel.scrollTo(qx, qy);
		}
	}

	private class ViewQuestLinksButton extends AbstractPanelButton {
		private final List<QuestLink> links;

		public ViewQuestLinksButton(Collection<QuestLink> links) {
			super(Component.translatable("ftbquests.gui.view_quest_links"), ThemeProperties.LINK_ICON.get());
			this.links = List.copyOf(links);
		}

		@Override
		public void onClicked(MouseButton button) {
			List<ContextMenuItem> items = new ArrayList<>();
			for (QuestLink link : links) {
				link.getQuest().ifPresent(quest -> {
					Component title = quest.getTitle().copy().append(": ").append(link.getChapter().getTitle().copy().withStyle(ChatFormatting.YELLOW));
					items.add(new ContextMenuItem(title, quest.getIcon(), () -> gotoLink(link)));
				});
			}
			if (!items.isEmpty()) {
				ViewQuestPanel.this.questScreen.openContextMenu(items);
			}
		}

		private void gotoLink(QuestLink link) {
			questScreen.closeQuest();
			questScreen.selectChapter(link.getChapter());
			questScreen.questPanel.scrollTo(link.getX() + 0.5D, link.getX() + 0.5D);
		}

		@Override
		public boolean isEnabled() {
			return !links.isEmpty();
		}

		@Override
		public boolean shouldDraw() {
			return !links.isEmpty();
		}
	}

	private class PinViewQuestButton extends AbstractPanelButton {
		private PinViewQuestButton() {
			super(Component.translatable(ClientQuestFile.isQuestPinned(quest.id) ? "ftbquests.gui.unpin" : "ftbquests.gui.pin"),
					ClientQuestFile.isQuestPinned(quest.id) ? ThemeProperties.PIN_ICON_ON.get() : ThemeProperties.PIN_ICON_OFF.get());
		}

		@Override
		public void onClicked(MouseButton button) {
			playClickSound();
			new TogglePinnedMessage(quest.id).sendToServer();
		}
	}

	private class CloseViewQuestButton extends AbstractPanelButton {
		private CloseViewQuestButton() {
			super(Component.translatable("gui.close"), ThemeProperties.CLOSE_ICON.get(quest));
		}

		@Override
		public void onClicked(MouseButton button) {
			playClickSound();
			questScreen.closeQuest();
		}
	}
}
