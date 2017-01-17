package com.chaudhry.najeeb.inventory;

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.chaudhry.najeeb.inventory.data.InventoryContract.InventoryEntry;

import java.io.ByteArrayOutputStream;




// Displays list of inventory that were entered and stored in the SQLite
public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // Identifies a particular Loader being used in this component
    private static final int INVENTORY_LOADER = 0;  //can use any integer

    // Adapter for ListView
    InventoryCursorAdapter mCursorAdapter;

    // list_view_button
    private Button listViewButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // list_view_button
        //listViewButton = (Button) view.findViewById(R.id.list_view_button);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Find ListView which will be populated with the pet data
        ListView inventoryListView = (ListView) findViewById(R.id.list);

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById(R.id.empty_view);
        inventoryListView.setEmptyView(emptyView);

        // Setup cursor adapter using cursor to create a list item for each row of pet data in the cursor
        mCursorAdapter = new InventoryCursorAdapter(this, null);

        // Attach cursor adapter to the ListView
        inventoryListView.setAdapter(mCursorAdapter);

        // Set up item click listener on listView
        inventoryListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            // AdapterView<?> parent: AdapterView is just list view
            // View view: view is particular view of the item
            // int position: position of item in list view
            // long id: id of the item
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);

                // Form the content URI that represents the specific inventory that was clicked on by appending
                // id passed as input to this method onto the InventoryEntry.CONTENT_URI
                // Both work, either      Uri.withAppendedPath(InventoryEntry.CONTENT_URI, String.valueOf(id));
                // or                     ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, id);
                Uri currentInventoryUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, id);

                // Set the Uri on the data field of the intent
                intent.setData(currentInventoryUri);

                //Launch EditorActivity to display the data for the current pet
                startActivity(intent);
            }
        });

        // Initialize the Query
        // Initializes the CursorLoader. The URL_LOADER value is eventually passed to onCreateLoader()
        // LoaderManager > initLoader(int id, Bundle args, LoaderCallbacks<D> callback)
        // initLoader() Returns Loader<D>
        getLoaderManager().initLoader(INVENTORY_LOADER, null, this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertInventory();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                deleteAllInventory();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    // This method inserts dummy data into databasae
    // Image is in drawable folder.  Create bitmap of this image first.
    private void insertInventory() {

        // Call method that will create bitmap of default image and convert bitmap image to byte array
        byte[] byteArray = prepareDefaultImageToBeSavedInSQLiteDatabase();

        // Create a ContentValues object where column names are the keys,
        // and Paper inventory attributes are the values.
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();

        values.put(InventoryEntry.COLUMN_INVENTORY_NAME, "Paper 50");
        values.put(InventoryEntry.COLUMN_INVENTORY_PRICE, 2);
        values.put(InventoryEntry.COLUMN_INVENTORY_QUANTITY, 100);
        values.put(InventoryEntry.COLUMN_INVENTORY_SUPPLIER, "National Supply Inc");
        values.put(InventoryEntry.COLUMN_INVENTORY_BLOB, byteArray);

        // Insert a new row for Paper into the provider using the ContentResolver.
        // Use the {@link PetEntry#CONTENT_URI} to indicate that we want to insert
        // into the pets database table.
        // Receive the new content URI that will allow us to access Toto's data in the future.
        Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);
    }


    private void deleteAllInventory() {
        int numOfRowsDeleted = getContentResolver().delete(InventoryEntry.CONTENT_URI, null, null);
        if (numOfRowsDeleted == 0) {
            Toast.makeText(this, getString(R.string.editor_delete_inventory_failed), Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(this, getString(R.string.editor_delete_inventory_successful), Toast.LENGTH_LONG).show();
        }
    }


    // This method queries data from database
    // This method creates CursorLoader and in doing so defines the data that you want to query from content provider
    // This method is automatically called when getLoaderManager().initLoader(INVENTORY_LOADER, null, this) is
    // read in onCreate method
    // This method is also automatically called when something changes in database
    // When this method is finished loading cursor with data, the cursor is passed to onLoadFinished() and
    // onLoadFinished() is called next automatically
    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle args) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_INVENTORY_NAME,
                InventoryEntry.COLUMN_INVENTORY_PRICE,
                InventoryEntry.COLUMN_INVENTORY_QUANTITY,
                InventoryEntry.COLUMN_INVENTORY_SUPPLIER };

        // Takes action based on the ID of the Loader that's being created
        switch (loaderID) {
            case INVENTORY_LOADER:
                // Returns a new CursorLoader
                return new CursorLoader(
                        this,                       // Parent activity context getActivity()
                        InventoryEntry.CONTENT_URI, // Table to query
                        projection,                 // Projection to return
                        null,                       // No selection clause
                        null,                       // No selection arguments
                        null                        // Default sort order
                );
            default:
                // An invalid id was passed in
                return null;
        }
    }


    // This method is called automatically when loader has finished loading cursor with data in onCreateLoader()
    // This method will have queried data in cursor.  This cursor will be accessable automatially to bindView()
    // method in InventoryCursorAdapter.java
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update inventoryCursorAdapter with this new cursor containing updated inventory data
        mCursorAdapter.swapCursor(data);
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Callback called when the data needs to be deleted
        mCursorAdapter.swapCursor(null);
    }


    private byte[] prepareDefaultImageToBeSavedInSQLiteDatabase() {
        // Create bitmap of image
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_dummy_image);
        // Convert bitmap image to byte array
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        return byteArray;
    }
}
