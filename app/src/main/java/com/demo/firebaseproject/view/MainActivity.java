package com.demo.firebaseproject.view;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.demo.firebaseproject.R;
import com.demo.firebaseproject.googleads.GoogleAdsACService;
import com.demo.firebaseproject.model.MockInventory;
import com.demo.firebaseproject.model.Product;
import com.demo.firebaseproject.model.Section;

import java.util.Collection;

import static com.demo.firebaseproject.model.Section.NEW_ARRIVAL;
import static com.demo.firebaseproject.model.Section.RECOMMENDATION;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final String FIRST_OPEN = "FIRST_OPEN";
    private static final String FIRST_OPEN_KEY = "FIRST_OPEN_ACTION";
    private static SharedPreferences prefs = null;
    public static void navigate(Activity activity) {
        Intent intent = new Intent(activity, MainActivity.class);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RecyclerView newArrivalView = findViewById(R.id.new_arrivals_view);
        renderProducts(NEW_ARRIVAL, newArrivalView);
        RecyclerView recommendView = findViewById(R.id.recommendation_view);
        renderProducts(RECOMMENDATION, recommendView);
        prefs = getSharedPreferences(FIRST_OPEN_KEY, MODE_PRIVATE);
        if (prefs.getBoolean(FIRST_OPEN, true)) {
            prefs.edit().putBoolean(FIRST_OPEN, false).apply();
            GoogleAdsACService.getInstance()
                    .adToAction(this, this::googleAdToAction);
        }
    }

    private void googleAdToAction(Object action) {
        try {
            if (action instanceof String) {
                String productName = (String) action;
                Product product = new MockInventory()
                        .getProductByName(productName);
                ProductActivity.navigateToProductPage(MainActivity.this, product.id);
                Log.d(TAG, "Init app on item: " + productName);
            }
        } catch (NullPointerException e) {
            Log.d(TAG, "Invalid item name or item name not found");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // settings is the only option for now
        new SizeSelectionDialog(this).show();
        return true;
    }

    private void renderProducts(Section section, RecyclerView view) {
        view.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
        Collection<Product> products = new MockInventory().productsForSection(section);
        ProductListAdapter adapter = new ProductListAdapter(products, R.layout.product_as_icons);
        view.setAdapter(adapter);
    }

    public void cartBtnClick(View v) {
        CartActivity.navigate(this);
    }

    public void recommendClick(View v) {
        SectionActivity.navigateToSectionPage(this, RECOMMENDATION);
    }

    public void newArrClick(View v) {
        SectionActivity.navigateToSectionPage(this, NEW_ARRIVAL);
    }
}