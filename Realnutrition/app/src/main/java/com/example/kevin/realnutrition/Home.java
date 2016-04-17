package com.example.kevin.realnutrition;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import hod.api.hodclient.HODApps;
import hod.api.hodclient.HODClient;
import hod.api.hodclient.IHODClientCallback;
import hod.response.parser.HODResponseParser;
import jp.wasabeef.glide.transformations.CropCircleTransformation;

public class Home extends Activity implements IHODClientCallback {

    static final int REQUEST_IMAGE_CAPTURE = 1;

    Button mTakePic;
    ImageView mPic;
    String mCurrentPhotoPath = "";
    String mJobID = "";

    HODResponseParser mHodParser = new HODResponseParser();
    HODClient mHodClient = new HODClient("a9ba52cf-4392-4212-bfdb-6c051673d75e", this);
    @Override
    public void requestCompletedWithJobID(String response){
        Log.d("Home:rCWJID:","");

        mJobID = mHodParser.ParseJobID(response);
        if (mJobID.length() > 0)
            mHodClient.GetJobResult(mJobID);
    }
    @Override
    public void requestCompletedWithContent(String response){
        Log.d("Home:rCWC:", response);

    }
    @Override
    public void onErrorOccurred(String errorMessage){
        Log.e("Home:oEO:", errorMessage);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mPic = (ImageView)findViewById(R.id.pic);
    }

    public void onTakePic(View view){
        dispatchTakePictureIntent();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e("Home:dispTakePicInt",ex.getMessage());
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && requestCode == REQUEST_IMAGE_CAPTURE){
            if(mCurrentPhotoPath != null && !mCurrentPhotoPath.isEmpty()){
                setPic();
            }
        }
    }

    private void setPic() {
        try{
            //InputStream is = new URL( mCurrentPhotoPath ).openStream();
            //Bitmap bitmap = BitmapFactory.decodeStream(is);
            Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath);

            //Determine the orientation and rotate the bitmap accordingly
            //This is done so the image is upright when displayed in the activity
            try {
                ExifInterface exif = new ExifInterface(mCurrentPhotoPath);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

                Matrix matrix = new Matrix();
                if (orientation == 6) {
                    matrix.postRotate(90);
                }
                else if (orientation == 3) {
                    matrix.postRotate(180);
                }
                else if (orientation == 8) {
                    matrix.postRotate(270);
                }
                bitmap = Bitmap.createBitmap(bitmap,
                        0, 0, bitmap.getWidth(),
                        bitmap.getHeight(),
                        matrix, true); // rotating bitmap
            }
            catch (Exception e) {
                Log.d("Home:setPic:rotation:", e.toString());
            }

            mPic.setImageBitmap(bitmap);
            getFaceCoordinates();

            Glide.with(Home.this).load(mCurrentPhotoPath)
                    .bitmapTransform(new CropCircleTransformation(getApplicationContext()))
                    .into(mPic);
        }catch (Exception e) {
            Log.e("Home:setPic:",e.getMessage());
        }
    }

    private void getFaceCoordinates(){
        Map<String,Object> params = new HashMap<String, Object>();
        params.put("file",mCurrentPhotoPath);
        params.put("mode","document_photo");

        Log.d("Home:gFC:", "");
        mHodClient.PostRequest(params, HODApps.DETECT_FACES, HODClient.REQ_MODE.ASYNC);
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "RN_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
}
