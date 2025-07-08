package dev.ftb.mods.ftbquests.quest;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.icon.Icon;
import dev.ftb.mods.ftblibrary.util.ClientUtils;
import dev.ftb.mods.ftbquests.gui.quests.QuestScreen;
import dev.ftb.mods.ftbquests.net.MoveMovableMessage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public class QuestLink extends QuestObject implements Movable {
    private Chapter chapter;
    private long linkId;

    private double x;
    private double y;
    private String shape;
    private double size;

    public QuestLink(Chapter chapter, long linkId) {
        this.chapter = chapter;
        this.linkId = linkId;

        shape = "";
        size = 1D;
    }

    @Override
    public QuestObjectType getObjectType() {
        return QuestObjectType.QUEST_LINK;
    }

    @Override
    public QuestFile getQuestFile() {
        return chapter.file;
    }

    @Override
    public Component getAltTitle() {
        return getQuest().map(Quest::getAltTitle).orElse(Component.empty());
    }

    @Override
    public Icon getAltIcon() {
        return getQuest().map(Quest::getAltIcon).orElse(null);
    }

    @Override
    public int getRelativeProgressFromChildren(TeamData data) {
        return 0;
    }

    public Optional<Quest> getQuest() {
        return chapter.file.get(linkId) instanceof Quest q ? Optional.of(q) : Optional.empty();
    }

    @Override
    public long getParentID() {
        return chapter.id;
    }

    @Override
    public void onCreated() {
        chapter.getQuestLinks().add(this);
    }

    @Override
    public void deleteSelf() {
        super.deleteSelf();
        chapter.getQuestLinks().remove(this);
    }

    @Override
    public void getConfig(ConfigGroup config) {
        super.getConfig(config);

        config.addEnum("shape", shape.isEmpty() ? "default" : shape, v -> shape = v.equals("default") ? "" : v, QuestShape.idMapWithDefault);
        config.addDouble("size", size, v -> size = v, 1, 0.0625D, 8D);
        config.addDouble("x", x, v -> x = v, 0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        config.addDouble("y", y, v -> y = v, 0, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void editedFromGUI() {
        QuestScreen gui = ClientUtils.getCurrentGuiAs(QuestScreen.class);

        if (gui != null) {
            gui.questPanel.refreshWidgets();
            gui.viewQuestPanel.refreshWidgets();
        }
    }

    @Override
    public void writeData(CompoundTag nbt) {
        super.writeData(nbt);

        nbt.putString("linked_quest", getCodeString(linkId));
        nbt.putDouble("x", x);
        nbt.putDouble("y", y);
        if (!shape.isEmpty()) {
            nbt.putString("shape", shape);
        }
        if (size != 1D) {
            nbt.putDouble("size", size);
        }
    }

    @Override
    public void readData(CompoundTag nbt) {
        super.readData(nbt);

        linkId = Long.parseLong(nbt.getString("linked_quest"), 16);
        x = nbt.getDouble("x");
        y = nbt.getDouble("y");
        shape = nbt.getString("shape");
        if (shape.equals("default")) {
            shape = "";
        }
        size = nbt.contains("size") ? nbt.getDouble("size") : 1D;
    }

    @Override
    public void writeNetData(FriendlyByteBuf buffer) {
        super.writeNetData(buffer);

        buffer.writeLong(linkId);
        buffer.writeDouble(x);
        buffer.writeDouble(y);
        buffer.writeDouble(size);
        buffer.writeUtf(shape);
    }

    @Override
    public void readNetData(FriendlyByteBuf buffer) {
        super.readNetData(buffer);

        linkId = buffer.readLong();
        x = buffer.readDouble();
        y = buffer.readDouble();
        size = buffer.readDouble();
        shape = buffer.readUtf(64);
    }

    public void setPosition(double qx, double qy) {
        x = qx;
        y = qy;
    }

    @Override
    public long getMovableID() {
        return id;
    }

    @Override
    public Chapter getChapter() {
        return chapter;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getWidth() {
        return size;
    }

    @Override
    public double getHeight() {
        return size;
    }

    @Override
    public String getShape() {
        return shape.isEmpty() ? chapter.getDefaultQuestShape() : shape;
    }

    @Override
    public void move(Chapter to, double x, double y) {
        new MoveMovableMessage(this, to.id, x, y).sendToServer();
    }

    @Override
    public void onMoved(double x, double y, long chapterId) {
        this.x = x;
        this.y = y;

        if (chapterId != chapter.id) {
            QuestFile f = getQuestFile();
            Chapter newChapter = f.getChapter(chapterId);

            if (newChapter != null) {
                chapter.getQuestLinks().remove(this);
                newChapter.getQuestLinks().add(this);
                chapter = newChapter;
            }
        }
    }

    public boolean linksTo(Quest quest) {
        return linkId == quest.id;
    }
}
