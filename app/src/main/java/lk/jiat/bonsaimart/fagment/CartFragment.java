package lk.jiat.bonsaimart.fagment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.jiat.bonsaimart.R;
import lk.jiat.bonsaimart.adapter.CartAdapter;
import lk.jiat.bonsaimart.databinding.FragmentCartBinding;
import lk.jiat.bonsaimart.db.CartDbHelper;
import lk.jiat.bonsaimart.model.CartItem;
import lk.jiat.bonsaimart.model.Product;

public class CartFragment extends Fragment {

    private static final String TAG = "CartFragment";

    private FragmentCartBinding binding;

    private final List<CartItem> cartItems = new ArrayList<>();

    private final Map<String, Product> productMap = new HashMap<>();

    private CartAdapter adapter;

    private FirebaseFirestore db;

    private FirebaseAuth auth;

    // ============================================================
    // SQLITE DATABASE HELPER
    // ============================================================
    private CartDbHelper cartDbHelper;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentCartBinding.inflate(
                inflater,
                container,
                false
        );

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {

        super.onViewCreated(view, savedInstanceState);

        // Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // ============================================================
        // INITIALIZE SQLITE DATABASE
        // ============================================================
        cartDbHelper = new CartDbHelper(requireContext());

        setupRecyclerView();

        setupProceedButton();

        checkUserAndLoadCart();
    }

    /*
     * CHECK LOGIN USER
     */
    private void checkUserAndLoadCart() {

        if (auth.getCurrentUser() == null) {

            cartItems.clear();
            productMap.clear();

            // ============================================================
            // LOAD FROM SQLITE WHEN NO USER
            // ============================================================
            loadFromSqliteOnly();

            updateTotal();

            Toast.makeText(
                    requireContext(),
                    "Please login to view your cart",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        String uid = auth.getCurrentUser().getUid();

        loadCartData(uid);
    }

    /*
     * SETUP RECYCLER VIEW
     */
    private void setupRecyclerView() {

        if (binding == null) {
            return;
        }

        binding.cartCartItems.setLayoutManager(
                new LinearLayoutManager(requireContext())
        );

        adapter = new CartAdapter(
                cartItems,
                productMap
        );

        /*
         * QUANTITY CHANGE
         */
        adapter.setOnQuantityChangeListener(cartItem -> {

            updateTotal();

            updateQuantityInFirestore(cartItem);
        });

        /*
         * REMOVE ITEM
         */
        adapter.setOnRemoveListener(position -> {

            removeCartItem(position);
        });

        // ============================================================
        // LONG PRESS LISTENER
        // ============================================================
        adapter.setOnItemLongClickListener(position -> {
            // Already handled in adapter (dialog shown there)
            Log.d(TAG, "Long press on item: " + position);
        });

        // ============================================================
        // DOUBLE TAP LISTENER - Open Product Detail
        // ============================================================
        adapter.setOnItemDoubleTapListener(position -> {
            if (position < 0 || position >= cartItems.size()) return;

            CartItem item = cartItems.get(position);
            if (item.getProductId() == null) return;

            Toast.makeText(requireContext(),
                    "Opening product details...",
                    Toast.LENGTH_SHORT).show();

            Bundle bundle = new Bundle();
            bundle.putString("productId", item.getProductId());

            ProductDetailFragment fragment = new ProductDetailFragment();
            fragment.setArguments(bundle);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        binding.cartCartItems.setAdapter(adapter);

        attachSwipeToDelete();
    }

    /*
     * PROCEED BUTTON
     */
    private void setupProceedButton() {

        binding.cartBtnProceed.setOnClickListener(v -> {

            if (cartItems.isEmpty()) {

                Toast.makeText(
                        requireContext(),
                        "Your cart is empty",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.fragment_container,
                            new CheckoutFragment()
                    )
                    .addToBackStack(null)
                    .commit();
        });
    }

    /*
     * ============================================================
     * LOAD FROM SQLITE ONLY (OFFLINE MODE)
     * ============================================================
     */
    private void loadFromSqliteOnly() {

        List<CartItem> localItems = cartDbHelper.getAllCartItems();

        if (localItems.isEmpty()) {
            return;
        }

        cartItems.clear();
        cartItems.addAll(localItems);

        // Load products from Firestore (if online)
        List<String> productIds = new ArrayList<>();
        for (CartItem item : localItems) {
            if (item.getProductId() != null) {
                productIds.add(item.getProductId());
            }
        }

        if (!productIds.isEmpty()) {
            fetchProductDetails(productIds);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    /*
     * ============================================================
     * LOAD CART (FIREBASE + SQLITE)
     * ============================================================
     */
    private void loadCartData(String uid) {

        // ------------------------------------------------------------
        // STEP 1: Load from SQLite first (Instant)
        // ------------------------------------------------------------
        List<CartItem> localItems = cartDbHelper.getAllCartItems();

        if (!localItems.isEmpty()) {

            Log.d(TAG, "Loaded " + localItems.size() + " items from SQLite");

            cartItems.clear();
            cartItems.addAll(localItems);
            adapter.notifyDataSetChanged();
            updateTotal();
        }

        // ------------------------------------------------------------
        // STEP 2: Load from Firebase (Background)
        // ------------------------------------------------------------
        db.collection("users")
                .document(uid)
                .collection("cart")
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (!isAdded() || binding == null) {
                        return;
                    }

                    cartItems.clear();
                    productMap.clear();

                    // Clear SQLite before re-saving (fresh sync)
                    cartDbHelper.clearCart();

                    List<String> productIds = new ArrayList<>();

                    for (DocumentSnapshot document :
                            querySnapshot.getDocuments()) {

                        CartItem cartItem =
                                document.toObject(CartItem.class);

                        if (cartItem == null) {
                            continue;
                        }

                        /*
                         * Firestore document ID
                         */
                        cartItem.setDocumentId(
                                document.getId()
                        );

                        /*
                         * Safety check
                         */
                        if (cartItem.getProductId() == null
                                || cartItem.getProductId().isEmpty()) {
                            continue;
                        }

                        if (cartItem.getQuantity() <= 0) {
                            cartItem.setQuantity(1);
                        }

                        cartItems.add(cartItem);

                        // ============================================================
                        // SAVE TO SQLITE FOR OFFLINE USE
                        // ============================================================
                        cartDbHelper.addToCart(
                                cartItem.getProductId(),
                                cartItem.getQuantity(),
                                null
                        );

                        if (!productIds.contains(
                                cartItem.getProductId()
                        )) {

                            productIds.add(
                                    cartItem.getProductId()
                            );
                        }
                    }

                    /*
                     * CART EMPTY
                     */
                    if (cartItems.isEmpty()) {

                        updateTotal();

                        updateEmptyCartUI();

                        return;
                    }

                    /*
                     * FETCH PRODUCTS
                     */
                    fetchProductDetails(
                            productIds
                    );

                })
                .addOnFailureListener(e -> {

                    Log.e(
                            TAG,
                            "Firebase failed, using SQLite cache",
                            e
                    );

                    if (!isAdded()) {
                        return;
                    }

                    // ============================================================
                    // FALLBACK TO SQLITE
                    // ============================================================
                    if (cartItems.isEmpty()) {
                        loadFromSqliteOnly();
                    }

                    Toast.makeText(
                            requireContext(),
                            "Offline mode - showing cached cart",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    /*
     * FETCH PRODUCTS
     *
     * Firestore whereIn supports limited number of values,
     * therefore split into batches.
     */
    private void fetchProductDetails(
            List<String> productIds
    ) {

        if (productIds.isEmpty()) {

            updateTotal();

            return;
        }

        List<List<String>> batches =
                new ArrayList<>();

        for (int i = 0;
             i < productIds.size();
             i += 10) {

            batches.add(
                    new ArrayList<>(
                            productIds.subList(
                                    i,
                                    Math.min(
                                            i + 10,
                                            productIds.size()
                                    )
                            )
                    )
            );
        }

        final int[] completedBatches = {0};

        for (List<String> batch : batches) {

            db.collection("products")
                    .whereIn("productId", batch)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {

                        if (!isAdded() || binding == null) {
                            return;
                        }

                        for (DocumentSnapshot document :
                                querySnapshot.getDocuments()) {

                            Product product =
                                    document.toObject(
                                            Product.class
                                    );

                            if (product != null
                                    && product.getProductId() != null) {

                                productMap.put(
                                        product.getProductId(),
                                        product
                                );
                            }
                        }

                        completedBatches[0]++;

                        if (completedBatches[0]
                                == batches.size()) {

                            /*
                             * ALL PRODUCTS LOADED
                             */
                            adapter.notifyDataSetChanged();

                            updateTotal();

                            updateEmptyCartUI();
                        }

                    })
                    .addOnFailureListener(e -> {

                        Log.e(
                                TAG,
                                "Error fetching products",
                                e
                        );

                        completedBatches[0]++;

                        if (completedBatches[0]
                                == batches.size()) {

                            if (isAdded()) {

                                adapter.notifyDataSetChanged();

                                updateTotal();

                                updateEmptyCartUI();
                            }
                        }
                    });
        }
    }

    /*
     * ============================================================
     * UPDATE QUANTITY (FIREBASE + SQLITE)
     * ============================================================
     */
    private void updateQuantityInFirestore(
            CartItem cartItem
    ) {

        // ------------------------------------------------------------
        // UPDATE SQLITE FIRST (Instant)
        // ------------------------------------------------------------
        cartDbHelper.updateQuantity(
                cartItem.getProductId(),
                cartItem.getQuantity()
        );

        if (auth.getCurrentUser() == null) {
            return;
        }

        if (cartItem.getDocumentId() == null) {
            return;
        }

        String uid =
                auth.getCurrentUser().getUid();

        // ------------------------------------------------------------
        // UPDATE FIREBASE
        // ------------------------------------------------------------
        db.collection("users")
                .document(uid)
                .collection("cart")
                .document(cartItem.getDocumentId())
                .update(
                        "quantity",
                        cartItem.getQuantity()
                )
                .addOnSuccessListener(unused -> {

                    Log.d(
                            TAG,
                            "Quantity updated successfully"
                    );

                })
                .addOnFailureListener(e -> {

                    Log.e(
                            TAG,
                            "Firebase update failed, SQLite saved locally",
                            e
                    );
                });
    }

    /*
     * ============================================================
     * REMOVE CART ITEM (FIREBASE + SQLITE)
     * ============================================================
     */
    private void removeCartItem(int position) {

        if (position < 0
                || position >= cartItems.size()) {
            return;
        }

        CartItem item =
                cartItems.get(position);

        if (item.getDocumentId() == null) {
            return;
        }

        if (auth.getCurrentUser() == null) {
            return;
        }

        String uid =
                auth.getCurrentUser().getUid();

        // ------------------------------------------------------------
        // DELETE FROM SQLITE FIRST
        // ------------------------------------------------------------
        cartDbHelper.removeItem(item.getProductId());

        // ------------------------------------------------------------
        // DELETE FROM FIREBASE
        // ------------------------------------------------------------
        db.collection("users")
                .document(uid)
                .collection("cart")
                .document(item.getDocumentId())
                .delete()
                .addOnSuccessListener(unused -> {

                    if (!isAdded() || binding == null) {
                        return;
                    }

                    cartItems.remove(position);

                    adapter.notifyItemRemoved(position);

                    updateTotal();

                    updateEmptyCartUI();

                    Toast.makeText(
                            requireContext(),
                            "Item removed from cart",
                            Toast.LENGTH_SHORT
                    ).show();

                })
                .addOnFailureListener(e -> {

                    Log.e(
                            TAG,
                            "Firebase delete failed, removed locally",
                            e
                    );

                    // Still remove from UI (SQLite already deleted)
                    if (isAdded() && binding != null) {

                        cartItems.remove(position);
                        adapter.notifyItemRemoved(position);
                        updateTotal();
                        updateEmptyCartUI();
                    }
                });
    }

    /*
     * CALCULATE TOTAL
     */
    private void updateTotal() {

        double total = 0.0;

        for (CartItem item : cartItems) {

            Product product =
                    productMap.get(
                            item.getProductId()
                    );

            if (product != null) {

                total +=
                        product.getPrice()
                                * item.getQuantity();
            }
        }

        if (binding != null) {

            binding.cartTextTotal.setText(
                    String.format(
                            Locale.US,
                            "LKR %,.2f",
                            total
                    )
            );
        }
    }

    /*
     * EMPTY CART UI
     */
    private void updateEmptyCartUI() {

        if (binding == null) {
            return;
        }

        if (cartItems.isEmpty()) {

            binding.cartCartItems.setVisibility(
                    View.GONE
            );

            binding.cartBtnProceed.setEnabled(
                    false
            );

        } else {

            binding.cartCartItems.setVisibility(
                    View.VISIBLE
            );

            binding.cartBtnProceed.setEnabled(
                    true
            );
        }
    }

    @Override
    public void onDestroyView() {

        super.onDestroyView();

        binding = null;
    }

    // ============================================================
// ATTACH SWIPE TO DELETE GESTURE
// ============================================================
    private void attachSwipeToDelete() {

        ItemTouchHelper.SimpleCallback swipeCallback =
                new ItemTouchHelper.SimpleCallback(
                        0,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
                ) {

                    @Override
                    public boolean onMove(@NonNull RecyclerView recyclerView,
                                          @NonNull RecyclerView.ViewHolder viewHolder,
                                          @NonNull RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder,
                                         int direction) {

                        int position = viewHolder.getBindingAdapterPosition();

                        if (position == RecyclerView.NO_POSITION) return;

                        // Confirm delete
                        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("🗑️ Remove Item?")
                                .setMessage("Swipe detected. Remove this item from cart?")
                                .setPositiveButton("REMOVE", (dialog, which) -> {
                                    removeCartItem(position);
                                })
                                .setNegativeButton("CANCEL", (dialog, which) -> {
                                    // Restore item in RecyclerView
                                    adapter.notifyItemChanged(position);
                                })
                                .setCancelable(false)
                                .show();
                    }
                };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
        itemTouchHelper.attachToRecyclerView(binding.cartCartItems);
    }
}