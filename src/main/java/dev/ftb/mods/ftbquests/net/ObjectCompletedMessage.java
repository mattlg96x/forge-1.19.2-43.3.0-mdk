package dev.ftb.mods.ftbquests.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbquests.FTBQuests;
import net.minecraft.network.FriendlyByteBuf;

import java.util.Date;
import java.util.UUID;

/**
 * @author LatvianModder
 */
public class ObjectCompletedMessage extends BaseS2CMessage {
	private final UUID team;
	private final long id;

	public ObjectCompletedMessage(FriendlyByteBuf buffer) {
		team = buffer.readUUID();
		id = buffer.readLong();
	}

	public ObjectCompletedMessage(UUID t, long i) {
		team = t;
		id = i;
	}

	@Override
	public MessageType getType() {
		return FTBQuestsNetHandler.OBJECT_COMPLETED;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUUID(team);
		buffer.writeLong(id);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBQuests.NET_PROXY.objectCompleted(team, id, new Date());
	}
}