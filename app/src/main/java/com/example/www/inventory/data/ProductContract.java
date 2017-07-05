package com.example.www.inventory.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * API contract for Inventory app
 */

public final class ProductContract {

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private ProductContract() {
    }

    // Content authority
    public static final String CONTENT_AUTHORITY = "com.example.www.inventory";

    // basi content URI
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // path for "product" data
    public static final String PRODUCT_PATH = "products";

    // inner class that defines constants for products database table
    // each entry representa a single product

    public static final class ProductEntry implements BaseColumns {

        // Content Uri for product data
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PRODUCT_PATH);

        // MIME type for a list of products
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PRODUCT_PATH;

        // MIME type for a single product
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PRODUCT_PATH;

        // name of database table for products
        public static final String TABLE_NAME = "products";

        /*
         * Columns of the database table
         */

        // UID, type INTEGER
        public static final String _ID = BaseColumns._ID;
        // product name, type STRING
        public static final String COLUMN_PRODUCT_NAME = "name";
        // product name, type INTEGER
        public static final String COLUMN_PRODUCT_PRICE = "price";
        // product name, type INTEGER
        public static final String COLUMN_PRODUCT_IN_STOCK = "stock";
        // URI to image of product, type STRING
        public static final String COLUMN_PRODUCT_IMAGE_URI = "image";
        // URI to image of product, type STRING
        public static final String COLUMN_PRODUCT_SUPPLIER_EMAIL = "supplier";

    }
}
