package com.demo.firebase.view;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.demo.firebase.R;
import com.demo.firebase.model.MockInventory;
import com.demo.firebase.model.Product;
import com.demo.firebase.model.Section;

import java.util.Collection;

import static android.widget.LinearLayout.HORIZONTAL;
import static com.demo.firebase.model.Section.NEW_ARRIVAL;
import static com.demo.firebase.model.Section.RECOMMENDATION;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private SharedPreferences.OnSharedPreferenceChangeListener deepLinkListener;

    @Override
    protected void onStart() {
        super.onStart();
        preferences.registerOnSharedPreferenceChangeListener(deepLinkListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        preferences.unregisterOnSharedPreferenceChangeListener(deepLinkListener);
        deepLinkListener = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView newArrivalView = findViewById(R.id.new_arrivals_view);
        renderProducts(NEW_ARRIVAL, newArrivalView);
        RecyclerView recommendView = findViewById(R.id.recommendation_view);
        renderProducts(RECOMMENDATION, recommendView);
        preferences =
                getSharedPreferences("google.analytics.deferred.deeplink.prefs", MODE_PRIVATE);

        deepLinkListener = (sharedPreferences, key) -> {
            Log.d("DEEPLINK_LISTENER", "Deep link changed");
            if ("deeplink".equals(key)) {
                String deeplink = sharedPreferences.getString(key, null);
                Double cTime = Double.longBitsToDouble(sharedPreferences.getLong("timestamp", 0));
                Log.d("DEEPLINK_LISTENER", "Deep link retrieved: " + deeplink);
                showDeepLinkResult(deeplink);
            }
        };
    }

    public void showDeepLinkResult(String result) {
        String toastText = result;
        if (toastText == null) {
            toastText = "The deep link retrieval failed";
        } else if (toastText.isEmpty()) {
            toastText = "Deep link empty";
        }
        Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_LONG).show();
        Log.d("DEEPLINK", toastText);

        try {
            Product product = new MockInventory()
                    .getProductByName(result.substring(result.lastIndexOf("/") + 1));
            ProductActivity.navigateToProductPage(MainActivity.this, product.id);
            Log.d("DEEPLINK", "Showing item from Deep Link");
        } catch (NullPointerException e) {
            Log.d("DEEPLINK", "Invalid Deep Link or item name not found");
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
        view.setLayoutManager(new LinearLayoutManager(this, HORIZONTAL, false));
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

    public static void navigate(Activity activity) {
        Intent intent = new Intent(activity, MainActivity.class);
        activity.startActivity(intent);
    }
}