/* 
 ** GoogleShoppingAPIHelperClass.java
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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 
 * @author Kyriakos Georgiou
 * @author Kyriakos Frangeskos
 * 
 * This class implements 2 methods that retrieve a information from
 * the Google Shopping API via an HTTP request and by parsing a 
 * JSON Object.
 *
 */
public class GoogleShoppingAPIHelperClass {

	private String SHOPPING_API_KEY = "AIzaSyBu2-udGIeSS0N088dYYpaFUZf0T5S88N4";
	
	/**
	 * Sends an HTTP request to Google API for Shopping and retrieves a JSON
	 * String of a product, based on the barcode number given in the search
	 * criteria.
	 * 
	 * @param barcodeContents
	 *            The product's barcode which we use to find that product.
	 * @return The JSON String that holds information about the product.
	 */
	private String getJsonStringFromGoogleShopping(String barcodeContents) {
		URL u;
		InputStream is = null;
		DataInputStream dis = null;
		String s;
		StringBuilder sb = new StringBuilder();
		String jsonString = null;
		try {
			u = new URL(
					"https://www.googleapis.com/shopping/search/v1/public/products?key="
							+ SHOPPING_API_KEY + "&country=US&restrictBy=gtin:"
							+ barcodeContents + "&startIndex=1&maxResults=1");
			is = u.openStream();
			dis = new DataInputStream(new BufferedInputStream(is));
			while ((s = dis.readLine()) != null) {
				sb = sb.append(s + "\n");
			}
		} catch (MalformedURLException mue) {
			mue.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}

		jsonString = sb.toString();
		return jsonString;
	}

	/**
	 * This method receives a JSON String and gets any information we want from
	 * it (e.g. Title, price, description, image of the product etc). Currently
	 * we are only getting the title of the product.
	 * 
	 * @param barcodeContents
	 *            The unique Global Trade Item Number of the product.
	 * @return The title of the product we are searching for.
	 */
	public String getGoogleShoppingInfo(String barcodeContents) {

		String jsonString = getJsonStringFromGoogleShopping(barcodeContents);
		String productTitle = null;

		if (jsonString != null) {
			try {
				JSONObject jsonObject = new JSONObject(jsonString);

				JSONArray itemsArray = jsonObject.getJSONArray("items");
				JSONObject itemObject = itemsArray.getJSONObject(0);
				JSONObject productObject = itemObject.getJSONObject("product");
				productTitle = productObject.getString("title");
				return productTitle;

			} catch (JSONException je) {
				je.printStackTrace();
			}
		}
		return null;
	}
	
}
