package diabeatIT;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

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
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "DiabeatIT.db";

    public static final String SQL_CREATE_DB =
        "CREATE TABLE HeartRateEntry " +
                "(Id INTEGER PRIMARY KEY, " +
                "Value INTEGER," +
                "Date TEXT)";

    public static final String SQL_DELETE_DB =
            "DROP TABLE HeartRateEntry";




    public DiabeatITDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_DB);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_DB);
        onCreate(db);
    }
}