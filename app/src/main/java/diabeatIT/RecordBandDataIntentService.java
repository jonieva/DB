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
public class RecordBandDataIntentService extends IntentService implements IOnTaskCompleted  {
    private BandManager bandManager;
////    private static final String ACTION_RECORD_HEART_RATE = "diabeatIT.action.RECORD_HEART_RATE";

    public RecordBandDataIntentService() {
        super("RecordBandDataIntentService");
    }

//    @Override
//    public void onStart(Intent intent, int startId) {
//        Log.d("DiabeatIT", "Start Old");
//        getDb();
//    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            bandManager = new BandManager(this);
            bandManager.SaveInDB = false;
            bandManager.connect(this);
        }
        catch(Exception ex){
            Log.e("DiabeatIT", ex.getMessage());
            stopSelf();
            return START_NOT_STICKY;
        }


        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
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

    @Override
    public void onDestroy() {
        try {
            this.bandManager.disconnect();
        }
        catch (Exception ex)
        {
            Log.e("DiabeatIT", "Error when destroying service: " + ex.getMessage());
        }
        super.onDestroy();
        Log.d("DiabeatIT", "Service destroyed");
    }

    @Override
    public void PostExecution() {
        this.bandManager.subscribeToEvents();
    }
}
