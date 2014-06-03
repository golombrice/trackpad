package com.example.mousifier;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

public class UDPservice extends Service {

	private Looper mServiceLooper;
	private ServiceHandler mServiceHandler;


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
	/** method for clients */
	public void sendScroll(int x, int y) {
		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the job
		Message msg = mServiceHandler.obtainMessage();
		String message = "scroll " + Integer.toString(y);
		msg.obj = message;
//		msg.arg1 = x;
//		msg.arg2 = y;
		mServiceHandler.sendMessage(msg);
	}
	
	/** method for clients */
	public void sendUDPPacket(int x, int y) {
		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the job
		Message msg = mServiceHandler.obtainMessage();
		String message = "move " + Integer.toString(x) + " " + Integer.toString(y);
		msg.obj = message;
//		msg.arg1 = x;
//		msg.arg2 = y;
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

	// Handler that receives messages from the thread
	private final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}
		@Override
		public void handleMessage(Message msg) {
			Log.d("fds", "handleMessage");

			int server_port = 32000;
			String message = (String)msg.obj;
//			Integer.toString(msg.arg1) + " " + Integer.toString(msg.arg2);
			InetAddress addr = null;
			try {
				addr = InetAddress.getByName("192.168.0.103");
			} catch (UnknownHostException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			DatagramPacket p = new DatagramPacket(message.getBytes(), message.getBytes().length, addr, server_port);
			DatagramSocket s = null;
			try {
				s = new DatagramSocket();
			} catch (SocketException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				s.send(p);
				Log.d("fds", "hei");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			s.close();

			// Normally we would do some work here, like download a file.
			// For our sample, we just sleep for 5 seconds.
//			long endTime = System.currentTimeMillis() + 5*1000;
//			while (System.currentTimeMillis() < endTime) {
//				synchronized (this) {
//					try {
//						wait(endTime - System.currentTimeMillis());
//					} catch (Exception e) {
//					}
//				}
//			}
//			// Stop the service using the startId, so that we don't stop
//			// the service in the middle of handling another job
//			stopSelf(msg.arg1);
		}
	}

	@Override
	public void onCreate() {
		Log.d("fds", "onCreate");
		Log.d("fds", "thread start start");

		// Start up the thread running the service.  Note that we create a
		// separate thread because the service normally runs in the process's
		// main thread, which we don't want to block.  We also make it
		// background priority so CPU-intensive work will not disrupt our UI.
		HandlerThread thread = new HandlerThread("ServiceStartArguments",
				Process.THREAD_PRIORITY_BACKGROUND);
		thread.start();
		Log.d("fds", "thread start");

		// Get the HandlerThread's Looper and use it for our Handler
		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
		Log.d("fds", "looper");

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();

		// For each start request, send a message to start a job and deliver the
		// start ID so we know which request we're stopping when we finish the job
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		mServiceHandler.sendMessage(msg);
		Log.d("fds", "fdsa");

		// If we get killed, after returning from here, restart
		return START_STICKY;
	}


	@Override
	public void onDestroy() {
		Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
	}
	



}
