/* 
 ** AskForPriceActivity.java
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This activity is called when the product's title is known but not the price.
 * It asks the user for the price and then passes it to the main activity.
 * 
 * @author Kyriakos Georgiou
 * @author Kyriakos Frangeskos
 * 
 */
public class AskForPriceActivity extends Activity {

	public static final int MAX_PRICE = 1000000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.prompt);

		// The product's barcode number and title is retrieved from the main
		// activity.
		Bundle getB = getIntent().getExtras();
		final String barcode = getB.getString("barcode");
		final String title = getB.getString("title");

		// The product's name is set as a title for this activity
		this.setTitle(title);

		TextView tvTitle = (TextView) findViewById(R.id.textView3);
		EditText etTitle = (EditText) findViewById(R.id.etUserDescription);
		final EditText etPrice = (EditText) findViewById(R.id.etUserPrice);
		tvTitle.setVisibility(View.GONE);
		etTitle.setVisibility(View.GONE);

		etPrice.requestFocus();

		Button bOk = (Button) findViewById(R.id.button1);
		Button bCancel = (Button) findViewById(R.id.button2);

		bOk.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				boolean success = false;

				if (etPrice.getText().toString().length() == 0) {
					// If any of the fields are left empty a toast message
					// appears
					// and the activity still waits for valid input.
					Toast.makeText(getApplicationContext(),
							"Please enter the product's price.",
							Toast.LENGTH_SHORT).show();
				} else {

					String strPrice = etPrice.getText().toString()
							.replace(',', '.');

					try {
						float p = Float.parseFloat(strPrice);

						if (p >= MAX_PRICE) {
							Toast.makeText(
									getApplicationContext(),
									"Extreme Value, only prices below "
											+ MAX_PRICE + " are accepted.",
									Toast.LENGTH_SHORT).show();
							success = true;
						} else {

							Intent i = new Intent();
							Bundle b = new Bundle();

							// Data to be passed to the main activity is put in
							// the bundle.
							b.putString("title", title);
							b.putFloat("price", p);
							b.putString("barcode", barcode);
							i.putExtras(b);
							setResult(RESULT_OK, i);
							success = true;
							finish();
						}
					} catch (NumberFormatException nfe) {
						// Indication that an error occurred.
						success = false;
					} finally {
						if (!success) {
							// If Float.parseFloat didn't work normally because
							// of invalid input,
							// a toast message appears and the activity still
							// waits for valid
							// input.
							Toast.makeText(getApplicationContext(),
									"Invalid price format", Toast.LENGTH_SHORT)
									.show();
						}
					}

				}
			}
		});

		bCancel.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				setResult(RESULT_CANCELED);
				finish();
			}
		});
	}

}
