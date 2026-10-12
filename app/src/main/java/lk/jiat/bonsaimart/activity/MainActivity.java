package lk.jiat.bonsaimart.activity;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.FileInputStream;
import java.io.IOException;

import lk.jiat.bonsaimart.R;
import lk.jiat.bonsaimart.databinding.ActivityMainBinding;
import lk.jiat.bonsaimart.databinding.SideNavHeaderBinding;
import lk.jiat.bonsaimart.fagment.CartFragment;
import lk.jiat.bonsaimart.fagment.CategoryFragment;
import lk.jiat.bonsaimart.fagment.HomeFragment;
import lk.jiat.bonsaimart.fagment.MapFragment;
import lk.jiat.bonsaimart.fagment.MessageFragment;
import lk.jiat.bonsaimart.fagment.OrdersFragment;
import lk.jiat.bonsaimart.fagment.ProfileFragment;
import lk.jiat.bonsaimart.fagment.SettingFragment;
import lk.jiat.bonsaimart.fagment.WishlistFragment;
import lk.jiat.bonsaimart.model.User;
import lk.jiat.bonsaimart.receiver.NetworkChangeReceiver;
import lk.jiat.bonsaimart.util.PreferenceManager;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        BottomNavigationView.OnItemSelectedListener {

    private ActivityMainBinding binding;
    private SideNavHeaderBinding sideNavHeaderBinding;

    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth firebaseAuth;
    private NetworkChangeReceiver networkReceiver;
    private FirebaseFirestore firebaseFirestore;

    private PreferenceManager preferenceManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ============================================================
        // INITIAL NETWORK CHECK
        // ============================================================
        if (!NetworkChangeReceiver.isNetworkAvailable(this)) {
            Toast.makeText(this,
                    "⚠️ No internet. Cart is saved locally.",
                    Toast.LENGTH_LONG).show();
        }

        // View Binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();

        preferenceManager = new PreferenceManager(this);

        // ---------------------------------------------------------
        // SIDE NAVIGATION HEADER
        // ---------------------------------------------------------

        View headerView = binding.sideNavigationView.getHeaderView(0);
        sideNavHeaderBinding = SideNavHeaderBinding.bind(headerView);

        // ---------------------------------------------------------
        // FIND VIEWS
        // ---------------------------------------------------------

        drawerLayout = binding.drawerLayout;
        toolbar = binding.toolbar;
        navigationView = binding.sideNavigationView;
        bottomNavigationView = binding.bottomNavigationView;

        // ---------------------------------------------------------
        // TOOLBAR NAVIGATION BUTTON
        // ---------------------------------------------------------

        toolbar.setNavigationOnClickListener(v -> {

            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {

                drawerLayout.closeDrawer(GravityCompat.START);

            } else {

                drawerLayout.openDrawer(GravityCompat.START);
            }
        });


        // ---------------------------------------------------------
        // TOOLBAR POPUP MENU
        // ---------------------------------------------------------

        toolbar.setOnMenuItemClickListener(item -> {

            int itemId = item.getItemId();

            // About App
            if (itemId == R.id.nav_favorites) {

                showAboutDialog();

                return true;
            }

            // Contact Details
            if (itemId == R.id.nav_about) {

                showContactDialog();

                return true;
            }

            // Share App
            if (itemId == R.id.nav_share) {

                shareApp();

                return true;
            }

            return false;
        });


        // ---------------------------------------------------------
        // DRAWER NAVIGATION
        // ---------------------------------------------------------

        navigationView.setNavigationItemSelectedListener(this);

        // ---------------------------------------------------------
        // BOTTOM NAVIGATION
        // ---------------------------------------------------------

        bottomNavigationView.setOnItemSelectedListener(this);


        // ---------------------------------------------------------
        // BACK BUTTON
        // ---------------------------------------------------------

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {

                        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {

                            drawerLayout.closeDrawer(GravityCompat.START);

                        } else {

                            finish();
                        }
                    }
                }
        );


        // ---------------------------------------------------------
        // DEFAULT FRAGMENT
        // ---------------------------------------------------------

        if (savedInstanceState == null) {

            loadFragment(new HomeFragment());

            syncSelection(R.id.bottom_nav_home);
        }


        // ---------------------------------------------------------
        // USER UI
        // ---------------------------------------------------------

        updateUIForUser();

        // ============================================================
        // REGISTER NETWORK CHANGE RECEIVER (Dynamic)
        // ============================================================
        networkReceiver = new NetworkChangeReceiver();

        IntentFilter networkFilter = new IntentFilter();
        networkFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        registerReceiver(networkReceiver, networkFilter);

        // Network change listener
        NetworkChangeReceiver.setListener(isConnected -> {
            runOnUiThread(() -> {
                if (isConnected) {
                    Log.d("MainActivity", "Network: ONLINE");
                    // Optional: cart sync කරන්න
                } else {
                    Log.d("MainActivity", "Network: OFFLINE");
                }
            });
        });

        // Initial network check
        if (!NetworkChangeReceiver.isNetworkAvailable(this)) {
            Toast.makeText(this,
                    "⚠️ No internet. Cart is saved locally.",
                    Toast.LENGTH_LONG).show();
        }

        // ============================================================
        // SEARCH BAR - SAVE SEARCH HISTORY (SharedPreferences)
        // ============================================================
        android.widget.EditText searchEdit = findViewById(R.id.search_edit_text);

        searchEdit.setOnEditorActionListener((v, actionId, event) -> {

            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {

                String query = searchEdit.getText().toString().trim();

                if (!query.isEmpty()) {
                    // Save to SharedPreferences
                    preferenceManager.addToSearchHistory(query);
                    preferenceManager.saveLastSearch(query);

                    Log.d("MainActivity", "Search saved: " + query);

                    // Navigate to ListingFragment with query
                    Bundle bundle = new Bundle();
                    bundle.putString("searchQuery", query);

                    lk.jiat.bonsaimart.fagment.ListingFragment fragment =
                            new lk.jiat.bonsaimart.fagment.ListingFragment();
                    fragment.setArguments(bundle);

                    loadFragment(fragment);

                    // Clear focus
                    searchEdit.clearFocus();
                }
                return true;
            }
            return false;
        });

        // Load last search
        String lastSearch = preferenceManager.getLastSearch();
        if (!lastSearch.isEmpty()) {
            Log.d("MainActivity", "Last search: " + lastSearch);
        }

    }


    // ============================================================
    // UPDATE USER UI
    // ============================================================

    private void updateUIForUser() {

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        Menu sideMenu = navigationView.getMenu();

        if (currentUser != null) {

            firebaseFirestore
                    .collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(ds -> {

                        if (ds.exists()) {

                            User user = ds.toObject(User.class);

                            if (user != null) {

                                sideNavHeaderBinding.headerUserName
                                        .setText(user.getName());

                                sideNavHeaderBinding.headerUserEmail
                                        .setText(user.getEmail());

                                preferenceManager.saveUser(user);
                                Log.d("MainActivity", "User saved as JSON: " + user.getName());


                                if (user.getProfilePicUrl() != null
                                        && !user.getProfilePicUrl().isEmpty()) {

                                    Glide.with(MainActivity.this)
                                            .load(user.getProfilePicUrl())
                                            .placeholder(R.drawable.person_24px)
                                            .circleCrop()
                                            .into(
                                                    sideNavHeaderBinding.headerProfilePic
                                            );
                                }
                            }
                        }
                    });


            // Logged in
            sideMenu.findItem(R.id.side_nav_login)
                    .setVisible(false);

            sideMenu.findItem(R.id.side_nav_profile)
                    .setVisible(true);

            sideMenu.findItem(R.id.side_nav_orders)
                    .setVisible(true);

            sideMenu.findItem(R.id.side_nav_wishlist)
                    .setVisible(true);

            sideMenu.findItem(R.id.side_nav_cart)
                    .setVisible(true);

            sideMenu.findItem(R.id.side_nav_message)
                    .setVisible(true);

            sideMenu.findItem(R.id.side_nav_logout)
                    .setVisible(true);


        } else {

            // Not logged in

            sideNavHeaderBinding.headerUserName
                    .setText("You're not logged in");

            sideNavHeaderBinding.headerUserEmail
                    .setText("");

            sideNavHeaderBinding.headerProfilePic
                    .setImageResource(
                            R.drawable.person_24px
                    );


            sideMenu.findItem(R.id.side_nav_login)
                    .setVisible(true);

            sideMenu.findItem(R.id.side_nav_profile)
                    .setVisible(false);

            sideMenu.findItem(R.id.side_nav_orders)
                    .setVisible(false);

            sideMenu.findItem(R.id.side_nav_wishlist)
                    .setVisible(false);

            sideMenu.findItem(R.id.side_nav_cart)
                    .setVisible(false);

            sideMenu.findItem(R.id.side_nav_message)
                    .setVisible(false);

            sideMenu.findItem(R.id.side_nav_logout)
                    .setVisible(false);
        }
    }


    // ============================================================
    // ABOUT APP
    // ============================================================

    private void showAboutDialog() {

        new AlertDialog.Builder(this)
                .setTitle("About BonsaiMart")
                .setMessage(
                        "BonsaiMart\n\n" +
                                "Your online bonsai plant shopping application.\n\n" +
                                "Browse bonsai plants, view product details, " +
                                "add products to your cart and place orders easily."
                )
                .setPositiveButton("OK", null)
                .show();
    }


    // ============================================================
    // CONTACT DETAILS
    // ============================================================

    private void showContactDialog() {

        String[] options = {
                "📞 Call BonsaiMart",
                "💬 SMS BonsaiMart",
                "📧 Email BonsaiMart"
        };


        new AlertDialog.Builder(this)
                .setTitle("Contact BonsaiMart")
                .setItems(
                        options,
                        (dialog, which) -> {

                            if (which == 0) {

                                callBonsaiMart();

                            } else if (which == 1) {

                                smsBonsaiMart();

                            } else if (which == 2) {

                                emailBonsaiMart();
                            }
                        }
                )
                .show();
    }


    // ============================================================
    // PHONE CALL
    // ============================================================

    private void callBonsaiMart() {

        Intent intent = new Intent(Intent.ACTION_DIAL);

        intent.setData(
                Uri.parse("tel:0763060956")
        );

        startActivity(intent);
    }


    // ============================================================
    // SMS
    // ============================================================

    private void smsBonsaiMart() {

        Intent intent = new Intent(Intent.ACTION_SENDTO);

        intent.setData(
                Uri.parse("smsto:0763060956")
        );

        intent.putExtra(
                "sms_body",
                "Hello BonsaiMart, I need some information."
        );

        startActivity(intent);
    }


    // ============================================================
    // EMAIL
    // ============================================================

    private void emailBonsaiMart() {

        Intent intent = new Intent(Intent.ACTION_SENDTO);

        intent.setData(
                Uri.parse("mailto:")
        );

        intent.putExtra(
                Intent.EXTRA_EMAIL,
                new String[]{
                        "support@bonsaimart.com"
                }
        );

        intent.putExtra(
                Intent.EXTRA_SUBJECT,
                "BonsaiMart Inquiry"
        );

        intent.putExtra(
                Intent.EXTRA_TEXT,
                "Hello BonsaiMart,\n\nI need some information."
        );

        startActivity(intent);
    }


    // ============================================================
    // SHARE APP
    // ============================================================

    private void shareApp() {

        Intent shareIntent =
                new Intent(Intent.ACTION_SEND);

        shareIntent.setType("text/plain");

        shareIntent.putExtra(
                Intent.EXTRA_TEXT,
                "Check out BonsaiMart - " +
                        "Your online bonsai shopping app!"
        );

        startActivity(
                Intent.createChooser(
                        shareIntent,
                        "Share BonsaiMart"
                )
        );
    }


    // ============================================================
    // NAVIGATION ITEM SELECTED
    // ============================================================

    @Override
    public boolean onNavigationItemSelected(
            @NonNull MenuItem item) {

        int itemId = item.getItemId();

        syncSelection(itemId);


        // HOME
        if (itemId == R.id.side_nav_home
                || itemId == R.id.bottom_nav_home) {

            loadFragment(new HomeFragment());


        }

        // PROFILE
        else if (itemId == R.id.side_nav_profile
                || itemId == R.id.bottom_nav_profile) {

            if (firebaseAuth.getCurrentUser() == null) {

                startActivity(
                        new Intent(
                                this,
                                SignInActivity.class
                        )
                );

            } else {

                loadFragment(new ProfileFragment());
            }


        }

        // ORDERS
        else if (itemId == R.id.side_nav_orders) {

            loadFragment(new OrdersFragment());


        }

        // WISHLIST
        else if (itemId == R.id.side_nav_wishlist) {

            loadFragment(new WishlistFragment());


        }

        // CART
        else if (itemId == R.id.side_nav_cart
                || itemId == R.id.bottom_nav_cart) {

            if (firebaseAuth.getCurrentUser() == null) {

                startActivity(
                        new Intent(
                                this,
                                SignInActivity.class
                        )
                );

            } else {

                loadFragment(new CartFragment());
            }


        }

        // MESSAGES / NOTIFICATIONS
        else if (itemId == R.id.side_nav_message) {

            loadFragment(new MessageFragment());


        }

        // SETTINGS
        else if (itemId == R.id.side_nav_settings) {

            loadFragment(new SettingFragment());


        }

        // CATEGORY
        else if (itemId == R.id.bottom_nav_category) {

            loadFragment(new CategoryFragment());


        }

        // LOGIN
        else if (itemId == R.id.side_nav_login) {

            startActivity(
                    new Intent(
                            this,
                            SignInActivity.class
                    )
            );


        }

        // LOGOUT
        else if (itemId == R.id.side_nav_logout) {

            firebaseAuth.signOut();

            preferenceManager.clearUser();

            recreate();
        }

        // MAP
        else if (itemId == R.id.side_nav_map) {
            loadFragment(new MapFragment());
        }

        // Close Drawer
        if (drawerLayout.isDrawerOpen(
                GravityCompat.START)) {

            drawerLayout.closeDrawer(
                    GravityCompat.START
            );
        }

        return true;
    }


    // ============================================================
    // SYNC NAVIGATION
    // ============================================================

    private void syncSelection(int itemId) {

        // --------------------------------------------------------
        // Bottom Navigation
        // --------------------------------------------------------

        if (itemId == R.id.side_nav_home) {

            bottomNavigationView.setSelectedItemId(
                    R.id.bottom_nav_home
            );

        } else if (itemId == R.id.side_nav_profile) {

            bottomNavigationView.setSelectedItemId(
                    R.id.bottom_nav_profile
            );

        } else if (itemId == R.id.side_nav_cart) {

            bottomNavigationView.setSelectedItemId(
                    R.id.bottom_nav_cart
            );
        }


        // --------------------------------------------------------
        // Side Navigation
        // --------------------------------------------------------

        if (itemId == R.id.bottom_nav_home) {

            navigationView.setCheckedItem(
                    R.id.side_nav_home
            );

        } else if (itemId == R.id.bottom_nav_profile) {

            navigationView.setCheckedItem(
                    R.id.side_nav_profile
            );

        } else if (itemId == R.id.bottom_nav_cart) {

            navigationView.setCheckedItem(
                    R.id.side_nav_cart
            );
        }
    }


    // ============================================================
    // LOAD FRAGMENT
    // ============================================================

    private void loadFragment(Fragment fragment) {

        getSupportFragmentManager()
                .beginTransaction()
                .replace(
                        R.id.fragment_container,
                        fragment
                )
                .commit();
    }

    // ============================================================
// ON RESUME - Reload profile image if updated
// ============================================================
    @Override
    protected void onResume() {
        super.onResume();

        // ============================================================
        // Check if profile image was updated in ProfileFragment
        // ============================================================
        if (preferenceManager != null && preferenceManager.isProfileImageUpdated()) {

            Log.d("MainActivity", "Profile image updated, reloading...");

            // Load image from Internal Storage
            Bitmap profileImage = loadProfileImageFromInternalStorage();

            if (profileImage != null) {
                sideNavHeaderBinding.headerProfilePic.setImageBitmap(profileImage);
                Log.d("MainActivity", "Side nav profile image updated");
            }

            // Reset flag (so we don't reload every time)
            preferenceManager.clearProfileImageUpdated();
        }
    }

    // ============================================================
    // LOAD PROFILE IMAGE FROM INTERNAL STORAGE
    // ============================================================
    private Bitmap loadProfileImageFromInternalStorage() {
        try {
            FileInputStream fis = openFileInput("profile_pic.jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            fis.close();
            return bitmap;

        } catch (IOException e) {
            Log.d("MainActivity", "No saved profile image");
            return null;
        }
    }


    // ============================================================
    // CLEAN UP BINDING
    // ============================================================

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // ============================================================
        // UNREGISTER NETWORK RECEIVER (memory leak prevent)
        // ============================================================
        if (networkReceiver != null) {
            try {
                unregisterReceiver(networkReceiver);
            } catch (IllegalArgumentException e) {
                Log.e("MainActivity", "Receiver not registered", e);
            }
        }

        NetworkChangeReceiver.removeListener();

        binding = null;
    }
}