package de.uvwxy.reversi;

import java.io.IOException;

import de.uvwxy.packsock.PackSock;
import de.uvwxy.packsock.Packet;
import de.uvwxy.packsock.PacketType;
import de.uvwxy.packsock.SocketPollPacketHookThread;
import de.uvwxy.packsock.chat.ChatClient;
import de.uvwxy.packsock.chat.ChatMessage;
import de.uvwxy.packsock.chat.IChatMessageHook;

public class GameClient extends ChatClient {

	public GameClient(int port, String address, IChatMessageHook msgHook) {
		super(port, address, msgHook);

	}

	
	@Override
	public void onMessageReceived(Packet p) {
		if (p == null) {
			return;
		}
		
		switch (p.getTypeByte()) {
		case PacketType.CHAT_MESSAGE:
			ChatMessage m = new ChatMessage(p.getPayloadAsBytes());
			if (msgHook != null)
				msgHook.onMessageReceived(m);
			break;
		case PacketType.GAME_DATA:
			
		default:
		}
	}
}
