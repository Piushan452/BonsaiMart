package lk.jiat.bonsaimart.fagment;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import lk.jiat.bonsaimart.R;
import lk.jiat.bonsaimart.databinding.FragmentSettingBinding;

public class SettingFragment extends Fragment {

    private FragmentSettingBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingBinding.inflate(inflater, container, false);

        // ============================================================
        // SHOW WI-FI INFORMATION (Bluetooth/Wi-Fi)
        // ============================================================
        showNetworkInfo();
        binding.settingWifiInfo.setOnClickListener(v -> showNetworkInfo());

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Profile navigation
        binding.settingProfile.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Password navigation (same as profile as they are in the same fragment now)
        binding.settingPassword.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ProfileFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Notification toggle
        binding.settingSwitchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status = isChecked ? "Notifications Enabled" : "Notifications Disabled";
            Toast.makeText(getContext(), status, Toast.LENGTH_SHORT).show();
        });

        // Privacy Policy
        binding.settingPrivacy.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Privacy Policy coming soon!", Toast.LENGTH_SHORT).show();
        });

        // About
        binding.settingAbout.setOnClickListener(v -> {
            Toast.makeText(getContext(), "BonsaiMart Version 1.0.0", Toast.LENGTH_SHORT).show();
        });

        getActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ============================================================
    // SHOW WI-FI + NETWORK INFO
    // ============================================================
    private void showNetworkInfo() {

        try {
            // ========================================================
            // WI-FI INFO
            // ========================================================
            WifiManager wifiManager = (WifiManager)
                    requireContext().getApplicationContext()
                            .getSystemService(Context.WIFI_SERVICE);

            String wifiInfo = "Not connected";

            if (wifiManager != null && wifiManager.isWifiEnabled()) {

                WifiInfo wifiInfoObj = wifiManager.getConnectionInfo();

                if (wifiInfoObj != null && wifiInfoObj.getNetworkId() != -1) {

                    String ssid = wifiInfoObj.getSSID();
                    int rssi = wifiInfoObj.getRssi();
                    int speed = wifiInfoObj.getLinkSpeed();

                    // Remove quotes from SSID
                    if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                        ssid = ssid.substring(1, ssid.length() - 1);
                    }

                    wifiInfo = "📶 " + ssid
                            + "\n🔊 Signal: " + rssi + " dBm"
                            + "\n⚡ Speed: " + speed + " Mbps";

                    Log.d("SettingFragment", "Wi-Fi: " + ssid);
                }
            }

            // ========================================================
            // NETWORK TYPE
            // ========================================================
            ConnectivityManager cm = (ConnectivityManager)
                    requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);

            String networkType = "Unknown";

            if (cm != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

                    android.net.Network activeNetwork = cm.getActiveNetwork();

                    if (activeNetwork != null) {
                        NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);

                        if (caps != null) {
                            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                                networkType = "Wi-Fi";
                            } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                                networkType = "Mobile Data";
                            } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                                networkType = "Ethernet";
                            }
                        }
                    }
                } else {
                    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                    if (activeNetwork != null) {
                        networkType = activeNetwork.getTypeName();
                    }
                }
            }

            // ========================================================
            // SHOW IN TOAST (or add a TextView for display)
            // ========================================================
            String finalNetworkType = networkType;
            String finalWifiInfo = wifiInfo;

            Toast.makeText(getContext(),
                    "Network: " + finalNetworkType + "\n\n" + finalWifiInfo,
                    Toast.LENGTH_LONG).show();

            Log.d("SettingFragment", "Network Type: " + networkType);
            Log.d("SettingFragment", "Wi-Fi Info: " + wifiInfo);

        } catch (SecurityException e) {
            Log.e("SettingFragment", "Permission error", e);
            Toast.makeText(getContext(),
                    "Wi-Fi permission required",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e("SettingFragment", "Network info error", e);
        }
    }
}
