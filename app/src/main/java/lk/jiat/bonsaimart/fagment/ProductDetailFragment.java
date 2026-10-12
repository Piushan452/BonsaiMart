package lk.jiat.bonsaimart.fagment;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lk.jiat.bonsaimart.R;
import lk.jiat.bonsaimart.activity.SignInActivity;
import lk.jiat.bonsaimart.adapter.ProductSliderAdapter;
import lk.jiat.bonsaimart.adapter.SectionAdapter;
import lk.jiat.bonsaimart.databinding.FragmentProductDetailBinding;
import lk.jiat.bonsaimart.db.CartDbHelper;
import lk.jiat.bonsaimart.model.CartItem;
import lk.jiat.bonsaimart.model.Product;
import lk.jiat.bonsaimart.sensor.ShakeDetector;

public class ProductDetailFragment extends Fragment {

    private static final String TAG = "ProductDetail";

    private FragmentProductDetailBinding binding;
    private String productId;
    private int quantity = 1;
    private int avbQuantity;

    // ============================================================
    // SQLITE DATABASE HELPER
    // ============================================================
    private CartDbHelper cartDbHelper;

    // ============================================================
    // SHAKE DETECTOR
    // ============================================================
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private ShakeDetector shakeDetector;
    private boolean isShaking = false; // Prevent multiple adds

    private Map<String, ChipGroup> attributeGroups = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            productId = getArguments().getString("productId");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProductDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ============================================================
        // INITIALIZE SQLITE DATABASE
        // ============================================================
        cartDbHelper = new CartDbHelper(requireContext());

        // ============================================================
        // INITIALIZE SHAKE DETECTOR
        // ============================================================
        setupShakeDetector();

        // ============================================================
        // HIDE BOTTOM NAVIGATION
        // ============================================================
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.bottom_navigation_view);
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
        }

        // ============================================================
        // BACK PRESS
        // ============================================================
        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        getParentFragmentManager().popBackStack();
                    }
                }
        );

        // ============================================================
        // LOAD PRODUCT FROM FIREBASE
        // ============================================================
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (productId != null) {
            db.collection("products")
                    .whereEqualTo("productId", productId)
                    .get().addOnSuccessListener(qds -> {
                        if (isAdded() && !qds.isEmpty()) {
                            Product product = qds.getDocuments().get(0).toObject(Product.class);
                            if (product == null) return;

                            ProductSliderAdapter adapter = new ProductSliderAdapter(product.getImages());
                            binding.productImageSlider.setAdapter(adapter);

                            binding.dotsIndicator.attachTo((binding.productImageSlider));

                            binding.title.setText(product.getTitle());
                            binding.description.setText(product.getDescription());
                            binding.ratingBar.setRating(product.getRating());
                            binding.price.setText("LKR " + product.getPrice());
                            binding.txtStock.setText(String.valueOf(product.getStockCount()));

                            avbQuantity = product.getStockCount();

                            // Reset attributes
                            binding.attributeContainer.removeAllViews();
                            attributeGroups.clear();

                            if (product.getAttributes() != null) {
                                product.getAttributes().forEach(attribute -> {
                                    renderAttribute(attribute, binding.attributeContainer);
                                });
                            }
                        }
                    });
        }

        // ============================================================
        // QUANTITY BUTTONS
        // ============================================================
        binding.btnMinus.setOnClickListener(v -> {
            if (quantity > 1) {
                quantity--;
                binding.productDetailQuantity.setText(String.valueOf(quantity));
            }
        });

        binding.btnPlus.setOnClickListener(v -> {
            if (quantity < avbQuantity) {
                quantity++;
                binding.productDetailQuantity.setText(String.valueOf(quantity));
            } else {
                Toast.makeText(getContext(), "Max stock reached", Toast.LENGTH_SHORT).show();
            }
        });

        // ============================================================
        // LOAD TOP SELLING PRODUCTS
        // ============================================================
        loadTopSellProduct();

        // ============================================================
        // ADD TO CART BUTTON
        // ============================================================
        binding.productDetailBtnAddToCart.setOnClickListener(v -> {
            addCurrentProductToCart();
        });
    }

    // ============================================================
    // SETUP SHAKE DETECTOR
    // ============================================================
    private void setupShakeDetector() {

        sensorManager = (SensorManager) requireActivity()
                .getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        if (accelerometer == null) {
            Log.d(TAG, "No accelerometer available on this device");
            return;
        }

        shakeDetector = new ShakeDetector();
        shakeDetector.setOnShakeListener(count -> {

            // Prevent multiple adds within short time
            if (isShaking) return;
            isShaking = true;

            Log.d(TAG, "Shake detected! Count: " + count);

            if (isAdded()) {
                Toast.makeText(requireContext(),
                        "📱 Shake detected! Adding to cart...",
                        Toast.LENGTH_SHORT).show();
            }

            // ============================================================
            // ADD TO CART
            // ============================================================
            addCurrentProductToCart();

            // Reset after 2 seconds (avoid double-add)
            if (binding != null) {
                binding.getRoot().postDelayed(() -> isShaking = false, 2000);
            } else {
                isShaking = false;
            }
        });
    }

    // ============================================================
    // ADD CURRENT PRODUCT TO CART (Button + Shake both use this)
    // ============================================================
    private void addCurrentProductToCart() {

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        // ------------------------------------------------------------
        // NOT LOGGED IN
        // ------------------------------------------------------------
        if (firebaseAuth.getCurrentUser() == null) {
            Intent intent = new Intent(getActivity(), SignInActivity.class);
            startActivity(intent);
            return;
        }

        // ------------------------------------------------------------
        // GET ATTRIBUTES
        // ------------------------------------------------------------
        List<CartItem.Attribute> attributes = getFinalSelections();

        if (attributes.size() < attributeGroups.size()) {
            Toast.makeText(getContext(),
                    "Please select all options",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (productId == null) return;

        // ------------------------------------------------------------
        // CREATE CART ITEM
        // ------------------------------------------------------------
        CartItem cartItem = new CartItem(productId, quantity, attributes);
        String uid = firebaseAuth.getCurrentUser().getUid();

        // ------------------------------------------------------------
        // SAVE TO SQLITE (Instant - Offline support)
        // ------------------------------------------------------------
        if (cartDbHelper != null) {
            cartDbHelper.addToCart(productId, quantity, null);
        } else {
            Log.e(TAG, "cartDbHelper is NULL!");
        }

        // ------------------------------------------------------------
        // SAVE TO FIREBASE (Cloud)
        // ------------------------------------------------------------
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).collection("cart")
                .document(productId)
                .set(cartItem)
                .addOnSuccessListener(unused -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                "✅ Product added to cart!",
                                Toast.LENGTH_SHORT).show();

                        createNotification(
                                "Product Added",
                                "Product has been added to your cart successfully."
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding to cart", e);
                    if (isAdded()) {
                        Toast.makeText(requireContext(),
                                "Saved locally (offline mode)",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ============================================================
    // LOAD TOP SELLING PRODUCTS
    // ============================================================
    private void loadTopSellProduct() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("products")
                .limit(10)
                .get()
                .addOnSuccessListener(qds -> {
                    if (isAdded() && !qds.isEmpty()) {
                        List<Product> products = qds.toObjects(Product.class);

                        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),
                                LinearLayoutManager.HORIZONTAL, false);

                        binding.productDetailTopSellSection.itemSectionContainer.setLayoutManager(layoutManager);

                        SectionAdapter adapter = new SectionAdapter(products, product -> {
                            Bundle bundle = new Bundle();
                            bundle.putString("productId", product.getProductId());

                            ProductDetailFragment productDetailFragment = new ProductDetailFragment();
                            productDetailFragment.setArguments(bundle);

                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, productDetailFragment)
                                    .addToBackStack(null)
                                    .commit();
                        });

                        binding.productDetailTopSellSection.itemSectionTitle.setText("Top Selling Products");
                        binding.productDetailTopSellSection.itemSectionContainer.setAdapter(adapter);
                    }
                });
    }

    // ============================================================
    // RENDER ATTRIBUTE CHIPS
    // ============================================================
    private void renderAttribute(Product.Attribute attribute, ViewGroup container) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 16, 0, 16);

        TextView label = new TextView(getContext());
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                120, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.gravity = Gravity.CENTER_VERTICAL;
        label.setLayoutParams(labelParams);
        label.setText(attribute.getName());
        label.setTextSize(14);
        row.addView(label);

        ChipGroup group = new ChipGroup(getContext());
        group.setSelectionRequired(true);
        group.setSingleSelection(true);

        attribute.getValues().forEach(value -> {
            Chip chip = new Chip(getContext());
            chip.setId(View.generateViewId());
            chip.setCheckable(true);
            chip.setTag(value);

            if ("color".equalsIgnoreCase(attribute.getType())) {
                try {
                    chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor(value)));
                    chip.setText("");
                } catch (Exception e) {
                    chip.setText(value);
                }
            } else {
                chip.setText(value);
            }

            group.addView(chip);
        });

        row.addView(group);
        container.addView(row);
        attributeGroups.put(attribute.getName(), group);
    }

    // ============================================================
    // GET FINAL SELECTIONS
    // ============================================================
    private List<CartItem.Attribute> getFinalSelections() {
        List<CartItem.Attribute> attributes = new ArrayList<>();
        for (Map.Entry<String, ChipGroup> entry : attributeGroups.entrySet()) {
            String attributeName = entry.getKey();
            ChipGroup chipGroup = entry.getValue();

            int checkedChipId = chipGroup.getCheckedChipId();
            if (checkedChipId != View.NO_ID) {
                Chip chip = chipGroup.findViewById(checkedChipId);
                if (chip != null && chip.getTag() != null) {
                    String value = chip.getTag().toString();
                    attributes.add(new CartItem.Attribute(attributeName, value));
                }
            }
        }
        return attributes;
    }

    // ============================================================
    // ON RESUME - Register Sensor
    // ============================================================
    @Override
    public void onResume() {
        super.onResume();

        // Hide bottom navigation
        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.bottom_navigation_view);
            if (bottomNav != null) bottomNav.setVisibility(View.GONE);
        }

        // Register accelerometer
        if (sensorManager != null && accelerometer != null && shakeDetector != null) {
            sensorManager.registerListener(
                    shakeDetector,
                    accelerometer,
                    SensorManager.SENSOR_DELAY_UI
            );
            Log.d(TAG, "Sensor registered");
        }
    }

    // ============================================================
    // ON PAUSE - Unregister Sensor (battery save)
    // ============================================================
    @Override
    public void onPause() {
        super.onPause();

        if (sensorManager != null && shakeDetector != null) {
            sensorManager.unregisterListener(shakeDetector);
            Log.d(TAG, "Sensor unregistered");
        }
    }

    // ============================================================
    // ON STOP - Show bottom navigation
    // ============================================================
    @Override
    public void onStop() {
        super.onStop();

        if (getActivity() != null) {
            View bottomNav = getActivity().findViewById(R.id.bottom_navigation_view);
            if (bottomNav != null) bottomNav.setVisibility(View.VISIBLE);
        }
    }

    // ============================================================
    // ON DESTROY VIEW
    // ============================================================
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ============================================================
    // CREATE NOTIFICATION
    // ============================================================
    private void createNotification(String title, String content) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> notification = new HashMap<>();
        notification.put("title", title);
        notification.put("content", content);
        notification.put("timestamp",
                com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("messages")
                .add(notification)
                .addOnSuccessListener(documentReference -> {
                    Log.d("NOTIFICATION", "Notification created");
                })
                .addOnFailureListener(e -> {
                    Log.e("NOTIFICATION", "Failed to create notification", e);
                });
    }
}