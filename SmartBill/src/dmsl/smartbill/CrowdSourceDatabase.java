/* 
 ** CrowdSourceDatabase.java
 **
 ** Copyright (C) 2012 Kyriakos Georgiou & Kyriakos Frangeskos
 **
 ** This program is free software: you can redistribute it and/or modify
 ** it under the terms of the GNU General Public License as published by
 ** the Free Software Foundation, either version 3 of the License, or
 ** at your option) any later version.
 **
 ** This program is distributed in the hope that it will be useful,
 ** but WITHOUT ANY WARRANTY; without even the implied warranty of
 ** MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 ** GNU General Public License for more details.
 **
 ** You should have received a copy of the GNU General Public License
 ** along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dmsl.smartbill;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * This class manages the local SQLite database on the android device.
 * 
 * @author Kyriakos Georgiou
 * @author Kyriakos Frangeskos
 * 
 */
public class CrowdSourceDatabase {

	// Database fields
	public static final String KEY_ROWID = "_id";
	public static final String KEY_LAT = "latitute";
	public static final String KEY_LONG = "longitude";
	public static final String KEY_BARCODE = "barcode";
	public static final String KEY_PRICE = "price";
	public static final String KEY_DESCRIPTION = "description";

	// Database information
	private static final String DATABASE_NAME = "CrowdSourcing_db";
	private static final String DATABASE_TABLE = "data_table";
	private static final int DATABASE_VERSION = 1;

	private DbHelper dbhelper;
	private final Context context;
	private SQLiteDatabase db;

	private static class DbHelper extends SQLiteOpenHelper {

		public DbHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			// TODO Auto-generated method stub
			db.execSQL("CREATE TABLE " + DATABASE_TABLE + " (" + KEY_ROWID
					+ " INTEGER PRIMARY KEY AUTOINCREMENT, " + KEY_LAT
					+ " REAL, " + KEY_LONG + " REAL, " + KEY_BARCODE
					+ " TEXT NOT NULL, " + KEY_PRICE + " REAL ,"
					+ KEY_DESCRIPTION + " TEXT);");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			// TODO Auto-generated method stub
			db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE);
			onCreate(db);
		}

	}

	public CrowdSourceDatabase(Context c) {
		this.context = c;
	}

	public CrowdSourceDatabase open() {
		dbhelper = new DbHelper(this.context);
		db = dbhelper.getWritableDatabase();
		return this;
	}

	public void close() {
		dbhelper.close();
	}

	/**
	 * Creates a new product entry in the database
	 * 
	 * @param lat
	 *            The latitude the product was scanned from.
	 * @param lon
	 *            The longitude the product was scanned from.
	 * @param bcode
	 *            The product's barcode number.
	 * @param price
	 *            The product's price.
	 * @param descr
	 *            The product's name/title/description.
	 * @return
	 */
	public long createEntry(double lat, double lon, String bcode, float price,
			String descr) {
		ContentValues cv = new ContentValues();

		cv.put(KEY_LAT, lat);
		cv.put(KEY_LONG, lon);
		cv.put(KEY_BARCODE, bcode);
		cv.put(KEY_PRICE, price);
		cv.put(KEY_DESCRIPTION, descr);

		return db.insert(DATABASE_TABLE, null, cv);
	}

	/**
	 * 
	 * @param gtin
	 *            The product's barcode number
	 * @param lat
	 *            The latitude the product was scanned from
	 * @param lon
	 *            The longitude the product was scanned from
	 * @return A product object is returned if it's found in the database, null
	 *         if the product was not found.
	 */
	public ProductObject getData(String gtin, double lat, double lon) {

		int precision = 1000; // Covers an area of 100m^2 approximately.
		Boolean found = false;
		ProductObject foundProduct = null;
		String[] columns = new String[] { KEY_ROWID, KEY_LAT, KEY_LONG,
				KEY_BARCODE, KEY_PRICE, KEY_DESCRIPTION };
		Cursor c = db.query(DATABASE_TABLE, columns, null, null, null, null,
				null);

		int iRow = c.getColumnIndex(KEY_ROWID);
		int iLat = c.getColumnIndex(KEY_LAT);
		int iLon = c.getColumnIndex(KEY_LONG);
		int iBarcode = c.getColumnIndex(KEY_BARCODE);
		int iPrice = c.getColumnIndex(KEY_PRICE);
		int iDescription = c.getColumnIndex(KEY_DESCRIPTION);

		for (c.moveToFirst(); !c.isAfterLast() && !found; c.moveToNext()) {
			if (c.getString(iBarcode).equals(gtin)) {
				foundProduct = new ProductObject(c.getInt(iRow),
						c.getString(iDescription), c.getFloat(iPrice), gtin,
						c.getFloat(iLat), c.getFloat(iLon));
				if ((int) (lat * precision) == (int) (c.getFloat(iLat) * precision)
						&& (int) (lon * precision) == (int) (c.getFloat(iLon) * precision)) {
					// Searching immediately ends when a product was found in
					// the current geolocation.
					found = true;
				}

			}
		}

		return foundProduct;
	}

	public ArrayList<ProductObject> getData(String gtin) {

		ArrayList<ProductObject> toBeReturned = new ArrayList<ProductObject>();

		ProductObject foundProduct = null;
		String[] columns = new String[] { KEY_ROWID, KEY_LAT, KEY_LONG,
				KEY_BARCODE, KEY_PRICE, KEY_DESCRIPTION };
		Cursor c = db.query(DATABASE_TABLE, columns, null, null, null, null,
				null);

		int iRow = c.getColumnIndex(KEY_ROWID);
		int iLat = c.getColumnIndex(KEY_LAT);
		int iLon = c.getColumnIndex(KEY_LONG);
		int iBarcode = c.getColumnIndex(KEY_BARCODE);
		int iPrice = c.getColumnIndex(KEY_PRICE);
		int iDescription = c.getColumnIndex(KEY_DESCRIPTION);

		for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			if (c.getString(iBarcode).equals(gtin)) {
				foundProduct = new ProductObject(c.getInt(iRow),
						c.getString(iDescription), c.getFloat(iPrice), gtin,
						c.getFloat(iLat), c.getFloat(iLon));
				toBeReturned.add(foundProduct);
			}
		}

		return toBeReturned;
	}

	/**
	 * A method to display the database contents.
	 * 
	 * @return A string with all the database's records
	 */
	public String viewDatabase() {
		String[] columns = new String[] { KEY_ROWID, KEY_LAT, KEY_LONG,
				KEY_BARCODE, KEY_PRICE, KEY_DESCRIPTION };
		Cursor c = db.query(DATABASE_TABLE, columns, null, null, null, null,
				null);
		String result = "";

		int iRow = c.getColumnIndex(KEY_ROWID);
		int iLat = c.getColumnIndex(KEY_LAT);
		int iLon = c.getColumnIndex(KEY_LONG);
		int iBarcode = c.getColumnIndex(KEY_BARCODE);
		int iPrice = c.getColumnIndex(KEY_PRICE);
		int iDescription = c.getColumnIndex(KEY_DESCRIPTION);

		for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
			result += c.getInt(iRow) + " " + c.getString(iBarcode) + " " + " "
					+ c.getString(iDescription) + " " + c.getFloat(iPrice)
					+ " " + c.getFloat(iLat) + "  " + c.getFloat(iLon) + "\n\n";
		}

		return result;
	}

	public void editRecord(long id, String newTitle, float newPrice) {
		ContentValues values = new ContentValues();
		values.put(KEY_DESCRIPTION, newTitle);
		values.put(KEY_PRICE, newPrice);
		db.update(DATABASE_TABLE, values, KEY_ROWID + "=" + id, null);
	}

}
