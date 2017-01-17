package com.chaudhry.najeeb.inventory.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;


public final class InventoryContract {
    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private
    private InventoryContract() { }

    // Following are steps to create Content Uri content://com.chaudhry.najeeb.inventory
    //
    // The "Content authority" is a name for the entire content provider, similar to the
    // relationship between a domain name and its website.  A convenient string to use for the
    // content authority is the package name for the app, which is guaranteed to be unique on the
    // device.
    public static final String CONTENT_AUTHORITY = "com.chaudhry.najeeb.inventory";
    // Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
    // the content provider.
    // BASE_CONTENT_URI: content://com.chaudhry.najeeb.inventory
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    // Possible path (appended to base content URI for possible URI's)
    // For instance, content://com.chaudhry.najeeb.inventory/inventory/ is a valid path for
    // looking at inventory data. content://com.chaudhry.najeeb.inventory/staff/ will fail,
    // as the ContentProvider hasn't been given any information on what to do with "staff".
    public static final String PATH_INVENTORY = "inventory";


    // Inner class that defines the table contents
    public static class InventoryEntry implements BaseColumns {

        // Constant for table name
        public static final String TABLE_NAME = "inventory";

        // Constants for column names
        //String _ID from BaseColumns interface  //This is necessary to use CursorAdapter class
        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_INVENTORY_NAME = "name";
        public static final String COLUMN_INVENTORY_PRICE = "price";
        public static final String COLUMN_INVENTORY_QUANTITY = "quantity";
        public static final String COLUMN_INVENTORY_SUPPLIER = "supplier";
        public static final String COLUMN_INVENTORY_BLOB = "blob";

        // Constants photo
        public static final int PHOTO_KEEP_CURRENT_PHOTO= 0;
        public static final int PHOTO_PHOTO_FROM_GALLERY = 1;
        public static final int PHOTO_TAKE_PHOTO_WITH_CAMERA= 2;

        // Inside each of the Entry classes in the contract, we create a full URI for the class as a
        // constant called CONTENT_URI. The Uri.withAppendedPath() method appends the BASE_CONTENT_URI (which
        // contains the scheme and the content authority) to the path segment.
        // CONTENT_URI: content://com.chaudhry.najeeb.inventory/inventory
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_INVENTORY);

        // The MIME type of the #CONTENT_URI for a list of inventory.
        // ContentResolver class: CURSOR_DIR_BASE_TYPE maps to the constant "vnd.android.cursor.dir"
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_INVENTORY;

        // The MIME type of the #CONTENT_URI for a single inventory.
        // CURSOR_ITEM_BASE_TYPE maps to the constant “vnd.android.cursor.item”
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_INVENTORY;
    }
}
