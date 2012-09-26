package de.uvwxy.reversi;

import java.io.IOException;

import de.uvwxy.packsock.chat.ChatMessage;
import de.uvwxy.packsock.chat.IChatMessageHook;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.DialogInterface;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class ActivityGame extends Activity {
	private Button btnSend = null;
	private EditText etChat = null;
	private EditText etInput = null;
	GameClient client;
	GameServer server;

	private UpdateUITask uiUpdater = new UpdateUITask();
	
	private IChatMessageHook mh0 = new IChatMessageHook() {

		@Override
		public void onMessageReceived(ChatMessage msg) {
			if (etChat != null) {
				uiUpdater.execute(msg);
			}

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

	private class NetworkThread implements Runnable {

		@Override
		public void run() {
			server = new GameServer(25567, 0, "Reversi Server");
			try {
				server.start();
			} catch (IOException e) {
				e.printStackTrace();
			}

			client = new GameClient(25567, "localhost", mh0);

			try {
				client.connect();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reversi);

		btnSend = (Button) findViewById(R.id.btnSend);
		btnSend.setOnClickListener(buttonSend);
		etChat = (EditText) findViewById(R.id.etChat);
		etInput = (EditText) findViewById(R.id.etInput);

		Thread t = new Thread(new NetworkThread());
		t.start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_reversi, menu);
		return true;
	}
}
