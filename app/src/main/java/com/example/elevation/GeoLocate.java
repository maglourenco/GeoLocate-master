package com.example.elevation;

import android.util.Log;
import com.esri.arcgisruntime.UnitSystem;
import com.esri.arcgisruntime.geoanalysis.LocationDistanceMeasurement;
import com.esri.arcgisruntime.geoanalysis.LocationLineOfSight;
import com.esri.arcgisruntime.geometry.AngularUnit;
import com.esri.arcgisruntime.geometry.AngularUnitId;
import com.esri.arcgisruntime.geometry.GeodeticCurveType;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.LinearUnit;
import com.esri.arcgisruntime.geometry.LinearUnitId;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.mapping.Surface;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class GeoLocate {
    //ToDo - only for assessing the distance error - remove afterwards
    // Monte grande
    //private Point targetPoint = new Point(-9.196119, 38.825730, 220.0, SpatialReference.create(4326));

    // Monte em frente à casa da Póvoa - esquina da esquerda
    private Point targetPoint = new Point(-9.145614, 38.810696, 93, SpatialReference.create(4326));

    // The curve's maximum value
    private int L = 15;
    // The logistic growth rate or steepness of the curve
    private double k = 0.9;
    // The x value of the sigmoid's midpoint
    private int x0 = 6;

    // Threshold to assume the viewray intersects ground
    private double VIEWRAY_GROUND_THRESHOLD = 1.0;

    private ArrayList<Point> viewraySteps;
    private LocationLineOfSight locationLineOfSight;
    private Point mInitPoint;
    private double distanceToTarget;
    private boolean hasFoundLocation;
    private double initElevation;
    private double observerHeight;
    private int nrIterations;
    private double totalDistance;
    private ArrayList<Double> distanceStep;
    private ArrayList<Double> newPointHeight;
    private ArrayList<Double> currentElevation;
    private ArrayList<Double> diffElevation;

    private double getInitToFinalPointsDistance() {
        if (viewraySteps.size() > 0) {
            return GeometryEngine.distanceGeodetic(this.mInitPoint,
                    viewraySteps.get(viewraySteps.size() - 1), new LinearUnit(LinearUnitId.METERS), new AngularUnit(AngularUnitId.DEGREES),
                    GeodeticCurveType.GEODESIC).getDistance();
        } else
            return -1;
    }

    public double getDistanceToTargetError() { return this.distanceToTarget;}

    public LocationLineOfSight getLineOfSight() {
        return this.locationLineOfSight;
    }

    public ArrayList<Point> getViewraySteps() {
        return this.viewraySteps;
    }

    public Point getFirstViewrayPoint() { return viewraySteps.get(0); }

    public Point getLastViewrayPoint(){
        return viewraySteps.get(viewraySteps.size()-1);
    }

    public double getTotalDistance() {
        return this.totalDistance;
    }

    public boolean hasFoundLocation() {
        return this.hasFoundLocation;
    }

    public GeoLocate() {
        viewraySteps = new ArrayList<>();
        locationLineOfSight = null;
        mInitPoint = null;
        distanceToTarget = 0.0;
        hasFoundLocation = true;
        totalDistance = 0.0;
        nrIterations = 1;
        distanceStep = new ArrayList<>();
        newPointHeight = new ArrayList<>();
        currentElevation = new ArrayList<>();
        diffElevation = new ArrayList<>();
    }

    /**
     * calculateViewRay - calculates the user's view ray and intersect it with a DEM to determine there the user is looking
     * @param elevationSurface - Digital Elevation Model (DEM)
     * @param initPoint - device's initial location (lat:lng)
     * @param angle - device's pitch (º) - obtained through the gyroscope (hardcoded to 4.4º)
     * @param height - device's height (m) - (hardcoded to 2m)
     * @param azimuth - device's azimuth (º) - obtained through the compass (hardcoded to 241º)
     * @param maxDistance - max distance of the view ray (m) - (hardcoded to 5,000m)
     */
    public void calculateViewRay(Surface elevationSurface, Point initPoint, double angle, int height, int azimuth, int maxDistance) {
        this.mInitPoint = initPoint;

        try {
            //ToDo: Assumed initial elevation difference of 2m (hypothetical distance from the smartphone to ground)
            double elevationDiff = height;

            // Get initial elevation
            this.initElevation = elevationSurface.getElevationAsync(initPoint).get();

            // Create observer point
            Point observer = new Point(initPoint.getX(), initPoint.getY(), this.initElevation + height, SpatialReference.create(4326));
            this.observerHeight = observer.getZ();

            while (true) {
                // Calculate next step size
                double distanceStep = calculateDistanceStepViewRay(elevationDiff);
                totalDistance += distanceStep;
                this.distanceStep.add(calculateDistanceStepViewRay(elevationDiff));

                // Calculate the new point vertically and horizontally moved
                Point newPoint = calculateNextStepPoint(observer, angle, totalDistance, azimuth);
                this.newPointHeight.add(newPoint.getZ());

                // Determine the elevation of this new point
                Double currentElevation = elevationSurface.getElevationAsync(newPoint).get();
                this.currentElevation.add(currentElevation.doubleValue());

                // Calculate the elevation difference from the viewray and ground
                // Used to calculate the step size in the viewray in the next iteration
                elevationDiff = newPoint.getZ() - currentElevation.doubleValue();
                this.diffElevation.add(elevationDiff);

                // Add the new viewray point to the viewray's list of points
                viewraySteps.add(newPoint);

                // When the elevation difference is < 1m - intersects the ground
                if (elevationDiff < VIEWRAY_GROUND_THRESHOLD) {
                    distanceToTarget = GeometryEngine.distanceGeodetic(newPoint,
                            targetPoint, new LinearUnit(LinearUnitId.METERS), new AngularUnit(AngularUnitId.DEGREES),
                            GeodeticCurveType.GEODESIC).getDistance();
                    // create a new line-of-sight analysis with observer and target locations
                    locationLineOfSight = new LocationLineOfSight(observer, newPoint);
                    break;
                }

                // When the maximum distance is reached (hardcoded to 5km) - no intersection with the ground
                if (totalDistance >= maxDistance) {
                    Log.i("DEBUG: Error",  "Não foi possível obter a localização");
                    hasFoundLocation = false;
                    LocationDistanceMeasurement distanceMeasurement = new LocationDistanceMeasurement(newPoint,
                            targetPoint);
                    distanceMeasurement.setUnitSystem(UnitSystem.METRIC);
                    // create a new line of sight analysis with observer and target locations
                    locationLineOfSight = new LocationLineOfSight(observer, newPoint);
                    break;
                }
                nrIterations++;
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e("ELEVATION ERROR", "Unable to get elevation data from the raster");
        }
    }

    /**
     * calculateDistanceStepViewRay - calculate the step size in the viewray using the custom logistic function developed
     * L (curve's maximum value) - 120
     * k (logistic growth rate) - 0.9
     * x0 (sigmoid's midpoint) - 6
     * @param elevationDiff - elevation difference from the viewray to the ground (from DEM)
     * @return the viewray's step size
     */
    public double calculateDistanceStepViewRay(double elevationDiff) {
        return L / (1 + Math.exp(-k * (elevationDiff - x0)));
    }

    /**
     * calculateNextStepPoint - after calculating the viewray's distance step size, calculate the viewray's next step point location
     * @param observer initial position in the viewray
     * @param angle angle to be shifted
     * @param totalDistance total distance to the next point
     * @param azimuth azimuth to be shifted
     * @return the next viewray step point according to the distance step size, and the previous pitch angle and azimuth
     */
    public Point calculateNextStepPoint(Point observer, double angle, double totalDistance, double azimuth){
        // Get horizontal distance
        double hd = Math.cos(Math.toRadians(angle)) * totalDistance;

        // Get vertical distance
        double vd = Math.sin(Math.toRadians(angle)) * totalDistance;

        // Get the horizontally advanced point
        Point horizontalPoint = GeometryEngine.moveGeodetic(observer, hd, new LinearUnit(LinearUnitId.METERS), azimuth, new AngularUnit(AngularUnitId.DEGREES), GeodeticCurveType.GEODESIC);

        // Calculate the new point vertically and horizontally moved
        Point newPoint = new Point(horizontalPoint.getX(), horizontalPoint.getY(), observer.getZ() + vd, SpatialReference.create(4326));

        return newPoint;
    }

    /**
     * showMetrics - shows metrics of the viewray calculation algorithm
     */
    public void showMetrics() {
        Log.i("*DEBUG*", "NR ITERATIONS: " + String.valueOf(this.nrIterations));
        Log.i("*DEBUG*", "INIT ELEVATION (m): " + String.valueOf(this.initElevation));
        Log.i("*DEBUG*", "OBSERVER'S HEIGHT (m): " + this.observerHeight);
        //ToDo - only for assessing the distance error - remove afterwards
        Log.i("*DEBUG*", "INIT TO TARGET DISTANCE (m):  " + getInitToFinalPointsDistance());
        Log.i("*DEBUG*", "ERROR DISTANCE TO TARGET (m): " + distanceToTarget);
        for (int i=0; i<nrIterations; i++) {
            Log.i("*DEBUG*", "***ITERATION NUMBER*** " + "\n" + String.valueOf(i+1));
            Log.i("*DEBUG*", "CURRENT VIEWRAY HEIGHT (m): " + this.newPointHeight.get(i));
            Log.i("*DEBUG*", "CURRENT GROUND HEIGHT (m): " + currentElevation.get(i));
            Log.i("*DEBUG*", "DIFF ELEVATION (m): " + diffElevation.get(i));
            Log.i("*DEBUG*", "DISTANCE STEP (m): " + distanceStep.get(i));
        }
    }
}

