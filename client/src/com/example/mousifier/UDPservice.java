package com.example.mousifier;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;

public class UDPservice extends Service {

	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;

	@Override
	public void onCreate() {
		// Start up the thread running the service.  Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block.  We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		HandlerThread thread = new HandlerThread("ServiceStartArguments",
				Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();

		// Get the HandlerThread's Looper and use it for our Handler
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the job
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		mServiceHandler.sendMessage(msg);

		// If we get killed, after returning from here, restart
		return START_STICKY;
	}


	@Override
	public void onDestroy() {
	}

	// Binder given to clients
	private final IBinder mBinder = new LocalBinder();

	/**
	 * Class used for the client Binder.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		UDPservice getService() {
			// Return this instance of LocalService so clients can call public methods
			return UDPservice.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	public void sendScroll(int x, int y) {
		Message msg = mServiceHandler.obtainMessage();
		String message = "scroll " + Integer.toString(y);
		msg.obj = message;
		mServiceHandler.sendMessage(msg);
	}
	public void sendButtondownMove(int x, int y) {
		Message msg = mServiceHandler.obtainMessage();
		String message = "bdmove " + Integer.toString(x) + " " + Integer.toString(y);
		msg.obj = message;
		mServiceHandler.sendMessage(msg);
	}
	public void sendMove(int x, int y) {
		Message msg = mServiceHandler.obtainMessage();
		String message = "move " + Integer.toString(x) + " " + Integer.toString(y);
		msg.obj = message;
		mServiceHandler.sendMessage(msg);
	}
	public void sendChar(char sendchar) {
		Message msg = mServiceHandler.obtainMessage();
		String message = "char " + sendchar;
		msg.obj = message;
		mServiceHandler.sendMessage(msg);		
	}
	public void sendText(String string) {
		Message msg = mServiceHandler.obtainMessage();
		String message = "text " + string;
		msg.obj = message;
		mServiceHandler.sendMessage(msg);		
	}

	public void sendClick() {
		Message msg = mServiceHandler.obtainMessage();
		String message = "click";
		msg.obj = message;
		mServiceHandler.sendMessage(msg);	
	}
	public void sendSecClick() {
		Message msg = mServiceHandler.obtainMessage();
		String message = "clicksec";
		msg.obj = message;
		mServiceHandler.sendMessage(msg);	
	}
	public void sendButtondown() {
		Message msg = mServiceHandler.obtainMessage();
		String message = "buttondown";
		msg.obj = message;
		mServiceHandler.sendMessage(msg);			
	}
	public void sendButtonup() {
		Message msg = mServiceHandler.obtainMessage();
		String message = "buttonup";
		msg.obj = message;
		mServiceHandler.sendMessage(msg);			
	}

	// Handler that receives messages from the thread
	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}
		@Override
		public void handleMessage(Message msg) {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(UDPservice.this);
			Integer serverPort = Integer.parseInt(sharedPref.getString("server_port", "35000"));
			String serverAddress = sharedPref.getString("server_addr", "");

			String message = (String)msg.obj;
			InetAddress addr = null;
			try {
				addr = InetAddress.getByName(serverAddress);
			} catch (UnknownHostException e1) {
				e1.printStackTrace();
			}
			DatagramPacket p = new DatagramPacket(message.getBytes(), message.getBytes().length, addr, serverPort.intValue());
			DatagramSocket s = null;
			try {
				s = new DatagramSocket();
			} catch (SocketException e1) {
				e1.printStackTrace();
			}
			try {
				s.send(p);
			} catch (IOException e1) {
				e1.printStackTrace();
			}

			s.close();
		}
	}
}
