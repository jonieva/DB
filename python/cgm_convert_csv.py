import sys, os
import csv
import xml.etree.ElementTree as et
import sqlite3
import glob
import time

def read_band_SQLite_data(phone_diretory, band_directory, output_csv_writer):
    # Get the last sync date
    ls = band_directory + "last_sync.txt"
    if os.path.exists(ls):
        # Read the date
        with open(ls, 'r+b') as file:
            last_sync = file.read().strip()
    else:
        last_sync = "2015-01-01 00:00:00"
    #last_sync = "2015-12-20 16:58:42"
    # Read all the files
    os.system("adb pull {0} {1}".format(phone_diretory, base_directory))
    # Get just the most recent file
    newest_db = min(glob.iglob(band_directory + "*.db"), key=os.path.getctime)
    # Connect to this file
    conn = sqlite3.connect(newest_db)
    cursor = conn.cursor()
    # Iterate over all the records newer than the most recent record saved
    last_record_date = None
    sql = "SELECT * FROM SensorEntry WHERE Date>'{0}' ORDER BY Date".format(last_sync)
    for row in cursor.execute(sql):
        output_csv_writer.writerow(row)
        last_record_date = row[0]
    # Write last record date
    if last_record_date is not None:
        with open(ls, 'w+') as f:
            f.write(last_record_date)

def read_cgm(xml_input_file, output_csv_writer):
    with open(xml_input_file, 'r+b') as f:
        xml = f.read()
    root = et.fromstring(xml)
    glucose_readings_node = root.findall("GlucoseReadings/Glucose")
    for elem in glucose_readings_node:
        date = elem.attrib["DisplayTime"]
        value = elem.attrib["Value"]
        writer.writerow((date, 0, value))

base_directory = "/Data/jonieva/Dropbox/DiabeatIT/Data/"
# file = sys.argv[1]
cgm_file = base_directory + "cgm/" + "cgm.export.2015-12-20.xml"
output_file = "{0}readings/{1}.csv".format(base_directory, time.strftime("%Y-%m-%d_%H-%M-%S"), ".csv")

with open(output_file, "a+b") as csvfile:
    writer = csv.writer(csvfile)
    read_cgm(cgm_file, writer)
    read_band_SQLite_data("/storage/emulated/legacy/diabeatit/", "/Data/jonieva/Dropbox/DiabeatIT/Data/band/", writer)