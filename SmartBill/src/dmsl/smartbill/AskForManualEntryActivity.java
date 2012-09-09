/* 
 ** AskForManualEntryActivity.java
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
 * This activity is called when the user wishes to manual input
 * a barcode. 
 * 
 * @author Kyriakos Georgiou
 * @author Kyriakos Frangeskos
 * 
 */
public class AskForManualEntryActivity extends Activity {

	public static final int MAX_BARCODE_LENGTH = 30;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.prompt);

		this.setTitle("Manual Entry");

		final EditText etTitle = (EditText) findViewById(R.id.etUserDescription);
		EditText etPrice = (EditText) findViewById(R.id.etUserPrice);
		TextView tvTitle = (TextView) findViewById(R.id.textView3);
		TextView tvPrice = (TextView) findViewById(R.id.textView2);
		tvTitle.setText("Barcode:");
		etPrice.setVisibility(View.GONE);
		tvPrice.setVisibility(View.GONE);

		etPrice.requestFocus();

		Button bOk = (Button) findViewById(R.id.button1);
		Button bCancel = (Button) findViewById(R.id.button2);

		bOk.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				String strBarcode = etTitle.getText().toString();
				
				if(strBarcode == null || strBarcode.length() == 0){
					Toast.makeText(getApplicationContext(),
							"Please enter a barcode.", Toast.LENGTH_SHORT)
							.show();
				} else if(strBarcode.length()>MAX_BARCODE_LENGTH){
					Toast.makeText(getApplicationContext(),
							"The barcode is too long. Maximum length is " + MAX_BARCODE_LENGTH + ".", Toast.LENGTH_SHORT)
							.show();
				}
				else {
					Intent i = new Intent();
					Bundle b = new Bundle();
					// Data to be passed to the main activity is put in the
					// bundle.
					b.putString("strBarcode", strBarcode);
					i.putExtras(b);
					setResult(RESULT_OK, i);
					finish();
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
