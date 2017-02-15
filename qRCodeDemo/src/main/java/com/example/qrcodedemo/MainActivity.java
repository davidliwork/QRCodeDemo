package com.example.qrcodedemo;


import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.zbar.lib.CaptureActivity;
import com.zbar.lib.RGBLuminanceSource;
import com.zbar.lib.SelectPictureUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private final static int SCANNIN_GREQUEST_CODE = 1;
	private final static int CHOOSE_PIC_CODE = 2;
	/**
	 * 显示扫描结果
	 */
	private TextView mTextView ;
	private Bitmap bm;
	private EditText editText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mTextView = (TextView) findViewById(R.id.result); 
		//点击按钮跳转到二维码扫描界面，这里用的是startActivityForResult跳转
		//扫描完了之后调到该界面
		Button mButton = (Button) findViewById(R.id.scan_QRCode);
		mButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(!TextUtils.isEmpty(mTextView.getText())){
					mTextView.setText("");
				}
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, CaptureActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivityForResult(intent, SCANNIN_GREQUEST_CODE);
			}
		});

		Button getQRCode = (Button) findViewById(R.id.get_QRCode);
		getQRCode.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
				intent.addCategory(Intent.CATEGORY_OPENABLE);
				intent.setType("image/*");
				startActivityForResult(intent, CHOOSE_PIC_CODE);
			}
		});

		editText = (EditText) findViewById(R.id.et);
		
	}
	/**
	 * 点击按钮生一个二维码图片
	 * @param v
	 */
	public void createQRCode(View v){
		final String text = editText.getText().toString();
		if(TextUtils.isEmpty(text)){
			Toast.makeText(getApplicationContext(),"内容不能为空",Toast.LENGTH_SHORT).show();
			return;
		}
		new AsyncTask<Void, Void, Bitmap>() {

			@Override
			protected Bitmap doInBackground(Void... params) {
				//bm = createLogoQRCode(createQRCode("抛砖引玉", dip2px(MainActivity.this, 150), 0xff000000));//生成中间有logo的二维码
				bm = createQRCode(text, dip2px(MainActivity.this, 150), 0xff000000);
				return bm;
			}
			@Override
			protected void onPostExecute(Bitmap bitmap) {
				if(bitmap != null){
					((ImageView)findViewById(R.id.show_qrcode)).setImageBitmap(bitmap);
				}
			}
		}.execute();
	}
	/**
	 * 在bmp中间画上一个logo
	 * @param bmp
	 * @return
	 */
	private Bitmap createLogoQRCode(Bitmap bmp){
		Bitmap logoBmp = small(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
        Bitmap bitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(bmp, 0, 0, null);
        canvas.drawBitmap(logoBmp, bmp.getWidth() / 2 - logoBmp.getWidth() / 2, bmp.getHeight() / 2 - logoBmp.getHeight() / 2, null);
		return bitmap;
	}
	/**
     * 方法说明：缩小Bitmap
     */
    @SuppressWarnings("unused")
    private  Bitmap small(Bitmap bitmap) {
        Matrix matrix = new Matrix();
        matrix.postScale(0.2f, 0.2f);
        Bitmap resizeBmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
        return resizeBmp;
    }
	/**
	 * 点击按钮save保存生成的二维码图片
	 * @param v
	 */
	public void saveQRCode(View v){
		if(bm != null){
			File qrcodeDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),"qrcode");
			if(!qrcodeDir.exists()){
				if(!qrcodeDir.mkdirs()){
					Log.d("QRCode", "failed to create directory");
					return;
				}
			}
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
			File qrcodeFile = new File(qrcodeDir, "qrcode_" + timeStamp + ".png");
			FileOutputStream out = null;
			try {
				out = new FileOutputStream(qrcodeFile);
				bm.compress(Bitmap.CompressFormat.PNG, 100, out);
				out.flush(); 
				out.close(); 
			} catch (Exception e) {
				e.printStackTrace();
			}
			Uri uri = Uri.fromFile(qrcodeFile);
			sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,uri));
			Toast.makeText(this, "二维码已保存到图库中了", Toast.LENGTH_SHORT).show();
			
		}
		else{
			Toast.makeText(this, "还没生成二位码", Toast.LENGTH_SHORT).show();
		}
	}
	/**
	 * 生成二维码的方法
	 * @param content 二维码内容
	 * @param size 大小
	 * @param color 颜色
	 * @return  bitmap 二维码图片
	 * @throws WriterException
	 */
	private  Bitmap createQRCode(String content, int size, int color) {
	        BitMatrix matrix = null;
			try {
				matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size);
			} catch (WriterException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	        int[] pixels = new int[size * size];
	        for (int y = 0; y < size; y++) {
	            for (int x = 0; x < size; x++) {
	                if (matrix.get(x, y)) {
	                    pixels[y * size + x] = color;
	                }
//	                注意：在这里可能会遇到“保存的二维码图片是全黑”的问题。
//	                原因是在源代码在生成二维码的时候，只是对有数据的地方使用了黑色填充，没有数据的地方没有处理，
//	                而bitmap在compress成jpg/png的时候，默认背景是黑色的，所以就是全黑了。
//	                解决办法是在生成二维码的时候讲没有数据的部分使用白色填充，就可以了。
	                else{
	                	pixels[y * size + x] = 0xaaffffff;//白色
	                }
	            }
	        }
	        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
	        bitmap.setPixels(pixels, 0, size, 0, 0, size, size);
	        return bitmap;
	}
	  /** 
	    * 根据手机的分辨率从 dip 的单位 转成为 px(像素),Java代码只认识像素，可是我们想设置成dip或dp，所以要转化一下
	    */  
    private  int dip2px(Context context, float dpValue) {  
	        final float scale = context.getResources().getDisplayMetrics().density;  
	        return (int) (dpValue * scale + 0.5f);  
	}  
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
		case SCANNIN_GREQUEST_CODE:
			if(resultCode == RESULT_OK&&data!=null){
				//显示扫描到的内容
				mTextView.setText(data.getStringExtra("result"));
			}
			break;
			case CHOOSE_PIC_CODE:
				Uri uri = data.getData();
				String imgPath = SelectPictureUtils.getPath(getApplicationContext(), uri);
				//��ȡ�������
				Result ret = parseQRcodeBitmap(imgPath);
				mTextView.setText("图片中的二维码是：" + ret.toString());
				break;
		}
    }	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(bm!=null){
			bm.recycle();
			bm = null;
		}
			
	}

	private com.google.zxing.Result  parseQRcodeBitmap(String bitmapPath){
		//����ת������UTF-8
		Hashtable<DecodeHintType, String> hints = new Hashtable<DecodeHintType, String>();
		hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
		//��ȡ����������ͼƬ
		BitmapFactory.Options options = new BitmapFactory.Options();
		//������ǰ�inJustDecodeBounds��Ϊtrue����ôBitmapFactory.decodeFile(String path, Options opt)
		//��������ķ���һ��Bitmap���㣬������������Ŀ���ȡ��������
		options.inJustDecodeBounds = true;
		//��ʱ��bitmap��null����δ���֮��options.outWidth �� options.outHeight����������Ҫ�Ŀ�͸���
		Bitmap bitmap = BitmapFactory.decodeFile(bitmapPath,options);
		//����������ȡ������ͼƬ�ı߳�����ά��ͼƬ�������εģ�����Ϊ400����
		/**
		 options.outHeight = 400;
		 options.outWidth = 400;
		 options.inJustDecodeBounds = false;
		 bitmap = BitmapFactory.decodeFile(bitmapPath, options);
		 */
		//����������������Ȼ��bitmap�޶���������Ҫ�Ĵ�С�����ǲ�û�н�Լ�ڴ棬���Ҫ��Լ�ڴ棬���ǻ���Ҫʹ��inSimpleSize�������
		options.inSampleSize = options.outHeight / 400;
		if(options.inSampleSize <= 0){
			options.inSampleSize = 1; //��ֹ��ֵС�ڻ����0
		}
		/**
		 * ������Լ�ڴ�����
		 *
		 * options.inPreferredConfig = Bitmap.Config.ARGB_4444;    // Ĭ����Bitmap.Config.ARGB_8888
		 * options.inPurgeable = true;
		 * options.inInputShareable = true;
		 */
		options.inJustDecodeBounds = false;
		bitmap = BitmapFactory.decodeFile(bitmapPath, options);
		//�½�һ��RGBLuminanceSource���󣬽�bitmapͼƬ�����˶���
		RGBLuminanceSource rgbLuminanceSource = new RGBLuminanceSource(bitmap);
		//��ͼƬת���ɶ�����ͼƬ
		BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(rgbLuminanceSource));
		//��ʼ����������
		QRCodeReader reader = new QRCodeReader();
		//��ʼ����
		Result result = null;
		try {
			result = reader.decode(binaryBitmap, hints);
		} catch (Exception e) {
			// TODO: handle exception
		}

		return result;
	}

}
