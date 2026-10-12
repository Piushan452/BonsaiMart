package lk.jiat.bonsaimart.fagment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import lk.jiat.bonsaimart.R;
import lk.jiat.bonsaimart.util.PermissionHelper;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 100;
    private GoogleMap mMap;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Hide bottom navigation (optional, clean look)
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.bottom_navigation_view);
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
        }

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // ============================================================
        // BONSAIMART STORE LOCATIONS
        // ============================================================
        LatLng colombo = new LatLng(6.9271, 79.8612);
        LatLng kandy = new LatLng(7.2906, 80.6337);
        LatLng galle = new LatLng(6.0535, 80.2210);

        // Add markers
        mMap.addMarker(new MarkerOptions()
                .position(kandy)
                .title("BonsaiMart Kandy")
                .snippet("Main Store - Open 9AM-9PM"));

        mMap.addMarker(new MarkerOptions()
                .position(colombo)
                .title("BonsaiMart Colombo")
                .snippet("Branch Store - Open 9AM-8PM"));

        mMap.addMarker(new MarkerOptions()
                .position(galle)
                .title("BonsaiMart Galle")
                .snippet("Branch Store - Open 10AM-7PM"));

        // Move camera to Colombo
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(colombo, 10f));

        // ============================================================
        // ENABLE CURRENT LOCATION
        // ============================================================
        enableMyLocation();
    }

    private void enableMyLocation() {

        if (PermissionHelper.hasLocationPermission(requireContext())) {
            // Permission granted
            if (mMap != null) {
                try {
                    mMap.setMyLocationEnabled(true);
                } catch (SecurityException e) {
                    Log.e("MapFragment", "Location permission error", e);
                }
            }
        } else {
            // Request permission
            PermissionHelper.requestLocationPermission(requireActivity());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionHelper.REQUEST_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (mMap != null) {
                    try {
                        mMap.setMyLocationEnabled(true);
                    } catch (SecurityException e) {
                        Log.e("MapFragment", "Location error", e);
                    }
                }

                Toast.makeText(requireContext(),
                        "Location permission granted",
                        Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(requireContext(),
                        "Location permission denied. Map will show stores only.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // Show bottom navigation when leaving fragment
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.bottom_navigation_view);
            if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Hide bottom navigation when in map
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.bottom_navigation_view);
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
        }
    }
}