package com.example.elevation;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

public class Orientation implements SensorEventListener {

    public interface Listener {
        void onOrientationChanged(float pitch, float roll, float azimuth);
        void onAzimuthChanged(float pitch, float roll, int azimuth);
    }

    private static final int SENSOR_DELAY_MICROS = 50 * 1000; // 50ms

    private final SensorManager mSensorManager;
    private final Sensor mRotationSensor;
    private final Sensor mMagneticSensor;
    private final Sensor mAccelerometerSensor;
    private final Sensor mGravitySensor;
    private final WindowManager mWindowManager;

    private float[] mGeomagnetic = new float[3];
    private float[] R = new float[9];
    private float[] I = new float[9];

    boolean haveGravity = false;
    boolean haveAccelerometer = false;
    boolean haveMagnetometer = false;

    private int mLastAccuracy;
    private Listener mListener;

    float[] orientation = new float[3];
    float[] rMat = new float[9];

    float[] gData = new float[3]; // gravity or accelerometer
    float[] mData = new float[3]; // magnetometer
    float[] iMat = new float[9];

    public Orientation(SensorManager sensorManager, WindowManager windowManager) {
        mSensorManager = sensorManager;
        mWindowManager = windowManager;

        // Can be null if the sensor hardware is not available
        mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mMagneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
    }

    public void startListening(Listener listener) {
        if (mListener == listener) {
            return;
        }
        mListener = listener;
        if (mRotationSensor == null) {
            Log.w("WARNING","Rotation vector sensor not available; will not provide orientation data.");
            return;
        }
        if (mMagneticSensor == null) {
            Log.w("WARNING","Magnetic sensor not available; will not provide magnetic data.");
            return;
        }
        if (mGravitySensor == null) {
            Log.w("WARNING","mGravitySensor sensor not available; will not provide data.");
            return;
        }
        if (mAccelerometerSensor == null) {
            Log.w("WARNING","mAccelerometerSensor sensor not available; will not provide data.");
            return;
        }
        mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY_MICROS);
        this.haveMagnetometer = mSensorManager.registerListener(this, mMagneticSensor, SENSOR_DELAY_MICROS);

        this.haveGravity = mSensorManager.registerListener(this, mGravitySensor, SENSOR_DELAY_MICROS);
        this.haveAccelerometer = mSensorManager.registerListener(this, mAccelerometerSensor, SENSOR_DELAY_MICROS);

        // if there is a gravity sensor we do not need the accelerometer
        if( this.haveGravity )
            mSensorManager.unregisterListener( this, mAccelerometerSensor );

        if ( ( haveGravity || haveAccelerometer ) && haveMagnetometer ) {
            // ready to go
        } else {
            // unregister and stop
        }
    }

    public void stopListening() {
        mSensorManager.unregisterListener(this);
        mListener = null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (mLastAccuracy != accuracy) {
            mLastAccuracy = accuracy;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float alpha = 0.97f;

        if (mListener == null) {
            return;
        }
        if (mLastAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }
        if (event.sensor == mRotationSensor) {
            updateOrientation(event.values);
        }
        if (event.sensor == mMagneticSensor) {
            mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * event.values[0];
            mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * event.values[1];
            mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * event.values[2];
        }

        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
            // calculate th rotation matrix
            SensorManager.getRotationMatrixFromVector(rMat, event.values);

            // get the azimuth value (orientation[0]) in degree
            int mAzimuth = (int) ( Math.toDegrees(SensorManager.getOrientation(rMat, orientation )[0]) + 360 ) % 360;
            int pitch = (int) ( Math.toDegrees(SensorManager.getOrientation(rMat, orientation )[1] + Math.PI/2) + 360 ) % 360;
            int roll = (int) ( Math.toDegrees(SensorManager.getOrientation(rMat, orientation )[2]) + 360 ) % 360;

            mListener.onAzimuthChanged(pitch, roll, mAzimuth);
        }
    }

    private void updateOrientation(float[] rotationVector) {
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);

        // By default, remap the axes as if the front of the
        // device screen was the instrument panel.
        int worldAxisForDeviceAxisX = SensorManager.AXIS_X;
        int worldAxisForDeviceAxisY = SensorManager.AXIS_Z;

        // Adjust the rotation matrix for the device orientation
        int screenRotation = mWindowManager.getDefaultDisplay().getRotation();
        if (screenRotation == Surface.ROTATION_0) {
            worldAxisForDeviceAxisX = SensorManager.AXIS_X;
            worldAxisForDeviceAxisY = SensorManager.AXIS_Z;
        } else if (screenRotation == Surface.ROTATION_90) {
            worldAxisForDeviceAxisX = SensorManager.AXIS_Z;
            worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_X;
        } else if (screenRotation == Surface.ROTATION_180) {
            worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_X;
            worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_Z;
        } else if (screenRotation == Surface.ROTATION_270) {
            worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_Z;
            worldAxisForDeviceAxisY = SensorManager.AXIS_X;
        }

        float[] adjustedRotationMatrix = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisForDeviceAxisX,
                worldAxisForDeviceAxisY, adjustedRotationMatrix);

        // Transform rotation matrix into azimuth/pitch/roll
        float[] orientation = new float[3];
        SensorManager.getOrientation(adjustedRotationMatrix, orientation);

        // Convert radians to degrees
        float pitch = orientation[1] * -57;
        float roll = orientation[2] * -57;
        float azimuth = (float) ( Math.toDegrees(orientation[0]) + 360 ) % 360;

        mListener.onOrientationChanged(pitch, roll, azimuth);
    }
}