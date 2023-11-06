package com.example.elevation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.chootdev.csnackbar.Align;
import com.chootdev.csnackbar.Duration;
import com.chootdev.csnackbar.Snackbar;
import com.chootdev.csnackbar.Type;
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.ArcGISFeature;
import com.esri.arcgisruntime.data.Attachment;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureEditResult;
import com.esri.arcgisruntime.data.FeatureQueryResult;
import com.esri.arcgisruntime.data.FeatureTableEditResult;
import com.esri.arcgisruntime.data.QueryParameters;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.data.ServiceGeodatabase;
import com.esri.arcgisruntime.geoanalysis.LineOfSight;
import com.esri.arcgisruntime.geoanalysis.LocationLineOfSight;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.GeoElement;
import com.esri.arcgisruntime.mapping.RasterElevationSource;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.view.AnalysisOverlay;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.DefaultSceneViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.SceneView;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.example.elevation.adapters.EstimationAttributesAdapter;
import com.example.elevation.utils.Utils;
import com.github.clans.fab.FloatingActionButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA = 2;
    private static final int REQUEST_CODE = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 50;
    private static final int REQUEST_IMAGE_PICK = 10;
    private static final int REQUEST_SETTINGS_CHANGE = 5;
    private SharedPreferences sharedPreferences;
    private int USER_HEIGHT = 2;
    private boolean IS_DEBUG = true;
    private ProgressDialog progressDialog;
    private SceneView mSceneView;
    private ArcGISScene scene;
    private SimpleMarkerSymbol circleSymbol, inter;
    private GraphicsOverlay graphicsOverlay;
    private GraphicsOverlay locationOverlay;
    private AnalysisOverlay mAnalysisOverlay;
    private Orientation mOrientation;
    private Location currentLocation;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private GeoLocate geoLocate;
    private Surface surface;
    private ServiceFeatureTable mServiceFeatureTable;
    private Point viewpointTest = new Point(-9.163043, 38.840922, SpatialReference.create(4326));
    private FeatureLayer mFeatureLayer;
    private ArcGISFeature mSelectedArcGISFeature;
    private BottomSheetDialog bottomSheetDialog;
    private String mObjectID;
    private ArrayList<String> attachmentList = new ArrayList<>();
    private List<Attachment> attachments;
    private boolean hasAttachment = false;
    private String imageFilename;
    private TextView attachmentFilename;
    private File imageFile;
    boolean isConnected = false;
    private ConnectivityManager connManager = null;
    private Uri imageAttachmentURI;
    private Envelope demEnvelope;

    public void showProgressDialog() {
        progressDialog.setTitle(getResources().getString(R.string.processing));
        progressDialog.setMessage(getResources().getString(R.string.wait));
        progressDialog.show();
    }

    public void hideProgressDialog() {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /**
     * isInsideDEM() - checks if the user's location is inside the DEM's Polygon
     */
    private boolean isInsideDEM() {
        if (currentLocation != null) {
            // Create a Point using the user's location
            Point userLocation = new Point(currentLocation.getLongitude(), currentLocation.getLatitude(), SpatialReference.create(4326));
            // Project the Point's Spatial Reference to the DEM's Spatial Reference
            Point projectedPoint = (Point) GeometryEngine.project(userLocation, demEnvelope.getSpatialReference());
            return GeometryEngine.contains((Geometry) demEnvelope, projectedPoint);
        } else {
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    public void createGeolocateUI3D() {
        // create a scene and add a basemap to it
        scene = new ArcGISScene();
        scene.setBasemap(Basemap.createImagery());

        // get a reference to the scene view and set the scene to it
        mSceneView = findViewById(R.id.sceneView);
        mSceneView.setScene(scene);

        FloatingActionButton locationButton = findViewById(R.id.location);
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Set the SceneView camera's viewpoint to the user's location
                if (currentLocation != null) {
                    // add a new graphic with the same location as the initial viewpoint
                    locationOverlay.getGraphics().clear();
                    Point userLocation = new Point(currentLocation.getLongitude(), currentLocation.getLatitude());
                    Graphic pinStarBlueGraphic = new Graphic(userLocation, circleSymbol);
                    locationOverlay.getGraphics().add(pinStarBlueGraphic);

                    mSceneView.setViewpointCameraAsync(new Camera(new Point(currentLocation.getLongitude(), currentLocation.getLatitude()),
                            500.0,
                            0,
                            0,
                            0.0), 1f);
                }
            }
        });

        // create an analysis overlay to contain the analysis and add it to the scene view
        mAnalysisOverlay = new AnalysisOverlay();
        mSceneView.getAnalysisOverlays().add(mAnalysisOverlay);

        mOrientation = new Orientation((SensorManager) getSystemService(Activity.SENSOR_SERVICE),
                getWindow().getWindowManager());

        // set the visible and obstructed colors (default would be green/red)
        // these are static properties that apply to all line of sight analyses in the scene view
        LineOfSight.setVisibleColor(Color.CYAN);
        LineOfSight.setObstructedColor(Color.MAGENTA);

        // Add Digital Elevation Model to the scene
        addDEMToScene();

        // create a point symbol to mark where elevation is being measured
        circleSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.RED, 10);
        inter = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.BLUE, 10);

        // create a graphics overlay
        graphicsOverlay = new GraphicsOverlay(GraphicsOverlay.RenderingMode.DYNAMIC);
        locationOverlay = new GraphicsOverlay(GraphicsOverlay.RenderingMode.DYNAMIC);
        mSceneView.getGraphicsOverlays().add(graphicsOverlay);
        mSceneView.getGraphicsOverlays().add(locationOverlay);

        // add a camera and initial camera position
        // specify the initial camera position
        Camera camera = new Camera(38.840922, -9.163043, 10000.0, 0, 0, 0.0);
        mSceneView.setViewpointCamera(camera);

        // create and load the service geodatabase
        ServiceGeodatabase serviceGeodatabase = new ServiceGeodatabase(getResources().getString(R.string.feature_service));
        serviceGeodatabase.loadAsync();
        serviceGeodatabase.addDoneLoadingListener(() -> {
            // create a feature layer using the first layer in the ServiceFeatureTable
            mServiceFeatureTable = serviceGeodatabase.getTable(0);
            // create a feature layer from table
            mFeatureLayer = new FeatureLayer(mServiceFeatureTable);
            // add the layer to the map
            mSceneView.getScene().getOperationalLayers().add(mFeatureLayer);
        });

        // create a touch listener to handle taps
        mSceneView.setOnTouchListener(new DefaultSceneViewOnTouchListener(mSceneView) {
            @Override public boolean onSingleTapConfirmed(MotionEvent motionEvent) {
                // clear any existing graphics from the graphics overlay
                clearUI();

                if (mFeatureLayer != null) {
                    mFeatureLayer.clearSelection();

                    android.graphics.Point screenPoint = new android.graphics.Point(
                            Math.round(motionEvent.getX()),
                            Math.round(motionEvent.getY()));

                    identifyResult(screenPoint);
                }
                return super.onSingleTapConfirmed(motionEvent);
            }
        });

        Button cameraButton = findViewById(R.id.camera);
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if the user's location is inside the DEM Polygon
                // If YES - proceed to the geolocating functionality
                if (isInsideDEM()){
                    Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                    startActivityForResult(intent, CAMERA);
                } else {
                    Toast.makeText(MainActivity.this, getString(R.string.not_inside_dem), Toast.LENGTH_LONG).show();
                }
            }
        });

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (currentLocation != null) {
                    Log.i("*GPS*", "Location: " + String.valueOf(currentLocation.toString()));
                    Log.i("*GPS*", "GetAccuracy: " + String.valueOf(currentLocation.getAccuracy()));
                }

                currentLocation = location;
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
                Log.i("onStatusChanged", s);
            }

            @Override
            public void onProviderEnabled(String s) {
                Log.i("onProviderEnabled", s);
            }

            @Override
            public void onProviderDisabled(String s) {
                Log.i("onProviderDisabled", s);
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        } else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (currentLocation != null) {
                Log.i("Location", currentLocation.toString());
                Log.i("GetAccuracy", String.valueOf(currentLocation.getAccuracy()));

                double heading = mSceneView.getCurrentViewpointCamera().getHeading();
                if (currentLocation.getBearing() != 0.0)
                    heading = currentLocation.getBearing();
                mSceneView.setViewpointCamera(new Camera(38.840922, -9.163043, 10000.0, 0, 0, 0.0));
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},2);
        }
    }

    private void identifyResult(android.graphics.Point screenPoint) {
         // Reset hasAttachment flag
         hasAttachment = false;
         final ListenableFuture<IdentifyLayerResult> identifyLayerResultsFuture = mSceneView
                 .identifyLayerAsync(scene.getOperationalLayers().get(0), screenPoint, 25, false);
         showProgressDialog();
         identifyLayerResultsFuture.addDoneListener(new Runnable() {
            @Override public void run() {
                try {
                    IdentifyLayerResult identifyLayerResult = identifyLayerResultsFuture.get();
                    List<GeoElement> resultGeoElements = identifyLayerResult.getElements();
                    hideProgressDialog();
                    if (!resultGeoElements.isEmpty()) {
                        if (resultGeoElements.get(0) instanceof ArcGISFeature) {
                            // Get the selected pressed and select it
                            mSelectedArcGISFeature = (ArcGISFeature) resultGeoElements.get(0);

                            // Try selectFeatureAsync for performance
                            mFeatureLayer.selectFeature(mSelectedArcGISFeature);

                            // Set the SceneView camera's viewpoint to the selected feature
                            mSceneView.setViewpointCameraAsync(new Camera((Point) mSelectedArcGISFeature.getGeometry(),
                                    2000.0,
                                    0,
                                    0,
                                    0.0), 1f);

                            bottomSheetDialog = new BottomSheetDialog(MainActivity.this, R.style.BottomSheetTheme);
                            View bottomsheetView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.estimation_options_bottomsheet_layout, null);
                            bottomsheetView.findViewById(R.id.infoSection).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    //Toast.makeText(MainActivity.this, String.valueOf(mSelectedArcGISFeature.getAttributes().get("Status")), Toast.LENGTH_SHORT).show();
                                    Map<String, Object> estimationAttrs = mSelectedArcGISFeature.getAttributes();
                                    Map<String, Object> filteredFeatureAttrs = Utils.filterAttributes(estimationAttrs);

                                    bottomSheetDialog.cancel();

                                    // get the number of attachments
                                    final ListenableFuture<List<Attachment>> attachmentResults = mSelectedArcGISFeature.fetchAttachmentsAsync();
                                    attachmentResults.addDoneListener(() -> {
                                        try {
                                            attachments = attachmentResults.get();
                                            attachmentList = new ArrayList<>();
                                            if (!attachments.isEmpty()) {
                                                for (Attachment attachment : attachments) {
                                                    attachmentList.add(attachment.getName());
                                                    Log.i("*ATTRS*", "Attachment name: " + attachment.getName());
                                                }
                                            }

                                            mObjectID = estimationAttrs.get("OBJECTID").toString();

                                            ArrayList<EstimationAttributes> dialogAttributes = new ArrayList<>();
                                            filteredFeatureAttrs.keySet().forEach(key -> {
                                                dialogAttributes.add(new EstimationAttributes(key, String.valueOf(filteredFeatureAttrs.get(key))));
                                            });

                                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                                            LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
                                            View customDialogView = inflater.inflate(R.layout.estimation_attributes_listview_layout, null);

                                            ListView listView = (ListView) customDialogView.findViewById(R.id.list);
                                            EstimationAttributesAdapter estimationAttributesAdapter = new EstimationAttributesAdapter(MainActivity.this, dialogAttributes);
                                            listView.setAdapter(estimationAttributesAdapter);
                                            listView.setEnabled(false);
                                            TextView numberOfAttachments = customDialogView.findViewById(R.id.numberOfAttachments);
                                            numberOfAttachments.setText(String.valueOf(attachmentList.size()));

                                            ImageView attachmentsInfo = (ImageView) customDialogView.findViewById(R.id.attachmentsInfo);
                                            attachmentsInfo.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    // start EditAttachmentActivity to view the attachments
                                                    Intent myIntent = new Intent(MainActivity.this, ViewAttachmentActivity.class);
                                                    myIntent.putExtra(getString(R.string.objectid), mObjectID);
                                                    myIntent.putExtra(getString(R.string.noOfAttachments), attachments.size());
                                                    Bundle bundle = new Bundle();
                                                    startActivityForResult(myIntent, REQUEST_CODE, bundle);
                                                }
                                            });

                                            builder.setTitle(getApplication().getString(R.string.estimation_attributes_dialog));
                                            builder.setNeutralButton(getApplication().getString(R.string.close_dialog), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    dialogInterface.cancel();
                                                }
                                            });
                                            builder.setView(customDialogView);

                                            AlertDialog alertDialog = builder.create();
                                            alertDialog.show();

                                        } catch (ExecutionException e) {
                                            Toast.makeText(MainActivity.this, getApplication().getString(R.string.error_message), Toast.LENGTH_SHORT).show();
                                            e.printStackTrace();
                                        } catch (InterruptedException e) {
                                            Toast.makeText(MainActivity.this, getApplication().getString(R.string.error_message), Toast.LENGTH_SHORT).show();
                                            e.printStackTrace();
                                        }
                                    });                                }
                            });
//                            bottomsheetView.findViewById(R.id.scene3DSection).setOnClickListener(new View.OnClickListener() {
//                                @Override
//                                public void onClick(View view) {
//                                    displayLineOfSightEstimation();
//                                    bottomSheetDialog.cancel();
//                                }
//                            });
                            bottomSheetDialog.setContentView(bottomsheetView);
                            bottomSheetDialog.show();
                        }
                    }
                } catch (InterruptedException | ExecutionException e) {
                    Log.e("ERROR", "Error identifying results: " + e.getMessage());
                }
            }
         });
    }

    public void displayLineOfSightEstimation() {
        try {
            double observerElevation = mSceneView.getScene().getBaseSurface().getElevationAsync(new Point(currentLocation.getLongitude(), currentLocation.getLatitude(), SpatialReference.create(4326))).get();
            Point observer = new Point(currentLocation.getLongitude(), currentLocation.getLatitude(), observerElevation, SpatialReference.create(4326));
            Log.i("SPATIAL REFS", observer.getSpatialReference().getWKText());
            Point target = (Point) mSelectedArcGISFeature.getGeometry();
            Point targetProj = (Point) GeometryEngine.project(target, SpatialReference.create(4326));
            Log.i("SPATIAL REFS", targetProj.getSpatialReference().getWKText());
            LocationLineOfSight lineOfSight = new LocationLineOfSight(observer, target);
            mAnalysisOverlay.getAnalyses().add(lineOfSight);

//            // Calculate the camera's heading angle
//            double heading = Math.atan2(targetProj.getY() - observer.getY(), targetProj.getX() - observer.getX());
//            heading = Math.toDegrees(heading);
//
//            // Calculate the camera's pitch angle
//            //double distance = GeometryEngine.distanceBetween(observer, targetProj);
//            double distance = GeometryEngine.distanceGeodetic(observer, targetProj, new LinearUnit(LinearUnitId.METERS), new AngularUnit(AngularUnitId.DEGREES), GeodeticCurveType.GEODESIC).getDistance();
//            Log.i("TEST_DIST", String.valueOf(distance));
//            double pitch = Math.toDegrees(Math.atan2(50, distance));

            //TODO - CORRECT CAMERA'S HEADING AND PITCH
            mSceneView.setViewpointCamera(new Camera(currentLocation.getLatitude(),
                    currentLocation.getLongitude(),
                    50.0,
                    241,
                    80,
                    0));

//            mSceneView.setViewpointCamera(new Camera(currentLocation.getLatitude(),
//                    currentLocation.getLongitude(),
//                    50.0,
//                    heading,
//                    pitch,
//                    0));

        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void addEstimation(Point estimationPoint, String estimationNote) {
        // create default attributes for the feature
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("Status", "Pending");
        attributes.put("Note", estimationNote);

        // 1 - Creates a new feature using the user's attributes and point
        Feature feature = mServiceFeatureTable.createFeature(attributes, estimationPoint);

        // 2 - Check if feature can be added to feature table
        if (mServiceFeatureTable.canAdd()) {
            // Add the new feature to the feature table and to server
            try {
                mServiceFeatureTable.addFeatureAsync(feature).addDoneListener(() -> {
                    // 3 - applyEdits
                    final ListenableFuture<List<FeatureTableEditResult>> editResult = mServiceFeatureTable.getServiceGeodatabase().applyEditsAsync();
                    editResult.addDoneListener(() -> {
                        try {
                            List<FeatureTableEditResult> editResults = editResult.get();
                            // Check if the server edit was successful
                            if (editResults != null && !editResults.isEmpty()) {
                                if (progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }

                                if (hasAttachment){
                                    // Get the Object ID of the newly added feature
                                    long objectIdObj = (long) feature.getAttributes().get("OBJECTID");
                                    // Retrieve the ArcGISFeature from the service using Object ID
                                    QueryParameters queryParameters = new QueryParameters();
                                    // For some reason the OBJECTID has a negated value - need to put the minus (-) sign
                                    // 4 - Query the AGOL Feature Service for the OBJECTID - because to deal with attachments we need an ArcGISFeature; Feature not enough
                                    queryParameters.setWhereClause("OBJECTID = " + String.valueOf(-objectIdObj));
                                    ListenableFuture<FeatureQueryResult> queryResultFuture = mServiceFeatureTable.queryFeaturesAsync(queryParameters, ServiceFeatureTable.QueryFeatureFields.LOAD_ALL);
                                    queryResultFuture.addDoneListener(() -> {
                                        try {
                                            FeatureQueryResult queryResult = queryResultFuture.get();
                                            Iterator<Feature> iterator = queryResult.iterator();
                                            if (iterator.hasNext()) {
                                                Feature queriedFeature = iterator.next();
                                                if (queriedFeature instanceof ArcGISFeature) {
                                                    ArcGISFeature arcgisFeature = (ArcGISFeature) queriedFeature;
                                                    try {
                                                        ListenableFuture<Attachment> addResult = arcgisFeature
                                                                .addAttachmentAsync(getAttachmentData(), "image/png", imageFilename);
                                                        addResult.addDoneListener(() -> {
                                                            final ListenableFuture<Void> tableResult = mServiceFeatureTable.updateFeatureAsync(arcgisFeature);
                                                            tableResult.addDoneListener(() -> {
                                                                applyEditsAttachment();
                                                            });
                                                        });
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                        Log.e("addAttachmentToEstimation", "addAttachmentToEstimation");
                                                    }
                                                } else {
                                                    Log.i("**TEST**", "The queried feature is not an ArcGISFeature");
                                                }
                                            }
                                        } catch (InterruptedException | ExecutionException e) {
                                            // Error occurred while querying features
                                        }
                                    });
                                }
                            } else {
                                Log.e("editResult", "Server did not return edit results");
                            }
                        } catch (InterruptedException | ExecutionException e) {
                            Log.i("applyEdits", "ERROR APPLYING EDITS");
                        }
                    });
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "ERROR IN ADDING FEATURE", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Applies changes from a Service Feature Table to the server after adding an attachment.
     */
    private void applyEditsAttachment() {
        try {
            // apply edits to the server
            final ListenableFuture<List<FeatureEditResult>> updatedServerResult = mServiceFeatureTable.applyEditsAsync();
            updatedServerResult.addDoneListener(() -> {
                try {
                    List<FeatureEditResult> edits = updatedServerResult.get();
                    // check that the feature table was successfully updated
                    if (!edits.isEmpty()) {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        hasAttachment = false;
                        imageFilename = "";
                        imageAttachmentURI = null;
                        // update the attachment list view on the control panel
                        Log.e("applyEditsAttachment", "Feature edited successfully");
                    } else {
                        Log.e("applyEditsAttachment", "Server did not return edit results");
                    }
                } catch (Exception e) {
                    Log.e("applyEditsAttachment", "Error getting feature edit result: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e("applyEditsAttachment", "Error applying edits to server: " + e.getMessage());
        }
    }

    private byte[] getAttachmentData() throws IOException {
        InputStream imageInputStream = getContentResolver().openInputStream(imageAttachmentURI);
        return bytesFromInputStream(imageInputStream);
    }

    public void displayEstimationDialog(Point lastPoint) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getApplication().getString(R.string.add_estimation_dialog));
        builder.setIcon(R.drawable.estimation_icon);

        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.new_estimation_layout, null);

        attachmentFilename = dialogLayout.findViewById(R.id.attachmentFilename);

        Button addAttachment = dialogLayout.findViewById(R.id.includeAttachment);
        addAttachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog = new BottomSheetDialog(MainActivity.this, R.style.BottomSheetTheme);
                View bottomsheetView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.attachment_from_bottomsheet_layout, null);
                // CAMERA ATTACHMENT
                bottomsheetView.findViewById(R.id.cameraAttachment).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        takePictureAttachment();
                        bottomSheetDialog.cancel();
                    }
                });
                // GALLERY ATTACHMENT
                bottomsheetView.findViewById(R.id.galleryAttachment).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        pickFromGalleryAttachment();
                        bottomSheetDialog.cancel();
                    }
                });
                bottomSheetDialog.setContentView(bottomsheetView);
                bottomSheetDialog.show();
            }
        });

        builder.setView(dialogLayout);
        builder.setPositiveButton(getApplication().getString(R.string.add_dialog), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                TextView estimationNote = dialogLayout.findViewById(R.id.estimationNote);
                showProgressDialog();
                addEstimation(lastPoint, estimationNote.getText().toString());
            }
        });
        builder.setNeutralButton(getApplication().getString(R.string.cancel_dialog), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void takePictureAttachment() {
        try {
            imageFile = null;
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            imageFilename = "IMG_" + timeStamp;
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            imageFile = File.createTempFile(
                    imageFilename,
                    ".jpg",
                    storageDir
            );
        } catch (IOException ex) {
            // Handle error
            ex.printStackTrace();
        }

        // Create an intent to capture a photo
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null && imageFile != null) {
            // Set the file URI where the captured image should be stored
            imageAttachmentURI = FileProvider.getUriForFile(getApplicationContext(), "com.example.elevation.provider", imageFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageAttachmentURI);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    public void pickFromGalleryAttachment() {
        // Create an intent to launch the gallery app
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK);
        pickImageIntent.setType("image/*");

        // Start the activity to pick an image
        startActivityForResult(pickImageIntent, REQUEST_IMAGE_PICK);
    }

    // adb push agol_30m.tif /storage/emulated/0/Android/data/com.example.elevation/files/agol_30m.tif
    // adb push dem_aw3d_prjwgs84.tif /storage/emulated/0/Android/data/com.example.elevation/files/dem_aw3d_prjwgs84.tif
    public void addDEMToScene() {
        try {
            // create an elevation source, and add this to the base surface of the scene
            // raster package file paths
            ArrayList<String> filePaths = new ArrayList<>();
            //filePaths.add(getExternalFilesDir(null) + "/dem_30m.tif");
            //filePaths.add(getExternalFilesDir(null) + "/Clip_dem_aw3d_pt_25m.tif");
            //filePaths.add(getExternalFilesDir(null) + "/dem_aw3d_prjwgs84.tif");
            filePaths.add(getExternalFilesDir(null) + "/agol_30m.tif");
            Log.i("DEM PATH", filePaths.toString());

            // add an elevation source to the scene by passing the URI of the raster package to the constructor
            RasterElevationSource rasterElevationSource = new RasterElevationSource(filePaths);

            // add a listener to perform operations when the load status of the elevation source changes
            rasterElevationSource.addLoadStatusChangedListener(loadStatusChangedEvent -> {
                // when elevation source loads
                if (loadStatusChangedEvent.getNewLoadStatus() == LoadStatus.LOADED) {
                    // add the elevation source to the elevation sources of the scene
                    Log.i("DEM", "DEM did load");
                    mSceneView.getScene().getBaseSurface().getElevationSources().add(rasterElevationSource);
                    demEnvelope = rasterElevationSource.getFullExtent();
                    Log.i("RASTER", "ELEVATION SOURCES: " + mSceneView.getScene().getBaseSurface().getElevationSources().get(0).getName());
                } else {
                    Toast.makeText(this, getApplication().getString(R.string.dem_failed_load), Toast.LENGTH_LONG).show();
                    Log.i("DEM", "DEM did NOT load");
                }
            });

            // load the elevation source asynchronously
            rasterElevationSource.loadAsync();
        } catch (IllegalArgumentException e) {
            // catch exception thrown by RasterElevationSource when a file is invalid/not found
            Log.i("DEM", "DEM did NOT load");
        }
    }

    public void clearUI() {
        graphicsOverlay.getGraphics().clear();
        mAnalysisOverlay.getAnalyses().clear();
        locationOverlay.getGraphics().clear();
    }

    private void addViewrayGraphics() {
        clearUI();

        if (IS_DEBUG) {
            // Add viewray steps graphics
            geoLocate.getViewraySteps().forEach(viewrayStep -> {
                Graphic intermediaryPoint = new Graphic(viewrayStep, inter);
                graphicsOverlay.getGraphics().add(intermediaryPoint);
            });
        }

        // Add end point graphic
        Graphic surfacePointGraphic = new Graphic(geoLocate.getLastViewrayPoint(), circleSymbol);
        graphicsOverlay.getGraphics().add(surfacePointGraphic);

        // Add line-of-sight graphic
        mAnalysisOverlay.getAnalyses().add(geoLocate.getLineOfSight());

//        // Add reference point
//        Point reference = new Point(-9.145614, 38.810696, 93, SpatialReference.create(4326));
//        Graphic referencePoint = new Graphic(reference, circleSymbol);
//        graphicsOverlay.getGraphics().add(referencePoint);
    }

    public File getFileFromUri(Uri uri) {
        String filePath = null;
        String[] projection = {MediaStore.Images.Media.DATA};

        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                filePath = cursor.getString(columnIndex);
            }
        }

        if (filePath != null) {
            return new File(filePath);
        }

        return null;
    }

     /**
      * Sets up an IntentFilter for the Internet Connection BroadcastReceiver
      * to only call the onReceive() callback function when the state
      * of the connectivity changes
      */
     private static IntentFilter makeConnectivityIntentFilter() {
         final IntentFilter intentFilter = new IntentFilter();
         intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
         intentFilter.addAction("android.net.conn");
         return intentFilter;
     };

     /**
      * Checks if there is connectivity from Wifi or Mobile Data Connections
      */
     public boolean checkConnectivity() {
         if (connManager != null) {
             if (Build.VERSION.SDK_INT < 23) {
                 @SuppressLint("MissingPermission") final NetworkInfo ni = connManager.getActiveNetworkInfo();

                 if (ni != null) {
                     return (ni.isConnected() && (ni.getType() == ConnectivityManager.TYPE_WIFI || ni.getType() == ConnectivityManager.TYPE_MOBILE));
                 }
             } else {
                 final Network n = connManager.getActiveNetwork();

                 if (n != null) {
                     final NetworkCapabilities nc = connManager.getNetworkCapabilities(n);
                     return (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI));
                 }
             }
         }
         return false;
     }

     /**
      * BroadcastReceiver that receives events for the connectivity change
      * CONNECTIVITY_CHANGE: internet connectivity changes
      */
     private final BroadcastReceiver connectivityReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
             NetworkInfo activeNetwork = connManager.getActiveNetworkInfo();
             isConnected = checkConnectivity();  Log.i("CONNECTION STATUS", Boolean.toString(isConnected));

             if (isConnected) {
                 Snackbar.with(MainActivity.this, null)
                         .type(Type.SUCCESS)
                         .message(getApplication().getString(R.string.yes_internet))
                         .duration(Duration.SHORT)
                         .fillParent(true)
                         .textAlign(Align.CENTER)
                         .show();
             } else {
                 Snackbar.with(MainActivity.this,null)
                         .type(Type.ERROR)
                         .message(getApplication().getString(R.string.no_internet))
                         .duration(Duration.SHORT)
                         .fillParent(true)
                         .textAlign(Align.CENTER)
                         .show();
             }
         }
     };

     @Override
     protected void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);
         // Terrain exaggeration layout - change afterwards
         //setContentView(R.layout.terrain_exaggeration_layout);
         setContentView(R.layout.activity_main);

         // Authentication with an API key or named user is required to access basemaps
         ArcGISRuntimeEnvironment.setLicense(getString(R.string.api_key));

         // Get the SharedPreferences object for the app
         // Get the current value of the seek bar preference
         sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
         USER_HEIGHT = sharedPreferences.getInt("user_height", 20);
         IS_DEBUG = sharedPreferences.getBoolean("is_debug", true);

         progressDialog = new ProgressDialog(MainActivity.this);

         connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

         //Select function that want to use (self-explanatory names)
         if (checkConnectivity()) {
             //createTerrainFromLocalRaster();
             //createTerrainExaggeration();
             //createTerrainExaggerationFromLocalRaster();
             //createEstimationsFeatureLayer();
             createGeolocateUI3D();
         }
     }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSceneView != null) {
            mSceneView.pause();
        }
        if (connectivityReceiver != null) {
            unregisterReceiver(connectivityReceiver);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSceneView != null) {
            mSceneView.resume();
        }

        if (connectivityReceiver != null) {
            registerReceiver(connectivityReceiver, makeConnectivityIntentFilter());
        }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (mSceneView != null) {
            mSceneView.dispose();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

     @Override
     public boolean onOptionsItemSelected(MenuItem item) {
         int id = item.getItemId();
         if (id == R.id.sync) {
             if (isConnected) {
                 showProgressDialog();
                 clearUI();
                 createGeolocateUI3D();
                 hideProgressDialog();
                 return true;
             } else {
                 Toast.makeText(this, getApplication().getString(R.string.no_internet), Toast.LENGTH_LONG).show();
             }
         }

         // Geolocate test button
         if (id == R.id.test) {
             geoLocate = new GeoLocate();
             geoLocate.calculateViewRay(scene.getBaseSurface(), viewpointTest, 4.4, 2, 241, 5000);
             geoLocate.showMetrics();
             addViewrayGraphics();
             Point lastPoint = geoLocate.getLastViewrayPoint();
             mSceneView.setViewpointCamera(new Camera(38.840922,
                     -9.163043,
                     50.0,
                     241,
                     80,
                     0));
             displayEstimationDialog(lastPoint);
         }

         if (id == R.id.app_settings) {
             Intent intent = new Intent(this, SettingsActivity.class);
             startActivityForResult(intent, REQUEST_SETTINGS_CHANGE);
             return true;
         }
         return super.onOptionsItemSelected(item);
     }


     @SuppressLint("MissingPermission")
     @Override
     public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (currentLocation != null) {
                    Log.i("*GPS*", "Location: " + String.valueOf(currentLocation.toString()));
                    Log.i("*GPS*", "GetAccuracy: " + String.valueOf(currentLocation.getAccuracy()));

                    double heading = mSceneView.getCurrentViewpointCamera().getHeading();
                    if (currentLocation.getBearing() != 0.0)
                        heading = currentLocation.getBearing();

                    mSceneView.setViewpointCamera(new Camera(currentLocation.getLatitude(),
                            currentLocation.getLongitude(),
                            300.0,
                            heading,
                            80.0,
                            0.0));
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA) {
            if (resultCode == Activity.RESULT_OK) {
                int azimuteAux = data.getIntExtra("azimuteAux", 0);
                double pitchAux = data.getDoubleExtra("pitchAux", 0.0);
                double latitude = data.getDoubleExtra("latitude", 0.0);
                double longitude = data.getDoubleExtra("longitude", 0.0);

                Log.i("azimuteAux", String.valueOf(azimuteAux));
                Log.i("pitchAux", String.valueOf(pitchAux));
                Log.i("latitude", String.valueOf(latitude));
                Log.i("longitude", String.valueOf(longitude));

                mSceneView.setViewpointCamera(new Camera(latitude,
                        longitude,
                        50.0,
                        azimuteAux,
                        pitchAux + 90,
                        0.0));

                Point userViewpoint = new Point(currentLocation.getLongitude(), currentLocation.getLatitude(), SpatialReference.create(4326));
                geoLocate = new GeoLocate();
                geoLocate.calculateViewRay(scene.getBaseSurface(), userViewpoint, pitchAux, USER_HEIGHT, azimuteAux, 5000);
                if (!geoLocate.hasFoundLocation()) {
                    Toast.makeText(this, getApplication().getString(R.string.error_message), Toast.LENGTH_SHORT).show();
                } else{
                    displayEstimationDialog(geoLocate.getLastViewrayPoint());
                    if (IS_DEBUG) {
                        logGeolocateResults(data, geoLocate);
                    }
                }
                addViewrayGraphics();
                geoLocate.showMetrics();
            } else {
                Log.e("geolocate_error","Something went wrong..");
            }
        }

        // Add attachment from the smartphone's camera app
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == RESULT_OK) {
                if (imageFile != null) {
                    imageFilename = imageFile.getName();
                    hasAttachment = true;
                    attachmentFilename.setText(imageFilename);
                }
            } else {
                // Handle error getting picture from the camera app
                Log.e("camera_error","Something went wrong. with the picture..");
            }
        }

        // Add attachment from the smartphone's gallery app
        if (requestCode == REQUEST_IMAGE_PICK) {
            if (resultCode == RESULT_OK) {
                // Get the URI of the selected image
                Uri imageUri = data.getData();
                // Save image URI in global variable
                imageAttachmentURI = imageUri;
                File file = getFileFromUri(imageUri);
                if (file != null) {
                    imageFilename = file.getName();
                    hasAttachment = true;
                    attachmentFilename.setText(imageFilename);
                }
            } else {
                // Handle error getting picture from the gallery app
                Log.e("gallery_error","Something went wrong. with the picture..");
            }
        }

        if (requestCode == REQUEST_SETTINGS_CHANGE) {
            //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            USER_HEIGHT = sharedPreferences.getInt("user_height", 2);
            IS_DEBUG = sharedPreferences.getBoolean("is_debug", false);
        }
    }

    /**
     * Converts the given input stream into a byte array.
     *
     * @param inputStream from an image
     * @return an array of bytes from the input stream
     * @throws IOException if input stream can't be read
     */
    private static byte[] bytesFromInputStream(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream()) {
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        }
    }

     private void logGeolocateResults(Intent sensors, GeoLocate geolocate) {
         DecimalFormat df = new DecimalFormat("#.#");
         String message = ""; // the log of the Geolocate algorithm

         message += getApplication().getString(R.string.total_distance) + " " + df.format(geolocate.getTotalDistance()) + "\n\n";
         message += getString(R.string.origin) + " " + String.valueOf(geolocate.getFirstViewrayPoint().getY()) + " " + String.valueOf(geolocate.getFirstViewrayPoint().getX()) + "\n\n";
         message += getString(R.string.destination) + " " + String.valueOf(geolocate.getLastViewrayPoint().getY()) + " " + String.valueOf(geolocate.getLastViewrayPoint().getX()) + "\n\n";
         message += getString(R.string.distance_target_error) + " " + df.format(geolocate.getDistanceToTargetError());

         message += " (";
         double distanceToTargetErrorPercentage = (geolocate.getDistanceToTargetError() / geolocate.getTotalDistance()) * 100;
         String roundedDistanceToTargetErrorPercentage = df.format(distanceToTargetErrorPercentage);
         message += roundedDistanceToTargetErrorPercentage + "%)" + "\n\n";

         String azimute = df.format(sensors.getIntExtra("azimuteAux", 0));
         String pitch = df.format(sensors.getDoubleExtra("pitchAux", 0.0));
         message += getApplication().getString(R.string.azimuth) + " " + azimute + "\n\n";
         message += getApplication().getString(R.string.pitch) + " " + pitch + "\n\n";

         Intent intent = new Intent();
         intent.setAction(Intent.ACTION_SEND);
         intent.putExtra(Intent.EXTRA_TEXT, message);
         intent.setType("text/plain");

         startActivity(Intent.createChooser(intent, getString(R.string.share_via)));
     }
 }