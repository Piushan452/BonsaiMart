package lk.jiat.bonsaimart.fagment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.jiat.bonsaimart.R;
import lk.jiat.bonsaimart.databinding.FragmentCheckoutBinding;
import lk.jiat.bonsaimart.listener.FirestoreCallback;
import lk.jiat.bonsaimart.model.CartItem;
import lk.jiat.bonsaimart.model.Order;
import lk.jiat.bonsaimart.model.Product;

import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;


public class CheckoutFragment extends Fragment {

    private FragmentCheckoutBinding binding;

    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;

    private double total = 0;

    private boolean paymentActive = false;

    // PayHere / Firestore Order ID
    private String paymentOrderId;


    // ============================================================
    // ON CREATE
    // ============================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();

        firebaseAuth = FirebaseAuth.getInstance();
    }


    // ============================================================
    // ON CREATE VIEW
    // ============================================================

    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentCheckoutBinding.inflate(
                inflater,
                container,
                false
        );


        // ========================================================
        // SHIPPING SECTION
        // ========================================================

        binding.shippingLayoutBtn.setOnClickListener(v -> {

            if (binding.shippingLayoutBody.getVisibility()
                    == View.GONE) {

                binding.shippingLayoutBody.setVisibility(
                        View.VISIBLE
                );

                binding.shippingLayoutBtn.setRotation(
                        180f
                );

            } else {

                binding.shippingLayoutBody.setVisibility(
                        View.GONE
                );

                binding.shippingLayoutBtn.setRotation(
                        0f
                );
            }
        });


        // ========================================================
        // BILLING SECTION
        // ========================================================

        binding.billingLayoutBtn.setOnClickListener(v -> {

            if (binding.billingLayoutBody.getVisibility()
                    == View.GONE) {

                binding.billingLayoutBody.setVisibility(
                        View.VISIBLE
                );

                binding.billingLayoutBtn.setRotation(
                        180f
                );

            } else {

                binding.billingLayoutBody.setVisibility(
                        View.GONE
                );

                binding.billingLayoutBtn.setRotation(
                        0f
                );
            }
        });


        // ========================================================
        // BILLING SAME AS SHIPPING
        // ========================================================

        binding.shippingDetailsCheckBilling
                .setOnCheckedChangeListener(
                        (buttonView, isChecked) -> {

                            if (isChecked) {

                                binding.billingLayout
                                        .setVisibility(
                                                View.GONE
                                        );

                            } else {

                                binding.billingLayout
                                        .setVisibility(
                                                View.VISIBLE
                                        );

                                binding.billingLayoutBody
                                        .setVisibility(
                                                View.VISIBLE
                                        );
                            }
                        }
                );


        return binding.getRoot();
    }


    // ============================================================
    // ON VIEW CREATED
    // ============================================================

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState
    ) {

        super.onViewCreated(
                view,
                savedInstanceState
        );


        final double shippingCost = 400;


        // ========================================================
        // LOAD CART
        // ========================================================

        getCartItems(cartItems -> {

            if (cartItems == null ||
                    cartItems.isEmpty()) {

                paymentActive = false;

                Toast.makeText(
                        requireContext(),
                        "Your cart is empty",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }


            ArrayList<String> productIds =
                    new ArrayList<>();


            for (CartItem cartItem : cartItems) {

                if (cartItem.getProductId() != null) {

                    productIds.add(
                            cartItem.getProductId()
                    );
                }
            }


            if (productIds.isEmpty()) {

                paymentActive = false;

                Toast.makeText(
                        requireContext(),
                        "No products found in cart",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }


            // ====================================================
            // GET PRODUCTS
            // ====================================================

            getProductsByIds(
                    productIds,
                    data -> {

                        double subTotal = 0;


                        for (CartItem cartItem :
                                cartItems) {

                            Product product =
                                    data.get(
                                            cartItem.getProductId()
                                    );


                            if (product != null) {

                                subTotal +=
                                        product.getPrice()
                                                * cartItem.getQuantity();
                            }
                        }


                        total =
                                subTotal
                                        + shippingCost;


                        binding.checkoutSubtotal
                                .setText(
                                        String.format(
                                                Locale.US,
                                                "LKR %,.2f",
                                                subTotal
                                        )
                                );


                        binding.checkoutShipping
                                .setText(
                                        String.format(
                                                Locale.US,
                                                "LKR %,.2f",
                                                shippingCost
                                        )
                                );


                        binding.checkoutTotal
                                .setText(
                                        String.format(
                                                Locale.US,
                                                "LKR %,.2f",
                                                total
                                        )
                                );


                        paymentActive = true;
                    }
            );
        });


        // ========================================================
        // PROCEED TO PAYMENT
        // ========================================================

        binding.checkoutBtnProceed
                .setOnClickListener(v -> {

                    startPayment();
                });
    }


    // ============================================================
    // START PAYHERE PAYMENT
    // ============================================================

    private void startPayment() {

        if (!paymentActive) {

            Toast.makeText(
                    requireContext(),
                    "Please wait. Loading cart...",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        if (firebaseAuth.getCurrentUser() == null) {

            Toast.makeText(
                    requireContext(),
                    "Please login first",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // ========================================================
        // GENERATE ORDER ID
        // ========================================================

        paymentOrderId =
                "ORD-" + System.currentTimeMillis();


        Log.d(
                "PAYHERE_DEBUG",
                "Payment Order ID = "
                        + paymentOrderId
        );


        Toast.makeText(
                requireContext(),
                "Starting payment...",
                Toast.LENGTH_SHORT
        ).show();


        // ========================================================
        // PAYHERE REQUEST
        // ========================================================

        InitRequest req =
                new InitRequest();


        req.setSandBox(true);


        req.setMerchantId(
                "1231383"
        );


        req.setMerchantSecret(
                "YOUR_PAYHERE_MERCHANT_SECRET"
        );


        req.setCurrency(
                "LKR"
        );


        req.setAmount(
                total
        );


        req.setOrderId(
                paymentOrderId
        );


        req.setItemsDescription(
                "Bonsai Mart Purchase"
        );


        // ========================================================
        // CUSTOMER DETAILS
        // ========================================================

        req.getCustomer()
                .setFirstName(
                        binding.shippingDetailsName
                                .getText()
                                .toString()
                );


        req.getCustomer()
                .setLastName("");


        req.getCustomer()
                .setEmail(
                        binding.shippingDetailsEmail
                                .getText()
                                .toString()
                );


        req.getCustomer()
                .setPhone(
                        binding.shippingDetailsContact
                                .getText()
                                .toString()
                );


        // ========================================================
        // ADDRESS
        // ========================================================

        req.getCustomer()
                .getAddress()
                .setAddress(
                        binding.shippingDetailsAddress1
                                .getText()
                                .toString()
                );


        req.getCustomer()
                .getAddress()
                .setCity(
                        binding.shippingDetailsCity
                                .getText()
                                .toString()
                );


        req.getCustomer()
                .getAddress()
                .setCountry(
                        "Sri Lanka"
                );


        // ========================================================
        // OPEN PAYHERE
        // ========================================================

        Intent intent =
                new Intent(
                        requireActivity(),
                        PHMainActivity.class
                );


        // IMPORTANT:
        // Payment request uses INTENT_EXTRA_DATA

        intent.putExtra(
                PHConstants.INTENT_EXTRA_DATA,
                req
        );


        Log.d(
                "PAYHERE_DEBUG",
                "Launching PayHere..."
        );


        payhereLauncher.launch(
                intent
        );
    }


    // ============================================================
    // PAYHERE RESULT
    // ============================================================

    private final ActivityResultLauncher<Intent>
            payhereLauncher =
            registerForActivityResult(
                    new ActivityResultContracts
                            .StartActivityForResult(),

                    result -> {

                        Log.d(
                                "PAYHERE_DEBUG",
                                "================================"
                        );

                        Log.d(
                                "PAYHERE_DEBUG",
                                "Result Code = "
                                        + result.getResultCode()
                        );


                        // ====================================================
                        // CANCELLED
                        // ====================================================

                        if (result.getResultCode()
                                == Activity.RESULT_CANCELED) {

                            Log.i(
                                    "PAYHERE",
                                    "Payment Cancelled"
                            );


                            Toast.makeText(
                                    requireContext(),
                                    "Payment cancelled",
                                    Toast.LENGTH_LONG
                            ).show();


                            return;
                        }


                        // ====================================================
                        // RESULT MUST BE OK
                        // ====================================================

                        if (result.getResultCode()
                                != Activity.RESULT_OK) {

                            Toast.makeText(
                                    requireContext(),
                                    "Payment result not OK",
                                    Toast.LENGTH_LONG
                            ).show();


                            Log.e(
                                    "PAYHERE_DEBUG",
                                    "Unexpected result code = "
                                            + result.getResultCode()
                            );


                            return;
                        }


                        // ====================================================
                        // GET RESULT INTENT
                        // ====================================================

                        Intent data =
                                result.getData();


                        Log.d(
                                "PAYHERE_DEBUG",
                                "Data = "
                                        + (data != null)
                        );


                        if (data == null) {

                            Toast.makeText(
                                    requireContext(),
                                    "Payment data is NULL",
                                    Toast.LENGTH_LONG
                            ).show();


                            Log.e(
                                    "PAYHERE_DEBUG",
                                    "Payment Intent data is NULL"
                            );


                            return;
                        }


                        // ====================================================
                        // IMPORTANT:
                        //
                        // REQUEST:
                        // INTENT_EXTRA_DATA
                        //
                        // RESULT:
                        // INTENT_EXTRA_RESULT
                        // ====================================================

                        boolean hasResult =
                                data.hasExtra(
                                        PHConstants
                                                .INTENT_EXTRA_RESULT
                                );


                        Log.d(
                                "PAYHERE_DEBUG",
                                "Has INTENT_EXTRA_RESULT = "
                                        + hasResult
                        );


                        if (!hasResult) {

                            Toast.makeText(
                                    requireContext(),
                                    "PayHere result not found",
                                    Toast.LENGTH_LONG
                            ).show();


                            Log.e(
                                    "PAYHERE_DEBUG",
                                    "INTENT_EXTRA_RESULT not found"
                            );


                            // Print all extras for debugging

                            Bundle extras =
                                    data.getExtras();


                            if (extras != null) {

                                for (String key :
                                        extras.keySet()) {

                                    Object value =
                                            extras.get(key);


                                    Log.d(
                                            "PAYHERE_DEBUG",
                                            "EXTRA KEY = "
                                                    + key
                                                    + " VALUE = "
                                                    + value
                                    );
                                }

                            } else {

                                Log.e(
                                        "PAYHERE_DEBUG",
                                        "No extras available"
                                );
                            }


                            return;
                        }


                        // ====================================================
                        // GET PAYHERE RESPONSE
                        // ====================================================

                        PHResponse<StatusResponse> response =
                                (PHResponse<StatusResponse>)
                                        data.getSerializableExtra(
                                                PHConstants.INTENT_EXTRA_RESULT
                                        );


                        Log.d(
                                "PAYHERE_DEBUG",
                                "Response = "
                                        + (response != null)
                        );


                        if (response == null) {

                            Toast.makeText(
                                    requireContext(),
                                    "PayHere response is NULL",
                                    Toast.LENGTH_LONG
                            ).show();


                            Log.e(
                                    "PAYHERE_DEBUG",
                                    "Response object is NULL"
                            );


                            return;
                        }


                        // ====================================================
                        // CHECK SUCCESS
                        // ====================================================

                        boolean success =
                                response.isSuccess();


                        Log.d(
                                "PAYHERE_DEBUG",
                                "Payment Success = "
                                        + success
                        );


                        if (success) {

                            // ================================================
                            // PAYMENT SUCCESS
                            // ================================================

                            Log.i(
                                    "PAYHERE",
                                    "Payment Success!"
                            );


                            Toast.makeText(
                                    requireContext(),
                                    "PAYMENT SUCCESS - Processing Order...",
                                    Toast.LENGTH_LONG
                            ).show();


                            Log.d(
                                    "CHECKOUT_DEBUG",
                                    "Calling saveOrderAfterPayment()"
                            );


                            saveOrderAfterPayment();


                        } else {

                            // ================================================
                            // PAYMENT FAILED
                            // ================================================

                            String message =
                                    "Payment failed";


                            if (response.getData() != null) {

                                String responseMessage =
                                        response.getData()
                                                .getMessage();


                                if (responseMessage != null &&
                                        !responseMessage.isEmpty()) {

                                    message =
                                            responseMessage;
                                }
                            }


                            Log.e(
                                    "PAYHERE",
                                    "Payment Failed: "
                                            + message
                            );


                            Toast.makeText(
                                    requireContext(),
                                    "Payment Failed: "
                                            + message,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );


    // ============================================================
    // SAVE ORDER AFTER PAYMENT
    // ============================================================

    private void saveOrderAfterPayment() {

        Toast.makeText(
                requireContext(),
                "saveOrderAfterPayment() STARTED",
                Toast.LENGTH_LONG
        ).show();


        Log.d(
                "CHECKOUT_DEBUG",
                "saveOrderAfterPayment STARTED"
        );


        // ========================================================
        // CHECK USER
        // ========================================================

        if (firebaseAuth.getCurrentUser()
                == null) {

            Toast.makeText(
                    requireContext(),
                    "User not logged in",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // ========================================================
        // CHECK ORDER ID
        // ========================================================

        if (paymentOrderId == null ||
                paymentOrderId.isEmpty()) {

            Toast.makeText(
                    requireContext(),
                    "Order ID not found",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        String uid =
                firebaseAuth.getCurrentUser()
                        .getUid();


        // ========================================================
        // LOAD CART AGAIN
        // ========================================================

        db.collection("users")
                .document(uid)
                .collection("cart")
                .get()
                .addOnSuccessListener(
                        cartSnapshot -> {


                            Toast.makeText(
                                    requireContext(),
                                    "Cart loaded: "
                                            + cartSnapshot.size(),
                                    Toast.LENGTH_LONG
                            ).show();


                            Log.d(
                                    "CHECKOUT_DEBUG",
                                    "Cart count = "
                                            + cartSnapshot.size()
                            );


                            if (cartSnapshot.isEmpty()) {

                                Toast.makeText(
                                        requireContext(),
                                        "Cart is empty",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }


                            // ====================================================
                            // CONVERT CART
                            // ====================================================

                            List<CartItem> cartItems =
                                    cartSnapshot.toObjects(
                                            CartItem.class
                                    );


                            ArrayList<String> productIds =
                                    new ArrayList<>();


                            for (CartItem cartItem :
                                    cartItems) {

                                if (cartItem.getProductId()
                                        != null) {

                                    productIds.add(
                                            cartItem.getProductId()
                                    );
                                }
                            }


                            if (productIds.isEmpty()) {

                                Toast.makeText(
                                        requireContext(),
                                        "No products in cart",
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }


                            // ====================================================
                            // GET LATEST PRODUCT DOCUMENTS
                            // ====================================================

                            getProductDocuments(
                                    productIds,
                                    productDocuments -> {


                                        Toast.makeText(
                                                requireContext(),
                                                "Products found: "
                                                        + productDocuments.size(),
                                                Toast.LENGTH_LONG
                                        ).show();


                                        Log.d(
                                                "CHECKOUT_DEBUG",
                                                "Products found = "
                                                        + productDocuments.size()
                                        );


                                        if (productDocuments.isEmpty()) {

                                            Toast.makeText(
                                                    requireContext(),
                                                    "Products not found",
                                                    Toast.LENGTH_SHORT
                                            ).show();

                                            return;
                                        }


                                        // ====================================================
                                        // VALIDATE STOCK
                                        // ====================================================

                                        for (CartItem cartItem :
                                                cartItems) {


                                            DocumentSnapshot productDoc =
                                                    productDocuments.get(
                                                            cartItem.getProductId()
                                                    );


                                            if (productDoc == null) {

                                                Toast.makeText(
                                                        requireContext(),
                                                        "Product not found: "
                                                                + cartItem.getProductId(),
                                                        Toast.LENGTH_LONG
                                                ).show();

                                                return;
                                            }


                                            Long stockValue =
                                                    productDoc.getLong(
                                                            "stockCount"
                                                    );


                                            long stock =
                                                    stockValue != null
                                                            ? stockValue
                                                            : 0;


                                            if (stock <
                                                    cartItem.getQuantity()) {


                                                Product product =
                                                        productDoc.toObject(
                                                                Product.class
                                                        );


                                                String productName =
                                                        product != null &&
                                                                product.getTitle() != null
                                                                ? product.getTitle()
                                                                : "Product";


                                                Toast.makeText(
                                                        requireContext(),
                                                        productName
                                                                + " has only "
                                                                + stock
                                                                + " items available",
                                                        Toast.LENGTH_LONG
                                                ).show();


                                                return;
                                            }
                                        }


                                        // ====================================================
                                        // CREATE ORDER
                                        // ====================================================

                                        Order order =
                                                createOrder(
                                                        uid,
                                                        cartItems,
                                                        productDocuments
                                                );


                                        // ====================================================
                                        // CREATE BATCH
                                        // ====================================================

                                        WriteBatch batch =
                                                db.batch();


                                        // ====================================================
                                        // ORDER REFERENCE
                                        // ====================================================

                                        com.google.firebase
                                                .firestore
                                                .DocumentReference orderRef =
                                                db.collection("orders")
                                                        .document(
                                                                paymentOrderId
                                                        );


                                        // ====================================================
                                        // SAVE ORDER
                                        // ====================================================

                                        batch.set(
                                                orderRef,
                                                order
                                        );


                                        // ====================================================
                                        // UPDATE STOCK
                                        // ====================================================

                                        for (CartItem cartItem :
                                                cartItems) {


                                            DocumentSnapshot productDoc =
                                                    productDocuments.get(
                                                            cartItem.getProductId()
                                                    );


                                            if (productDoc == null) {
                                                continue;
                                            }


                                            Long stockValue =
                                                    productDoc.getLong(
                                                            "stockCount"
                                                    );


                                            long currentStock =
                                                    stockValue != null
                                                            ? stockValue
                                                            : 0;


                                            long newStock =
                                                    currentStock
                                                            - cartItem.getQuantity();


                                            batch.update(
                                                    productDoc.getReference(),
                                                    "stockCount",
                                                    newStock
                                            );


                                            Log.d(
                                                    "CHECKOUT_DEBUG",
                                                    "Stock update: "
                                                            + cartItem.getProductId()
                                                            + " "
                                                            + currentStock
                                                            + " -> "
                                                            + newStock
                                            );
                                        }


                                        // ====================================================
                                        // DELETE CART ITEMS
                                        // ====================================================

                                        for (DocumentSnapshot cartDoc :
                                                cartSnapshot.getDocuments()) {

                                            batch.delete(
                                                    cartDoc.getReference()
                                            );
                                        }


                                        // ====================================================
                                        // COMMIT
                                        // ====================================================

                                        Toast.makeText(
                                                requireContext(),
                                                "Saving order...",
                                                Toast.LENGTH_SHORT
                                        ).show();


                                        batch.commit()
                                                .addOnSuccessListener(
                                                        unused -> {


                                                            Log.d(
                                                                    "CHECKOUT_DEBUG",
                                                                    "BATCH COMMIT SUCCESS"
                                                            );


                                                            Toast.makeText(
                                                                    requireContext(),
                                                                    "ORDER + STOCK + CART SUCCESS!",
                                                                    Toast.LENGTH_LONG
                                                            ).show();


                                                            Toast.makeText(
                                                                    requireContext(),
                                                                    "Order Success!",
                                                                    Toast.LENGTH_LONG
                                                            ).show();


                                                            // ====================================================
                                                            // GO HOME
                                                            // ====================================================

                                                            if (isAdded()) {

                                                                getParentFragmentManager()
                                                                        .beginTransaction()
                                                                        .replace(
                                                                                R.id.fragment_container,
                                                                                new HomeFragment()
                                                                        )
                                                                        .commit();
                                                            }
                                                        }
                                                )
                                                .addOnFailureListener(
                                                        e -> {


                                                            Log.e(
                                                                    "CHECKOUT_DEBUG",
                                                                    "BATCH COMMIT FAILED",
                                                                    e
                                                            );


                                                            Toast.makeText(
                                                                    requireContext(),
                                                                    "FIRESTORE ERROR: "
                                                                            + e.getMessage(),
                                                                    Toast.LENGTH_LONG
                                                            ).show();
                                                        }
                                                );
                                    }
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {


                            Log.e(
                                    "CHECKOUT",
                                    "Failed to read cart",
                                    e
                            );


                            Toast.makeText(
                                    requireContext(),
                                    "Failed to process cart: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }


    // ============================================================
    // GET CART ITEMS
    // ============================================================

    private void getCartItems(
            FirestoreCallback<List<CartItem>> callback
    ) {

        if (firebaseAuth.getCurrentUser()
                == null) {

            callback.onCallback(
                    new ArrayList<>()
            );

            return;
        }


        String uid =
                firebaseAuth.getCurrentUser()
                        .getUid();


        db.collection("users")
                .document(uid)
                .collection("cart")
                .get()
                .addOnSuccessListener(
                        qds -> {


                            List<CartItem> cartItems =
                                    qds.toObjects(
                                            CartItem.class
                                    );


                            callback.onCallback(
                                    cartItems
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {


                            Log.e(
                                    "CHECKOUT",
                                    "Error loading cart",
                                    e
                            );


                            Toast.makeText(
                                    requireContext(),
                                    "Failed to load cart",
                                    Toast.LENGTH_SHORT
                            ).show();


                            callback.onCallback(
                                    new ArrayList<>()
                            );
                        }
                );
    }


    // ============================================================
    // GET PRODUCTS BY IDS
    // ============================================================

    private void getProductsByIds(
            List<String> productIds,
            FirestoreCallback<Map<String, Product>> callback
    ) {

        Map<String, Product> products =
                new HashMap<>();


        if (productIds == null ||
                productIds.isEmpty()) {

            callback.onCallback(
                    products
            );

            return;
        }


        List<List<String>> batches =
                new ArrayList<>();


        for (int i = 0;
             i < productIds.size();
             i += 10) {


            int end =
                    Math.min(
                            i + 10,
                            productIds.size()
                    );


            batches.add(
                    new ArrayList<>(
                            productIds.subList(
                                    i,
                                    end
                            )
                    )
            );
        }


        getProductBatches(
                batches,
                0,
                products,
                callback
        );
    }


    // ============================================================
    // PRODUCT BATCHES
    // ============================================================

    private void getProductBatches(
            List<List<String>> batches,
            int index,
            Map<String, Product> products,
            FirestoreCallback<Map<String, Product>> callback
    ) {

        if (index >= batches.size()) {

            callback.onCallback(
                    products
            );

            return;
        }


        List<String> currentBatch =
                batches.get(index);


        db.collection("products")
                .whereIn(
                        "productId",
                        currentBatch
                )
                .get()
                .addOnSuccessListener(
                        qds -> {


                            for (DocumentSnapshot ds :
                                    qds.getDocuments()) {


                                Product product =
                                        ds.toObject(
                                                Product.class
                                        );


                                if (product != null &&
                                        product.getProductId()
                                                != null) {


                                    products.put(
                                            product.getProductId(),
                                            product
                                    );
                                }
                            }


                            getProductBatches(
                                    batches,
                                    index + 1,
                                    products,
                                    callback
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {


                            Log.e(
                                    "CHECKOUT",
                                    "Error loading products",
                                    e
                            );


                            Toast.makeText(
                                    requireContext(),
                                    "Failed to load products: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();


                            callback.onCallback(
                                    products
                            );
                        }
                );
    }


    // ============================================================
    // GET PRODUCT DOCUMENTS
    // ============================================================

    private void getProductDocuments(
            List<String> productIds,
            FirestoreCallback<Map<String, DocumentSnapshot>> callback
    ) {

        Map<String, DocumentSnapshot> documents =
                new HashMap<>();


        if (productIds == null ||
                productIds.isEmpty()) {

            callback.onCallback(
                    documents
            );

            return;
        }


        List<List<String>> batches =
                new ArrayList<>();


        for (int i = 0;
             i < productIds.size();
             i += 10) {


            int end =
                    Math.min(
                            i + 10,
                            productIds.size()
                    );


            batches.add(
                    new ArrayList<>(
                            productIds.subList(
                                    i,
                                    end
                            )
                    )
            );
        }


        getProductDocumentBatches(
                batches,
                0,
                documents,
                callback
        );
    }


    // ============================================================
    // PRODUCT DOCUMENT BATCHES
    // ============================================================

    private void getProductDocumentBatches(
            List<List<String>> batches,
            int index,
            Map<String, DocumentSnapshot> documents,
            FirestoreCallback<Map<String, DocumentSnapshot>> callback
    ) {

        if (index >= batches.size()) {

            callback.onCallback(
                    documents
            );

            return;
        }


        List<String> currentBatch =
                batches.get(index);


        db.collection("products")
                .whereIn(
                        "productId",
                        currentBatch
                )
                .get()
                .addOnSuccessListener(
                        qds -> {


                            for (DocumentSnapshot ds :
                                    qds.getDocuments()) {


                                String productId =
                                        ds.getString(
                                                "productId"
                                        );


                                if (productId != null) {

                                    documents.put(
                                            productId,
                                            ds
                                    );
                                }
                            }


                            getProductDocumentBatches(
                                    batches,
                                    index + 1,
                                    documents,
                                    callback
                            );
                        }
                )
                .addOnFailureListener(
                        e -> {


                            Log.e(
                                    "CHECKOUT",
                                    "Product query failed",
                                    e
                            );


                            Toast.makeText(
                                    requireContext(),
                                    "Failed to find products: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();


                            callback.onCallback(
                                    documents
                            );
                        }
                );
    }


    // ============================================================
    // CREATE ORDER
    // ============================================================

    private Order createOrder(
            String uid,
            List<CartItem> cartItems,
            Map<String, DocumentSnapshot> productDocuments
    ) {


        Calendar calendar =
                Calendar.getInstance();


        Timestamp orderDate =
                new Timestamp(
                        calendar.getTime()
                );


        calendar.add(
                Calendar.DAY_OF_YEAR,
                7
        );


        Timestamp deliveryDate =
                new Timestamp(
                        calendar.getTime()
                );


        Order order =
                new Order();


        order.setOrderId(
                paymentOrderId
        );


        order.setUserId(
                uid
        );


        order.setTotalAmount(
                total
        );


        order.setStatus(
                "PAID"
        );


        order.setOrderDate(
                orderDate
        );


        order.setDeliveryDate(
                deliveryDate
        );


        // ========================================================
        // SHIPPING ADDRESS
        // ========================================================

        Order.Address shippingAddress =
                Order.Address.builder()

                        .name(
                                binding.shippingDetailsName
                                        .getText()
                                        .toString()
                        )

                        .email(
                                binding.shippingDetailsEmail
                                        .getText()
                                        .toString()
                        )

                        .contact(
                                binding.shippingDetailsContact
                                        .getText()
                                        .toString()
                        )

                        .address1(
                                binding.shippingDetailsAddress1
                                        .getText()
                                        .toString()
                        )

                        .address2(
                                binding.shippingDetailsAddress2
                                        .getText()
                                        .toString()
                        )

                        .city(
                                binding.shippingDetailsCity
                                        .getText()
                                        .toString()
                        )

                        .postcode(
                                binding.shippingDetailsPostcode
                                        .getText()
                                        .toString()
                        )

                        .build();


        order.setShippingAddress(
                shippingAddress
        );


        // ========================================================
        // BILLING ADDRESS
        // ========================================================

        if (!binding.shippingDetailsCheckBilling
                .isChecked()) {


            Order.Address billingAddress =
                    Order.Address.builder()

                            .name(
                                    binding.billingDetailsName
                                            .getText()
                                            .toString()
                            )

                            .email(
                                    binding.billingDetailsEmail
                                            .getText()
                                            .toString()
                            )

                            .contact(
                                    binding.billingDetailsContact
                                            .getText()
                                            .toString()
                            )

                            .address1(
                                    binding.billingDetailsAddress1
                                            .getText()
                                            .toString()
                            )

                            .address2(
                                    binding.billingDetailsAddress2
                                            .getText()
                                            .toString()
                            )

                            .city(
                                    binding.billingDetailsCity
                                            .getText()
                                            .toString()
                            )

                            .postcode(
                                    binding.billingDetailsPostcode
                                            .getText()
                                            .toString()
                            )

                            .build();


            order.setBillingAddress(
                    billingAddress
            );

        } else {

            order.setBillingAddress(
                    shippingAddress
            );
        }


        // ========================================================
        // ORDER ITEMS
        // ========================================================

        List<Order.OrderItem> orderItems =
                new ArrayList<>();


        for (CartItem cartItem :
                cartItems) {


            DocumentSnapshot productDoc =
                    productDocuments.get(
                            cartItem.getProductId()
                    );


            if (productDoc == null) {
                continue;
            }


            Product product =
                    productDoc.toObject(
                            Product.class
                    );


            if (product == null) {
                continue;
            }


            // ====================================================
            // ATTRIBUTES
            // ====================================================

            List<Order.OrderItem.Attribute>
                    attributes =
                    new ArrayList<>();


            if (cartItem.getAttributes()
                    != null) {


                for (CartItem.Attribute attribute :
                        cartItem.getAttributes()) {


                    attributes.add(
                            Order.OrderItem.Attribute
                                    .builder()

                                    .name(
                                            attribute.getName()
                                    )

                                    .value(
                                            attribute.getValue()
                                    )

                                    .build()
                    );
                }
            }


            // ====================================================
            // ORDER ITEM
            // ====================================================

            Order.OrderItem orderItem =
                    Order.OrderItem.builder()

                            .productId(
                                    cartItem.getProductId()
                            )

                            .productName(
                                    product.getTitle()
                            )

                            .productImage(
                                    product.getImages() != null &&
                                            !product.getImages().isEmpty()
                                            ? product.getImages().get(0)
                                            : null
                            )

                            .unitPrice(
                                    product.getPrice()
                            )

                            .quantity(
                                    cartItem.getQuantity()
                            )

                            .attributes(
                                    attributes
                            )

                            .build();


            orderItems.add(
                    orderItem
            );
        }


        order.setOrderItems(
                orderItems
        );


        return order;
    }


    // ============================================================
    // CLEANUP
    // ============================================================

    @Override
    public void onDestroyView() {

        super.onDestroyView();

        binding = null;
    }
}