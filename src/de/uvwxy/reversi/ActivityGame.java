package de.uvwxy.reversi;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
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
	Activity me = null;
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
		public void onMessageReceived(ChatMessage msg) {
			if (etChat != null && msg != null) {
				me.runOnUiThread(new ChatPoster(msg));
			}

		}

	};

	private IGameMessageHook clientGameMessageReceived = new IGameMessageHook() {

		@Override
		public void onMessageReceived(GameMessage msg) {
			Log.i("REV", "Received Game Message on the client side");
		}

	};

	private IGameMessageHook serverGameMessageReceived = new IGameMessageHook() {

		@Override
		public void onMessageReceived(GameMessage msg) {
			Log.i("REV", "Received Game Message on the server side");
		}

	};

	private class UpdateChatUITask extends AsyncTask<ChatMessage, ChatMessage, Integer> {

		@Override
		protected Integer doInBackground(ChatMessage... params) {
			// for (ChatMessage )
			publishProgress(params);
			return null;
		}

		protected void onProgressUpdate(ChatMessage... values) {
			for (ChatMessage m : values) {

			}
		};
	}

	private class SwitchUITaskSetup extends AsyncTask<Integer, Integer, Integer> {

		protected void onProgressUpdate(Integer... values) {
			Log.i("REV", "CREATE SETUP UI 2/2");
			createSetupUI();
		}

		@Override
		protected Integer doInBackground(Integer... params) {
			// for (ChatMessage )
			Log.i("REV", "CREATE SETUP UI 1/2");
			publishProgress(params);
			return null;
		}
	}

	private OnClickListener buttonSend = new OnClickListener() {

		@Override
		public void onClick(View v) {
			if (etInput != null && !etInput.getText().toString().equals("")) {
				String msg = etInput.getText().toString();
				etInput.setText("");

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
