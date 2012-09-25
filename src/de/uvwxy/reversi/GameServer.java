package de.uvwxy.reversi;

import de.uvwxy.packsock.chat.ChatServer;

public class GameServer extends ChatServer {

	public GameServer(int port, int maxConnectionCount, String serverName) {
		super(port, maxConnectionCount, serverName);
	}

}
