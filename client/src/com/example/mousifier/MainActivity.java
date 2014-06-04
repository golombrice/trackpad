package com.example.mousifier;

import com.example.mousifier.UDPservice.LocalBinder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {
	private final int SCROLL_THRESHOLD = 10;
	private final int MOVE_THRESHOLD = 5;
	private final int CLICK_TIME_THRESHOLD = 200;
	private final String TBNAME = "Keyboard";

	UDPservice serverService_;
	boolean serverBound_ = false;

	private int[] fingerPosX_ = new int[2];
	private int[] fingerPosY_ = new int[2];


	private boolean isMovingWindow_ = false;
	private boolean isOnClick_;
	private boolean secondFingerDownWhileOnClick_ = false;
	private boolean secondFingerDown_ = false;
	private long startTime_ = System.currentTimeMillis();

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className,
				IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			LocalBinder binder = (LocalBinder) service;
			serverService_ = binder.getService();
			serverBound_ = true;
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			serverBound_ = false;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Intent intent = new Intent(this, UDPservice.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

		final Button button = (Button) findViewById(R.id.kb_button);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Show soft keyboard
				EditText txtName = (EditText) findViewById(R.id.txtName);
				InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				inputMethodManager.showSoftInput(txtName, InputMethodManager.SHOW_IMPLICIT);
			}
		});
		final EditText txtBox = (EditText) findViewById(R.id.txtName);
		txtBox.setText(TBNAME);
		txtBox.setSelection(TBNAME.length());

		// A listener is added to the EditText. This implements the keyboard feature of the program.
		// Whenever the text is changed, we take the added letter and send it further to the server.
		txtBox.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){
			}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count){
				if(s.length() == TBNAME.length()) {
					return;
				}

				String saveString = s.toString();
				char sendchar = 0;
				if(s.length() == TBNAME.length()-1 ) {
					// backspace
					sendchar = (char)8;
				}
				if(s.length() > TBNAME.length() ) { // something else than backspace
					sendchar = saveString.charAt(TBNAME.length());
				}
				// set the default text to TextEdit
				txtBox.setText(TBNAME); 
				txtBox.setSelection(TBNAME.length());

//				Log.d("txt", start + " " + before + " " + count + " " + saveString);
				if( serverBound_ ) {
					serverService_.sendChar(sendchar);
				}
			}
			@Override
			public void afterTextChanged(Editable arg0) {				
			}
		}); 
	}

	// This function implements the listening of cursor events on the screen. Most important function in this activity.
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// We are here because a finger moved on screen.
		
		// Lets find the new positions and movement for first and second fingers.
		int[] moveX = new int[2];
		int[] moveY = new int[2];
		int pointerCount = ev.getPointerCount();
		for(int pointerIndex = 0; pointerIndex < pointerCount; ++pointerIndex)
		{
			int pointerId = ev.getPointerId(pointerIndex);
			if( pointerId == 0 || pointerId == 1 )
			{
				moveX[pointerId] = fingerPosX_[pointerId]-(int)ev.getX(pointerIndex);
				moveY[pointerId] = fingerPosY_[pointerId]-(int)ev.getY(pointerIndex);
				fingerPosX_[pointerId] = (int)ev.getX(pointerIndex);
				fingerPosY_[pointerId] = (int)ev.getY(pointerIndex);
			}
		}

		// Act depending on what type of action happened.
		switch (ev.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN: // first finger down
			startTime_ = System.currentTimeMillis();
			isOnClick_ = true;
			break;
		case MotionEvent.ACTION_UP: // first finger up
			if (isOnClick_) {
				long elapsedTime = System.currentTimeMillis()-startTime_;
				if(elapsedTime < CLICK_TIME_THRESHOLD) {
					if( secondFingerDownWhileOnClick_ ) {
						serverService_.sendSecClick();
					} else {
						serverService_.sendClick();
					}
				}
			}
			isOnClick_ = false;
			secondFingerDownWhileOnClick_ = false;
			break;
		case MotionEvent.ACTION_POINTER_DOWN: // second finger down
			secondFingerDown_ = true;
			if( isOnClick_ ) {
				secondFingerDownWhileOnClick_ = true;
			}
			break;
		case MotionEvent.ACTION_POINTER_UP: // second finger up
			if( isMovingWindow_ ) {
				isMovingWindow_ = false;
				serverService_.sendButtonup();
			}
			secondFingerDown_ = false;
			break;
		case MotionEvent.ACTION_MOVE: // first or second finger moved
			if(serverBound_) {
				// both fingers moved as much
				if( secondFingerDown_ && Math.abs(moveX[0]-moveX[1]) < MOVE_THRESHOLD && Math.abs(moveY[0]-moveY[1]) < MOVE_THRESHOLD ) {
					serverService_.sendScroll(moveX[0],moveY[0]);
				}
				// we are currently moving a window
				if( isMovingWindow_ ){
					serverService_.sendMove(moveX[1],moveY[1]);
				}
				// let's start moving a window because one finger stays still and another moves
				if( !isMovingWindow_ && secondFingerDown_ && !(Math.abs(moveX[0]-moveX[1]) < MOVE_THRESHOLD && Math.abs(moveY[0]-moveY[1]) < MOVE_THRESHOLD ) ){
					isMovingWindow_ = true;
					serverService_.sendButtondown();
					serverService_.sendMove(moveX[1],moveY[1]);
				}
				// only one finger moving so just move cursor
				if( !secondFingerDown_ ) {
					serverService_.sendMove(moveX[0],moveY[0]);
				}
			}

			// the first finger moved so much that we are not simply clicking on the screen but rather moving
			if (isOnClick_ && (Math.abs(moveX[0]) > SCROLL_THRESHOLD || Math.abs(moveY[0]) > SCROLL_THRESHOLD)) {
				isOnClick_ = false;
				
			}
			break;
		case MotionEvent.ACTION_CANCEL:
			break;
		default:
			break;
		}
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
		    Intent intent = new Intent(this, SettingsActivity.class);
		    startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


}
