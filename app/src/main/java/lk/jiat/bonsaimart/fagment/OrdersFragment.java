package lk.jiat.bonsaimart.fagment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import lk.jiat.bonsaimart.adapter.OrderAdapter;
import lk.jiat.bonsaimart.databinding.FragmentOrdersBinding;
import lk.jiat.bonsaimart.model.Order;

public class OrdersFragment extends Fragment {

    private FragmentOrdersBinding binding;
    private OrderAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        binding.recyclerViewOrders.setLayoutManager(new LinearLayoutManager(getContext()));

        loadOrders();
    }

    private void loadOrders() {
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            Timestamp now = Timestamp.now();

            db.collection("orders")
                    .whereEqualTo("userId", uid)
                    .orderBy("orderDate", Query.Direction.DESCENDING)
                    .addSnapshotListener((value, error) -> {
                        if (value != null) {
                            List<Order> allOrders = value.toObjects(Order.class);
                            List<Order> activeOrders = new ArrayList<>();

                            for (Order order : allOrders) {
                                // Requirement: Remove from list if delivered (Delivery Date <= Now)
                                // Only add to list if Delivery Date is in the future
                                if (order.getDeliveryDate() != null && order.getDeliveryDate().compareTo(now) > 0) {
                                    activeOrders.add(order);
                                }
                            }

                            if (activeOrders.isEmpty()) {
                                // You might want to show a "No active orders" message here
                                binding.recyclerViewOrders.setVisibility(View.GONE);
                            } else {
                                binding.recyclerViewOrders.setVisibility(View.VISIBLE);
                                adapter = new OrderAdapter(activeOrders);
                                binding.recyclerViewOrders.setAdapter(adapter);
                            }
                        }
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
