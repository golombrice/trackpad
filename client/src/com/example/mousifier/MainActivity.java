package com.example.mousifier;

import com.example.mousifier.UDPservice.LocalBinder;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.os.Build;

public class MainActivity extends Activity {
	UDPservice mService;
	boolean mBound = false;

	/** Defines callbacks for service binding, passed to bindService() */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className,
				IBinder service) {
			// We've bound to LocalService, cast the IBinder and get LocalService instance
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
			mBound = true;

		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mBound = false;
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Log.d("fds", "fdsasdd");

//		if (savedInstanceState == null) {
//			getFragmentManager().beginTransaction()
//			.add(R.id.container, new PlaceholderFragment())
//			.commit();
//		}
		Intent intent = new Intent(this, UDPservice.class);
		//        startService(intent);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		Log.d("fds", "kakka");
		
		final Button button = (Button) findViewById(R.id.kb_button);
		button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				EditText txtName = (EditText) findViewById(R.id.txtName);
				InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				// only will trigger it if no physical keyboard is open
				inputMethodManager.showSoftInput(txtName, InputMethodManager.SHOW_IMPLICIT);
			}
		});
		final EditText txtBox = (EditText) findViewById(R.id.txtName);
//		txtBox.addTextChangedListener(new TextWatcher(){
//			@Override
//	        public void beforeTextChanged(CharSequence s, int start, int count, int after){
//				
//			}
//			@Override
//			public void onTextChanged(CharSequence s, int start, int before, int count){
//				String saveString = s.toString();
//				if(s.length() != 0 ) {
//					txtBox.setText("");
//				}
//				Log.d("fds", saveString);
//	        }
//			@Override
//			public void afterTextChanged(Editable arg0) {
//				// TODO Auto-generated method stub
//				
//			}
//	    }); 
		
		txtBox.setOnEditorActionListener(new EditText.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			 if (actionId == EditorInfo.IME_ACTION_DONE) {
			 	mService.sendText(v.getText().toString());
			 	v.setText("");
			 	return true;
			 }
			 return false;
			}

			});
	}
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
	    Log.i("key pressed", String.valueOf(event.getKeyCode()));
	    return super.dispatchKeyEvent(event);
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
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			return rootView;
		}
	}

	private int mDownX;
	private int mDownY;
	private final int SCROLL_THRESHOLD = 10;
	private boolean isOnClick;
	private boolean secondFingerDown = false;
	private long startTime = System.currentTimeMillis();

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		int moveX = mDownX-(int)ev.getX();
		int moveY = mDownY-(int)ev.getY();
		mDownX = (int)ev.getX();
		mDownY = (int)ev.getY();

		switch (ev.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			startTime = System.currentTimeMillis();
			isOnClick = true;
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			if (isOnClick) {
				Log.i("fds", "onClick ");
				long elapsedTime = System.currentTimeMillis()-startTime;
				if(elapsedTime < 200) {
					mService.sendClick();
				}
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if(mBound) {
				if( secondFingerDown ) {
					int pointerCount = ev.getPointerCount();
			        for(int i = 0; i < pointerCount; ++i)
			        {
			            int pointerIndex = i;
			            int pointerId = ev.getPointerId(pointerIndex);
			            Log.d("pointer id - move",Integer.toString(pointerId));
//			            if(pointerId == 0)
//			            {
//			                fingerOneDown = 1;
//			                fingerOneX = ev.getX(pointerIndex);
//			                fingerOneY = ev.getY(pointerIndex);
//			            }
			            if(pointerId == 1)
			            {
//			                fingerTwoDown = 1;
//			                fingerTwoX =;
//			                fingerTwoY = ;
							Log.d("sec", Float.toString( ev.getX(pointerIndex))+" "+ Float.toString(ev.getY(pointerIndex)));

			            }
			        }
					mService.sendScroll(moveX,moveY);
				}
				else {
					mService.sendUDPPacket(moveX,moveY);
				}
			}

			if (isOnClick && (Math.abs(moveX) > SCROLL_THRESHOLD || Math.abs(moveY) > SCROLL_THRESHOLD)) {
				Log.i("few", "movement detected");
				isOnClick = false;
			}
			break;
		case MotionEvent.ACTION_POINTER_DOWN:
			Log.i("few", "finger down");
			secondFingerDown = true;
			break;
		case MotionEvent.ACTION_POINTER_UP:
			Log.i("few", "finger up");
			secondFingerDown = false;
			break;
		default:
			break;
		}


		//    	int x = (int)event.getX();
		//    	Log.d("onTouch", Integer.toString(x));
		//    	int y = (int)event.getY();
		//    	Log.d("onTouch", Integer.toString(y));



		return true;
	}


	//    @Override
	//    public boolean onTouchEvent(MotionEvent event) {
	//    	int x = (int)event.getX();
	//    	Log.d("onTouch", Integer.toString(x));
	//    	int y = (int)event.getY();
	//    	Log.d("onTouch", Integer.toString(y));
	//    	
	//    	if(mBound) {
	//    		mService.sendUDPPacket(x,y);
	//    	}
	//
	//    	//        switch (event.getAction()) {
	//    	//            case MotionEvent.ACTION_DOWN:
	//    	//            case MotionEvent.ACTION_MOVE:
	//    	//            case MotionEvent.ACTION_UP:
	//    	//        }
	//    	return false;
	//    }

}
