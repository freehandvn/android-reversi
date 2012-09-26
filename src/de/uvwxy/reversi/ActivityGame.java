package de.uvwxy.reversi;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import de.uvwxy.packsock.chat.ChatMessage;
import de.uvwxy.packsock.chat.IChatMessageHook;
import de.uvwxy.packsock.game.GameClient;
import de.uvwxy.packsock.game.GameMessage;
import de.uvwxy.packsock.game.GameServer;
import de.uvwxy.packsock.game.IGameMessageHook;

public class ActivityGame extends Activity {

	// SETUP UI
	private EditText etServer = null;
	private EditText etPort = null;
	private EditText etName = null;
	private CheckBox cbSpectators = null;

	// GAME UI
	private Button btnSend = null;
	private EditText etChat = null;
	private EditText etInput = null;
	GameClient client;
	GameServer server;

	private String userName;
	private long userID = System.currentTimeMillis();

	private UpdateUITask uiUpdater = new UpdateUITask();

	private IChatMessageHook clientChatMessageReceived = new IChatMessageHook() {

		@Override
		public void onMessageReceived(ChatMessage msg) {
			if (etChat != null) {
				uiUpdater.execute(msg);
			}

		}

	};

	private IGameMessageHook clientGameMessageReceived = new IGameMessageHook() {

		@Override
		public void onMessageReceived(GameMessage msg) {
			// TODO Auto-generated method stub

		}

	};

	private IGameMessageHook serverGameMessageReceived = new IGameMessageHook() {

		@Override
		public void onMessageReceived(GameMessage msg) {
			// TODO Auto-generated method stub

		}

	};

	private class UpdateUITask extends AsyncTask<ChatMessage, ChatMessage, Integer> {

		@Override
		protected Integer doInBackground(ChatMessage... params) {
			// for (ChatMessage )
			publishProgress(params);
			return null;
		}

		protected void onProgressUpdate(ChatMessage... values) {
			for (ChatMessage m : values) {
				String t = etChat.getText() + "\n" + m.toString();
				// TODO: get text properly (modify object)!
				etChat.setText(t);
				etChat.setSelection(t.length(), t.length());
			}
		};
	}

	private OnClickListener buttonSend = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (etInput != null) {
				String msg = etInput.getText().toString();
				etInput.setText("");

				ChatMessage m = new ChatMessage("client0", msg);
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
				ChatMessage m = new ChatMessage(userName + "(" + (userID%10) + ")", "Hello World from Client!");
				if (client != null) {
					try {
						client.sendMessage(m);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				// REMOVE BEFORE FLIGHT <
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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

		createGameUI();
		clientConnect(server, port);

	}

	public void clientConnect(String server, int port) {
		ClientThread x = new ClientThread(port, server);
		Thread t = new Thread(x);
		t.start();
	}

	private void createSetupUI() {
		setContentView(R.layout.activity_setup);

		etServer = (EditText) findViewById(R.id.etSever);
		etPort = (EditText) findViewById(R.id.etPort);
		etName = (EditText) findViewById(R.id.etName);
		cbSpectators = (CheckBox) findViewById(R.id.cbSpectators);

	}

	private void createGameUI() {
		setContentView(R.layout.activity_reversi);

		btnSend = (Button) findViewById(R.id.btnSend);
		btnSend.setOnClickListener(buttonSend);
		etChat = (EditText) findViewById(R.id.etChat);
		etInput = (EditText) findViewById(R.id.etInput);

	}

	private void longToast(String msg) {
		Context context = getApplicationContext();
		int duration = Toast.LENGTH_LONG;

		Toast toast = Toast.makeText(context, msg, duration);
		toast.show();
	}
}
