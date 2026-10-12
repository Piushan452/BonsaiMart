package lk.jiat.bonsaimart.model;

import com.google.firebase.firestore.Exclude;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CartItem {

    @Exclude
    private String documentId;

    private String productId;

    private int quantity;

    private List<Attribute> attributes;

    public CartItem(String productId, int quantity, List<Attribute> attributes) {
        this.productId = productId;
        this.quantity = quantity;
        this.attributes = attributes;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Attribute {

        private String name;

        private String value;
    }
}