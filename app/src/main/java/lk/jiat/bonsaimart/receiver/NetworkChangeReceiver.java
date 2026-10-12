package lk.jiat.bonsaimart.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

public class NetworkChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "NetworkReceiver";

    // ============================================================
    // STATIC VARIABLE
    // ============================================================
    private static boolean isConnected = true;
    private static OnNetworkChangeListener listener;

    // ============================================================
    // INTERFACE
    // ============================================================
    public interface OnNetworkChangeListener {
        void onNetworkChanged(boolean isConnected);
    }

    // ============================================================
    // LISTENER
    // ============================================================
    public static void setListener(OnNetworkChangeListener l) {
        listener = l;
    }

    public static void removeListener() {
        listener = null;
    }

    // ============================================================
    // STATIC METHOD
    // ============================================================
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return false;

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    public static boolean isConnected() {
        return isConnected;
    }

    // ============================================================
    // ON RECEIVE
    // ============================================================
    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null || intent.getAction() == null) {
            return;
        }

        if (!intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            return;
        }

        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return;

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean connected = activeNetwork != null && activeNetwork.isConnected();

        // State change වුනාම පමණක් notify කරන්න
        if (connected != isConnected) {

            isConnected = connected;

            if (connected) {
                Log.d(TAG, "✅ Back Online");
                Toast.makeText(context, "✅ Back Online", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "❌ You are Offline");
                Toast.makeText(context, "❌ You are Offline", Toast.LENGTH_SHORT).show();
            }

            // Activity එකට notify
            if (listener != null) {
                listener.onNetworkChanged(connected);
            }
        }
    }
}