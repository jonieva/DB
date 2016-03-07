import sys, os
import xml.etree.ElementTree as et
import sqlite3
import datetime, time
import pandas as pd
import numpy as np
import logging


CGM = 0
Band = 1

class DataImporter(object):
    date_format = '%Y-%m-%d %H:%M:%S'

    def __init__(self, base_directory, current_db_path):
        """
        :param base_directory: Working base directory where all the main files are going to be stored
        :param current_db_path: Current database where the last version of all the files is stored
        :return:
        """
        self.base_directory = base_directory
        self.current_db_path = current_db_path

    # def copyPhoneFile(self, phone_file, local_directory):
    #     """ Copy a file in the smartphone to a local directory
    #     :param phone_file: file to
    #     :param local_path:
    #     :return: full path to the imported file
    #     """

    def get_last_smartphone_db_file(self, phone_directory, band_directory, output_csv_writer=None):
        """ Read a file from a SQLite database containing the band data and output them to a csv file.
        If there is more than one file, import just the most recent one.
        :param phone_directory: directory where the SQLite database file/s are stored
        :param band_directory: local directory where the band files are stored (relative to the base directory), included the last_sync file
        :param output_csv_writer: output csv writer. If None, just copy the sql db file from the smartphone
        :return: most recent db file (local path) or None if there' not any db file
        """
        # Read all the files
        os.system("adb pull {0} {1}".format(phone_directory, band_directory))
        last = ""
        #if output_csv_writer is not None:
            # Export to csv
        # Get the most recent file from db files in the local folder

        try:
            for file in os.listdir(band_directory):
                if file.endswith(".db") and file > last:
                    last = file
            return os.path.join(band_directory, last)
            #newest_db = min(glob.iglob(os.path.join(band_directory, "*.db")), key=os.path.getctime)
            #return newest_db
        except:
            return None
            # Connect to this file
            # conn = sqlite3.connect(newest_db)
            # cursor = conn.cursor()
            # # Iterate over all the records newer than the most recent record saved
            # last_record_date = None
            # sql = "SELECT * FROM SensorEntry WHERE Date>'{0}' ORDER BY Date".format(last_sync)
            # for row in cursor.execute(sql):
            #     output_csv_writer.writerow(row)
            #     last_record_date = row[0]
            # # Write last record date
            # if last_record_date is not None:
            #     with open(ls, 'w+') as f:
            #         f.write(last_record_date)

    def import_cgm_xml_into_db(self, cgm_file_to_import, current_db_file):
        """ Import a CSV file with data from a CGM to a SQLite database
        :param cgm_file_to_import:
        :param current_db_file:
        :return:
        """
        db = sqlite3.connect(current_db_file)
        db.isolation_level = None
        cursor = db.cursor()

        self.read_cgm(cgm_file_to_import, sqldb_cursor=cursor)

    def read_cgm(self, xml_input_file, sqldb_cursor=None, output_csv_writer=None):
        """ Read the data of an exported XML file (from Dexcom Studio) and save the data in:
        - Import table in the Database if sqldb_cursor != None  (common use)
        - CSV file if output_csv_writer != None
        :param xml_input_file: XMl that contains the cgm data
        :param output_csv_writer: csv file where the data are exported (optional)
        :param sqldb_cursor: sqlite cursor used to store the data in a SQLite database (optional)
        """
        with open(xml_input_file, 'r+b') as f:
            xml = f.read()
        root = et.fromstring(xml)
        glucose_readings_node = root.findall("GlucoseReadings/Glucose")
        current_date = self.__get_current_date__()
        i = 0
        for elem in glucose_readings_node:
            date = elem.attrib["DisplayTime"]
            value = elem.attrib["Value"]
            if output_csv_writer is not None:
                output_csv_writer.writerow((date, 0, value))
            if sqldb_cursor is not None:
                if value.upper() == "LOW":
                    value = 40
                elif value.upper() == "HIGH":
                    value = 400
                #sqldb_cursor.execute("insert into Import values ('2015-12-21 00:27:34', '2015-12-21 00:27:34', 0, 0)")
                sqldb_cursor.execute("""INSERT INTO Import (Import_date, Date, Type, Value) VALUES (?, ?, ?, ?);""",
                                     (current_date, date, 0, value))
                i += 1
                if i%500 == 0:
                    print("Read {0} elems...".format(i))
                #print("insert into Import (Import_date, Date, Type, Value) values ('{0}', '{1}', 0, {2})".format(current_date, date, value))

    # def export_all_data_to_csv(self, smartphone_directory, cgm_file, output_csv_file):
    #     """ Export cgm and Band data to a CSV file
    #     :param smartphone_directory: smartphone directory where SQLite files are stored
    #     :param cgm_file: name of the file to import (located in base_directory/cgm folder)
    #     :param output_csv_file: name of the exported file (located in base_directory
    #     :return:
    #     """
    #     with open(output_file, "a+b") as csvfile:
    #         writer = csv.writer(csvfile)
    #         read_cgm(cgm_file, writer)
    #         read_band_SQLite_data(smartphone_directory, self.base_directory + "band/", writer)


    def import_band_data(self, source_db_file):
        """ Read a db with data generated by the Microsoft Band.
        To have a backup, it massively import all the data to SensorEntry
        TODO: optimize the function copying all the data into the Import table without checking anything else
        :param source_db_file:
        :return:
        """
        # Read the last record of the source
        db_source = sqlite3.connect(source_db_file)
        origin_cursor = db_source.cursor()
        origin_cursor.execute("select * from SensorEntry order by Date desc")

        # Check if there is any record like that in the destination already
        db_dest = sqlite3.connect(self.current_db_path)
        db_dest.isolation_level = None
        destination_cursor = db_dest.cursor()
        imported_records = 0
        # if destination_row is None:
        try:
            # There are rows that should be copied. Begin a transaction
            origin_row = origin_cursor.fetchone()
            destination_cursor.execute("begin")
            while origin_row is not None:
                # Search for the record in the destination database
                destination_cursor.execute("select * from SensorEntry where Date=? and Type=? and Value=?", origin_row)
                destination_row = destination_cursor.fetchone()
                # If the record is not there, insert it
                if destination_row is None:
                    destination_cursor.execute("insert into SensorEntry (Date, Type, Value) values (?, ?, ?)", origin_row)
                    imported_records += 1
                    if imported_records % 100 == 0:
                        print("Imported {0} records...", imported_records)
                # Read next record from the source
                origin_row = origin_cursor.fetchone()
            destination_cursor.execute("commit")
        except Exception as ex:
            print ("Error in the import. Rollback changes: " + ex.message)
            destination_cursor.execute("rollback")
            raise ex

        print("Imported {0} records".format(imported_records))

    def __get_current_date__(self):
        """ Return the current date and time formatted as YYYY-MM-dd HH:mm:ss (by default)
        :return:
        """
        return time.strftime(self.date_format)

    def read_CGM_xdrip(self, cgm_db_path):
        """ Read all the data from the CGM database (xdrip) and copy them to "SensorEntry" table
        Then, normalize all those data in 1 minute interpolated series and save them into SensorEntryNormalized table
        """
        destination_db = sqlite3.connect(self.current_db_path)
        cgm_db = sqlite3.connect(cgm_db_path)

        # Read the last time the process was made
        destination_cursor = destination_db.cursor()
        destination_cursor.execute("select max(Date) from SensorEntry where Type = 0")
        date = destination_cursor.fetchone()[0]
        destination_cursor.close()
        if date is None:
            # First time doing the process
            date = "2015-01-01 00:00:00"
        logging.debug("Date to process from: " + date)


        # Read unique values from SensorEntry (DEPRECATED)!!
        # df = pd.read_sql_query("select distinct * from SensorEntry where Type = 0 and Date>'{0}' order by Date".format(date),
        #                                destination_db, index_col="Date", parse_dates=["Date"])
        # first_record = "A"
        # last_record = "A"
        # num_records = len(df)

        # Read the values from the CGM database
        converted_date = datetime.datetime.strptime(date, self.date_format)
        timestamp = time.mktime(converted_date.timetuple()) * 1000
        df = pd.read_sql_query("select filtered_data, timestamp from BgReadings where timestamp > ? order by timestamp",
                               cgm_db, params=[timestamp])


        first_record = datetime.datetime.fromtimestamp(df["timestamp"].min() / 1000).strftime(self.date_format)
        last_record = datetime.datetime.fromtimestamp(df["timestamp"].max() / 1000).strftime(self.date_format)
        num_records = len(df)
        # Create a new DataFrame from the read values
        import_date = self.__get_current_date__()
        # df = pd.DataFrame({"Value": df["filtered_data"], "Type":0, "ImportedDate": import_date,
        #                    "Date": map(lambda x:datetime.datetime.fromtimestamp(x/1000),df["timestamp"])})

        df = pd.DataFrame({"Value": df["filtered_data"], "Type":0, "ImportedDate": import_date,
                           "Date": map(DataImporter.timestamp_to_datetime, df["timestamp"])})

        #df = df.set_index("Date")
        logging.debug("Found {0} new entries in xDrip db. First record: {1}; Last record: {2}".format(num_records,
                                                                                          first_record, last_record))

        # Insert into SensorEntry
        df['Date'] = df["Date"].map(lambda x: x.strftime(self.date_format))
        df.to_sql("SensorEntry", destination_db, if_exists="append", index=False)
        logging.debug("Written {0} new entries in SensorEntry with ImportDate={1}".format(num_records, import_date))

        self.normalize_sensor_entries(destination_db, first_record_date=first_record, entry_type=0, method="upsample")

        # Resample to 5 minutes tracks to for with a normalized time series. Then interpolate in 1 minute series
        # df_resampled = df.resample("5T").resample("1T").interpolate(method="time")
        # #df.to_csv("/Data/jonieva/Dropbox/DiabeatIT/Data/temp.csv")
        # # Copy to database (convert the Date column because of sqlite limitations)
        # df_resampled['Date'] = df_resampled.index.map(lambda x: x.strftime(self.date_format))
        # df_resampled.to_sql("SensorEntryNormalized", destination_db, if_exists="append", index=False)
        #
        # # Write a record in ImportDate with the current datetime
        # destination_cursor = destination_db.cursor()
        # operation_date = datetime.datetime.now().strftime(self.date_format)
        # destination_cursor.execute("insert into ImportDate (Type, Date, FirstRecord, LastRecord, NumRecords) values (?,?,?,?,?)",
        #                            (0, operation_date, first_record, last_record, num_records))
        destination_db.commit()
        #logging.debug("{0} rows written succesfully to SensorEntryNormalized table".format(len(df_resampled)))

    def normalize_sensor_entries(self, entry_type, first_record_date=None,):
        """ Take the records from SensorEntry from a particular record date and migrate them to SensorEntryNormalized,
        where all the data will be normalized to 1 minute
        :param entry_type: type of data to transfer: CG or Band. It can be Band or CGM. When entry_type==CGM,
        entry data will be upsampled to fit in 1 minute batches using interpolation.
        Otherwise (entry_type==Band), the data will be downsampled using average
        :param first_record_date: date of the first record to be moved to SensorEntryNormalized
        """
        if entry_type not in (CGM, Band):
            raise Exception("entry_type must be CGM or Band")

        db_connection = sqlite3.connect(self.current_db_path)

        if first_record_date is None:
            first_record_date = "2000-01-01 00:00:00"
        params = [first_record_date]
        query = "select Date, Type, Value from SensorEntry where Date >= ?"

        if entry_type==CGM:
            # Only CGM
            query += "and Type=0"
            #params.append(entry_type)
        else:
            # Any Band types
            query += " and Type > 1"

        df = pd.read_sql_query(query, db_connection, coerce_float=False, params=params, parse_dates={"Date": self.date_format})
        # df = pd.read_sql_query(query, db_connection, coerce_float=False, params=params, parse_dates={"Date": '%Y-%m-%d %H:%M:%S'})
        if len(df) == 0:
            logging.info("No registers to migrate!")
            return

        logging.debug("Migrating {0} records starting on {1}".format(len(df), first_record_date))

        if entry_type == CGM:
            # CGM. Group in 5 minutes batches and then upsample to 1 minute to normalize, using interpolation
            # Group by dates that are further than 15 minutes (consider different series, we don't want interpolation here)
            df['delta'] = (df['Date']-df['Date'].shift()).fillna(0)
            df['serie'] = df["delta"].apply(lambda x: 1 if x > np.timedelta64(15, 'm') else 0).cumsum()

            df = df.set_index("Date")
            #groups = list(df.groupby("groups"))
            groups = df.groupby("serie")
            # Upsample each one of the series
            result_dataframes = []
            for g in groups:
                result_dataframes.append(g[1].resample("5T").resample("1T").interpolate(method="time"))
            # Concat all of them
            df_resampled = pd.concat(result_dataframes)

            #df_resampled = df.resample("5T").resample("1T").interpolate(method="time")
            # Copy to database (convert the Date column because of sqlite limitations)
            df_resampled['Date'] = df_resampled.index.map(lambda x: x.strftime(self.date_format))
            # Removed unused columns
            df_resampled = df_resampled.drop("serie", axis=1)


            # df_resampled['Date'] = df_resampled.index.map(lambda x: x.strftime('%Y-%m-%d %H:%M:%S'))


            # Write a record in ImportDate with the current datetime
            # destination_cursor = db_connection.cursor()
            # operation_date = datetime.datetime.now().strftime(self.date_format)
            # destination_cursor.execute("insert into ImportDate (Type, Date, FirstRecord, LastRecord, NumRecords) values (?,?,?,?,?)",
            #                            (0, operation_date, first_record, last_record, num_records))
            df_resampled.to_sql("SensorEntryNormalized", db_connection, if_exists="append", index=False)
        else:
            # Band. Group measures in 1 minute batches. Ex: band data
            df = df.set_index("Date")
            for t in range(2, 6, 1):
                df_resampled = df[(df["Type"]==t)].resample("1T")
                df_resampled['Date'] = df_resampled.index.map(lambda x: x.strftime(self.date_format))
                df_resampled.to_sql("SensorEntryNormalized", db_connection, if_exists="append", index=False)
                print "{0} records written to SensorEntryNormalized for type {1}".format(len(df_resampled), t)

    def remove_duplicates_db(self):
        """ Remove the duplicates from SensorEntry database
        """
        db = sqlite3.connect(self.current_db_path)
        db_cursor = db.cursor()
        db_cursor.execute("delete from SensorEntry where Id not in (select min(Id) from SensorEntry	group by Date, Type, Value)")
        db.commit()

    @staticmethod
    def timestamp_to_datetime(timestamp, convert_to_string=False):
        date = datetime.datetime.fromtimestamp(timestamp/1000)
        if convert_to_string:
            return time.strftime(DataImporter.date_format)
        return date

base_directory = "/Data/jonieva/Dropbox/DiabeatIT/Data/"
current_db_path = base_directory + "current.db"

logger = logging.getLogger()
logger.setLevel(logging.DEBUG)
# logging.basicConfig(format="%(asctime)s [%(threadName)-12.12s] [%(levelname)-5.5s]  %(message)s")
#logFormatter = logging.Formatter("%(asctime)s [%(threadName)-12.12s] [%(levelname)-5.5s]  %(message)s")
logFormatter = logging.Formatter('%(asctime)s - %(levelname)s - %(message)s')
consoleHandler = logging.StreamHandler()
consoleHandler.setLevel(logging.DEBUG)
consoleHandler.setFormatter(logFormatter)
logger.addHandler(consoleHandler)


if __name__ == "__main__":
    importer = DataImporter(base_directory, current_db_path)
    phone_dir = "/storage/sdcard0/diabeatit/"
    # if sys.argv[1] == "-exportcsv":
    #     cgm_file = sys.argv[2]
    #     cgm_file = base_directory + "cgm/" + "cgm.export.2015-12-20.xml"
    #     output_file = "{0}csv_export/{1}.csv".format(base_directory, time.strftime("%Y-%m-%d_%H-%M-%S"), ".csv")
    #     importer.export_all_data_to_csv(phone_dir, cgm_file, output_file)
    if sys.argv[1] == "-read_smartphone-db":
        file = importer.get_last_smartphone_db_file(phone_dir, os.path.join(base_directory, "band"), None)
        if file is None:
            print("No files imported!")
        else:
            print("Importing {0} to current db...".format(file))
            importer.import_band_data(file)
    elif sys.argv[1] == "-import_cgm_xml":
        import_file = sys.argv[2]
        importer.import_cgm_xml_into_db(import_file, current_db_path)
    elif sys.argv[1] == "-import_band_data":
        f = sys.argv[2]
        importer.import_band_data(f)
    elif sys.argv[1] == "-read_xdrip":
        importer.read_CGM_xdrip("/Data/jonieva/Dropbox/DiabeatIT/Data/cgm/xdrip.sqlite")
    elif sys.argv[1] == "-normalize_sensor_entry":
        importer.normalize_sensor_entries(CGM)



