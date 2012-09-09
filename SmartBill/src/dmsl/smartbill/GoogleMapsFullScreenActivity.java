/* 
 ** GoogleMapsFullScreenActivity.java
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
import java.util.ArrayList;
import java.util.List;

import android.database.SQLException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

/**
 * This activity displays a full screen map that has points
 * which represent the locations that a specific product was scanned
 * before and its local price.
 * 
 * @author Kyriakos Georgiou
 * @author Kyriakos Frangeskos
 *
 */
public class GoogleMapsFullScreenActivity extends MapActivity {

	private MapView mapView;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);

		Button back = (Button) findViewById(R.id.bBackFromMap);

		back.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finish();
			}
		});

		mapView = (MapView) findViewById(R.id.map_view);
		mapView.setBuiltInZoomControls(true);

		Bundle getBu = getIntent().getExtras();

		// Items received from the previous activity.
		String barcode = getBu.getString("barcode");
		double currentLat = getBu.getDouble("lat");
		double currentLon = getBu.getDouble("lon");

		List<Overlay> mapOverlays = mapView.getOverlays();

		Drawable drawable = this.getResources().getDrawable(
				R.drawable.map_point);

		CustomItemizedOverlay itemizedOverlay = new CustomItemizedOverlay(
				drawable, this);

		ArrayList<ProductObject> prodList = searchCrowdDatabase(barcode);

		NumberFormat form = NumberFormat.getInstance();
		form.setMinimumFractionDigits(2);
		form.setMaximumFractionDigits(2);

		// Iterating through all the items that match the current item's barcode
		// and put there
		// geoLocation points on the map along with their details.
		for (int i = 0; i < prodList.size(); i++) {
			GeoPoint point = new GeoPoint(
					(int) (prodList.get(i).getLatitude() * 1e6),
					(int) (prodList.get(i).getLongitude() * 1e6));
			OverlayItem overlayitem = new OverlayItem(point, prodList.get(i)
					.getTitle(), "€" + form.format(prodList.get(i).getPrice()));
			itemizedOverlay.addOverlay(overlayitem);
		}

		mapOverlays.add(itemizedOverlay);

		MapController mapController = mapView.getController();

		// Making the map to show at the current location of the device. The
		// coordinates were
		// received from the previous activity.
		GeoPoint point1 = new GeoPoint((int) (currentLat * 1e6),
				(int) (currentLon * 1e6));
		mapController.animateTo(point1);
		mapController.setZoom(10);

	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	/**
	 * Performs a query on our SQLite database trying to find a product using
	 * its barcode number.
	 * 
	 * @param barcode
	 *            The products Global Trade Item Number.
	 * @return The product in object form. Null if no such product was found.
	 */
	public ArrayList<ProductObject> searchCrowdDatabase(String barcode) {

		ArrayList<ProductObject> list = new ArrayList<ProductObject>();

		CrowdSourceDatabase myCS_DbHelper = new CrowdSourceDatabase(this);

		try {
			myCS_DbHelper.open();
			list = myCS_DbHelper.getData(barcode);
			myCS_DbHelper.close();
		} catch (SQLException sqle) {
			throw sqle;
		}

		return list;
	}
}