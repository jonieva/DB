//Copyright (c) Microsoft Corporation All rights reserved.  
// 
//MIT License: 
// 
//Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
//documentation files (the  "Software"), to deal in the Software without restriction, including without limitation
//the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
//to permit persons to whom the Software is furnished to do so, subject to the following conditions: 
// 
//The above copyright notice and this permission notice shall be included in all copies or substantial portions of
//the Software. 
// 
//THE SOFTWARE IS PROVIDED ""AS IS"", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
//TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
//THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
//CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
//IN THE SOFTWARE.
package diabeatIT;

import diabeatIT.streaming.R;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.app.Activity;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class BandStreamingAppActivity extends Activity implements IOnTaskCompleted {

	private Button btnStart;
	private Button btnStop;
	private TextView txtStatus;
	private TextView txtHeartRate;
	private BandStreamingAppActivity mainActivity = this;

	private Button btnSaveData;
	private Button backupDatabaseButton;
	private Button removeDatabaseButton;
	private TextView txtDataStatus;
	private int currentHeartRate = 0;

	private Button btnService;
	private boolean isServiceStarted = false;
	private int step = 0;

	SQLiteDatabase db;
	BandManager bandManager;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.txtStatus = (TextView) findViewById(R.id.txtStatus);
		this.txtHeartRate = (TextView) findViewById(R.id.txtHeartRate);
		this.txtDataStatus = (TextView) findViewById(R.id.txtDataStatus);
        this.btnStart = (Button) findViewById(R.id.btnStart);
		this.btnStop = (Button) findViewById(R.id.btnStop);
		this.btnService = (Button) findViewById(R.id.btnService);

		bandManager = new BandManager(this);


		this.btnStart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					txtStatus.setText("");
					bandManager.SaveInDB = false;
					bandManager.connect(mainActivity);
					bandManager.subscribeToAccelerometer();
					txtStatus.setText("Accelerometer: " + bandManager.sampleCoord);
				}
				catch (Exception ex){
					txtStatus.setText("Error in the connection: " + ex.getMessage());
				}

			}
		});

		this.btnStop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				bandManager.unsubscribeAllListeners();
			}
		});

		this.backupDatabaseButton = (Button) findViewById(R.id.backupDatabaseButton);
		this.backupDatabaseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					bandManager.backupDatabase();
//					printReadings();
					appendToUI("Backup finished!");
				} catch (Exception ex) {
					showException(ex.getMessage());
				}
			}
		});

		this.removeDatabaseButton = (Button) findViewById(R.id.removeDatabaseButton);
		this.removeDatabaseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					bandManager.removeDatabases();
					appendToUI("Database removed!");
				} catch (Exception ex) {
					showException(ex.getMessage());
				}
			}
		});

		this.btnService = (Button) findViewById(R.id.btnService);
		this.btnService.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try{
					Intent serviceIntent = new Intent(getApplicationContext(), RecordBandDataIntentService.class);
					if (!isServiceStarted) {
						startService(serviceIntent);
						isServiceStarted = true;
					}
					else {
						stopService(serviceIntent);
						isServiceStarted = false;
					}
					appendToUI("Service started: " + isServiceStarted, (TextView) findViewById(R.id.txtServiceState));
				}
				catch (Exception ex){
					Log.e("DiabeatIT", ex.getStackTrace().toString());
				}
			}
		});
		this.mainActivity = this;
    }

	@Override
	protected void onResume() {
		super.onResume();
		txtStatus.setText("");
	}
	
    @Override
	protected void onPause() {
		super.onPause();
		pause();
	}

	private void pause() {
		this.bandManager.unsubscribeAllListeners();
	}

	/**
	 * Shows all the readings stored in db
	 */
	private void printReadings() {
		String[] projection = {"Id, Type, Value, Date"};
		Cursor c = db.query(
				"SensorEntry",  // The table to query
				projection,                               // The columns to return
				null,                                // The columns for the WHERE clause
				null,                            // The values for the WHERE clause
				null,                                     // don't group the rows
				null,                                     // don't filter by row groups
				"Id DESC"                                 // The sort order
		);
		c.moveToFirst();

		do {
			int rate = c.getInt(c.getColumnIndex("Value"));
			String date = c.getString(c.getColumnIndex("Date"));
			//appendToUI("Read " + rate + " at " + date, txtDataStatus);
			Log.d("DiabeatIT", "Read " + rate + " at " + date);
		} while(c.moveToNext());
	}

	private void showException(String message) {
		pause();
		appendToUI(message);
	}

	private void appendToUI(final String string){
		appendToUI(string, txtStatus);
	}
	
	private void appendToUI(final String string, final TextView control) {
		this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            	control.setText(string);
            }
        });
	}

	@Override
	public void PostExecution() {
		// Invoked afer connection. Subscribe to events
		if (step == 0){
			this.bandManager.askForPermissions(this, this);
			step++;
		}
		else {
			this.bandManager.subscribeToEvents();
			step++;
		}
	}
}

