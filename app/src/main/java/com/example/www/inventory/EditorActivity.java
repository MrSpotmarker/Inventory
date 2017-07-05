package com.example.www.inventory;

import android.app.Activity;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.FileProvider;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.www.inventory.data.ProductContract.ProductEntry;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Allow to create or edit products.
 */

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int REQUEST_TAKE_PHOTO = 1;

    private static final String LOG_TAG = EditorActivity.class.getSimpleName();
    // static for LoaderManager instance
    private static final int SINGLE_PRODUCT_LOADER = 1;
    // Edit text field to enter product name
    private EditText mNameEditText;
    // Edit text field to enter product price
    private EditText mPriceEditText;
    // Edit text field to enter available stock
    private EditText mStockEditText;
    // Edit text field to enter supplier email address
    private EditText mSupplierEditText;
    // ImageView for product image
    private ImageView mProductImageView;

    // String were current photo path is saved
    String mCurrentPhotoPath;
    String mCurrentImageUri;

    // Uri passed by Intent, gets initialized if Uri gets passed
    private Uri mCurrentProductUri;
    // Boolean that checks whether edits were made
    private boolean mProductHasChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent that was created to launch the activity
        final Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_product_name);
        mPriceEditText = (EditText) findViewById(R.id.edit_product_price);
        mStockEditText = (EditText) findViewById(R.id.edit_product_stock);
        mSupplierEditText = (EditText) findViewById(R.id.edit_product_supplier);
        // find ImageView for product image
        mProductImageView = (ImageView) findViewById(R.id.product_image);
        // find Buttons for increasing and decreasing stock, taking photo and ordering more;
        Button mReduceStockButton = (Button) findViewById(R.id.reduceStock);
        Button mIncreaseStockButton = (Button) findViewById(R.id.increaseStock);
        ImageButton mTakeImageButton = (ImageButton) findViewById(R.id.take_image);
        Button mOrderMoreButton = (Button) findViewById(R.id.order_more_button);

        // If the intent doens't contain a pet URI we create a new pet. Else we edit an existing one
        if (mCurrentProductUri == null) {
            // This is a new pet, so change the app bar to say "Add a Pet"
            setTitle(getString(R.string.editor_activity_title_new_product));
            mIncreaseStockButton.setVisibility(View.GONE);
            mReduceStockButton.setVisibility(View.GONE);

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a pet that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            setTitle(getString(R.string.editor_activity_title_edit_product));
            getSupportLoaderManager().initLoader(SINGLE_PRODUCT_LOADER, null, this);
            // Set TouchListeners for reduce stock button
            mReduceStockButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // get current stock
                    if (mStockEditText.getText() != null) {
                        String currentStockAsString = mStockEditText.getText().toString();
                        int currentStock = Integer.parseInt(currentStockAsString);
                        if (currentStock != 0 && currentStock > 0) {
                            currentStock--;
                            mStockEditText.setText(String.valueOf(currentStock));
                        }
                    }
                }
            });

            // Set TouchListeners for increase stock button
            mIncreaseStockButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mStockEditText.getText() != null) {
                        // get current stock
                        String currentStockAsString = mStockEditText.getText().toString();
                        int currentStock = Integer.parseInt(currentStockAsString);
                        currentStock++;
                        mStockEditText.setText(String.valueOf(currentStock));
                    }
                }
            });

        }

        // Set TouchListeners that check if values have changed
        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mStockEditText.setOnTouchListener(mTouchListener);
        mSupplierEditText.setOnTouchListener(mTouchListener);

        // Set TouchListeners for take image botton
        mTakeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // Ensure that there's a camera activity to handle the intent
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        Log.e(LOG_TAG, "Error creating image");
                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(getApplicationContext(),
                                "com.example.www.inventory.fileprovider",
                                photoFile);
                        mCurrentImageUri = photoURI.toString();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                    }

                }
            }
        });

        // Set touch listener for mOrderMoreButton
        mOrderMoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get values
                String productName = mNameEditText.getText().toString();
                String supplierMail = mSupplierEditText.getText().toString();
                String subject = getString(R.string.mail_subject);
                String textBase = getString(R.string.mail_text);
                String text = String.format(textBase, productName);

                //create Intent
                Intent mailIntent = new Intent(Intent.ACTION_SENDTO);
                mailIntent.setType("*/*");
                mailIntent.setData(Uri.parse("mailto:"));
                mailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{supplierMail});
                mailIntent.putExtra(Intent.EXTRA_SUBJECT, subject + productName);
                mailIntent.putExtra(Intent.EXTRA_TEXT, text);
                if (mailIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mailIntent);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        // If the pet hasn't changed, continue with handling back button press
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save pet to database
                if (mCurrentProductUri == null) {
                    boolean productInserted = insertProduct();
                    // Exit activity
                    if (productInserted) {
                        finish();
                    }
                    return true;
                } else {
                    boolean productUpteted = updateProduct();
                    // Exit activity
                    if (productUpteted) {
                        finish();
                    }
                    return true;
                }
                // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the pet hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new pet, hide the "Delete" menu item.
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_IN_STOCK,
                ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL,
                ProductEntry.COLUMN_PRODUCT_IMAGE_URI
        };

        // Perform a query on the provider using the ContentResolver.
        // Use the {@link PetEntry#CONTENT_URI} to access the pet data.
        return new CursorLoader(this,   // Parent activity context
                mCurrentProductUri,     // The content URI of the words table
                projection,             // The columns to return for each row
                null,                   // Selection criteria
                null,                   // Selection criteria
                null);                  // The sort order for the returned rows
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            String name = data.getString(data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME));
            Integer priceAsInt = data.getInt(data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE));
            Integer stock = data.getInt(data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IN_STOCK));
            String supplier = data.getString(data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL));
            String imageUri = data.getString(data.getColumnIndex(ProductEntry.COLUMN_PRODUCT_IMAGE_URI));

            // set values on EditText --> possible alternative - play with BigDecimal
            mNameEditText.setText(name);
            Float price = Float.valueOf(priceAsInt / 100); // Calculate back from cent to Euro
            mPriceEditText.setText(String.valueOf(price));
            mStockEditText.setText(Integer.toString(stock));
            mSupplierEditText.setText(supplier);
            if (imageUri != null) {
                mProductImageView.setImageBitmap(getBitmapFromUri(Uri.parse(imageUri)));
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mStockEditText.setText("");
        mSupplierEditText.setText("");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == Activity.RESULT_OK) {
            // mProductImageView.setImageBitmap(getBitmapFromUri(Uri.parse(mCurrentImageUri)));
            mProductImageView.setImageURI(Uri.parse(mCurrentImageUri));
        }
    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Get user input from editor and save new product into database.
     */
    private boolean insertProduct() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String stockString = mStockEditText.getText().toString().trim();
        String supplierString = mSupplierEditText.getText().toString().trim();

        if (mCurrentProductUri == null &&
                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(priceString) &&
                TextUtils.isEmpty(stockString)) {
            return false;
        }

        if (TextUtils.isEmpty(priceString)) {
            Toast.makeText(this, getString(R.string.editor_price_is_zero),
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(nameString)) {
            Toast.makeText(this, getString(R.string.editor_name_is_zero),
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        // Create a ContentValues object where column names are the keys,
        // and product attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL, supplierString);

        Integer stock = 0;
        if (!TextUtils.isEmpty(stockString)) {
            stock = Integer.parseInt(stockString);
        }
        values.put(ProductEntry.COLUMN_PRODUCT_IN_STOCK, stock);

        BigDecimal priceBd = new BigDecimal(priceString);
        int price = priceBd.movePointRight(2).intValueExact();
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);

        if (mCurrentImageUri != null)
            values.put(ProductEntry.COLUMN_PRODUCT_IMAGE_URI, mCurrentImageUri);

        // Insert a new product into the provider, returning the content URI for the new pet.
        Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

        // Show a toast message depending on whether or not the insertion was successful
        if (newUri == null) {
            // If the new content URI is null, then there was an error with insertion.
            Toast.makeText(this, getString(R.string.editor_insert_product_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the insertion was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.editor_insert_product_successful),
                    Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    /**
     * Get user input from editor and save new pet into database.
     */
    private boolean updateProduct() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String stockString = mStockEditText.getText().toString().trim();
        String supplierString = mSupplierEditText.getText().toString().trim();

        if (TextUtils.isEmpty(priceString)) {
            Toast.makeText(this, getString(R.string.editor_price_is_zero),
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(nameString)) {
            Toast.makeText(this, getString(R.string.editor_name_is_zero),
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        // Create a ContentValues object where column names are the keys,
        // and product attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER_EMAIL, supplierString);

        Integer stock = 0;
        if (!TextUtils.isEmpty(stockString)) {
            stock = Integer.parseInt(stockString);
        }
        values.put(ProductEntry.COLUMN_PRODUCT_IN_STOCK, stock);

        BigDecimal priceBd = new BigDecimal(priceString);
        int price = priceBd.movePointRight(2).intValueExact();
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, price);

        if (mCurrentImageUri != null)
            values.put(ProductEntry.COLUMN_PRODUCT_IMAGE_URI, mCurrentImageUri);

        // Insert a new product into the provider, returning the content URI for the new pet.
        int rowsAffected = getContentResolver().update(
                mCurrentProductUri,
                values,
                null,
                null
        );

        // Show a toast message depending on whether or not the update was successful
        if (rowsAffected == 0) {
            // If the new content URI is null, then there was an error with the update.
            Toast.makeText(this, getString(R.string.editor_update_product_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the update was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.editor_update_product_successful),
                    Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    /**
     * Perform the deletion of the pet in the database.
     */
    private void deleteProduct() {
        // get product to delete from the provider
        int rowsAffected = getContentResolver().delete(
                mCurrentProductUri,
                null,
                null
        );

        // Show a toast message depending on whether or not the deletion was successful
        if (rowsAffected == 0) {
            // If the new content URI is null, then there was an error with deletion.
            Toast.makeText(this, getString(R.string.editor_delete_product_failed),
                    Toast.LENGTH_SHORT).show();
        } else {
            // Otherwise, the deletion was successful and we can display a toast.
            Toast.makeText(this, getString(R.string.editor_delete_product_successful),
                    Toast.LENGTH_SHORT).show();
            // exit activity
            finish();
        }

    }

    private final View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private Bitmap getBitmapFromUri(Uri uri) {

        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            int maxWidth = mProductImageView.getWidth();
            int maxHeight = mProductImageView.getHeight();
            resizeImage(image, maxWidth, maxHeight);
            return image;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Error closing ParcelFile Descriptor");
            }
        }
    }

    private Bitmap resizeImage(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > 1) {
                finalWidth = (int) ((float) maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float) maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
            return image;
        } else {
            return image;
        }
    }
}
