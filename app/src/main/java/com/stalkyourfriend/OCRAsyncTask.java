package com.stalkyourfriend;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;


public class OCRAsyncTask extends AsyncTask {

    private static final String TAG = OCRAsyncTask.class.getName();

    String url = "https://api.ocr.space/parse/image";

    private String mApiKey;
    private boolean isOverlayRequired = false;
    private Bitmap bitmap;
    private String mLanguage;
    private Activity mActivity;
    private ProgressDialog mProgressDialog;
    private IOCRCallBack mIOCRCallBack;

    public OCRAsyncTask(Activity activity, String apiKey, boolean isOverlayRequired, Bitmap img, String language, IOCRCallBack iOCRCallBack) {
        this.mActivity = activity;
        this.mApiKey = apiKey;
        this.isOverlayRequired = isOverlayRequired;
        this.bitmap = img;
        this.mLanguage = language;
        this.mIOCRCallBack = iOCRCallBack;
    }

    @Override
    protected void onPreExecute() {
        mProgressDialog = new ProgressDialog(mActivity);
        mProgressDialog.setTitle("Wait while processing....");
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(Object[] params) {

        try {
            return sendPost(mApiKey, isOverlayRequired, bitmap, mLanguage);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private String sendPost(String apiKey, boolean isOverlayRequired, Bitmap img, String language) throws Exception {

        URL obj = new URL(url);
        HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");


        JSONObject postDataParams = new JSONObject();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        img.compress(Bitmap.CompressFormat.PNG, 15, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String encodedImg = Base64.encodeToString(byteArray, Base64.DEFAULT);


        postDataParams.put("apikey", apiKey);//TODO Add your Registered API key
        postDataParams.put("isOverlayRequired", isOverlayRequired);
        postDataParams.put("base64Image",("data:image/png;base64," + encodedImg));
        postDataParams.put("language", language);
        postDataParams.put("isTable", "true");
        postDataParams.put("issearchablepdfhidetextlayer", "false");
        postDataParams.put("iscreatesearchablepdf", "false");

        Log.d(TAG, encodedImg.toString());

        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(getPostDataString(postDataParams));
        wr.flush();
        wr.close();

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return String.valueOf(response);
    }

    @Override
    protected void onPostExecute(Object result) {
        super.onPostExecute(result);

        if (mProgressDialog != null && mProgressDialog.isShowing())
            mProgressDialog.dismiss();
        String response = (String) result;
        mIOCRCallBack.getOCRCallBackResult(response);
    }

    public String getPostDataString(JSONObject params) throws Exception {

        StringBuilder result = new StringBuilder();
        boolean first = true;

        Iterator<String> itr = params.keys();

        boolean ok = false;
        while (itr.hasNext()) {
            String key = itr.next();
            Object value = params.get(key);

            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(key, "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(value.toString(), "UTF-8"));
        }
        return result.toString();
    }
}


