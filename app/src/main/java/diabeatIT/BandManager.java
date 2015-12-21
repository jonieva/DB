package diabeatIT;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.microsoft.band.BandClient;
import com.microsoft.band.BandClientManager;
import com.microsoft.band.BandErrorType;
import com.microsoft.band.BandException;
import com.microsoft.band.BandInfo;
import com.microsoft.band.ConnectionState;
import com.microsoft.band.UserConsent;
import com.microsoft.band.sensors.BandGsrEvent;
import com.microsoft.band.sensors.BandGsrEventListener;
import com.microsoft.band.sensors.BandHeartRateEvent;
import com.microsoft.band.sensors.BandHeartRateEventListener;
import com.microsoft.band.sensors.BandRRIntervalEvent;
import com.microsoft.band.sensors.BandRRIntervalEventListener;
import com.microsoft.band.sensors.BandSkinTemperatureEvent;
import com.microsoft.band.sensors.BandSkinTemperatureEventListener;
import com.microsoft.band.sensors.HeartRateConsentListener;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Perform all the interactions with the Band (Reading, notifications, etc.)
 */
public class BandManager{
    public enum MeasurementType {
        GlucoseCGM,         // 0: Continuous glucose monitor
        GlucoseMeter,       // 1: Glucometer
        HeartRate,          // 2: Heart Rate
        GSR,                // 3: Galvanic skin response
        SkinTemperature,    // 4: Skin temperature
        RR,                 // 5: interval in seconds between the last two continuous heart beats.
                            //    (The data returned should be used only in resting mode)

    }

    private BandClient bandClient;
    private DiabeatITDbHelper dbHelper;
    private Context currentContext;
    private FileWriter resultsFileWriter;
    private StringBuilder sb = new StringBuilder();

    public Boolean SaveInDB = true;
//    public IOnTaskCompleted Callback;

    public BandManager(Context context) {
        this.currentContext = context;
    }

    public void connect(IOnTaskCompleted callback) {
        ConnectToBandTask task = new ConnectToBandTask(callback);
        task.execute();
    }



//    public void openResultsFile(){
//        try {
//            File dir = new File(this.currentContext.getFilesDir().getPath());
//            dir = new File(Environment.getExternalStorageDirectory().getPath() + "DiabeatIT");
//            dir.mkdir();
//
//            Log.d("DiabeatIT", "Created dir: " + this.currentContext.getFilesDir());
//            File resultsFile = new File(this.currentContext.getFilesDir(), "diabeatIT_results.csv");
//
//            if (!resultsFile.exists())
//                resultsFile.createNewFile();
//            resultsFileWriter = new FileWriter(resultsFile, true);
//            resultsFileWriter.write("hola");
//            resultsFileWriter.close();
//        }
//        catch(Exception ex)
//        {
//            Log.e("DiabeatIT", "Error creating the results file: " + ex.getMessage());
//        }
//    }

    public void askForPermissions(Activity activity, IOnTaskCompleted callback) {
        AskForConsentAppTask task = new AskForConsentAppTask(activity, callback);
        task.execute();
    }

    public void subscribeToEvents() { //throws BandException {
        try {
            bandClient.getSensorManager().registerHeartRateEventListener(heartRateEventListener);
            bandClient.getSensorManager().registerGsrEventListener(gsrEventListener);
            bandClient.getSensorManager().registerSkinTemperatureEventListener(skinTemperatureEventListener);
            bandClient.getSensorManager().registerRRIntervalEventListener(rrEventListener);
        }
        catch (Exception ex) {
            Log.e("DiabeatIT", "Error when subscribing to events: " + ex.getMessage());
        }
    }

    public DiabeatITDbHelper getDb() {
        if (dbHelper == null) {
            dbHelper = new DiabeatITDbHelper(this.currentContext);
            //db = dbHelper.getWritableDatabase();
        }
//        return db;
        return dbHelper;
    }

    public void testDB() throws IOException{
//        this.getDb().removeAllDatabases();
        ContentValues values = new ContentValues();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = dateFormat.format(new Date());
        values.put("Type", MeasurementType.HeartRate.ordinal());
        values.put("Value", 69);
        values.put("Date", dateStr);
        this.getDb().insertEntry(values);
        this.getDb().backupDb();
    }

    public void backupDatabase() throws IOException {
        this.getDb().backupDb();
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
    private BandHeartRateEventListener heartRateEventListener = new BandHeartRateEventListener() {
        @Override
        public void onBandHeartRateChanged(BandHeartRateEvent bandHeartRateEvent) {
            if (bandHeartRateEvent != null) {
                saveSensorData(MeasurementType.HeartRate, bandHeartRateEvent.getHeartRate());
            }
        }
    };

    private BandGsrEventListener gsrEventListener = new BandGsrEventListener() {
        @Override
        public void onBandGsrChanged(BandGsrEvent bandGsrEvent) {
            saveSensorData(MeasurementType.GSR, bandGsrEvent.getResistance());
        }
    };

    private BandSkinTemperatureEventListener skinTemperatureEventListener = new  BandSkinTemperatureEventListener() {
        @Override
        public void onBandSkinTemperatureChanged(BandSkinTemperatureEvent bandSkinTemperatureEvent) {
            saveSensorData(MeasurementType.SkinTemperature, bandSkinTemperatureEvent.getTemperature());
        }
    };

    private BandRRIntervalEventListener rrEventListener = new BandRRIntervalEventListener() {
        @Override
        public void onBandRRIntervalChanged(BandRRIntervalEvent bandRRIntervalEvent) {
            saveSensorData(MeasurementType.RR, bandRRIntervalEvent.getInterval());
        }
    };

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void saveSensorData(MeasurementType type, double value) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateStr = dateFormat.format(new Date());

        if (this.SaveInDB) {
            ContentValues values = new ContentValues();
            values.put("Type", type.ordinal());
            values.put("Value", value);
            values.put("Date", dateStr);
            this.getDb().insertEntry(values);
        }
        String data = "Saved " + dateStr + "," + type + "," + value + "\n";
        Log.d("DiabeatIT_Data", data);
        // Write in "file"
        //sb.append(data);

    }



    public void unsubscribeAllListeners() {
        try {
            this.bandClient.getSensorManager().unregisterAllListeners();
            this.closeResultsFile();
        }
        catch (Exception ex) {
            Log.e("DiabeatIT", "Error when unregistering listeners: " + ex.getMessage());
        }
    }

    public void closeResultsFile() {
        try
        {
            resultsFileWriter.close();
        }
        catch (Exception ex)
        {
            Log.e("DiabeatIT", "Error when closing the results file: " + ex.getMessage());
        }
    }

    public void removeDatabases(){
        this.getDb().removeAllDatabases();
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
