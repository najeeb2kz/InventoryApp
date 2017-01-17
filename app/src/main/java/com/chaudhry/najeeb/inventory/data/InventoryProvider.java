package com.chaudhry.najeeb.inventory.data;


import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.chaudhry.najeeb.inventory.data.InventoryContract.InventoryEntry;


public class InventoryProvider extends ContentProvider {

    // Tag for the log messages
    public static final String LOG_TAG = InventoryProvider.class.getSimpleName();

    // URI matcher code for the content URI for the inventory table
    private static final int INVENTORY = 100;

    // URI matcher code for the content URI for a single item in the inventory table
    private static final int INVENTORY_ID = 101;

    // UriMatcher object to match a content URI to a corresponding code.
    // The input passed into the constructor represents the code to return for the root URI.
    // It's common to use NO_MATCH as the input for this case.
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static initializer. This is run the first time anything is called from this class.
    static {
        // The calls to addURI() go here, for all of the content URI patterns that the provider
        // should recognize. All paths added to the UriMatcher have a corresponding code to return
        // when a match is found.
        // if uri is content://com.chaudhry.najeeb.inventory/inventory then return int code INVENTORY (return 100)
        // if uri is content://com.chaudhry.najeeb.inventory/inventory/3 then return int code INVENTORY_ID (return 101)
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY, INVENTORY);
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY + "/#", INVENTORY_ID);
    }

    // Database helper object
    private InventoryDbHelper mDbHelper;



    @Override
    public boolean onCreate() {
        // Create database helper
        mDbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:  // if returned code is 100 then do following
                // For the INVENTORY code, query the inventory table directly with the given
                // projection, selection, selection arguments, and sort order. The cursor
                // could contain multiple rows of the inventory table.
                cursor = database.query(InventoryEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            case INVENTORY_ID:  //if returned code is 101 then do following
                // For the INVENTORY_ID code, extract out the ID from the URI.
                // For an example URI such as "content://com.example.android.inventory/inventory/3",
                // the selection will be "_id=?" and the selection argument will be a
                // String array containing the actual ID of 3 in this case.
                //
                // For every "?" in the selection, we need to have an element in the selection
                // arguments that will fill in the "?". Since we have 1 question mark in the
                // selection, we have 1 String in the selection arguments' String array.
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                // Above ContentUris.parseId(uri) is reading 5 from uri: content://com.chaudhry.najeeb.inventory/inventory/5

                // This will perform a query on the inventory table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(InventoryEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }

        // Set notification URI on the cursor
        // so we know what content URI cursor was created for
        // If data at this URI changes, then we know we need to update cursor
        // First parameter is ConetentResolver() so that the listener which in our case is catalog activity
        // that's attached to this resolver will automatically be notified
        // 2nd parameter is URI which is he content URI we want to watch
        // Doing this sets notification URI for any cursor returned by the query method
        // So if data at this URI changes we know we need to update the cursor
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }


    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return InventoryEntry.CONTENT_LIST_TYPE;
            case INVENTORY_ID:
                return InventoryEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }


    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return insertInventory(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }


    // Insert an inventory item into the database with the given content values. Return the new content URI
    // for that specific row in the database.
    private Uri insertInventory(Uri uri, ContentValues values) {

        // Check that the name is not null
        String name = values.getAsString(InventoryEntry.COLUMN_INVENTORY_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Inventory requires a name");
        }

        // Check that the price is valid
        Integer price = values.getAsInteger(InventoryEntry.COLUMN_INVENTORY_PRICE);
        if (price != null && price < 0) {
            throw new IllegalArgumentException("Inventory requires valid price");
        }

        // If the quantity is provided, check that it's greater than or equal to 0
        Integer quantity = values.getAsInteger(InventoryEntry.COLUMN_INVENTORY_QUANTITY);
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Inventory requires valid quantity");
        }

        // Check that the supplier is not null
        String supplier = values.getAsString(InventoryEntry.COLUMN_INVENTORY_SUPPLIER);
        if (supplier == null) {
            throw new IllegalArgumentException("Inventory requires valid supplier");
        }

        // Check if blob image is not null
        byte[] blobByteArray = values.getAsByteArray(InventoryEntry.COLUMN_INVENTORY_BLOB);
        if (blobByteArray == null) {
            throw new IllegalArgumentException("Inventory requires a valid image");
        }

        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the new inventory with the given values
        long id = database.insert(InventoryEntry.TABLE_NAME, null, values);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that data has changed for the inventory content URI
        // 2nd parameter is optional content observer parameter
        // Content Observer is meant as a class that receives callbacks or changes to content.  If we
        // pass in null, by default the cursor adaptor object will get notified.  So that means loader
        // callbacks will still be automatically triggered.
        // The content URI we are interested in is this: content://com.chaudhry.najeeb.inventory/inventory
        // So having this call with this uri will update the cursors such as catalog activity cursor
        // which is listening for notifications for this particular URI
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        return ContentUris.withAppendedId(uri, id);
    }


    //Delete the data at the given selection and selection arguments.
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                // Delete all rows that match the selection and selection args
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, selection, selectionArgs);

                // If 1 or more rows were deleted, then notify all listeners that the data at the
                // given URI has changed
                if (rowsDeleted != 0) getContext().getContentResolver().notifyChange(uri, null);

                return rowsDeleted;
            case INVENTORY_ID:
                // Delete a single row given by the ID in the URI
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, selection, selectionArgs);

                // If 1 or more rows were deleted, then notify all listeners that the data at the
                // given URI has changed
                if (rowsDeleted != 0) getContext().getContentResolver().notifyChange(uri, null);

                return rowsDeleted;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
    }

    /* You’ll notice that the INVENTORY and INVENTORY_ID cases both call the updateInventory() method to do the actual
    database operation. The only difference is that in the INVENTORY_ID case, we have 2 more lines of code to
    manually set the selection string and selection arguments array to pinpoint a single inventory based on
    the incoming inventory URI. Similar to the logic in the InventoryProvider query() method, we set the selection
    string to be “id=?” and the selection args to be the row ID that we care about (by extracting out
    the ID from the URI using ContentUris.parseId() method). */
    //Updates the data at the given selection and selection arguments, with the new ContentValues.
    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return updateInventory(uri, contentValues, selection, selectionArgs);
            case INVENTORY_ID:
                // For the INVENTORY_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                return updateInventory(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    // Update inventory in the database with the given content values. Apply the changes to the rows
    // specified in the selection and selection arguments (which could be 0 or 1 or more inventory).
    // Return the number of rows that were successfully updated.
    private int updateInventory(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        // Check if key's value exist then check that the name is not null
        if (values.containsKey(InventoryEntry.COLUMN_INVENTORY_NAME)) {
            String name = values.getAsString(InventoryEntry.COLUMN_INVENTORY_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Inventory requires a name");
            }
        }

        // Check that the price is valid
        if (values.containsKey(InventoryEntry.COLUMN_INVENTORY_NAME)) {
            Integer price = values.getAsInteger(InventoryEntry.COLUMN_INVENTORY_PRICE);
            if (price != null && price < 0) {
                throw new IllegalArgumentException("Inventory requires valid price");
            }
        }

        // If the quantity is provided, check that it's greater than or equal to 0
        if (values.containsKey(InventoryEntry.COLUMN_INVENTORY_QUANTITY)) {
            Integer quantity = values.getAsInteger(InventoryEntry.COLUMN_INVENTORY_QUANTITY);
            if (quantity != null && quantity < 0) {
                throw new IllegalArgumentException("Inventory requires valid quantity");
            }
        }

        // Check that the supplier is not null
        if (values.containsKey(InventoryEntry.COLUMN_INVENTORY_SUPPLIER)) {
            String supplier = values.getAsString(InventoryEntry.COLUMN_INVENTORY_SUPPLIER);
            if (supplier == null) {
                throw new IllegalArgumentException("Inventory requires valid supplier");
            }
        }

        // Check if blob image is not null
        if (values.containsKey(InventoryEntry.COLUMN_INVENTORY_BLOB)) {
            byte[] blobByteArray = values.getAsByteArray(InventoryEntry.COLUMN_INVENTORY_BLOB);
            if (blobByteArray == null) {
                throw new IllegalArgumentException("Inventory requires a valid image");
            }
        }

        // If there are no values to update, then don't try to update the database
        if (values.size() == 0) {
            return 0;
        }

        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Perform the update on the database and get the number of rows affected
        int rowsUpdated = database.update(InventoryEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the new URI with the number of rows returned from update() appended at the end
        return rowsUpdated;
    }

}
