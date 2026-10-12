package lk.jiat.bonsaimart.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.jiat.bonsaimart.R;
import lk.jiat.bonsaimart.model.CartItem;
import lk.jiat.bonsaimart.model.Product;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private final List<CartItem> cartItems;
    private final Map<String, Product> productMap;

    private OnQuantityChangeListener changeListener;
    private OnRemoveListener removeListener;

    // ============================================================
    // LONG PRESS LISTENER (gesture)
    // ============================================================
    private OnItemLongClickListener longClickListener;

    // ============================================================
    // DOUBLE TAP LISTENER (gesture)
    // ============================================================
    private OnItemDoubleTapListener doubleTapListener;

    public CartAdapter(
            List<CartItem> cartItems,
            Map<String, Product> productMap
    ) {
        this.cartItems = cartItems;
        this.productMap = productMap;
    }

    public void setOnQuantityChangeListener(OnQuantityChangeListener listener) {
        this.changeListener = listener;
    }

    public void setOnRemoveListener(OnRemoveListener listener) {
        this.removeListener = listener;
    }

    // ============================================================
    // SET LONG CLICK LISTENER
    // ============================================================
    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    // ============================================================
    // SET DOUBLE TAP LISTENER
    // ============================================================
    public void setOnItemDoubleTapListener(OnItemDoubleTapListener listener) {
        this.doubleTapListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position
    ) {

        CartItem cartItem = cartItems.get(position);

        Product product = productMap.get(cartItem.getProductId());

        // Reset listeners first because RecyclerView reuses views
        holder.btnPlus.setOnClickListener(null);
        holder.btnMinus.setOnClickListener(null);
        holder.btnRemove.setOnClickListener(null);
        holder.itemView.setOnLongClickListener(null);
        holder.itemView.setOnClickListener(null);

        if (product == null) {

            holder.productTitle.setText("Product unavailable");
            holder.productPrice.setText("LKR 0.00");
            holder.productQuantity.setText(
                    String.valueOf(cartItem.getQuantity())
            );

            Glide.with(holder.itemView.getContext())
                    .clear(holder.productImage);

            return;
        }

        // Product title
        holder.productTitle.setText(product.getTitle());

        // Product price
        holder.productPrice.setText(
                String.format(
                        Locale.US,
                        "LKR %,.2f",
                        product.getPrice()
                )
        );

        // Quantity
        holder.productQuantity.setText(
                String.valueOf(cartItem.getQuantity())
        );

        // Product image
        Glide.with(holder.itemView.getContext())
                .clear(holder.productImage);

        if (product.getImages() != null
                && !product.getImages().isEmpty()
                && product.getImages().get(0) != null
                && !product.getImages().get(0).isEmpty()) {

            Glide.with(holder.itemView.getContext())
                    .load(product.getImages().get(0))
                    .centerCrop()
                    .into(holder.productImage);
        }

        // ============================================================
        // GESTURE 1: LONG PRESS - Delete Confirmation
        // ============================================================
        holder.itemView.setOnLongClickListener(v -> {

            int adapterPosition = holder.getBindingAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION) return true;

            CartItem item = cartItems.get(adapterPosition);
            Product p = productMap.get(item.getProductId());

            String productName = (p != null && p.getTitle() != null)
                    ? p.getTitle() : "this item";

            new AlertDialog.Builder(v.getContext())
                    .setTitle("🗑️ Remove Item?")
                    .setMessage("Do you want to remove \"" + productName + "\" from your cart?")
                    .setPositiveButton("REMOVE", (dialog, which) -> {
                        if (removeListener != null) {
                            removeListener.onRemove(adapterPosition);
                        }
                    })
                    .setNegativeButton("CANCEL", null)
                    .show();

            // Vibrate for feedback
            v.performHapticFeedback(
                    android.view.HapticFeedbackConstants.LONG_PRESS
            );

            return true;
        });

        // ============================================================
        // GESTURE 2: DOUBLE TAP - Show Product Details
        // ============================================================
        holder.itemView.setOnClickListener(new View.OnClickListener() {

            private long lastClickTime = 0;
            private static final long DOUBLE_TAP_TIMEOUT = 300; // ms

            @Override
            public void onClick(View v) {

                long currentTime = System.currentTimeMillis();

                if (currentTime - lastClickTime < DOUBLE_TAP_TIMEOUT) {
                    // Double tap detected!
                    lastClickTime = 0;

                    int adapterPosition = holder.getBindingAdapterPosition();
                    if (adapterPosition == RecyclerView.NO_POSITION) return;

                    if (doubleTapListener != null) {
                        doubleTapListener.onItemDoubleTap(adapterPosition);
                    }

                    // Vibrate
                    v.performHapticFeedback(
                            android.view.HapticFeedbackConstants.VIRTUAL_KEY
                    );

                } else {
                    lastClickTime = currentTime;
                }
            }
        });

        // ============================================================
        // PLUS BUTTON
        // ============================================================
        holder.btnPlus.setOnClickListener(v -> {

            int currentQuantity = cartItem.getQuantity();
            int stockCount = product.getStockCount();

            if (stockCount <= 0) {
                return;
            }

            if (currentQuantity < stockCount) {

                cartItem.setQuantity(currentQuantity + 1);

                holder.productQuantity.setText(
                        String.valueOf(cartItem.getQuantity())
                );

                if (changeListener != null) {
                    changeListener.onChange(cartItem);
                }

            } else {

                android.widget.Toast.makeText(
                        holder.itemView.getContext(),
                        "Maximum stock available: " + stockCount,
                        android.widget.Toast.LENGTH_SHORT
                ).show();
            }
        });

        // ============================================================
        // MINUS BUTTON
        // ============================================================
        holder.btnMinus.setOnClickListener(v -> {

            int currentQuantity = cartItem.getQuantity();

            if (currentQuantity > 1) {

                cartItem.setQuantity(currentQuantity - 1);

                holder.productQuantity.setText(
                        String.valueOf(cartItem.getQuantity())
                );

                if (changeListener != null) {
                    changeListener.onChange(cartItem);
                }

            } else {

                android.widget.Toast.makeText(
                        holder.itemView.getContext(),
                        "Minimum quantity is 1",
                        android.widget.Toast.LENGTH_SHORT
                ).show();
            }
        });

        // ============================================================
        // REMOVE BUTTON
        // ============================================================
        holder.btnRemove.setOnClickListener(v -> {

            int adapterPosition = holder.getBindingAdapterPosition();

            if (adapterPosition == RecyclerView.NO_POSITION) {
                return;
            }

            if (removeListener != null) {
                removeListener.onRemove(adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        ImageView productImage;
        TextView productTitle;
        TextView productPrice;
        TextView productQuantity;

        ImageButton btnPlus;
        ImageButton btnMinus;
        ImageView btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            productImage = itemView.findViewById(
                    R.id.item_cart_image
            );

            productTitle = itemView.findViewById(
                    R.id.item_cart_title
            );

            productPrice = itemView.findViewById(
                    R.id.item_cart_price
            );

            productQuantity = itemView.findViewById(
                    R.id.item_cart_quantity
            );

            btnPlus = itemView.findViewById(
                    R.id.item_cart_btn_plus
            );

            btnMinus = itemView.findViewById(
                    R.id.item_cart_btn_minus
            );

            btnRemove = itemView.findViewById(
                    R.id.item_cart_remove
            );
        }
    }

    // ============================================================
    // INTERFACES
    // ============================================================
    public interface OnQuantityChangeListener {
        void onChange(CartItem cartItem);
    }

    public interface OnRemoveListener {
        void onRemove(int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    public interface OnItemDoubleTapListener {
        void onItemDoubleTap(int position);
    }
}