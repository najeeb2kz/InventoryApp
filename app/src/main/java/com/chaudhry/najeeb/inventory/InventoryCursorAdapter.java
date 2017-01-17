package com.chaudhry.najeeb.inventory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.chaudhry.najeeb.inventory.data.InventoryContract.InventoryEntry;
import com.chaudhry.najeeb.inventory.data.InventoryDbHelper;


public class InventoryCursorAdapter extends CursorAdapter {

    // Constructor
    // @param context = The context
    // @param c = The cursor from which to get the data.
    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flag */);
    }


    // Makes a new blank list item view. No data is set (or bound) to the views yet.
    // @param context = app context
    // @param cursor = The cursor from which to get the data. The cursor is already moved to the correct position
    // @param parent = The parent to which the new view is attached to
    // @return the newly created list item view.
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        //inflate(XmlPullParser parser, ViewGroup root, boolean attachToRoot)
        //Inflate a new view hierarchy from the specified XML node
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }


    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Find individual views that we want to modify in the list_item.xml layout
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView summaryTextView = (TextView) view.findViewById(R.id.summary);
        Button listView1SoldButton = (Button) view.findViewById(R.id.listview_1Sold_button);

        // Find row id where user clicked button and save in Tag to be used in onClick() below
        listView1SoldButton.setTag(cursor.getPosition());

        // Find the index of columns of inventory attributes that we are interested in
        int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_NAME);
        int summaryColumnIndexPrice = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_PRICE);
        int summaryColumnIndexQuantity = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_QUANTITY);
        int summaryColumnIndexSupplier = cursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_SUPPLIER);

        //Read the inventory attributes from the cursor for the current inventory
        String inventoryName = cursor.getString(nameColumnIndex);
        int inventoryPrice = cursor.getInt(summaryColumnIndexPrice);
        int inventoryQuantity = cursor.getInt(summaryColumnIndexQuantity);
        String inventorySupplier = cursor.getString(summaryColumnIndexSupplier);

        // If the inventory supplier is empty string or null, then use some default text
        // that says "Unknown Supplier", so the TextView isn't blank.
        if (TextUtils.isEmpty(inventorySupplier)) {
            inventorySupplier = context.getString(R.string.unknown_supplier);
        }

        // Populate fields with extracted properties
        nameTextView.setText("Name: " + inventoryName);
        summaryTextView.setText("Price: $" + inventoryPrice);
        summaryTextView.append("\t\tQuantity: "  + inventoryQuantity + "\t\tSupplier: " + inventorySupplier);

        // listView1SoldButton
        final Context tempContext = context;
        final Cursor tempCursor = cursor;
        listView1SoldButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Get row id of the row where user clicked button which was saved in Tag above
                int rowID = Integer.parseInt(view.getTag().toString());

                // Read data, in particular inventoryQuantity, from cursor based on rowID
                tempCursor.moveToFirst();
                // Find the index of columns of inventory attributes that we are interested in
                int idColumnIndex = tempCursor.getColumnIndex(InventoryEntry._ID);
                int tempQuantityColumnIndex = tempCursor.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_QUANTITY);

                // Move cursor to location in list where user clicked on the button
                tempCursor.move(rowID);
                // Extract out the value we are interested in from the Cursor for the given column index
                // Reminder: temInventoryID is id in database which is AUTOINCREMENTing & rowID is id of row where user clicked
                int tempInventoryId = tempCursor.getInt(idColumnIndex);
                int tempInventoryQuantity = tempCursor.getInt(tempQuantityColumnIndex);

                // Reduce quantity by 1
                if (tempInventoryQuantity > 0) {
                    tempInventoryQuantity--;
                } else {
                    Toast.makeText(tempContext, "Can't reduce quantity, already 0", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Prepare  quantity to be stored/updated in database
                // Create ContentValues object where column names are the keys and extracted quantity value
                // from cursor is the values
                ContentValues values = new ContentValues();
                values.put(InventoryEntry.COLUMN_INVENTORY_QUANTITY, tempInventoryQuantity);

                // Set up WHERE clause, e.g: WHERE NAME=PAPER, WHERE _ID=4;
                String selection = InventoryEntry._ID + " = ? ";
                String[] selectionArgs = {Integer.toString(tempInventoryId)};

                // Update inventory into the provider, returning number of rows updated
                InventoryDbHelper mDbHelper = new InventoryDbHelper(tempContext);  // Create database helper

                // Gets the data repository in write mode
                SQLiteDatabase db = mDbHelper.getWritableDatabase();

                // Update database.  Returned int the number of rows affected
                int numberOfRowsAffected = db.update(InventoryEntry.TABLE_NAME, values, selection, selectionArgs);

                // Show a toast message depending on whether or not the quantity update was successful
                if (numberOfRowsAffected < 1) {
                    Toast.makeText(tempContext, "Error with reducing quantity by 1", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(tempContext, "Quantity reduced by 1", Toast.LENGTH_SHORT).show();
                }
                if (numberOfRowsAffected != 0) {
                    tempContext.getContentResolver().notifyChange(InventoryEntry.CONTENT_URI, null);
                }
            }
        });

    }
}
