package diabeatIT;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;


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
            bandManager.SaveInDB = true;
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
            this.bandManager.unsubscribeAllListeners();
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
        //this.sleepAndListen();
    }

    private void sleepAndListen() {
        try {
            while (true) {
                Log.d("DiabeatIT", "Listening for 10 seconds");
//                Thread.sleep(30000);
                Thread.sleep(10000);
                this.bandManager.unsubscribeAllListeners();
                Log.d("DiabeatIT", "Sleeping for 5 secs");
//                Thread.sleep(300000);
                Thread.sleep(5000);
                // Resume listening
                Log.d("DiabeatIT", "Listening for 10 seconds");
                this.bandManager.subscribeToEvents();

            }
        }
        catch (Exception ex)
        {
            Log.e("DiabeatIT", "Error in service execution: " + ex.getMessage());
        }
    }
}
