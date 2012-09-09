/* 
 ** AskForEditInputActivity.java
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

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * 
 * @author Kyriakos Georgiou 
 * @author Kyriakos Frangeskos
 * 
 * This activity is called when the user wishes to change a product's
 * name or price. It asks the user for the new title and/or price.
 * 
 */
public class AskForEditInputActivity extends Activity {

	public static final int MAX_TITLE_LENGTH = 60;
	public static final int MAX_PRICE = 1000000;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.prompt);

		final EditText etPrice = (EditText) findViewById(R.id.etUserPrice);
		final EditText etDescr = (EditText) findViewById(R.id.etUserDescription);

		Bundle getB = getIntent().getExtras();
		String barcode = getB.getString("barcode");
		String oldTitle = getB.getString("oldTitle");
		float oldPrice = getB.getFloat("oldPrice");
		final long pos = getB.getLong("pos");
		final int listPos = getB.getInt("listPosition");
		
		NumberFormat form = NumberFormat.getInstance();
		form.setMinimumFractionDigits(2);
		form.setMaximumFractionDigits(2);
		
		etDescr.setText(oldTitle);
		etPrice.setText(form.format(oldPrice));
		
		this.setTitle(barcode);
		
		etDescr.requestFocus();
		
		Button bOk = (Button) findViewById(R.id.button1);
		Button bCancel = (Button) findViewById(R.id.button2);

		bOk.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				boolean success = false;

				if (etPrice.getText().toString().length() == 0
						|| etDescr.getText().toString().length() == 0) {
					//If any of the fields are left empty a toast message appears
					//and the activity still waits for valid input.
					Toast.makeText(getApplicationContext(),
							"One or both of the fields are empty.",
							Toast.LENGTH_SHORT).show();
				} else {
					//Some users may use ',' instead of '.' when it comes to decimal
					//point notation so we cover this fact so Float.parseFloat works
					//properly.
					String strPrice = etPrice.getText().toString()
							.replace(',', '.');

					try {
						float p = Float.parseFloat(strPrice);
						String t = etDescr.getText().toString();
						
						if( p >= MAX_PRICE){
							Toast.makeText(getApplicationContext(),
									"Extreme Value, only prices below " + MAX_PRICE + " are accepted.", Toast.LENGTH_SHORT)
									.show();
							success = true;
						}
						else if(t.length()>MAX_TITLE_LENGTH){
							Toast.makeText(getApplicationContext(),
									"Product's Title too long. Please remove " + (t.length() - MAX_TITLE_LENGTH) + " characters.", Toast.LENGTH_SHORT)
									.show();
							success = true;
						}
						else{
							Intent i = new Intent();
							Bundle b = new Bundle();
							
							//Data to be passed to the main activity is put in the bundle.
							b.putFloat("newPrice", p);
							b.putString("newTitle", t);
							b.putLong("pos", pos);
							b.putInt("listPos", listPos);
							
							i.putExtras(b);
							setResult(RESULT_OK, i);
							success = true;
							finish();
						}
						
					} catch (NumberFormatException nfe) {
						//Indication that an error occurred.
						success = false;
					} finally {
						if (!success) {
							//If Float.parseFloat didn't work normally because of invalid input,
							//a toast message appears and the activity still waits for valid
							//input.
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
