/* 
 ** CaptureActivity.java
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

package com.google.zxing.client.android;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.SQLException;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.client.android.camera.CameraManager;

import dmsl.smartbill.CrowdSourceDatabase;
import dmsl.smartbill.GoogleShoppingAPIHelperClass;
import dmsl.smartbill.ProductObject;
import dmsl.smartbill.R;
import dmsl.smartbill.TransparentPanel;

/**
 * This activity opens the camera and does the actual scanning on a background
 * thread. It draws a viewfinder to help the user place the barcode correctly
 * and shows feedback as the image processing is happening.
 * 
 * When a product is found, its price and title are shown on the screen. Also a
 * google map is shown with the user's current location and it can be expanded
 * to full screen in order to show points on the map where the specific product
 * was scanned before.
 * 
 * (ZXing Authors)
 * 
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 * 
 *         (SmartBill Authors)
 * @author Kyriakos Georgiou
 * @author Kyriakos Frangeskos
 */
public final class CaptureActivity extends MapActivity implements
		SurfaceHolder.Callback, LocationListener, OnItemLongClickListener {

	private ArrayList<String> listTitleAndPrice = new ArrayList<String>();
	private ArrayList<String> listBarcodes = new ArrayList<String>();
	private ArrayList<Integer> listItemQuantities = new ArrayList<Integer>();
	private ArrayList<ProductObject> listProdObj = new ArrayList<ProductObject>();

	private float bill = 0;
	private int basketCount = 0;
	private String currentBarcode = null;

	// UI views
	private ListView lvItems;
	private TextView tvBasket;
	private TextView tvBill;
	private TextView tvTitle;
	private TextView tvPrice;
	private ImageView ivCart;

	// GeoLocation components
	private double geoLongitude;
	private double geoLatitude;
	private String provider;
	private LocationManager locationManager;

	private static final String TAG = CaptureActivity.class.getSimpleName();

	// Menu components
	private static final int MANUALENTRY_ID = Menu.FIRST;
	private static final int HELP_ID = Menu.FIRST + 1;
	private static final int ABOUT_ID = Menu.FIRST + 2;
	private static final int EXIT_ID = Menu.FIRST + 3;

	private CameraManager cameraManager;
	private CaptureActivityHandler handler;
	private Result savedResultToShow;
	private ViewfinderView viewfinderView;
	private boolean hasSurface;
	private Collection<BarcodeFormat> decodeFormats;
	private String characterSet;
	private InactivityTimer inactivityTimer;
	private BeepManager beepManager;

	// onActivityResult codes
	private static final int USER_INPUT_FOR_RESULT = 0;
	private static final int USER_INPUT_PRICE = 1;
	private static final int USER_EDIT = 2;
	private static final int USER_MANUAL_ENTRY = 3;

	// Pop-up panel components
	private int key = 0;
	private TextView tvPopUpSum;
	private TransparentPanel popup;
	private Button popupBtn;

	// Google maps components
	private MapView mapView;
	private MapController mc;

	/**
	 * This method receives a number and formats it in order to have "n" digits
	 * after the decimal point notation.
	 * 
	 * @param number
	 *            The real number to be formatted.
	 * @param n
	 *            The number of decimal points the new real number will have.
	 * @return The new formatted number in a string form.
	 */
	public String formatToNDigits(double number, int n) {
		NumberFormat form = NumberFormat.getInstance();
		form.setMinimumFractionDigits(n);
		form.setMaximumFractionDigits(n);

		return form.format(number);
	}

	/**
	 * Checks if the Wi-Fi is on and if an internet connection is available.
	 * 
	 * @return True if a connection is available, False otherwise.
	 */
	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null
				&& activeNetworkInfo.isConnectedOrConnecting();
	}

	ViewfinderView getViewfinderView() {
		return viewfinderView;
	}

	public Handler getHandler() {
		return handler;
	}

	CameraManager getCameraManager() {
		return cameraManager;
	}

	/**
	 * Gets the current location of the device running the application in the
	 * form of coordinates (Latitude, Longitude).
	 */
	public void getGeoLocation() {
		// Get the location manager
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		// Define the criteria how to select the location provider -> use
		// default
		Criteria criteria = new Criteria();
		provider = locationManager.getBestProvider(criteria, false);
		Location location = locationManager.getLastKnownLocation(provider);

		if (location != null) {
			geoLatitude = location.getLatitude();
			geoLongitude = location.getLongitude();
		} else {
			// Somewhere in the Atlantic Ocean.
			geoLatitude = 0;
			geoLongitude = 0;
		}
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.capture);
		hasSurface = false;
		inactivityTimer = new InactivityTimer(this);
		beepManager = new BeepManager(this);

		if (!isNetworkAvailable()) {
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setTitle("Wi-Fi is turned off/No internet connection available");
			dialog.setMessage("SmartBill needs an internet connection for all its functions to work properly.");
			dialog.setNegativeButton("OK", null);
			dialog.show();
		}

		// Getting the coordinates of the device's location.
		getGeoLocation();

		mapView = (MapView) findViewById(R.id.mapview);
		mc = mapView.getController();

		// Making the Google map to show the device's location.
		GeoPoint p = new GeoPoint((int) (geoLatitude * 1E6),
				(int) (geoLongitude * 1E6));

		mc.animateTo(p);
		mc.setZoom(15);
		mapView.invalidate();

		// Initialization of the pop up panel that will contain the user's
		// shopping list.
		popup = (TransparentPanel) findViewById(R.id.popup_window);
		popup.setVisibility(View.GONE);

		popupBtn = (Button) findViewById(R.id.show_popup_button);
		popupBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				if (key == 0) {
					if (listTitleAndPrice.isEmpty()) {
						Toast.makeText(getApplicationContext(),
								"Your list is empty.", Toast.LENGTH_SHORT)
								.show();
					} else {

						tvPopUpSum = (TextView) findViewById(R.id.tvPopUpSum);
						tvPopUpSum.setText("Total: €"
								+ formatToNDigits(bill, 2));
						key = 1;
						popup.setVisibility(View.VISIBLE);
						popupBtn.setBackgroundResource(R.drawable.list_popdown);
					}
				} else if (key == 1) {
					key = 0;
					popup.setVisibility(View.GONE);
					popupBtn.setBackgroundResource(R.drawable.list_popup);
				}
			}
		});

		ImageButton bUcyLogo = (ImageButton) findViewById(R.id.ibUcyLogo);
		ImageButton bDmslLogo = (ImageButton) findViewById(R.id.ibDmslLogo);

		Button bViewMap = (Button) findViewById(R.id.bViewMap);

		// The button for the full screen map.
		bViewMap.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if (currentBarcode != null) {
					Intent i = new Intent(
							"dmsl.smartbill.GOOGLEMAPSFULLSCREENACTIVITY");
					Bundle b = new Bundle();
					b.putString("barcode", currentBarcode);
					// Coordinates are sent to the full screen map activity in
					// order
					// to have the map showing the current geoLocation.
					b.putDouble("lat", geoLatitude);
					b.putDouble("lon", geoLongitude);
					i.putExtras(b);
					startActivity(i);
				} else {
					Toast.makeText(CaptureActivity.this,
							"You have to scan a barcode first",
							Toast.LENGTH_SHORT).show();
				}

			}
		});

		// University of Cyprus logo with link to the website.
		bUcyLogo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://www.cs.ucy.ac.cy/index.php?lang=el"));
				startActivity(browserIntent);
			}
		});

		// DMSL logo with link to the website.
		bDmslLogo.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri
						.parse("http://dmsl.cs.ucy.ac.cy/"));
				startActivity(browserIntent);
			}
		});

		initializeViews();
	}

	@Override
	protected void onResume() {
		super.onResume();

		cameraManager = new CameraManager(getApplication());

		viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
		viewfinderView.setCameraManager(cameraManager);

		handler = null;

		resetStatusView();

		SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
		SurfaceHolder surfaceHolder = surfaceView.getHolder();
		if (hasSurface) {
			// The activity was paused but not stopped, so the surface still
			// exists. Therefore
			// surfaceCreated() won't be called, so init the camera here.
			initCamera(surfaceHolder);
		} else {
			// Install the callback and wait for surfaceCreated() to init the
			// camera.
			surfaceHolder.addCallback(this);
			surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		locationManager.requestLocationUpdates(provider, 400, 50, this);

		beepManager.updatePrefs();

		inactivityTimer.onResume();

		GeoPoint p = new GeoPoint((int) (geoLatitude * 1E6),
				(int) (geoLongitude * 1E6));

		mc.animateTo(p);
		mc.setZoom(15);
		mapView.invalidate();
	}

	@Override
	protected void onPause() {
		if (handler != null) {
			handler.quitSynchronously();
			handler = null;
		}
		inactivityTimer.onPause();
		cameraManager.closeDriver();
		if (!hasSurface) {
			SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
			SurfaceHolder surfaceHolder = surfaceView.getHolder();
			surfaceHolder.removeCallback(this);
		}
		super.onPause();
		locationManager.removeUpdates(this);
	}

	@Override
	protected void onDestroy() {
		inactivityTimer.shutdown();
		super.onDestroy();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			setResult(RESULT_CANCELED);
			AlertDialog.Builder aboutBuilder = new AlertDialog.Builder(this);
			aboutBuilder.setTitle("Closing Application");
			aboutBuilder.setMessage("Are you sure you want to exit SmartBill?");
			aboutBuilder.setPositiveButton("Yes",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							viewfinderView.setVisibility(View.VISIBLE);
							finish();
						}
					});
			aboutBuilder.setNegativeButton("Cancel", null);
			aboutBuilder.show();
			return true;
		case KeyEvent.KEYCODE_FOCUS:
		case KeyEvent.KEYCODE_CAMERA:
		case KeyEvent.KEYCODE_SEARCH:
			// Handle these events so they don't launch the Camera app
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(Menu.NONE, MANUALENTRY_ID, Menu.NONE, "Manual Entry").setIcon(
				android.R.drawable.ic_input_add);
		menu.add(Menu.NONE, HELP_ID, Menu.NONE, R.string.menu_help).setIcon(
				android.R.drawable.ic_menu_help);
		menu.add(Menu.NONE, ABOUT_ID, Menu.NONE, R.string.menu_about).setIcon(
				android.R.drawable.ic_menu_info_details);
		menu.add(Menu.NONE, EXIT_ID, Menu.NONE, "Exit").setIcon(
				android.R.drawable.ic_menu_close_clear_cancel);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
		switch (item.getItemId()) {
		case HELP_ID:
			viewfinderView.setVisibility(View.INVISIBLE);
			AlertDialog.Builder helpBuilder = new AlertDialog.Builder(this);
			helpBuilder.setTitle("Help");
			helpBuilder.setMessage(R.string.strHelpMenu);
			helpBuilder.setIcon(android.R.drawable.ic_menu_help);
			helpBuilder.setPositiveButton("Back",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							viewfinderView.setVisibility(View.VISIBLE);
							dialog.dismiss();
						}
					});
			helpBuilder.show();
			break;
		case ABOUT_ID:
			viewfinderView.setVisibility(View.INVISIBLE);
			AlertDialog.Builder aboutBuilder = new AlertDialog.Builder(this);
			aboutBuilder.setTitle("About");
			aboutBuilder.setMessage(R.string.strAbutMenu);
			aboutBuilder.setIcon(android.R.drawable.ic_menu_info_details);
			aboutBuilder.setPositiveButton("Back",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							viewfinderView.setVisibility(View.VISIBLE);
							dialog.dismiss();
						}
					});
			aboutBuilder.show();
			break;
		case MANUALENTRY_ID:
			Intent i = new Intent("dmsl.smartbill.ASKFORMANUALENTRYACTIVITY");
			startActivityForResult(i, USER_MANUAL_ENTRY);
			break;
		case EXIT_ID:
			AlertDialog.Builder exitBuilder = new AlertDialog.Builder(this);
			exitBuilder.setTitle("Closing Application");
			exitBuilder.setMessage("Are you sure you want to exit SmartBill?");
			exitBuilder.setPositiveButton("Yes",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							// TODO Auto-generated method stub
							viewfinderView.setVisibility(View.VISIBLE);
							finish();
						}
					});
			exitBuilder.setNegativeButton("Cancel", null);
			exitBuilder.show();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	public void openMap() {
		Intent i = new Intent("dmsl.smartbill.GOOGLEMAPSFULLSCREENACTIVITY");
		Bundle b = new Bundle();
		b.putString("barcode", currentBarcode);
		b.putInt("lat", (int) (geoLatitude * 1e6));
		b.putInt("lon", (int) (geoLongitude * 1e6));
		i.putExtras(b);
		startActivity(i);
	}

	private void decodeOrStoreSavedBitmap(Bitmap bitmap, Result result) {
		// Bitmap isn't used yet -- will be used soon
		if (handler == null) {
			savedResultToShow = result;
		} else {
			if (result != null) {
				savedResultToShow = result;
			}
			if (savedResultToShow != null) {
				Message message = Message.obtain(handler,
						R.id.decode_succeeded, savedResultToShow);
				handler.sendMessage(message);
			}
			savedResultToShow = null;
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (holder == null) {
			Log.e(TAG,
					"*** WARNING *** surfaceCreated() gave us a null surface!");
		}
		if (!hasSurface) {
			hasSurface = true;
			initCamera(holder);
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		hasSurface = false;
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {

	}

	/**
	 * A valid barcode has been found, so the data is displayed on the screen
	 * and the preview is restarted so that the user can scan a new barcode.
	 * 
	 * @param rawResult
	 *            The contents of the barcode.
	 * @param barcode
	 *            A greyscale bitmap of the camera data which was decoded.
	 *            (Currently not used, might be used in future extensions.)
	 */
	public void handleDecode(Result rawResult, Bitmap barcode) {

		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				R.layout.mylist, listTitleAndPrice);

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);

		final String barcodeText = rawResult.getText();

		currentBarcode = barcodeText;

		String prodTitle = null;
		ProductObject testProduct = null;

		inactivityTimer.onActivity();
		beepManager.playBeepSoundAndVibrate();

		restartPreviewAfterDelay(2500L);

		basketCount++;

		// Trying to find the product in our database.
		testProduct = searchCrowdDatabase(barcodeText);

		if (testProduct == null && isNetworkAvailable()) {
			// If the barcode was not matched in our database we search for
			// a title in Google for Shopping API.
			GoogleShoppingAPIHelperClass googleShopHelper = new GoogleShoppingAPIHelperClass();
			prodTitle = googleShopHelper.getGoogleShoppingInfo(barcodeText);
			googleShopHelper = null; // Garbage Collector will free this memory
										// on its next run.
		}

		if (testProduct != null
				&& !(geoLocationMatch(testProduct.getLatitude(),
						testProduct.getLongitude()))) {
			// Product was scanned before but not from the same location
			// (store).
			// So we assume the price is probably not the same, therefore we ask
			// for
			// the user to add the product's price.
			Intent i = new Intent("dmsl.smartbill.ASKFORPRICEACTIVITY");
			Bundle b = new Bundle();
			b.putString("barcode", barcodeText);
			b.putString("title", testProduct.getTitle());
			i.putExtras(b);
			startActivityForResult(i, USER_INPUT_PRICE);
		} else if (testProduct == null) {
			// Product has not been scanne before by any user.
			if (prodTitle != null) {
				// The barcode was matched with a product's title from google
				// shopping API so we keep that title and ask only for the price
				// from the user.
				Intent i = new Intent("dmsl.smartbill.ASKFORPRICEACTIVITY");
				Bundle b = new Bundle();
				b.putString("barcode", barcodeText);
				b.putString("title", prodTitle);
				i.putExtras(b);
				startActivityForResult(i, USER_INPUT_PRICE);
			} else {
				// The product was not found, the title also. We ask for both
				// title and price from the user.
				Intent i = new Intent("dmsl.smartbill.ASKFORINPUTACTIVITY");
				Bundle b = new Bundle();
				b.putString("barcode", barcodeText);
				i.putExtras(b);
				startActivityForResult(i, USER_INPUT_FOR_RESULT);
			}
		} else {
			// The product was scanned before from the same exact location
			// (store probably) so we assume the price is still the same
			// and present that information to the user.
			productFoundActions(testProduct);
		}

		// Empty cart icon is shown when the basket count is 0, a full cart icon
		// is shown otherwise.
		if (basketCount > 0) {
			ivCart.setImageResource(R.drawable.shopping_cart_full);
		}

		lvItems.setOnItemLongClickListener(new OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					final int position, long id) {

				builder.setTitle(listTitleAndPrice.get(position));
				builder.setMessage("What would you like to do with this item?");
				builder.setIcon(R.drawable.map_point);
				builder.setNeutralButton("Delete",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(
									DialogInterface dialogInterface, int i) {

								if (listItemQuantities.get(position) == 1) {
									listTitleAndPrice.remove(position);
									listItemQuantities.remove(position);
									listBarcodes.remove(position);
									bill -= listProdObj.get(position)
											.getPrice();
									listProdObj.remove(position);
								} else {
									listItemQuantities
											.set(position, listItemQuantities
													.get(position) - 1);
									bill -= listProdObj.get(position)
											.getPrice();
									if (listItemQuantities.get(position) == 1) {
										listTitleAndPrice.set(position,
												listProdObj.get(position)
														.toString());
									} else {
										listTitleAndPrice
												.set(position,
														listProdObj.get(
																position)
																.toString()
																+ " [x"
																+ listItemQuantities
																		.get(position)
																+ " = €"
																+ formatToNDigits(
																		listItemQuantities
																				.get(position)
																				* listProdObj
																						.get(position)
																						.getPrice(),
																		2)
																+ "]");
									}

								}

								basketCount--;

								lvItems.setAdapter(adapter);

								tvBasket.setText(basketCount + "");

								tvBill.setText("€" + formatToNDigits(bill, 2));
								tvPopUpSum.setText("Total: €"
										+ formatToNDigits(bill, 2));

								if (basketCount == 0) {
									ivCart.setImageResource(R.drawable.shopping_cart_empty);
									tvBill.setText("€00.00");
								}

								if (listTitleAndPrice.isEmpty()) {
									key = 0;
									popup.setVisibility(View.GONE);
									popupBtn.setBackgroundResource(R.drawable.list_popup);
								}
								dialogInterface.dismiss();
							}
						});
				builder.setNegativeButton("Nothing",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(
									DialogInterface dialogInterface, int i) {
								dialogInterface.cancel();
							}
						});

				builder.setPositiveButton("Edit",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								Intent i = new Intent(
										"dmsl.smartbill.ASKFOREDITINPUTACTIVITY");
								Bundle b = new Bundle();
								b.putString("barcode", listProdObj
										.get(position).getBarcode());
								b.putString("oldTitle",
										listProdObj.get(position).getTitle());
								b.putFloat("oldPrice", listProdObj
										.get(position).getPrice());
								b.putLong("pos", listProdObj.get(position)
										.getId());
								b.putInt("listPosition", position);
								i.putExtras(b);

								startActivityForResult(i, USER_EDIT);
							}

						});

				builder.show();
				return true;
			}
		});

		lvItems.setAdapter(adapter);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (requestCode == USER_INPUT_FOR_RESULT) {
				Bundle extras = data.getExtras();
				String title = extras.getString("title");
				float price = extras.getFloat("price");
				String barcode = extras.getString("barcode");

				tvTitle.setText(title);
				tvPrice.setText(formatToNDigits(price, 2) + "");

				ProductObject product = new ProductObject(-1, title, price,
						barcode, geoLatitude, geoLongitude);

				productFoundActions(product);
				long temp = product.writeToDatabase(this);
				product.changeId(temp);

			} else if (requestCode == USER_INPUT_PRICE) {
				Bundle extras = data.getExtras();
				String title = extras.getString("title");
				float price = extras.getFloat("price");
				String barcode = extras.getString("barcode");

				tvTitle.setText(title);
				tvPrice.setText(formatToNDigits(price, 2) + "");

				ProductObject product = new ProductObject(-1, title, price,
						barcode, geoLatitude, geoLongitude);

				productFoundActions(product);
				long temp = product.writeToDatabase(this);
				product.changeId(temp);
			} else if (requestCode == USER_EDIT) {

				Bundle extras = data.getExtras();
				String newTitle = extras.getString("newTitle");
				float newPrice = extras.getFloat("newPrice");
				long position = extras.getLong("pos");
				int listPosition = extras.getInt("listPos");

				CrowdSourceDatabase myCS_DbHelper = new CrowdSourceDatabase(
						CaptureActivity.this);

				try {
					myCS_DbHelper.open();
					myCS_DbHelper.editRecord(position, newTitle, newPrice);
					myCS_DbHelper.close();

					float oldPrice = listProdObj.get(listPosition).getPrice();

					listProdObj.get(listPosition).changeTitle(newTitle);
					listProdObj.get(listPosition).changePrice(newPrice);

					if (listItemQuantities.get(listPosition) != 1) {
						listTitleAndPrice.set(
								listPosition,
								listProdObj.get(listPosition).toString()
										+ " [x"
										+ listItemQuantities.get(listPosition)
										+ " = €"
										+ formatToNDigits(
												listItemQuantities
														.get(listPosition)
														* newPrice, 2) + "]");
					} else {
						listTitleAndPrice.set(listPosition,
								listProdObj.get(listPosition).toString());
					}

					bill = bill - oldPrice
							* listItemQuantities.get(listPosition);
					bill = bill + newPrice
							* listItemQuantities.get(listPosition);

					tvBill.setText(formatToNDigits(bill, 2));

					tvPopUpSum.setText(formatToNDigits(bill, 2));

					tvTitle.setText(newTitle);
					tvPrice.setText(formatToNDigits(newPrice, 2));

					ArrayAdapter<String> adapter = new ArrayAdapter<String>(
							this, R.layout.mylist, listTitleAndPrice);

					lvItems.setAdapter(adapter);

				} catch (SQLException sqle) {
					throw sqle;
				}
			}

			else if (requestCode == USER_MANUAL_ENTRY) {
				Bundle extras = data.getExtras();
				String manualBarcode = extras.getString("strBarcode");
				handleDecode(new Result(manualBarcode, null, null, null), null);
			}

		} else if (resultCode == RESULT_CANCELED
				&& !(requestCode == USER_EDIT || requestCode == USER_MANUAL_ENTRY)) {
			basketCount--;
			currentBarcode = null;
			if (basketCount == 0) {
				ivCart.setImageResource(R.drawable.shopping_cart_empty);
			}
		}
	}

	/**
	 * Initialization of the Views in capture.xml.
	 */
	private void initializeViews() {
		tvTitle = (TextView) findViewById(R.id.tvTitle);
		tvPrice = (TextView) findViewById(R.id.tvPrice);
		tvBasket = (TextView) findViewById(R.id.tvBasketCount);
		tvBill = (TextView) findViewById(R.id.tvBill);
		lvItems = (ListView) findViewById(R.id.lvItems);
		ivCart = (ImageView) findViewById(R.id.ivCart);
	}

	/**
	 * This function is called when a product was successfully found in the
	 * database or was just entered by the user. In this function the bill's
	 * state is calculated, puts up some indications on the screen and adds the
	 * product in the shopping list.
	 * 
	 * @param testProduct
	 */
	private void productFoundActions(ProductObject testProduct) {

		float currentPrice = testProduct.getPrice();
		int pos;

		bill += currentPrice;

		tvTitle.setText(testProduct.getTitle());
		tvTitle.setTextColor(Color.GREEN);

		tvPrice.setText("€" + formatToNDigits(currentPrice, 2));
		tvPrice.setTextColor(Color.GREEN);

		tvBasket.setText(basketCount + "");

		tvBill.setText("€" + formatToNDigits(bill, 2));

		pos = listBarcodes.indexOf(testProduct.getBarcode());

		if (pos == -1) {
			// If the product was not scanned before it must be add as a new
			// item in the list.
			listBarcodes.add(testProduct.getBarcode());
			listProdObj.add(testProduct);
			listTitleAndPrice.add(testProduct.toString());
			listItemQuantities.add(1);
		} else {
			// The product was scanned before so we take action with a stacking
			// mechanism for
			// the shopping list.
			listItemQuantities.set(pos, listItemQuantities.get(pos) + 1);
			listTitleAndPrice
					.set(pos,
							testProduct.toString()
									+ " [x"
									+ listItemQuantities.get(pos)
									+ " = €"
									+ formatToNDigits(
											(listItemQuantities.get(pos) * currentPrice),
											2) + "]");
		}

	}

	private void initCamera(SurfaceHolder surfaceHolder) {
		if (surfaceHolder == null) {
			throw new IllegalStateException("No SurfaceHolder provided");
		}
		if (cameraManager.isOpen()) {
			Log.w(TAG,
					"initCamera() while already open -- late SurfaceView callback?");
			return;
		}
		try {
			cameraManager.openDriver(surfaceHolder);
			// Creating the handler starts the preview, which can also throw a
			// RuntimeException.
			if (handler == null) {
				handler = new CaptureActivityHandler(this, decodeFormats,
						characterSet, cameraManager);
			}
			decodeOrStoreSavedBitmap(null, null);
		} catch (IOException ioe) {
			Log.w(TAG, ioe);
			displayFrameworkBugMessageAndExit();
		} catch (RuntimeException e) {
			// Barcode Scanner has seen crashes in the wild of this variety:
			// java.?lang.?RuntimeException: Fail to connect to camera service
			Log.w(TAG, "Unexpected error initializing camera", e);
			displayFrameworkBugMessageAndExit();
		}
	}

	private void displayFrameworkBugMessageAndExit() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(getString(R.string.app_name));
		builder.setMessage(getString(R.string.msg_camera_framework_bug));
		builder.setPositiveButton("OK", new FinishListener(this));
		builder.setOnCancelListener(new FinishListener(this));
		builder.show();
	}

	/**
	 * Restarts the scanner
	 * 
	 * @param delayMS
	 *            The time in which it will restart (in milliseconds).
	 */
	public void restartPreviewAfterDelay(long delayMS) {
		if (handler != null) {
			handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
		}
		resetStatusView();
	}

	private void resetStatusView() {
		viewfinderView.setVisibility(View.VISIBLE);
	}

	public void drawViewfinder() {
		viewfinderView.drawViewfinder();
	}

	/**
	 * This method is called when the location of the device changes and it
	 * updates the application's stored coordinates.
	 */
	@Override
	public void onLocationChanged(Location location) {
		geoLatitude = location.getLatitude();
		geoLongitude = location.getLongitude();
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {

	}

	@Override
	public void onProviderDisabled(String provider) {

	}

	/**
	 * Performs a query on our SQLite database trying to find a product using
	 * its barcode number.
	 * 
	 * @param barcode
	 *            The products Global Trade Item Number.
	 * @return The product in object form. Null if no such product was found.
	 */
	public ProductObject searchCrowdDatabase(String barcode) {

		CrowdSourceDatabase myCS_DbHelper = new CrowdSourceDatabase(this);
		ProductObject prod = null;

		try {
			myCS_DbHelper.open();
			prod = myCS_DbHelper.getData(barcode, geoLatitude, geoLongitude);
			myCS_DbHelper.close();
		} catch (SQLException sqle) {
			throw sqle;
		}

		return prod;
	}

	/**
	 * Checks if 2 sets of coordinates point to the same position on earth.
	 * 
	 * @param lati
	 *            Latitude to be checked.
	 * @param longi
	 *            Longitude to be checked.
	 * @return True if the 2 sets of coordinates point to the same place, false
	 *         otherwise.
	 */
	public boolean geoLocationMatch(double lati, double longi) {
		int precision = 1000; // Covers an area of 100m^2 approximately.
		return (compareOnApproximation(lati, geoLatitude, precision) && compareOnApproximation(
				longi, geoLongitude, precision));
	}

	/**
	 * Compares 2 doubles with a given precision approach.
	 * 
	 * @param x
	 *            The 1st double to be compared.
	 * @param y
	 *            The 2nd double to be compared.
	 * @param precision
	 *            The number of decimals that the double will keep.
	 * @return True if the 2 numbers are equal with the precision given, false
	 *         otherwise.
	 */
	public boolean compareOnApproximation(double x, double y, int precision) {
		int e = 2; // e is the maximum difference the 2 numbers' "precision"th
					// digits can have.
					// 3.121 and 3.124 would be considered as equal. In terms of
					// coordinates that's
					// about 200m distance on earth.
		return (Math.abs((((int) (x * precision)) - ((int) (y * precision)))) <= e);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean isRouteDisplayed() {
		// TODO Auto-generated method stub
		return false;
	}

}