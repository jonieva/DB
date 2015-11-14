package diabeatIT;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandErrorType;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandSensorManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class RecordBandDataIntentService extends IntentService {
    private BandClient client = null;

    private static final String ACTION_RECORD_HEART_RATE = "diabeatIT.action.RECORD_HEART_RATE";
    //private static final String ACTION_BAZ = "diabeatIT.action.BAZ";

    public RecordBandDataIntentService() {
        super("RecordBandDataIntentService");
    }

    private SQLiteDatabase db = null;
    private SQLiteDatabase getDb()
    {
        if (db == null)
        {
            DiabeatITDbHelper dbHelper = new DiabeatITDbHelper(this);
            db = dbHelper.getWritableDatabase();
        }
        return db;
    }

    private BandSensorManager SensorManager() {
        return client.getSensorManager();
    }


    // TODO: Rename parameters
    private static final String PARAM_DB = "diabeatIT.extra.PARAM_DB";
//    private static final String EXTRA_PARAM2 = "diabeatIT.extra.PARAM2";




//    @Override
//    public void onStart(Intent intent, int startId) {
//        Log.d("MMM", "Start Old");
//        getDb();
//    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getDb();
        try {
            // Connect to the band
            new connectToBandTask().execute();
            // Listen for events
            this.subscribeToEvents();
        }
        catch(Exception ex){
            Log.e("MMM", ex.getMessage());
            stopSelf();
            return START_NOT_STICKY;
        }


        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    private void subscribeToEvents() throws BandException {
        try {
            client.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
        }
        catch (Exception ex) {
            Log.e("MMM", "Error when subscribing to events: " + ex.getMessage());
        }
    }


    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionRecordHeartRate(Context context) {
        Intent intent = new Intent(context, RecordBandDataIntentService.class);
        intent.setAction(ACTION_RECORD_HEART_RATE);

//        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    /**
     * Starts this service to perform an action with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            Log.d("MM", "Requested action: " + action);
//            if (ACTION_RECORD_HEART_RATE.equals(action)) {
////                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
////                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
//
////                intent.putExtra(PARAM_DB, db);
//                this.handleActionHeartRate();
//            }
        }
    }

    private void connectToBand() throws InterruptedException, BandException {
        if (client == null) {
            BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
            if (devices.length == 0) {
                throw new BandException("Band is not paired!", BandErrorType.DEVICE_ERROR);
            }
            client = BandClientManager.getInstance().create(getBaseContext(), devices[0]);
        }
        if (client.connect().await() != ConnectionState.CONNECTED) {
            throw new InterruptedException("Band not connected");
        }
        subscribeToEvents();
        Log.d("MMM", "Band connected!");
    }

    /**
     * Listener for heart rate
     */
    private BandHeartRateEventListener mHeartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(BandHeartRateEvent bandHeartRateEvent) {
            if (bandHeartRateEvent != null) {
                saveHeartRate(bandHeartRateEvent.getHeartRate());
            }
        }
    };

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void saveHeartRate(int heartRate) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String dateStr = dateFormat.format(new Date());

        ContentValues values = new ContentValues();
        values.put("Value", heartRate);
        values.put("Date", dateStr);
        db.insert("HeartRateEntry", "null", values);
        Log.d("MMM", "Written " + heartRate + " at " + dateStr);
    }


    @Override
    public void onDestroy() {
        try {
            SensorManager().unregisterAllListeners();
        }
        catch (Exception ex)
        {
            Log.e("MMM", "Error when destroying service: " + ex.getMessage());
        }
        super.onDestroy();
        Log.d("MMM", "Service destroyed");
    }


    private class connectToBandTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                connectToBand();

            } catch (BandException e) {
                String exceptionMessage = "";
                switch (e.getErrorType()) {
                    case UNSUPPORTED_SDK_VERSION_ERROR:
                        exceptionMessage = "Microsoft Health BandService doesn't support your SDK Version. Please update to latest SDK.";
                        break;
                    case SERVICE_ERROR:
                        exceptionMessage = "Microsoft Health BandService is not available. Please make sure Microsoft Health is installed and that you have the correct permissions.";
                        break;
                    default:
                        exceptionMessage = "Unknown error occured: " + e.getMessage();
                        break;
                }
                Log.e("MMM", exceptionMessage);
            } catch (Exception e) {
                Log.e("MMM", e.getMessage());
            }
            return null;
        }
    }
}
