package lk.jiat.bonsaimart.adapter;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Locale;

import lk.jiat.bonsaimart.R;
import lk.jiat.bonsaimart.model.Order;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    private List<Order> orders;

    public OrderAdapter(List<Order> orders) {
        this.orders = orders;
    }

    @NonNull
    @Override
    public OrderAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderAdapter.ViewHolder holder, int position) {
        Order order = orders.get(position);

        holder.orderId.setText("Order #" + (order.getOrderId() != null ? order.getOrderId() : String.valueOf(position + 1)));
        holder.orderStatus.setText(order.getStatus());
        holder.orderTotal.setText(String.format(Locale.US, "LKR %,.2f", order.getTotalAmount()));

        if (order.getOrderDate() != null) {
            String date = DateFormat.format("MMM dd, yyyy", order.getOrderDate().toDate()).toString();
            holder.orderDate.setText(date);
        }

        if (order.getDeliveryDate() != null) {
            String dDate = DateFormat.format("MMM dd, yyyy", order.getDeliveryDate().toDate()).toString();
            holder.deliveryDate.setText(dDate);
        }

        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            Order.OrderItem firstItem = order.getOrderItems().get(0);
            holder.orderProductName.setText(firstItem.getProductName());

            if (firstItem.getProductImage() != null) {
                Glide.with(holder.itemView.getContext())
                        .load(firstItem.getProductImage())
                        .into(holder.orderProductImage);
            }

            if (order.getOrderItems().size() > 1) {
                holder.orderItemCount.setVisibility(View.VISIBLE);
                holder.orderItemCount.setText("and " + (order.getOrderItems().size() - 1) + " more items");
            } else {
                holder.orderItemCount.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderId, orderStatus, orderDate, deliveryDate, orderTotal, orderProductName, orderItemCount;
        ImageView orderProductImage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.order_id);
            orderStatus = itemView.findViewById(R.id.order_status);
            orderDate = itemView.findViewById(R.id.order_date);
            deliveryDate = itemView.findViewById(R.id.delivery_date);
            orderTotal = itemView.findViewById(R.id.order_total);
            orderProductName = itemView.findViewById(R.id.order_product_name);
            orderItemCount = itemView.findViewById(R.id.order_item_count);
            orderProductImage = itemView.findViewById(R.id.order_product_image);
        }
    }
}
