package diabeatIT;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandErrorType;
import com.microsoft.band.BandException;
import com.microsoft.band.BandIOException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Perform all the interactions with the Band (Reading, notifications, etc.)
 */
public class BandManager {
    private BandClient bandClient;
    private SQLiteDatabase db;
    private Context currentContext;

    public Boolean SaveInDB = true;
//    public IOnTaskCompleted Callback;

    public BandManager(Context context) {
        this.currentContext = context;
    }

    public void connect(IOnTaskCompleted callback) {
        ConnectToBandTask task = new ConnectToBandTask(callback);
        task.execute();
    }

    public void askForPermissions(Activity activity, IOnTaskCompleted callback) {
        AskForConsentAppTask task = new AskForConsentAppTask(activity, callback);
        task.execute();
    }

    public void subscribeToEvents() { //throws BandException {
        try {
            bandClient.getSensorManager().registerHeartRateEventListener(mHeartRateEventListener);
        }
        catch (Exception ex) {
            Log.e("DiabeatIT", "Error when subscribing to events: " + ex.getMessage());
        }
    }

    private SQLiteDatabase getDb() {
        if (db == null) {
            DiabeatITDbHelper dbHelper = new DiabeatITDbHelper(this.currentContext);
            db = dbHelper.getWritableDatabase();
        }
        return db;
    }

    /***********************
     * LISTENERS
     ***********************/
    /**
     * Listen to the CONSENT for reading heart rate
     */
    private HeartRateConsentListener heartRateConsentListener = new HeartRateConsentListener() {
        @Override
        public void userAccepted(boolean b) {

        }
    };
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
        if (this.SaveInDB) {
            this.getDb().insert("HeartRateEntry", "null", values);
        }
        Log.d("DiabeatIT", "Written " + heartRate + " at " + dateStr);
    }

    public void disconnect() {
        try {
            this.bandClient.getSensorManager().unregisterAllListeners();
        }
        catch (Exception ex) {
            Log.e("DiabeatIT", "Error when unregistering listeners: " + ex.getMessage());
        }
    }


    /*******************
     * PRIVATE TASKS
     ******************/
    /**
     * Task that asks for the consent to read all the required sensors
     */
    private class AskForConsentAppTask extends AsyncTask<Void, Void, Void> {
        private Activity activity;
        private IOnTaskCompleted callback;
        public AskForConsentAppTask(Activity activity, IOnTaskCompleted callback) {
            this.activity = activity;
            this.callback = callback;
        }
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if(bandClient.getSensorManager().getCurrentHeartRateConsent() != UserConsent.GRANTED) {
                    // user has not consented, request it
                    bandClient.getSensorManager().requestHeartRateConsent(activity, heartRateConsentListener);
                }
            } catch (Exception e) {
                Log.e("DiabeatIT", "Error when asking for consent: " + e.getMessage());
                throw e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (callback != null) {
                callback.PostExecution();
            }
        }
    }



    /**
     * Connect to the Band (in background)
     */
    private class ConnectToBandTask extends AsyncTask<Void, Void, Void> {
        IOnTaskCompleted callback;
        public ConnectToBandTask(IOnTaskCompleted callback){
            this.callback = callback;
        }

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
                Log.e("DiabeatIT", exceptionMessage);
            } catch (Exception e) {
                Log.e("DiabeatIT", e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d("DiabeatIT", "Band connected!");
            if (callback != null)
                callback.PostExecution();
        }

        private void connectToBand() throws InterruptedException, BandException {
            if (bandClient == null) {
                BandInfo[] devices = BandClientManager.getInstance().getPairedBands();
                if (devices.length == 0) {
                    throw new BandException("Band is not paired!", BandErrorType.DEVICE_ERROR);
                }
                bandClient = BandClientManager.getInstance().create(currentContext, devices[0]);
            }
            if (bandClient.connect().await() != ConnectionState.CONNECTED) {
                throw new InterruptedException("Band not connected");
            }
        }
    }
}
