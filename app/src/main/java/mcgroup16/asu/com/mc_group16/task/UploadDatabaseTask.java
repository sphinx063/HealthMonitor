package mcgroup16.asu.com.mc_group16.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by Siddharth on 3/7/2017.
 * Credits - Reference taken from http://www.codicode.com and sample code uploaded by TA
 */

public class UploadDatabaseTask extends AsyncTask<String, Integer, String> {

    private static final String TAG = UploadDatabaseTask.class.getName();
    Context appContext = null;
    String serverURI = null;
    String dbName = null;
    ProgressDialog uploadProgressDialog = null;

    public UploadDatabaseTask(Context appContext, String serverURI, String dbName) {
        super();
        this.appContext = appContext;
        this.serverURI = serverURI;
        this.dbName = dbName;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        uploadProgressDialog = new ProgressDialog(appContext);
        uploadProgressDialog.setMessage("Uploading Database...");
        uploadProgressDialog.setIndeterminate(true);
        uploadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        uploadProgressDialog.setCancelable(true);
        uploadProgressDialog.show();
    }

    @Override
    protected void onPostExecute(String response) {
        super.onPostExecute(response);
        uploadProgressDialog.dismiss();
        if (response != null && response.equals("OK")) {
            Toast.makeText(appContext, "Upload Completed Successfully", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(appContext, "Upload Error: " + response, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);
        uploadProgressDialog.setIndeterminate(false);
        uploadProgressDialog.setMax(100);
        uploadProgressDialog.setProgress(progress[0]);
    }

    @Override
    protected String doInBackground(String... databases) {

        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        FileInputStream dataFileInputStream = null;
        HttpsURLConnection httpsConnection = null;
        DataOutputStream dataOutputStream = null;
        URL connectURL;

        try {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                }
            }};
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Setting file path here
            String databasePath = databases[0];
            File dbFile = new File(databasePath);
            dataFileInputStream = new FileInputStream(dbFile);

            // Open a HTTP connection to the URL
            connectURL = new URL(serverURI);
            httpsConnection = (HttpsURLConnection) connectURL.openConnection();
            httpsConnection.setDoInput(true);
            httpsConnection.setDoOutput(true);
            httpsConnection.setUseCaches(false);
            httpsConnection.setRequestMethod("POST");
            httpsConnection.setRequestProperty("Connection", "Keep-Alive");
            httpsConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            httpsConnection.setRequestProperty("uploaded_file", dbName);

            dataOutputStream = new DataOutputStream(httpsConnection.getOutputStream());
            dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd);
            dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\"" + dbName + "\"" + lineEnd);
            dataOutputStream.writeBytes(lineEnd);

            // create a buffer of maximum size
            int bytesAvailable = dataFileInputStream.available();
            int maxBufferSize = 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);
            byte[] buffer = new byte[bufferSize];

            // write the file into data output stream...
            int totalBytesRead = 0;
            int bytesRead = dataFileInputStream.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                dataOutputStream.write(buffer, 0, bufferSize);
                totalBytesRead += bytesRead;
                bytesAvailable = dataFileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = dataFileInputStream.read(buffer, 0, bufferSize);
                // publishing the progress....
                if (bytesAvailable > 0) // only if total length is known
                    publishProgress((int) (totalBytesRead * 100 / bytesAvailable));
            }
            dataOutputStream.writeBytes(lineEnd);
            dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // expecting OK response from server side
            if (httpsConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                return httpsConnection.getResponseMessage();
            }else{
                return "Server returned HTTP " + httpsConnection.getResponseCode() + " " + httpsConnection.getResponseMessage();
            }


        } catch (Exception ex) {
            Log.e(TAG, "Error: " + ex.getMessage(), ex);
        } finally {
            try {
                if (dataFileInputStream != null) {
                    dataFileInputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.flush();
                    dataOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (httpsConnection != null) {
                httpsConnection.disconnect();
            }
        }
        return null;
    }
}
