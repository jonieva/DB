package diabeatIT;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

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
    // TODO: Rename actions, choose action names that describe tasks that this
    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_RECORD_HEART_RATE = "diabeatIT.action.RECORD_HEART_RATE";
    //private static final String ACTION_BAZ = "diabeatIT.action.BAZ";


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


    // TODO: Rename parameters
    private static final String PARAM_DB = "diabeatIT.extra.PARAM_DB";
//    private static final String EXTRA_PARAM2 = "diabeatIT.extra.PARAM2";

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
     * Starts this service to perform action Baz with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
//    public static void startActionBaz(Context context, String param1, String param2) {
//        Intent intent = new Intent(context, RecordBandDataIntentService.class);
//        intent.setAction(ACTION_BAZ);
//        intent.putExtra(EXTRA_PARAM1, param1);
//        intent.putExtra(EXTRA_PARAM2, param2);
//        context.startService(intent);
//    }

    public RecordBandDataIntentService() {
        super("RecordBandDataIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_RECORD_HEART_RATE.equals(action)) {
//                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
//                final String param2 = intent.getStringExtra(EXTRA_PARAM2);

//                intent.putExtra(PARAM_DB, db);
                this.handleActionHeartRate();
            }
//            else if (ACTION_BAZ.equals(action)) {
//                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
//                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
//                handleActionBaz(param1, param2);
//            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleActionHeartRate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String dateStr = dateFormat.format(new Date());

        // Read he

        ContentValues values = new ContentValues();
        int currentHeartRate = 75;
        values.put("Value", currentHeartRate);
        values.put("Date", dateStr);
        db.insert("HeartRateEntry", "null", values);
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private void handleActionBaz(String param1, String param2) {
        // TODO: Handle action Baz
        throw new UnsupportedOperationException("Not yet implemented");
    }


    private void createDb() {
        DiabeatITDbHelper dbHelper = new DiabeatITDbHelper(this);
        db = dbHelper.getWritableDatabase();
    }

//    private void saveCurrentHeartRate() {
//
//
//        appendToUI("Written " + currentHeartRate + " at " + dateStr, txtDataStatus);
//    }
}
