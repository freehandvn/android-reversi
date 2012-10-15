package de.uvwxy.reversi;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import de.uvwxy.packsock.PackSock;
import de.uvwxy.packsock.Packet;
import de.uvwxy.packsock.PacketType;
import de.uvwxy.packsock.chat.ChatMessage;
import de.uvwxy.packsock.chat.IChatMessageHook;
import de.uvwxy.packsock.game.GameClient;
import de.uvwxy.packsock.game.GameMessage;
import de.uvwxy.packsock.game.GameServer;
import de.uvwxy.packsock.game.IGameMessageHook;

public class ActivityGame extends Activity implements SendGameReply {
	Activity me = null;
	// SETUP UI
	private EditText etServer = null;
	private EditText etPort = null;
	private EditText etName = null;
	// private CheckBox cbSpectators = null;

	// GAME UI
	private Button btnSend = null;
	private EditText etChat = null;
	private EditText etInput = null;

	GameClient client;
	GameServer server;

	private byte myPlayerID;
	private Reversi currentGame = null;
	private final static byte CONTAINS_BOARD_DATA = 1;
	private final static byte CONTAINS_PLAYER_ID_REQUEST = 2;
	private final static byte CONTAINS_PLAYER_ID = 3;
	private final static byte RESET_ID = 4;
	protected static final String SERVER_STRING = "Server";

	private String userName;
	private long userID = System.currentTimeMillis();

	private BoardPainter boardPainter = null;

	private class ChatPoster implements Runnable {
		ChatMessage msg;

		public ChatPoster(ChatMessage msg) {
			this.msg = msg;
		}

		@Override
		public void run() {
			Date time = new Date(msg.getTimestamp());
			Calendar d = new GregorianCalendar();
			d.setTime(time);
			String t = etChat.getText() + "\n[" + d.get(Calendar.HOUR) + ":" + d.get(Calendar.MINUTE) + "]:"
					+ msg.getSender() + ": " + msg.getMessage();
			// TODO: get text properly (modify object)!
			etChat.setText(t);
			etChat.setSelection(t.length(), t.length());
		}
	}

	private IChatMessageHook clientChatMessageReceived = new IChatMessageHook() {

		@Override
		public void onMessageReceived(ChatMessage msg, PackSock s) {
			if (etChat != null && msg != null) {
				me.runOnUiThread(new ChatPoster(msg));
			}

		}

	};

	private IGameMessageHook clientGameMessageReceived = new IGameMessageHook() {

		@Override
		public void onMessageReceived(GameMessage msg, PackSock s) {
			Log.i("REV", "Received Game Message on the client side");

			switch (msg.getId()) {
			case CONTAINS_PLAYER_ID:
				Log.i("REV", "Client received PLAYER ID");
				myPlayerID = msg.getGameObjData()[0];
				me.runOnUiThread(new ChatPoster(new ChatMessage("Server", "You are player " + myPlayerID)));
				break;
			case CONTAINS_BOARD_DATA:
				Log.i("REV", "Client received BOARD DATA");
				currentGame = new Reversi(msg.getGameObjData());
				if (currentGame.getPlayerWhichHasToMove() == myPlayerID) {
					me.runOnUiThread(new ChatPoster(new ChatMessage("Server", "It's your move!")));
				}
				Log.i("REV", "game id in board: " + currentGame.getPlayerWhichHasToMove());
				boardPainter.setReversiGame(currentGame, myPlayerID == currentGame.getPlayerWhichHasToMove());
				break;
			case RESET_ID:
				Log.i("REV", "Client received RESET ID");
				me.runOnUiThread(new ChatPoster(new ChatMessage("local", ".. have to rejoin again")));
				myPlayerID = 0;
			default:
			}

		}

	};

	@Override
	public void sendGameReply(Reversi r) {
		GameMessage m = new GameMessage(CONTAINS_BOARD_DATA, r.getObjectData());
		try {
			Log.i("REV", "Client sending game to server!");
			client.sendGameMessage(m);
		} catch (SocketException e) {
			me.runOnUiThread(new ChatPoster(new ChatMessage("local", "sending move failed")));
		} catch (IOException e) {
			// TODO:
			e.printStackTrace();
		}
	}
	
	@Override
	public byte getID() {
		return myPlayerID;
	}

	LinkedList<PackSock> clientSockets = new LinkedList<PackSock>();
	private Object sentIDLOCK = new Object();
	private IGameMessageHook serverGameMessageReceived = new IGameMessageHook() {

		@Override
		public void onMessageReceived(GameMessage msg, PackSock s) {
			Log.i("REV", "Received Game Message on the server side");
			if (!clientSockets.contains(s))
				clientSockets.add(s);
			switch (msg.getId()) {
			case CONTAINS_BOARD_DATA:
				Log.i("REV", "Server received BOARD DATA");
				// determine next move
				Reversi r = new Reversi(msg.getGameObjData());
				Log.i("REV", "Player was " + r.getPlayerWhichHasToMove());
				if (!r.setNextPlayer()) {
					server.distributePacket(new ChatMessage(SERVER_STRING, "Game Over, player " + r.getWinner()
							+ " has won (" + r.getPointsString() + ")"));
					server.distributePacket(new ChatMessage(SERVER_STRING, "Please \"join\" again."));
					server.distributePacket(new GameMessage(RESET_ID, new byte[1]));
					synchronized (sentIDLOCK) {
						sentID = 0;
					}
				}
				Log.i("REV", "Next player is " + r.getPlayerWhichHasToMove());
				// distribute new message
				server.distributePacket(new GameMessage(CONTAINS_BOARD_DATA, r.getObjectData()));
				break;
			case CONTAINS_PLAYER_ID_REQUEST:
				Log.i("REV", "Server received PLAYER ID REQUEST");
				synchronized (sentIDLOCK) {
					if (sentID != maxPlayers) {
						sentID++;
						byte[] god = new byte[1];
						god[0] = sentID;
						GameMessage m = new GameMessage(CONTAINS_PLAYER_ID, god);
						Packet p = new Packet(PacketType.GAME_MESSAGE, m.getObjectData());
						try {
							s.sendPacket(p);
						} catch (SocketException e) {
							clientSockets.remove(s);
							sentID--;
						} catch (IOException e) {
							// TODO:
							e.printStackTrace();
						}

						if (sentID == maxPlayers) {
							Log.i("REV", "MAX PLAYERS NEW GAME!");
							r = new Reversi();
							r.selectRandomPlayerForMove();
							server.distributePacket(new ChatMessage(SERVER_STRING, "New Game hast Started"));
							server.distributePacket(new GameMessage(CONTAINS_BOARD_DATA, r.getObjectData()));
						}
					}
				}
				break;
			default:
				Log.i("REV", "Server received game message with ID " + msg.getId());
			}
		}

	};

	private byte sentID = 0;
	private int maxPlayers = 2;

	private OnClickListener buttonSend = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (etInput != null && !etInput.getText().toString().equals("")) {
				String msg = etInput.getText().toString();
				etInput.setText("");

				if (msg.equals("join")) {
					// TODO: send ID request
					GameMessage m = new GameMessage(CONTAINS_PLAYER_ID_REQUEST, new byte[1]);
					try {
						client.sendGameMessage(m);
					} catch (SocketException e) {
						me.runOnUiThread(new ChatPoster(new ChatMessage("local", "Join failed")));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				ChatMessage m = new ChatMessage(userName, msg);
				if (client != null) {
					try {
						client.sendMessage(m);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

	};

	private class ServerThread implements Runnable {

		private int port;

		public ServerThread(int port) {
			this.port = port;
		}

		@Override
		public void run() {
			// TODO: check param "0" for spectator function?
			server = new GameServer(port, 0, "Server", serverGameMessageReceived);
			try {
				server.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private class ClientThread implements Runnable {

		private int port;
		private String address;

		public ClientThread(int port, String address) {
			this.port = port;
			this.address = address;
		}

		@Override
		public void run() {
			// TODO: check param "0" for spectator function?
			client = new GameClient(port, address, clientChatMessageReceived, clientGameMessageReceived);
			try {
				client.connect();
				// REMOVE BEFORE FLIGHT >
				ChatMessage m = new ChatMessage(userName + "(" + (userID % 10) + ")", "New Connection from Client!");
				if (client != null) {
					try {
						client.sendMessage(m);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				// REMOVE BEFORE FLIGHT <
			} catch (ConnectException e) {
				Log.i("REV", "SWITCH TASK SETUP");
				e.printStackTrace();
				me.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						createSetupUI();
					}

				});
			} catch (IOException e) {
				Log.i("REV", "IO ERR");
				e.printStackTrace();
			} catch (Exception e) {
				Log.i("REV", "ERR");
				e.printStackTrace();
			}

			me.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					createGameUI();
				}

			});
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		me = this;
		createSetupUI();

	}

	public void hostGame(View v) {
		boolean all_ok = true;

		String name = etName.getText().toString();
		if (name == null || name.equals("")) {
			if (etName.getText().toString().equals("")) {
				etName.setText("User_" + (char) (Math.random() * 128));
				all_ok = false;
			}
			name = etName.getText().toString();
		}

		int port = 0;
		try {
			port = Integer.parseInt(etPort.getText().toString());
		} catch (Exception e) {
			port = 0;
			all_ok = false;
		}
		if (port <= 0 || port >= 65535) {
			port = (int) (Math.random() * 256) + 25500;
			all_ok = false;
		}
		etPort.setText("" + port);

		if (all_ok) {
			Thread t = new Thread(new ServerThread(port));
			t.start();

			etServer.setText("localhost");
			// TODO: connect as client aswell and then create game ui
			longToast("Now clieck connect as client");
		} else {
			longToast("Your input was not 100% correct. Guessing further settings. Click again to start with new values");
		}
	}

	public void connectGame(View v) {
		boolean all_ok = true;

		String name = etName.getText().toString();
		if (name == null || name.equals("")) {
			if (etName.getText().toString().equals("")) {
				etName.setText("User_" + (char) (Math.random() * 128));
				all_ok = false;
			}
			name = etName.getText().toString();
		}

		userName = name;

		int port = 0;
		try {
			port = Integer.parseInt(etPort.getText().toString());
		} catch (Exception e) {
			port = 0;
			all_ok = false;
		}
		if (port <= 0 || port >= 65535) {
			port = (int) (Math.random() * 256) + 25500;
			all_ok = false;
		}
		etPort.setText("" + port);

		String server = etServer.getText().toString();

		if (server.equals("")) {
			all_ok = false;
			longToast("Please enter a valid server address");
		}

		if (!all_ok) {
			longToast("Your input was not 100% correct. Guessing further settings. Click again to start with new values");
			return;
		}

		new Thread(new ClientThread(port, server)).start();

	}

	private void createSetupUI() {
		setContentView(R.layout.activity_setup);

		etServer = (EditText) findViewById(R.id.etSever);
		etServer.setText("192.168.178.27");
		etPort = (EditText) findViewById(R.id.etPort);
		etPort.setText("25667");
		etName = (EditText) findViewById(R.id.etName);
		// cbSpectators = (CheckBox) findViewById(R.id.cbSpectators);

	}

	private void createGameUI() {
		setContentView(R.layout.activity_reversi);

		btnSend = (Button) findViewById(R.id.btnSend);
		btnSend.setOnClickListener(buttonSend);
		etChat = (EditText) findViewById(R.id.etChat);
		etInput = (EditText) findViewById(R.id.etInput);
		boardPainter = (BoardPainter) findViewById(R.id.boardPainter1);
		boardPainter.setSendGameReply(this);

	}

	private void longToast(String msg) {
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_LONG;

		Toast toast = Toast.makeText(context, msg, duration);
		toast.show();
	}
}
