package com.stalkyourfriend;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements IOCRCallBack {

    private static final String TAG = MainActivity.class.getName();

    public static final int PICK_IMAGE = 1;
    public static int PICK_FILE = 2;

    private String mAPiKey = "10fdb8530988957";
    private boolean isOverlayRequired;
    private Bitmap img;
    private String mLanguage;
    private TextView mTxtResult;
    private IOCRCallBack mIOCRCallBack;
    private Map<String, String> idName = new HashMap<String, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mIOCRCallBack = this;
        mLanguage = "eng"; //Language
        isOverlayRequired = true;
        init();

    }


    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        return Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
    }

    public String getPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE) {
            try {
                Uri imageUri = data.getData();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                OCRAsyncTask oCRAsyncTask = new OCRAsyncTask(MainActivity.this, mAPiKey, isOverlayRequired, bitmap, mLanguage, mIOCRCallBack);
                oCRAsyncTask.execute();

            } catch (Exception e) {
                Log.e("FileSelectorActivity", "File select error", e);
            }
        }

        if (requestCode == PICK_FILE) {
            if (resultCode == RESULT_OK) {
                // User pick the file
                Uri uri = data.getData();
                readTextFile(uri);
            } else {
                Log.i(TAG, data.toString());
            }
        }

    }

    private void readTextFile(Uri uri){
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));
            String line = "";

            while ((line = reader.readLine()) != null) {
                String split[] = StringUtils.split(line,"|");
                idName.put(split[1], split[0]);
            }
            mTxtResult.setText("File was loaded");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    private void init() {
        mTxtResult = (TextView) findViewById(R.id.actual_result);
        TextView btnCallAPI = (TextView) findViewById(R.id.btn_call_api);

        if (btnCallAPI != null) {
            btnCallAPI.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("image/*");
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"),PICK_IMAGE);

                }
            });
        }

        TextView btnCallAPI2 = (TextView) findViewById(R.id.btn_call_api2);

        if (btnCallAPI2 != null) {
            btnCallAPI2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("text/plain");
                    startActivityForResult(intent, PICK_FILE);
                }
            });
        }

    }

    @Override
    public void getOCRCallBackResult(String response) {
        String left = response.split("\"ParsedText\":")[1];
        String middle = left.split("\"ErrorMessage\":")[0];



        String[] elements = StringUtils.split(middle, "\\t\\r\\n");
        StringBuilder res = new StringBuilder();
        for(String val : elements) {
            boolean vr = false;
            for (String key : idName.keySet()) {
               if(val.contains(key)) {
                   val = val.replace(key, idName.get(key));
                   res.append("\n\n"+ val +"       ");
                   vr = true;
               }

            }
            if(!vr)
                res.append(val + "    ");
        }

        mTxtResult.setText(res);
    }
}
