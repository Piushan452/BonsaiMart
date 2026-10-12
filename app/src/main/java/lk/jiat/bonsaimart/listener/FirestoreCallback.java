package lk.jiat.bonsaimart.listener;

public interface FirestoreCallback<T> {
    void onCallback(T data);
}
