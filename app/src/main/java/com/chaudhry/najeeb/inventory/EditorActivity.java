package com.chaudhry.najeeb.inventory;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.chaudhry.najeeb.inventory.data.InventoryContract.InventoryEntry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;




// Allows user to create a new inventory or edit an existing one.
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    // Identifies a particular Loader being used in this component
    private static final int EDIT_INVENTORY_LOADER = 1;  //can use any integer

    private EditText mNameEditText;     // EditText field to enter the inventory name
    private EditText mPriceEditText;    // EditText field to enter the inventory price
    private EditText mQuantityEditText; // EditText field to enter the inventory quantity
    private EditText mSupplierEditText; // EditText field to enter the inventory supplier
    private Spinner mPhotoSpinner;      // EditText field to enter the inventory item photo
    private ImageView mBlobImageView;   // ImageView field to enter the inventory blob image
    private Button mSaleButton;         // sale_button
    private Button mShipmentButton;     // shipment_button
    private Button mDeleteButton;       // delete_button
    private Button mOrderMoreButton;    // order_more_button

    // Set photo of inventory item to "No Photo at the moment"
    // The possible values are: 0 for "No Photo at the moment"
    //                          1 for "Photo from Gallery"
    //                          2 for "Take photo with camera"
    private int mPhoto = InventoryEntry.PHOTO_KEEP_CURRENT_PHOTO;

    private int PICK_IMAGE_REQUEST = 1;             // This to be used when selecting image from gallery
    static final int REQUEST_IMAGE_CAPTURE = 2;     // This to be used when taking image through camera

    byte[] byteArray;               // For photo

    // Uri received from CatalogActivity when user clicked on any item from ListView.  In this case EditorActivity
    // will pop up form filled with info of that item.  User can edit that info for update
    private Uri currentInventoryUri;

    // For update of Inventory
    // If user clicked any part of form to make changes but then pushed up/back button, discard/edit dialog will pop up
    // onTouchListener will be used to see if user clicked on any part of form to make changes
    // Boolean flag that keeps track of whether the inventory has been edited (true) or not (false)
    private boolean mInventoryHasChanged = false;

    // OnTouchListener that listens for any user touches on a View, implying that user intended to modify
    // the view, and we change the mInventoryHasChanged boolean to true.
    // Implementing View.OnTouchListener interface using anonymous class
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mInventoryHasChanged = true;
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Use getIntent() and getData() to get associated URL
        Intent intent = getIntent();
        currentInventoryUri = intent.getData();

        // Set title of EditorActivity on which situation we have
        // If the EditorActivity was opened using the ListView then we will use uri of inventory so change app
        // bar to say "Edit Inventory"
        // otherwise if this is new inventory, uri is null, so change app bar to say "Add an Inventory"
        if (currentInventoryUri == null) {
            this.setTitle(getString(R.string.editor_activity_title_new_inventory));
            // This is insert mode for a new inventory so no need to show delete option menu
            // Invalidate the options menu, so the "Delete" menu option can be hidden
            // It doesn't make sense to delete a inventory that hasn't been created yet
            invalidateOptionsMenu();
        } else {
            this.setTitle(getString(R.string.editor_activity_title_edit_inventory));

            // Initialize the Query
            // Initializes the CursorLoader. The URL_LOADER value is eventually passed to onCreateLoader()
            // LoaderManager > initLoader(int id, Bundle args, LoaderCallbacks<D> callback)
            // initLoader() Returns Loader<D>
            getLoaderManager().initLoader(EDIT_INVENTORY_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_inventory_name);
        mPriceEditText = (EditText) findViewById(R.id.edit_inventory_price);
        mQuantityEditText = (EditText) findViewById(R.id.edit_inventory_quantity);
        mSupplierEditText = (EditText) findViewById(R.id.edit_inventory_supplier);
        mBlobImageView = (ImageView) findViewById(R.id.blob_image);
        mPhotoSpinner = (Spinner) findViewById(R.id.spinner_photo);
        mSaleButton = (Button) findViewById(R.id.sales_button);
        mShipmentButton = (Button) findViewById(R.id.shipment_button);
        mDeleteButton = (Button) findViewById(R.id.delete_button);
        mOrderMoreButton = (Button) findViewById(R.id.order_more_button);

        // If user clicked on FAB button meaning insert inventory (not update) then there is no URL
        // so don't show these buttons: sale_button, shipment_button, delete_button, order_more_button
        if (currentInventoryUri == null) {
            mSaleButton.setVisibility(View.INVISIBLE);
            mShipmentButton.setVisibility(View.INVISIBLE);
            mDeleteButton.setVisibility(View.INVISIBLE);
            mOrderMoreButton.setVisibility(View.INVISIBLE);
        }

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them/any. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);
        mBlobImageView.setOnTouchListener(mTouchListener);
        mPhotoSpinner.setOnTouchListener(mTouchListener);

        setupSpinner();
    }


    // Setup the dropdown spinner that allows the user to select the gender of the inventory.
    private void setupSpinner() {
        // Create adapter for spinner. The list options are from the String array it will use
        // the spinner will use the default layout
        ArrayAdapter photoSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_photo_options, android.R.layout.simple_spinner_item);

        // Specify dropdown layout style - simple list view with 1 item per line
        photoSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        // Apply the adapter to the spinner
        mPhotoSpinner.setAdapter(photoSpinnerAdapter);

        // Set the integer mSelected to the constant values
        mPhotoSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.photo_photo_from_gallery))) {
                        mPhoto = InventoryEntry.PHOTO_PHOTO_FROM_GALLERY;
                        Intent intent = new Intent();
                        // Show only images from media storage, no videos or anything else
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        // Always show the chooser (if there are multiple options available)
                        // Calling  startActivityForResult(Intent, int) version
                        // The result will come back through onActivityResult(int, int, Intent) method
                        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
                        setResult(RESULT_OK, intent);
                    } else if (selection.equals(getString(R.string.photo_take_photo_with_camera))) {
                        mPhoto = InventoryEntry.PHOTO_TAKE_PHOTO_WITH_CAMERA;
                        // Check if camera is available
                        if (!(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))) {
                            Toast.makeText(EditorActivity.this, getString(R.string.editor_no_camera_on_device),
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                        }

                    } else {
                        mPhoto = InventoryEntry.PHOTO_KEEP_CURRENT_PHOTO;
                        // Create bitmap of image
                        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.ic_dummy_image);
                        // Convert bitmap image to byte array
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        //byte[] defaultImageByteArray = stream.toByteArray();
                        byteArray = stream.toByteArray();
                    }
                }
            }

            // Because AdapterView is an abstract class, onNothingSelected must be defined
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mPhoto = InventoryEntry.PHOTO_KEEP_CURRENT_PHOTO;
            }
        });
    }


    // Get user input from editor and INSERT new inventory into database if (currentInventoryUri == null)  OR
    // Get user input from editor and UPDATE inventory into database if (currentInventoryUri != null)
    private void saveInventory() {

        // Retrieve values entered by user in editor activity
        // trim() eliminates any leading or trailing white spaces
        String nameString = mNameEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String supplierString = mSupplierEditText.getText().toString().trim();
        // Image is already stored in byteArray when setting up spinner

        // Check if user didn't enter any values, that is, text fields are blank
        if (currentInventoryUri == null &&
                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(priceString) &&
                TextUtils.isEmpty(quantityString) && TextUtils.isEmpty(supplierString)) {
                Toast.makeText(this, "        Inventory not saved\nAt least enter mandatory field",
                        Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if name field is not filled out
        if (currentInventoryUri == null && TextUtils.isEmpty(nameString)) {
            Toast.makeText(this, "   Inventory not saved\nName field is mandatory", Toast.LENGTH_SHORT).show();
            return;
        }

        // If the price and quantity are not provided by the user, don't try to parse the string into an
        // integer value. Use 0 by default.
        int priceInt = 0;
        if (!TextUtils.isEmpty(priceString)) {
            priceInt = Integer.parseInt(priceString);
        }
        int quantityInt = 0;
        if (!TextUtils.isEmpty(quantityString)) {
            quantityInt = Integer.parseInt(quantityString);
        }

        // Create a ContentValues object where column names are the keys,
        // and inventory attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_INVENTORY_NAME, nameString);
        values.put(InventoryEntry.COLUMN_INVENTORY_PRICE, priceInt);
        values.put(InventoryEntry.COLUMN_INVENTORY_QUANTITY, quantityInt);
        values.put(InventoryEntry.COLUMN_INVENTORY_SUPPLIER, supplierString);

        // In case of updating inventory, image might be in database already which user selected before.  In that
        // case check if the image is already in database and on screen user chose "keep Current photo" then don't
        // update image, otherwise update image
        String currentValueInSpinner = mPhotoSpinner.getSelectedItem().toString();
        if ((currentInventoryUri == null) ||
                ((currentInventoryUri != null) && (currentValueInSpinner != getString(R.string.photo_keep_current_photo)))) {
            values.put(InventoryEntry.COLUMN_INVENTORY_BLOB, byteArray);
        }

        // insert a new inventory item or edit/update an existing inventory item based on if currentInventoryUri
        // is null (insert) or not null (update)
        if (currentInventoryUri == null) {  //user touched FloatingActionButton to insert new item so uri is null
            // Insert a new item into the provider, returning the content URI for the new inventory uri
            Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_inventory_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_inventory_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {  //user pressed an item from ListView so uri is present
            // Update inventory into the provider, returning number of rows updated
            int rowsAffected = getContentResolver().update(currentInventoryUri, values, null, null);

            // Show a toast message depending on whether or not update was successful
            if (rowsAffected == 0) {
                Toast.makeText(this, getString(R.string.editor_update_inventory_failed), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_update_inventory_successful), Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }


    // invalidateOptionsMenu() is used when contents of menu change so and should be redrawn.
    // invalidateOptionsMenu() is a signal for OS to call onPrepareOptionsMenu() in order to
    // redraw menu options each time an event occurs.
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new inventory, meaning user clicked on fab, hide the "Delete" menu item.
        if (currentInventoryUri == null) {
            //abstract method findItem() is in menu Menu Interface.
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save inventory to database
                saveInventory();
                // finish() exits activity.  Will go back to previous activity.
                finish(); // Call this when your activity is done and should be closed. Activity class.
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Call showDeleteConfirmationDialog() method to delete inventory
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the inventory hasn't changed, continue with navigating up to parent activity
                // which is the CatalogActivity
                if (!mInventoryHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, navigate to parent activity.
                        NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    }
                };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle args) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_INVENTORY_NAME,
                InventoryEntry.COLUMN_INVENTORY_PRICE,
                InventoryEntry.COLUMN_INVENTORY_QUANTITY,
                InventoryEntry.COLUMN_INVENTORY_SUPPLIER,
                InventoryEntry.COLUMN_INVENTORY_BLOB};

        // Takes action based on the ID of the Loader that's being created
        switch (loaderID) {
            case EDIT_INVENTORY_LOADER:
                // Returns a new CursorLoader
                // This loader will execute the ContentProvider's query method on a background thread
                return new CursorLoader(
                        this,                       // Parent activity context getActivity()
                        currentInventoryUri,        // Table to query
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


    // When the data from inventory is loaded into a cursor, onLoadFinished() is called. Here, I’ll first
    // move cursor to it’s first item position. Even though it only has one item, it starts at position -1.
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        //Move cursor to 0th position even though there is only one entry in this query
        if (data.moveToFirst()) {
            // Find the index of columns of inventory attributes that we are interested in
            int nameColumnIndex = data.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_NAME);
            int priceColumnIndex = data.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_PRICE);
            int quantityColumnIndex = data.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_QUANTITY);
            int supplierColumnIndex = data.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_SUPPLIER);
            int blobColumnIndex = data.getColumnIndex(InventoryEntry.COLUMN_INVENTORY_BLOB);

            // Extract out the value from the Cursor for the given column index
            String inventoryName = data.getString(nameColumnIndex);
            int inventoryPrice = data.getInt(priceColumnIndex);
            int inventoryQuantity = data.getInt(quantityColumnIndex);
            String inventorySupplier = data.getString(supplierColumnIndex);
            byte[] inventoryBlobByteArray = data.getBlob(blobColumnIndex);

            // Convert byte array retrieved from database to bitmap
            Bitmap bm = BitmapFactory.decodeByteArray(inventoryBlobByteArray, 0, inventoryBlobByteArray.length);

            // Populate fields with extracted properties, that is, update views
            mNameEditText.setText(inventoryName);
            mPriceEditText.setText(Integer.toString(inventoryPrice));
            mQuantityEditText.setText(Integer.toString(inventoryQuantity));
            mSupplierEditText.setText(inventorySupplier);
            mBlobImageView.setImageBitmap(bm);
        }
    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
        mSupplierEditText.setText("");
        mBlobImageView.setImageDrawable(null);
    }


    @Override
    public void onBackPressed() {
        // If the inventory hasn't changed, continue with handling back button press
        if (!mInventoryHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // User clicked "Discard" button, close the current activity.
                finish();
            }
        };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }


    // Show a dialog that warns the user there are unsaved changes that will be lost
    // if they continue leaving the editor
    //
    // @param discardButtonClickListener is click listener for what to do when user confirms they want to discard their changes
    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners for positive and negative buttons on the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the inventory.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);

        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the inventory.
                deleteInventory();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the inventory.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    // Perform the deletion of the inventory in the database
    private void deleteInventory() {

        // delete a single inventory item, the one user clicked on from CatalogActivyt>ListView
        // delete inventory if currentInventoryUri is not null
        if (currentInventoryUri != null) {
            int numOfRowsDeleted = getContentResolver().delete(currentInventoryUri, null, null);
            if (numOfRowsDeleted == 0) {
                Toast.makeText(this, getString(R.string.editor_delete_inventory_failed), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, getString(R.string.editor_delete_inventory_successful), Toast.LENGTH_LONG).show();
            }
        }
        // Close the activity
        finish();
    }


    // When user chooses to save an image from gallery or from camera, the result of that is handeled in this method
    // This method will get the result of intent from mPhotoSpinner.setOnItemSelectedListener() onItemSelected()
    // either gallery or camera
    // requestCode returned to this method as RESULT_CANCELED or RESULT_OK.  RESULT_OK is returned as -1
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Retrieve image from gallery
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            if (data == null) {
                // Display error
                return;
            }

            Bitmap galleryImageBitmap = null;
            Bitmap resized = null;
            try {
                galleryImageBitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), data.getData());

                // Set image on the image view
                mBlobImageView.setImageBitmap(galleryImageBitmap);

                // Resize gallery image to a smaller size
                try {
                    int newWidth = 200;
                    int newHeight = 200;
                    resized = Bitmap.createScaledBitmap(galleryImageBitmap, newWidth, newHeight, true);
                } catch (IllegalArgumentException iae) {
                    iae.printStackTrace();
                }

                // Assign image to byteArray[] to be saved in SQLite database table in saveInventory
                // outPutStream: The stream to write the compressed data
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                assert resized != null;
                resized.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
                byteArray = outputStream.toByteArray();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Retrieve image taken by camera
        // When image taken with camera, intent sends a smaller version of thumbnail image.  Use
        // requestCode returns RESULT_CANCELED or RESULT_OK or RESULT_FIRST_USER
        // RESULT_OK is returned as -1
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            if (data == null) {
                // Display error
                return;
            }

            Bundle extras = data.getExtras();
            Bitmap cameraImageBitmap = (Bitmap) extras.get("data");

            mBlobImageView.setImageBitmap(cameraImageBitmap);

            // Assign image to byteArray[] to be saved in SQLite database table in saveInventory
            // outPutStream: The stream to write the compressed data
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            assert cameraImageBitmap != null;
            cameraImageBitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
            byteArray = outputStream.toByteArray();
        }
    }


    public void salesButtonClicked(View view) {
        // Call showSalesConfirmationDialog() method to get quantity items sold
        showSalesConfirmationDialog();
    }


    public void shipmentButtonClicked(View view) {
        // Call showShipmentConfirmationDialog() method to get quantity items shipment in
        showShipmentConfirmationDialog();
    }


    public void deleteButtonClicked(View view) {
        showDeleteConfirmationDialog();
    }

    // When this method is clicked, it launches email intent
    public void orderMoreButtonClicked(View view) {
        showOrderMoreConfirmationDialog();
    }


    // When Sales button salesButtonClicked() is pressed, following method is called
    private void showSalesConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners for the positive
        // and negative buttons on the dialog with edittext for user to enter quantity items sold
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(EditorActivity.this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);  // InputType numbers only
        builder.setMessage(R.string.sales_dialog_msg);
        builder.setView(input);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {  // User clicked "Ok" button

                // Read input value
                String inputValueString = input.getText().toString();

                // If user clicked OK without entering any value then simply end the dialog
                if (inputValueString.equals("")) {
                    return;
                }

                // Retrieve quantity value entered by user in editor activity
                // trim() eliminates any leading or trailing white spaces
                String quantityString = mQuantityEditText.getText().toString().trim();

                //Convert both strings to int
                int intInputValue = Integer.parseInt(inputValueString);
                int quantityInt = Integer.parseInt(quantityString);

                // Subtract
                int finalQuantity = quantityInt - intInputValue;

                if (finalQuantity < 0) {
                    // can't be performed since subtracting produces -ve number
                    Toast.makeText(EditorActivity.this, inputValueString + " is not valid quantity",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                else {
                    mQuantityEditText.setText(String.valueOf(finalQuantity));
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the inventory
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // When Shipment button shipmentButtonClicked() is pressed, following method is called
    private void showShipmentConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners for the positive and
        // negative buttons on the dialog with edittext for user to enter quantity items shipment in
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(EditorActivity.this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);  // InputType numbers only
        builder.setMessage(R.string.shipment_dialog_msg);
        builder.setView(input);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) { // User clicked "Ok" button

                // Read input value
                String inputValueString = input.getText().toString();

                // If user clicked OK without entering any value then simply end the dialog
                if (inputValueString.equals("")) {
                    return;
                }

                // Retrieve quantity value entered by user in editor activity
                // trim() eliminates any leading or trailing white spaces
                String quantityString = mQuantityEditText.getText().toString().trim();

                //Convert both strings to int
                int intInputValue = Integer.parseInt(inputValueString);
                int quantityInt = Integer.parseInt(quantityString);

                // Subtract
                int finalQuantity = quantityInt + intInputValue;

                // Set updated quantity in edittext view
                mQuantityEditText.setText(String.valueOf(finalQuantity));
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the inventory
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


    private void showOrderMoreConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners for the positive and
        // negative buttons on the dialog with edittext for user to enter quantity items shipment in
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(EditorActivity.this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);  // InputType numbers only
        builder.setMessage(R.string.order_more_dialog_msg);
        builder.setView(input);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) { // User clicked "Ok" button

                // Read input value
                String inputValueString = input.getText().toString();

                // If user clicked OK without entering any value then simply end the dialog
                if (inputValueString.equals("")) {
                    return;
                }

                // Retrieve values entered by user in editor activity that we are interested in
                // trim() eliminates any leading or trailing white spaces
                String nameString = mNameEditText.getText().toString().trim();
                String supplierString = mSupplierEditText.getText().toString().trim();

                String Message = "Dear " + supplierString + "," +
                        "\n\nPlease ship following:   " +
                        "\nProduct name:     " + nameString +
                        "\nProduct quantity: " + inputValueString +
                        "\n\nThank you";

                Intent intent = new Intent(Intent.ACTION_SENDTO); //for no attachment
                intent.setData(Uri.parse("mailto:"));  //only email apps should handle this
                intent.putExtra(Intent.EXTRA_EMAIL, (new String[] { supplierString }));
                intent.putExtra(Intent.EXTRA_SUBJECT, "New shipment order");
                intent.putExtra(Intent.EXTRA_TEXT, Message);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the inventory
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}