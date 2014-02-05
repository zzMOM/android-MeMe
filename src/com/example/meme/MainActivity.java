package com.example.meme;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Bitmap.CompressFormat;
import android.text.TextPaint;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String TAG = MainActivity.class.getSimpleName();
	private static final int REQUEST_CODE = 1;
	ImageView imageView;
	Bitmap mBitmap;
	private Paint mPaint;
	// A preference key
	private static final String KEY_COUNT = "count";
	private SharedPreferences mPrefs;
	private int count;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "onCreate!");
		setContentView(R.layout.activity_main);
		
		mPrefs = getPreferences(MODE_PRIVATE);
		count = mPrefs.getInt(KEY_COUNT, 1);
		
		imageView = (ImageView)findViewById(R.id.imageView0);
		imageView.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				// Do some Intent magic to open the Gallery
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.setType("image/*");
				startActivityForResult(Intent.createChooser(intent, "Select..."), REQUEST_CODE);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		
		if(requestCode == REQUEST_CODE && resultCode == RESULT_OK){
			Uri uri = data.getData();
			Log.d(TAG, uri.toString());
			Toast.makeText(getApplicationContext(), uri.toString(), Toast.LENGTH_LONG).show();
			
			try {
				InputStream stream = getContentResolver().openInputStream(uri);
				
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				
				BitmapFactory.decodeStream(stream, null, options);
				stream.close();
				
				int w = options.outWidth;
				int h = options.outHeight;
				Log.d(TAG, "Bitmap raw size:" + w + " x " + h);
				
				int displayW = getResources().getDisplayMetrics().widthPixels;
				int displayH = getResources().getDisplayMetrics().heightPixels;
				int sample = 1;
				while(w > displayW * sample || h > displayH * sample){
					sample *= 2;
				}
				Log.d(TAG, "Sampling at " + sample);
				
				options.inJustDecodeBounds = false;
				options.inSampleSize = sample;
				
				stream = getContentResolver().openInputStream(uri);
				Bitmap bm = BitmapFactory.decodeStream(stream, null, options);
				stream.close();
				
				if(mBitmap != null){
					mBitmap.recycle();
				}
				
				// Make a mutable bitmap...
				mBitmap = Bitmap.createBitmap(bm.getWidth(), bm.getHeight(), Bitmap.Config.ARGB_8888);
				Canvas c = new Canvas(mBitmap);
				c.drawBitmap(bm, 0, 0, null);
				
				//create stamp bitmap
				//Bitmap stamp = Bitmap.createBitmap(60, 60, Bitmap.Config.ARGB_8888);
				
				mPaint = new Paint();
				mPaint.setColor(0xff000099); // Blue
				mPaint.setAlpha(100);
				mPaint.setStrokeWidth(5); // A thick line
				c.drawCircle(bm.getWidth() - 80, bm.getHeight() - 80, 50, mPaint);
				c.drawCircle(bm.getWidth() - 120, bm.getHeight() - 120, 30, mPaint);
				c.drawCircle(bm.getWidth() - 40, bm.getHeight() - 120, 30, mPaint);
				
				TextPaint tp = new TextPaint();
				tp.setTextSize(20);
				tp.setColor(0xffffff00); // AARRGGBB-yellow
				tp.setAlpha(80);
				// 0xff....... Fully opaque
				// 0x00....... Fully transparent (useless!)
				
				EditText nameText = (EditText) findViewById(R.id.nameEditText);
				String nameStr = nameText.getText().toString() + "\'s";
				c.drawText(nameStr, bm.getWidth() - 100, bm.getHeight() - 100, tp);
				c.drawText("photo copy NO."+ count, bm.getWidth() - 150, bm.getHeight() - 80, tp);
				
				tp.setColor(0xffffffff); // AARRGGBB-white
				tp.setTextSize(25);
				String timestamp = new SimpleDateFormat("MM/dd/yyyy").format(new Date());
				c.drawText("Copy Date: " + timestamp, bm.getWidth() - 300, bm.getHeight() - 20, tp);

				bm.recycle();

				imageView.setImageBitmap(mBitmap);
				
			} catch (Exception e) {
				// TODO: handle exception
				Log.e(TAG, "Decoding Bitmap", e);
			}
		}
		
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	public void saveAndShare(View v) {
		if (mBitmap == null) {
			return;
		}
		
		count = count + 1;
		Editor editor = mPrefs.edit();
		editor.putInt(KEY_COUNT, count);
		editor.commit();
		
		
		File path = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		Log.d(TAG, "saveAndShare path = " + path);
		path.mkdirs();

		// Note, for display purposes
		// SimpleDateFormat.getTimeInstance()
		// getDateTimeInstance() or getDateIntance
		// are more appropriate.
		// For filenames we can use the following specification
		String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

		String filename = "Imagen_" + timestamp + ".jpg";
		// Alternatively ... use System.currentTimeMillis()

		// Creating a new File object in Java does not create a new
		// file on the device. The file object just represents
		// a location or path that may or may not exist
		File file = new File(path, filename);
		FileOutputStream stream;
		try {
			// This can fail if the external storage is mounted via USB
			stream = new FileOutputStream(file);
			mBitmap.compress(CompressFormat.JPEG, 100, stream);
			stream.close();
		} catch (Exception e) {
			Log.e(TAG, "saveAndShare (compressing):", e);
			return; // Do not continue
		}
		
		Uri uri = Uri.fromFile(file);
		
		EditText contentText = (EditText) findViewById(R.id.addText);
		String content = contentText.getText().toString();
		
		// Tell Android that a new public picture exists
		
		Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		intent.setData(uri);
		sendBroadcast(intent);

		// Send the public picture file to my friend... 
		Intent share = new Intent(Intent.ACTION_SEND);
		/*share.setType("image/jpeg");
		share.putExtra(Intent.EXTRA_STREAM, uri);
		startActivity(Intent.createChooser(share, "Share using..."));*/
		share.setType("*/*");
		share.putExtra(Intent.EXTRA_TEXT, content);
		share.putExtra(Intent.EXTRA_STREAM, uri);
		startActivity(Intent.createChooser(share, "Share using..."));
	}

}
