package lk.jiat.bonsaimart.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import lk.jiat.bonsaimart.model.CartItem;

public class CartDbHelper extends SQLiteOpenHelper {

    // ============================================================
    // DATABASE INFO
    // ============================================================
    private static final String DB_NAME = "bonsai_cart.db";
    private static final int DB_VERSION = 1;

    // Table name
    public static final String TABLE_CART = "cart_items";

    // Columns
    public static final String COL_ID = "id";
    public static final String COL_PRODUCT_ID = "product_id";
    public static final String COL_QUANTITY = "quantity";
    public static final String COL_ATTRIBUTES = "attributes"; // JSON string

    // ============================================================
    // CONSTRUCTOR
    // ============================================================
    public CartDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // ============================================================
    // CREATE TABLE (app එක පළමු වතාවට open කරාම run වෙනවා)
    // ============================================================
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_CART + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PRODUCT_ID + " TEXT NOT NULL, " +
                COL_QUANTITY + " INTEGER NOT NULL, " +
                COL_ATTRIBUTES + " TEXT)";

        db.execSQL(createTable);
    }

    // ============================================================
    // UPGRADE TABLE (database version එක වෙනස් වුනාම)
    // ============================================================
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CART);
        onCreate(db);
    }

    // ============================================================
    // ADD ITEM TO CART
    // ============================================================
    public void addToCart(String productId, int quantity, String attributesJson) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_PRODUCT_ID, productId);
        values.put(COL_QUANTITY, quantity);
        values.put(COL_ATTRIBUTES, attributesJson);

        // insert() method එකෙන් row එකක් add කරනවා
        db.insert(TABLE_CART, null, values);
        db.close();
    }

    // ============================================================
    // GET ALL CART ITEMS
    // ============================================================
    public List<CartItem> getAllCartItems() {
        List<CartItem> cartItems = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_CART, null);

        if (cursor.moveToFirst()) {
            do {
                String productId = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRODUCT_ID));
                int quantity = cursor.getInt(cursor.getColumnIndexOrThrow(COL_QUANTITY));
                String attributesJson = cursor.getString(cursor.getColumnIndexOrThrow(COL_ATTRIBUTES));

                // TODO: attributesJson parse කරලා CartItem එකක් හදන්න
                CartItem cartItem = new CartItem(productId, quantity, null);
                cartItems.add(cartItem);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();

        return cartItems;
    }

    // ============================================================
    // UPDATE QUANTITY
    // ============================================================
    public void updateQuantity(String productId, int newQuantity) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COL_QUANTITY, newQuantity);

        db.update(TABLE_CART, values,
                COL_PRODUCT_ID + " = ?",
                new String[]{productId});

        db.close();
    }

    // ============================================================
    // REMOVE ITEM
    // ============================================================
    public void removeItem(String productId) {
        SQLiteDatabase db = getWritableDatabase();

        db.delete(TABLE_CART,
                COL_PRODUCT_ID + " = ?",
                new String[]{productId});

        db.close();
    }

    // ============================================================
    // CLEAR ALL CART ITEMS
    // ============================================================
    public void clearCart() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_CART, null, null);
        db.close();
    }

    // ============================================================
    // CHECK IF PRODUCT EXISTS
    // ============================================================
    public boolean isProductInCart(String productId) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + TABLE_CART + " WHERE " + COL_PRODUCT_ID + " = ?",
                new String[]{productId});

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();

        return exists;
    }
}