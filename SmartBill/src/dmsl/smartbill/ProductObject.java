/* 
 ** ProductObject.java
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

import java.text.NumberFormat;

import android.content.Context;

/**
 * This class is an object which represents a product. This representation
 * is used through out the program.
 * 
 * @author Kyriakos Georgiou
 * @author Kyriakos Frangeskos
 *
 */
public class ProductObject {

	private long id = 0;
	private String title = null;
	private float price = 0;
	private String barcode = null;
	private double lat = 0;
	private double lon = 0;
	
	private NumberFormat form = NumberFormat.getInstance();
	
	/**
	 * Constructor 
	 * 
	 * @param t Title
	 * @param p Price
	 * @param b Global Trade Item Number (Barcode number)
	 * @param la Geo Latitude
	 * @param lo Geo Longitude
	 */
	public ProductObject(long i, String t, float p, String b, double la, double lo) {
		// TODO Auto-generated constructor stub
		this.id = i;
		this.title = t;
		this.price = p;
		this.barcode = b;
		this.lat = la;
		this.lon = lo;	
	}
	public String getTitle(){
		return this.title;
	}
	
	public String getBarcode(){
		return this.barcode;
	}
	
	public float getPrice(){
		return this.price;
	}
	
	public double getLatitude(){
		return this.lat;
	}
	
	public double getLongitude(){
		return this.lon;
	}
	
	public String toString(){
	    form.setMinimumFractionDigits(2);
	    form.setMaximumFractionDigits(2);
		return (this.title + "\n- €" + form.format(this.price));
	}
	
	public long getId(){
		return this.id;
	}
	
	public void changeId(long newId){
		this.id = newId;
	}
	
	public void changeTitle(String newT){
		this.title = newT;
	}
	
	public void changePrice(float newP){
		this.price = newP;
	}
	
	/**
	 * Writes this object's fields in an SQLite database.
	 * 
	 * @param c
	 */
	public long writeToDatabase(Context c){
		CrowdSourceDatabase entry = new CrowdSourceDatabase(c);
		entry.open();
		long id = entry.createEntry(this.lat, this.lon, this.barcode, this.price, this.title);
		entry.close();
		return id;
	}
}