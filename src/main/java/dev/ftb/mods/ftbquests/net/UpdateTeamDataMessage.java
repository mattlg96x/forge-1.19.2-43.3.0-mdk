package dev.ftb.mods.ftbquests.net;

import dev.architectury.networking.NetworkManager;
import dev.architectury.networking.simple.BaseS2CMessage;
import dev.architectury.networking.simple.MessageType;
import dev.ftb.mods.ftbquests.FTBQuests;
import dev.ftb.mods.ftbquests.quest.TeamData;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class UpdateTeamDataMessage extends BaseS2CMessage {
	private final UUID team;
	private final String name;

	UpdateTeamDataMessage(FriendlyByteBuf buffer) {
		team = buffer.readUUID();
		name = buffer.readUtf(Short.MAX_VALUE);
	}

	public UpdateTeamDataMessage(TeamData data) {
		team = data.uuid;
		name = data.name;
	}

	@Override
	public MessageType getType() {
		return FTBQuestsNetHandler.UPDATE_TEAM_DATA;
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeUUID(team);
		buffer.writeUtf(name, Short.MAX_VALUE);
	}

	@Override
	public void handle(NetworkManager.PacketContext context) {
		FTBQuests.NET_PROXY.updateTeamData(team, name);
	}
}
