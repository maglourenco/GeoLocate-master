package com.example.elevation;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ArcGISFeature;
import com.esri.arcgisruntime.data.Attachment;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.data.ServiceGeodatabase;
import com.example.elevation.adapters.CustomList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class ViewAttachmentActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_EXTERNAL_STORAGE_PERMISSION = 5;
    private CustomList adapter;
    private List<Attachment> attachments;
    private ArcGISFeature mSelectedArcGISFeature;
    private ServiceFeatureTable mServiceFeatureTable;
    private String mAttributeID;
    private ArrayList<String> attachmentList = new ArrayList<>();
    private ListView listView;
    private ProgressDialog progressDialog;
    private String objectID;
    private int noOfAttachments;

    private ArrayList<String> dummy = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_attachments);

        // Set the title of the toolbar
        setTitle(getApplication().getString(R.string.attachments_activity));

        Bundle bundle = getIntent().getExtras();
        objectID = bundle.getString(getString(R.string.objectid));
        noOfAttachments = bundle.getInt(getApplication().getString(R.string.noOfAttachments));

        // create and load the service geodatabase
        ServiceGeodatabase serviceGeodatabase =  new ServiceGeodatabase(getResources().getString(R.string.feature_service));
        serviceGeodatabase.loadAsync();
        serviceGeodatabase.addDoneLoadingListener(() -> {
            // create a feature layer using the first layer in the ServiceFeatureTable
            mServiceFeatureTable = serviceGeodatabase.getTable(0);
            if (noOfAttachments != 0) {
                progressDialog.setTitle(getApplication().getString(R.string.fetching_attachments));
                progressDialog.setMessage(getApplication().getString(R.string.wait));
                progressDialog.show();
                // listener on attachment items to download the attachment
                listView.setOnItemClickListener((parent, view, position, id) -> {
                    fetchAttachmentAsync(position);
                    //Toast.makeText(this, attachmentList.get(position), Toast.LENGTH_SHORT).show();
                });
                fetchAttachmentsFromServer(objectID);
            } else {
                Toast.makeText(this, getApplication().getString(R.string.empty_attachments), Toast.LENGTH_SHORT).show();
            }

        });

        progressDialog = new ProgressDialog(this);

        // get a reference to the list view
        listView = findViewById(R.id.listView);
        // create custom adapter
        adapter = new CustomList(this, attachmentList);
        // set custom adapter on the list
        listView.setAdapter(adapter);
        //fetchAttachmentsFromServer(s);
    }

    /**
     * Asynchronously fetch the given feature's attachments and show them a list view.
     *
     * @param objectID of the feature from which to fetch attachments
     */
    private void fetchAttachmentsFromServer(String objectID) {
        Log.i("OBJECTID: ", objectID);
        attachmentList = new ArrayList<>();
        // create objects required to do a selection with a query
        QueryParameters query = new QueryParameters();
        // set the where clause of the query
        query.setWhereClause("OBJECTID = " + objectID);

        // query the feature table
        final ListenableFuture<FeatureQueryResult> featureQueryResultFuture = mServiceFeatureTable
                .queryFeaturesAsync(query);
        featureQueryResultFuture.addDoneListener(() -> {
            try {
                FeatureQueryResult result = featureQueryResultFuture.get();
                Feature feature = result.iterator().next();
                mSelectedArcGISFeature = (ArcGISFeature) feature;
                // get the number of attachments
                final ListenableFuture<List<Attachment>> attachmentResults = mSelectedArcGISFeature.fetchAttachmentsAsync();
                attachmentResults.addDoneListener(() -> {
                    try {
                        attachments = attachmentResults.get();
                        // if selected feature has attachments, display them in a list fashion
                        if (!attachments.isEmpty()) {
                            for (Attachment attachment : attachments) {
                                attachmentList.add(attachment.getName());
                            }
                            runOnUiThread(() -> {
                                if (progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                adapter = new CustomList(this, attachmentList);
                                listView.setAdapter(adapter);
                                adapter.notifyDataSetChanged();
                            });
                        }
                    } catch (Exception e) {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        String error = "Error getting attachment: " + e.getMessage();
                        Log.e("ERROR", error);
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                String error = "Error getting feature query result: " + e.getMessage();
                Log.e("ERROR", error);
                Toast.makeText(this, error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void fetchAttachmentAsync(final int position) {
        progressDialog.setTitle(getApplication().getString(R.string.downloading_attachments));
        progressDialog.setMessage(getApplication().getString(R.string.wait));
        progressDialog.show();

        // Gets the attachment selected in the ListView
        Attachment attachment = attachments.get(position);

        // Fetch the data for the attachment
        ListenableFuture<InputStream> fetchDataFuture = attachment.fetchDataAsync();
        fetchDataFuture.addDoneListener(() -> {
            try {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                // check if the fetch operation completed successfully
                if (fetchDataFuture.isDone() && !fetchDataFuture.isCancelled() && fetchDataFuture.get() != null) {
                    InputStream inputStream = fetchDataFuture.get(); // get the input stream for the attachment data

                    // write the attachment data to a file
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), attachment.getName());
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, len);
                    }
                    fileOutputStream.flush();
                    fileOutputStream.close();
                    inputStream.close();

                    // launch an intent to open the file in the gallery app
                    Uri fileUri = FileProvider.getUriForFile(getApplicationContext(), "com.example.elevation.provider", file);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(fileUri, "image/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(intent);
                }
            } catch (ExecutionException | InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });
    }
}