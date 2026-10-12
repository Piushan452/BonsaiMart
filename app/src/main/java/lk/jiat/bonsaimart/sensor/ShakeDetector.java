package lk.jiat.bonsaimart.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class ShakeDetector implements SensorEventListener {

    // ============================================================
    // SHAKE THRESHOLD
    // ============================================================
    private static final float SHAKE_THRESHOLD_GRAVITY = 2.7f;
    private static final int SHAKE_SLOP_TIME_MS = 500;
    private static final int SHAKE_COUNT_RESET_TIME_MS = 3000;

    private long mShakeTimestamp;
    private int mShakeCount;

    private OnShakeListener mListener;

    // ============================================================
    // CALLBACK INTERFACE
    // ============================================================
    public interface OnShakeListener {
        void onShake(int count);
    }

    public void setOnShakeListener(OnShakeListener listener) {
        this.mListener = listener;
    }

    // ============================================================
    // SENSOR CHANGE
    // ============================================================
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (mListener == null) return;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // ============================================================
            // CALCULATE GRAVITY FORCE
            // ============================================================
            float gX = x / android.hardware.SensorManager.GRAVITY_EARTH;
            float gY = y / android.hardware.SensorManager.GRAVITY_EARTH;
            float gZ = z / android.hardware.SensorManager.GRAVITY_EARTH;

            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            // ============================================================
            // SHAKE DETECTED
            // ============================================================
            if (gForce > SHAKE_THRESHOLD_GRAVITY) {

                final long now = System.currentTimeMillis();

                // Ignore shake events too close to each other
                if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                    return;
                }

                // Reset shake count if too much time has passed
                if (mShakeTimestamp + SHAKE_COUNT_RESET_TIME_MS < now) {
                    mShakeCount = 0;
                }

                mShakeTimestamp = now;
                mShakeCount++;

                // Notify listener
                mListener.onShake(mShakeCount);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    // ============================================================
    // RESET SHAKE COUNT
    // ============================================================
    public void resetShakeCount() {
        mShakeCount = 0;
        mShakeTimestamp = 0;
    }
}