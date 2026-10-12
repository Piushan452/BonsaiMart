package lk.jiat.bonsaimart.fagment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

import lk.jiat.bonsaimart.adapter.MessageAdapter;
import lk.jiat.bonsaimart.databinding.FragmentMessageBinding;
import lk.jiat.bonsaimart.model.Message;

public class MessageFragment extends Fragment {

    private FragmentMessageBinding binding;
    private MessageAdapter adapter;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        // Updated to the correct ID: recycler_view_messages
        binding.recyclerViewMessages.setLayoutManager(new LinearLayoutManager(getContext()));

        loadMessages();

        getActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });
    }

    private void loadMessages() {
        db.collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (value != null && !value.isEmpty()) {
                        List<Message> messages = value.toObjects(Message.class);
                        adapter = new MessageAdapter(messages);
                        binding.recyclerViewMessages.setAdapter(adapter);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
