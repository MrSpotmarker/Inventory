package com.example.www.inventory;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.www.inventory.data.ProductContract.ProductEntry;

import java.math.BigDecimal;


/**
 * Adapter class for inflating Views on the list.
 */

public class ProductCursorAdpapter extends CursorAdapter {

    public ProductCursorAdpapter(Context context) {
        super(context, null, 0);
    }
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {

        ViewHolder holder = (ViewHolder) view.getTag();
        if (holder == null) {
            holder = new ViewHolder();
            holder.saleButton = (Button) view.findViewById(R.id.sale_button);
            view.setTag(holder);
        }

        // Find Views to populate
        TextView nameTextView = (TextView) view.findViewById(R.id.text_view_name);
        TextView stockTextView = (TextView) view.findViewById(R.id.text_view_stock);
        TextView priceTextView = (TextView) view.findViewById(R.id.text_view_price);

        // get values
        String name = cursor.getString(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_NAME));
        final String stockString = cursor.getString(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_IN_STOCK));
        String priceString = cursor.getString(cursor.getColumnIndexOrThrow(ProductEntry.COLUMN_PRODUCT_PRICE));

        // id declared final to work in ClickListener
        final int id = cursor.getInt(cursor.getColumnIndex(ProductEntry._ID));

        BigDecimal priceBd = new BigDecimal(priceString).movePointLeft(2);
        String priceFinal = priceBd.toString();

        // populate fields
        nameTextView.setText(name);
        stockTextView.setText(stockString);
        priceTextView.setText("€" + priceFinal);

        holder.saleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Initialize Content resolver and Values object
                ContentResolver resolver = view.getContext().getContentResolver();
                ContentValues values = new ContentValues();

                // check if stock > 0 so item can be sold
                int stock = Integer.parseInt(stockString);

                // update dataset
                if (stock > 0) {
                    Uri uri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, id);
                    values.put(ProductEntry.COLUMN_PRODUCT_IN_STOCK, stock - 1);
                    resolver.update(uri, values, null, null);
                }
            }
        });

    }

    static class ViewHolder {
        Button saleButton;
    }
}
