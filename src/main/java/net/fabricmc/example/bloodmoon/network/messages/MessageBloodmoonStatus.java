package net.fabricmc.example.bloodmoon.network.messages;


import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketByteBuf;

public class MessageBloodmoonStatus {
	public boolean bloodmoonActive;

	public MessageBloodmoonStatus(boolean bloodmoonActive) {
		this.bloodmoonActive = bloodmoonActive;
	}

	public MessageBloodmoonStatus(PacketByteBuf buf) {
		this.bloodmoonActive = buf.readBoolean();
	}

	public void toBytes(PacketByteBuf buf) {
		buf.writeBoolean(bloodmoonActive);
	}

	public MessageBloodmoonStatus setStatus(boolean active) {
		this.bloodmoonActive = active;
		return this;
	}

	@Override
	public String toString() {
		return "MessageBloodmoonStatus{" +
				"bloodmoonActive=" + bloodmoonActive +
				'}';
	}
}