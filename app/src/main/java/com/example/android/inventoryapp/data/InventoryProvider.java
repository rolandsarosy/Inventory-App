package com.example.android.inventoryapp.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.example.android.inventoryapp.R;
import com.example.android.inventoryapp.data.InventoryContract.ProductsEntry;

public class InventoryProvider extends ContentProvider {

    // Log tag for debugging purposes
    private static final String LOG_TAG = InventoryProvider.class.getSimpleName();

    // URI matcher code for a single product
    private static final int PRODUCT_TYPE_SINGLE = 0;

    // URI matcher code for a list of products
    private static final int PRODUCT_TYPE_LIST = 1;

    // Global variable for the DB helper
    private InventoryDbHelper mDbHelper;

    // URI matcher instance with the passed NO_MATCH variable
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static URI matcher initializer
    static {
        // Match a single product: content://com.example.android/products/#
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY,
                InventoryContract.PATH_PRODUCTS + "/#", PRODUCT_TYPE_SINGLE);

        // Match a list of products: content://com.example.android/products
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY,
                InventoryContract.PATH_PRODUCTS, PRODUCT_TYPE_LIST);
    }

    // Initialize the provider with an instance of the InventoryDbHelper class
    @Override
    public boolean onCreate() {
        mDbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        // Get a readable database
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Declare the cursor
        Cursor cursor;

        // Match given URI with the UriMatcher
        int match = sUriMatcher.match(uri);

        switch (match) {
            case PRODUCT_TYPE_LIST:
                // Query the database for a list of products
                cursor = db.query(ProductsEntry.TABLE_NAME, projection,
                        selection, selectionArgs, null, null, sortOrder);
                break;
            case PRODUCT_TYPE_SINGLE:
                // Query the database for a single product
                selection = ProductsEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = db.query(ProductsEntry.TABLE_NAME, projection,
                        selection, selectionArgs, null, null, sortOrder);
                break;
            default:
                Toast.makeText(getContext(), R.string.content_provider_hint_uri_match_error, Toast.LENGTH_SHORT).show();
                return null;
        }

        // Set the notification URI on the cursor
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        // Match the given URI with the UriMatcher
        int match = sUriMatcher.match(uri);

        switch (match) {
            case PRODUCT_TYPE_SINGLE:
                return ProductsEntry.CONTENT_ITEM_TYPE;
            case PRODUCT_TYPE_LIST:
                return ProductsEntry.CONTENT_LIST_TYPE;
            default:
                Toast.makeText(getContext(), R.string.content_provider_hint_mime_match_error, Toast.LENGTH_SHORT).show();
                return null;
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        // Get a writable database
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Variable to hold the returned row's ID
        long rowId;

        // Match the given URI with the UriMatcher
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCT_TYPE_LIST:
                rowId = db.insert(ProductsEntry.TABLE_NAME, null, contentValues);
                break;
            default:
                Toast.makeText(getContext(), R.string.content_provider_hint_insert_error, Toast.LENGTH_SHORT).show();
                return null;
        }
        // Check whether the insertion was successful or not. Return early (and null) if not.
        if (rowId == -1) {
            Log.i(LOG_TAG, "Insertion failed");
            return null;
        }
        // Notify all that the data has changed for the URI
        getContext().getContentResolver().notifyChange(uri, null);

        return ContentUris.withAppendedId(uri, rowId);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        // Get a writable database
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Variable to hold the number of rows affected
        int affectedRows;

        // Match the given URI with the UriMatcher
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCT_TYPE_LIST:
                // Drop the whole products table and notify the observers about the change
                try {
                    affectedRows = db.delete(ProductsEntry.TABLE_NAME, selection, selectionArgs);
                } finally {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                break;
            case PRODUCT_TYPE_SINGLE:
                try {
                    // Delete a single row of the products table and notify the observers about the change
                    selection = ProductsEntry._ID + "=?";
                    selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                    affectedRows = db.delete(ProductsEntry.TABLE_NAME, selection, selectionArgs);
                } finally {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
                break;
            default:
                Toast.makeText(getContext(), R.string.content_provider_hint_deletion_error, Toast.LENGTH_SHORT).show();
                // Return 0 which is the same as no rows affected
                affectedRows = 0;
        }

        return affectedRows;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        // Get a writable database
        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        // Match the given URI with the UriMatcher and notify all observers about the change
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PRODUCT_TYPE_LIST:
                try {
                    return db.update(ProductsEntry.TABLE_NAME, contentValues, selection, selectionArgs);
                } finally {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
            case PRODUCT_TYPE_SINGLE:
                try {
                    selection = ProductsEntry._ID + "=?";
                    selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                    return db.update(ProductsEntry.TABLE_NAME, contentValues, selection, selectionArgs);
                } finally {
                    getContext().getContentResolver().notifyChange(uri, null);
                }
            default:
                Toast.makeText(getContext(), R.string.content_provider_hint_update_error, Toast.LENGTH_SHORT).show();
                return 0;
        }
    }
}
