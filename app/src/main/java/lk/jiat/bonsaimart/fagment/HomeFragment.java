package lk.jiat.bonsaimart.fagment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

import lk.jiat.bonsaimart.R;
import lk.jiat.bonsaimart.adapter.SectionAdapter;
import lk.jiat.bonsaimart.databinding.FragmentHomeBinding;
import lk.jiat.bonsaimart.model.Product;
import lk.jiat.bonsaimart.model.WeatherModel;
import lk.jiat.bonsaimart.network.WeatherApiService;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    // Kandy coordinates
    private static final String CITY_NAME = "Kandy";

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadWeather();
        loadAllProducts();
    }

    // ============================================================
    // LOAD WEATHER FROM API (Async)
    // ============================================================
    private void loadWeather() {

        // Show loading
        binding.weatherProgress.setVisibility(View.VISIBLE);
        binding.weatherTemp.setText("--°C");
        binding.weatherDesc.setText("Loading...");

        WeatherApiService.getWeather(requireContext(), CITY_NAME,
                new WeatherApiService.WeatherCallback() {
                    @Override
                    public void onSuccess(WeatherModel weather) {

                        // ✅ CRITICAL: Check if binding is null (Fragment destroyed)
                        if (!isAdded() || binding == null) {
                            Log.d(TAG, "Fragment destroyed, skipping weather update");
                            return;
                        }

                        binding.weatherProgress.setVisibility(View.GONE);

                        // Temperature
                        binding.weatherTemp.setText(
                                String.format(Locale.US, "%.0f°C", weather.getTemperature())
                        );

                        // Description (Capitalize)
                        String desc = weather.getDescription();
                        if (desc != null && !desc.isEmpty()) {
                            desc = desc.substring(0, 1).toUpperCase() + desc.substring(1);
                        }
                        binding.weatherDesc.setText(desc);

                        // City
                        binding.weatherCity.setText("🌤️ " + weather.getCityName() + " Weather");

                        // Humidity
                        binding.weatherHumidity.setText("💧 " + weather.getHumidity() + "%");

                        // Wind
                        binding.weatherWind.setText(
                                String.format(Locale.US, "💨 %.0f km/h", weather.getWindSpeed())
                        );

                        // Weather Icon (Emoji based on description)
                        binding.weatherIcon.setText(getWeatherEmoji(weather.getDescription()));

                        // Bonsai Tip
                        binding.weatherTip.setText(getBonsaiTip(weather.getTemperature(),
                                weather.getHumidity()));
                    }

                    @Override
                    public void onError(String message) {

                        // ✅ CRITICAL: Check if binding is null
                        if (!isAdded() || binding == null) {
                            Log.d(TAG, "Fragment destroyed, skipping error update");
                            return;
                        }

                        Log.e(TAG, "Weather error: " + message);

                        binding.weatherProgress.setVisibility(View.GONE);
                        binding.weatherTemp.setText("--°C");
                        binding.weatherDesc.setText("Weather unavailable");
                        binding.weatherTip.setText("Check your internet connection");
                    }
                });
    }

    // ============================================================
    // WEATHER EMOJI BASED ON DESCRIPTION
    // ============================================================
    private String getWeatherEmoji(String description) {
        if (description == null) return "🌡️";
        String desc = description.toLowerCase();

        if (desc.contains("clear") || desc.contains("sunny")) return "☀️";
        if (desc.contains("cloud") && desc.contains("few")) return "🌤️";
        if (desc.contains("cloud") && desc.contains("scattered")) return "⛅";
        if (desc.contains("cloud") && desc.contains("broken")) return "☁️";
        if (desc.contains("rain") && desc.contains("light")) return "🌦️";
        if (desc.contains("rain")) return "🌧️";
        if (desc.contains("thunder")) return "⛈️";
        if (desc.contains("snow")) return "❄️";
        if (desc.contains("mist") || desc.contains("fog")) return "🌫️";

        return "🌤️";
    }

    // ============================================================
    // BONSAI CARE TIP BASED ON WEATHER
    // ============================================================
    private String getBonsaiTip(double temp, int humidity) {
        String tip;

        if (temp > 32) {
            tip = "🌡️ Hot! Water your bonsai twice today.";
        } else if (temp < 15) {
            tip = "❄️ Cold! Bring bonsai indoors if possible.";
        } else if (humidity > 80) {
            tip = "💧 High humidity. Watch out for fungal issues.";
        } else if (humidity < 40) {
            tip = "🌵 Low humidity. Mist your bonsai leaves.";
        } else {
            tip = "✨ Perfect weather for bonsai care!";
        }

        return "🌱 " + tip;
    }

    // ============================================================
    // LOAD PRODUCTS FROM FIREBASE
    // ============================================================
    private void loadAllProducts() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("products")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    // ✅ CRITICAL: Check if binding is null (Fragment destroyed)
                    if (!isAdded() || binding == null) {
                        Log.d(TAG, "Fragment destroyed, skipping products update");
                        return;
                    }

                    if (!queryDocumentSnapshots.isEmpty()) {

                        List<Product> products =
                                queryDocumentSnapshots.toObjects(Product.class);

                        GridLayoutManager gridLayoutManager =
                                new GridLayoutManager(getContext(), 2);

                        binding.homeAllProductsSection.setLayoutManager(gridLayoutManager);

                        SectionAdapter adapter =
                                new SectionAdapter(products, product -> {

                                    // ✅ Check again before navigation
                                    if (!isAdded() || binding == null) return;

                                    Bundle bundle = new Bundle();
                                    bundle.putString("productId", product.getProductId());

                                    ProductDetailFragment fragment =
                                            new ProductDetailFragment();
                                    fragment.setArguments(bundle);

                                    getParentFragmentManager().beginTransaction()
                                            .replace(R.id.fragment_container, fragment)
                                            .addToBackStack(null)
                                            .commit();
                                });

                        binding.homeAllProductsSection.setAdapter(adapter);
                    }
                })
                .addOnFailureListener(e -> {
                    // ✅ Check if binding is null
                    if (!isAdded() || binding == null) return;

                    Log.e(TAG, "Failed to load products", e);
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}