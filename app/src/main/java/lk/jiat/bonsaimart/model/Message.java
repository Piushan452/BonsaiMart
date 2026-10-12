package lk.jiat.bonsaimart.model;

import com.google.firebase.Timestamp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private String id;
    private String title;
    private String content;
    private Timestamp timestamp;
    private boolean read;
}
