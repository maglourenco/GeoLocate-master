package com.example.elevation.utils;

import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.RasterElevationSource;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.example.elevation.R;
import java.util.ArrayList;

public class UnusedMethodsStorage {
    private static UnusedMethodsStorage instance;

    private UnusedMethodsStorage() {
        // Private constructor to prevent instantiation from outside
    }

    public static UnusedMethodsStorage getInstance() {
        if (instance == null) {
            instance = new UnusedMethodsStorage();
        }
        return instance;
    }


//    public void createEstimationsFeatureLayer() {
//        // inflate MapView from layout
//        mSceneView = findViewById(R.id.sceneView);
//
//        // create a scene and add a basemap to it
//        ArcGISScene scene = new ArcGISScene(Basemap.createImagery());
//
//        // add the scene to the sceneview
//        mSceneView.setScene(scene);
//
//        // create feature layer with its service feature table
//        // create the service feature table
//        ServiceFeatureTable serviceFeatureTable = new ServiceFeatureTable(getResources().getString(R.string.feature_service));
//
//        // create the feature layer using the service feature table
//        mFeatureLayer = new FeatureLayer(serviceFeatureTable);
//
//        // add the layer to the map
//        scene.getOperationalLayers().add(mFeatureLayer);
//
//        // set the map to be displayed in the mapview
//        mSceneView.setScene(scene);
//    }

//    public void createTerrainFromLocalRaster() {
//        mSceneView = findViewById(R.id.sceneView);
//
//        // create a scene and add a basemap to it
//        ArcGISScene scene = new ArcGISScene(Basemap.createImagery());
//        Log.i("SPATIAL REF", scene.getSpatialReference().getWKText());
//
//        // add the scene to the sceneview
//        mSceneView.setScene(scene);
//
//        // specify the initial camera position
//        Camera camera = new Camera(36.525, -121.80, 300.0, 180, 80.0, 0.0);
//        mSceneView.setViewpointCamera(camera);
//
//        // raster package file paths
//        ArrayList<String> filePaths = new ArrayList<>();
//        filePaths.add(getExternalFilesDir(null) + "/MontereyElevation.dt2");
//
//        try {
//            // add an elevation source to the scene by passing the URI of the raster package to the constructor
//            RasterElevationSource rasterElevationSource = new RasterElevationSource(filePaths);
//
//            // add a listener to perform operations when the load status of the elevation source changes
//            rasterElevationSource.addLoadStatusChangedListener(loadStatusChangedEvent -> {
//
//                // when elevation source loads
//                if (loadStatusChangedEvent.getNewLoadStatus() == LoadStatus.LOADED) {
//                    // add the elevation source to the elevation sources of the scene
//                    mSceneView.getScene().getBaseSurface().getElevationSources().add(rasterElevationSource);
//                    Log.i("RASTER", "ELEVATION SOURCES: " + mSceneView.getScene().getBaseSurface().getElevationSources().get(0).getName());
//
//                    Log.i("RASTER", "Raster successfully loaded");
//                } else if (loadStatusChangedEvent.getNewLoadStatus() == LoadStatus.FAILED_TO_LOAD) {
//                    // notify user that the elevation source has failed to load
//                    Log.e("ERROR", "Error loading the raster");
//                }
//            });
//
//            // load the elevation source asynchronously
//            rasterElevationSource.loadAsync();
//        } catch (IllegalArgumentException e) {
//            // catch exception thrown by RasterElevationSource when a file is invalid/not found
//            Log.e("ERROR", "The elevation source has failed to load. Please ensure that the .dt2 has been pushed to the device\\'s storage.");
//        }
//    }

//    private void createTerrainExaggeration() {
//        // get a reference to the scene view
//        mSceneView = findViewById(R.id.sceneView);
//        // create a scene and add it to the scene view
//        ArcGISScene scene = new ArcGISScene(Basemap.createTopographic());
//        mSceneView.setScene(scene);
//
//        // add base surface for elevation data
//        final Surface surface = new Surface();
//        ArcGISTiledElevationSource elevationSource = new ArcGISTiledElevationSource("http://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer");
//        surface.getElevationSources().add(elevationSource);
//        scene.setBaseSurface(surface);
//
//        // add a camera and initial camera position
//        Point initialLocation = new Point(-119.94891542688772, 46.75792111605992, 3183, mSceneView.getSpatialReference());
//        Camera camera = new Camera(initialLocation, 0, 7, 70, 0);
//        mSceneView.setViewpointCamera(camera);
//
//        // create TextView to show SeekBar value
//        final TextView exaggerationTextView = findViewById(R.id.exaggerationValueTextView);
//        // create SeekBar
//        final SeekBar exaggerationSeekBar = findViewById(R.id.exaggerationSeekBar);
//        exaggerationSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
//                // disallow any progress value below 1
//                progress = Math.max(1, progress);
//                // set the text to SeekBar value
//                exaggerationTextView.setText(String.valueOf(progress));
//                // set exaggeration of surface to the value the user selected
//                surface.setElevationExaggeration(progress);
//            }
//
//            @Override public void onStartTrackingTouch(SeekBar seekBar) {
//
//            }
//
//            @Override public void onStopTrackingTouch(SeekBar seekBar) {
//                // disallow any progress value below 1
//                seekBar.setProgress(Math.max(1, seekBar.getProgress()));
//            }
//        });
//    }

//    private void createTerrainExaggerationFromLocalRaster() {
//        // get a reference to the scene view
//        mSceneView = findViewById(R.id.sceneView);
//        // create a scene and add it to the scene view
//        ArcGISScene scene = new ArcGISScene(Basemap.createImagery());
//        mSceneView.setScene(scene);
//
//        // raster package file paths
//        ArrayList<String> filePaths = new ArrayList<>();
//        //filePaths.add(getExternalFilesDir(null) + "/MontereyElevation.dt2");
//        filePaths.add(getExternalFilesDir(null) + "/ArcGIS_Online_30m.tif");
//
//        try {
//            // add an elevation source to the scene by passing the URI of the raster package to the constructor
//            RasterElevationSource rasterElevationSource = new RasterElevationSource(filePaths);
//
//            // add a listener to perform operations when the load status of the elevation source changes
//            rasterElevationSource.addLoadStatusChangedListener(loadStatusChangedEvent -> {
//                // when elevation source loads
//                if (loadStatusChangedEvent.getNewLoadStatus() == LoadStatus.LOADED) {
//                    // add the elevation source to the elevation sources of the scene
//                    mSceneView.getScene().getBaseSurface().getElevationSources().add(rasterElevationSource);
//                    surface = mSceneView.getScene().getBaseSurface();
//                    Log.i("RASTER", "Raster successfully loaded");
//
//                    // create feature layer with its service feature table
//                    // create the service feature table
//                    ServiceFeatureTable serviceFeatureTable = new ServiceFeatureTable("https://services3.arcgis.com/epYSERo2s3KvpxDO/ArcGIS/rest/services/GeoLocate_Wildfires/FeatureServer/0");
//                    mFeatureLayer = new FeatureLayer(serviceFeatureTable);
//                    mSceneView.getScene().getOperationalLayers().add(mFeatureLayer);
//
//                } else if (loadStatusChangedEvent.getNewLoadStatus() == LoadStatus.FAILED_TO_LOAD) {
//                    // notify user that the elevation source has failed to load
//                    Log.e("ERROR", "Error loading the raster");
//                }
//            });
//
//            // load the elevation source asynchronously
//            rasterElevationSource.loadAsync();
//        } catch (IllegalArgumentException e) {
//            // catch exception thrown by RasterElevationSource when a file is invalid/not found
//            Log.e("ERROR", "The elevation source has failed to load. Please ensure that the .dt2 has been pushed to the device\\'s storage.");
//        }
//
//        // add a camera and initial camera position
//        //Point initialLocation = new Point(-119.94891542688772, 46.75792111605992, 3183, mSceneView.getSpatialReference());
//        //Camera camera = new Camera(initialLocation, 0, 7, 70, 0);
//        //mSceneView.setViewpointCamera(camera);
//        //Camera camera = new Camera(36.525, -121.80, 300.0, 180, 80.0, 0.0);
//        Camera camera = new Camera(38.840922, -9.163043, 300.0, 190, 80.0, 0.0);
//        mSceneView.setViewpointCamera(camera);
//
//        // create TextView to show SeekBar value
//        final TextView exaggerationTextView = findViewById(R.id.exaggerationValueTextView);
//        // create SeekBar
//        final SeekBar exaggerationSeekBar = findViewById(R.id.exaggerationSeekBar);
//        exaggerationSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
//                // disallow any progress value below 1
//                progress = Math.max(1, progress);
//                // set the text to SeekBar value
//                exaggerationTextView.setText(String.valueOf(progress));
//                // set exaggeration of surface to the value the user selected
//                surface.setElevationExaggeration(progress);
//            }
//
//            @Override public void onStartTrackingTouch(SeekBar seekBar) {
//
//            }
//
//            @Override public void onStopTrackingTouch(SeekBar seekBar) {
//                // disallow any progress value below 1
//                seekBar.setProgress(Math.max(1, seekBar.getProgress()));
//            }
//        });
//    }
}
