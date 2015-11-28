package diabeatIT;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

/**
 * Created by Jorge on 05/05/15.
 */
//public final class DbStructure {
//    public DbStructure() {}
//
//    public static abstract class HeartRateEntry implements BaseColumns {
//        public static final String TABLE_NAME = "HeartRateEntry";
//        public static final String COLUMN_NAME_ENTRY_ID = "Id";
//        public static final String COLUMN_NAME_VALUE = "Value";
//        public static final String COLUMN_NAME_DATE = "Date";
//
//    }
//}

public class DiabeatITDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "DiabeatIT2.db";

    private static final String TABLE_NAME = "SensorEntry";
    private static final String[] columnNames = {"Id", "Date", "Type", "Value"};
    private static final String[] columnTypes = {"INTEGER", "TEXT", "INTEGER", "REAL"};

    private SQLiteDatabase db;

    public static final String SQL_CREATE_DB =
        "CREATE TABLE SensorEntry " +
                "(Id INTEGER PRIMARY KEY, " +
                "Date TEXT" +
                "Type INTEGER" +
                "Value REAL)";

    public static final String SQL_DELETE_DB =
            "DROP TABLE SensorEntry";




    public DiabeatITDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_DB);
        this.db = db;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_DB);
        onCreate(db);
    }

    public void insertEntry(ContentValues values){
        db.insert(this.TABLE_NAME, "null", values);
    }

    public void copyDatabase(String path){
        try {
            FileInputStream inStream = new FileInputStream(db.getPath());
            FileOutputStream outStream = new FileOutputStream(path);
            FileChannel inChannel = inStream.getChannel();
            FileChannel outChannel = outStream.getChannel();
            inChannel.transferTo(0, inChannel.size(), outChannel);
            inStream.close();
            outStream.close();
        }
        catch (Exception ex) {
            Log.e("DiabeatIT", "Error copying database: " + ex.getMessage());
        }
    }


//    public Boolean backupDatabaseCSV(String outFileName) {
//        Boolean returnCode = false;
//        int i = 0;
//        String csvHeader = "";
//        String csvValues = "";
//        for (i = 0; i < GC.CURCOND_COLUMN_NAMES.length; i++) {
//            if (csvHeader.length() > 0) {
//                csvHeader += ",";
//            }
//            csvHeader += "\"" + GC.CURCOND_COLUMN_NAMES[i] + "\"";
//        }
//
//        csvHeader += "\n";
//        MyLog.d(TAG, "header=" + csvHeader);
//        dbAdapter.open();
//        try {
//            File outFile = new File(outFileName);
//            FileWriter fileWriter = new FileWriter(outFile);
//            BufferedWriter out = new BufferedWriter(fileWriter);
//            Cursor cursor = dbAdapter.getAllRows();
//            if (cursor != null) {
//                out.write(csvHeader);
//                while (cursor.moveToNext()) {
//                    csvValues = Long.toString(cursor.getLong(0)) + ",";
//                    csvValues += Double.toString(cursor.getDouble(1))
//                            + ",";
//                    csvValues += Double.toString(cursor.getDouble(2))
//                            + ",";
//                    csvValues += "\"" + cursor.getString(3) + "\",";
//                    csvValues += Double.toString(cursor.getDouble(4))
//                            + ",";
//                    csvValues += Double.toString(cursor.getDouble(5))
//                            + ",";
//                    csvValues += "\"" + cursor.getString(6) + "\",";
//                    csvValues += Double.toString(cursor.getDouble(7))
//                            + ",";
//                    csvValues += Double.toString(cursor.getDouble(8))
//                            + ",";
//                    csvValues += Double.toString(cursor.getDouble(9))
//                            + "\n";
//                    out.write(csvValues);
//                }
//                cursor.close();
//            }
//            out.close();
//            returnCode = true;
//        } catch (IOException e) {
//            returnCode = false;
//            MyLog.d(TAG, "IOException: " + e.getMessage());
//        }
//        dbAdapter.close();
//        return returnCode;
//    }


}