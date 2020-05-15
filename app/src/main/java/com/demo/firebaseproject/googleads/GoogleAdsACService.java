package com.demo.firebaseproject.googleads;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class GoogleAdsACService {
    private static final String AD_TO_ACTION = "adToAction";
    private static final String TAG = "GoogleAdsACService";
    private static GoogleAdsACService instance = null;
    private FirebaseFunctions mFunctions;

    private GoogleAdsACService() {
         mFunctions = FirebaseFunctions.getInstance();
    }

    public static GoogleAdsACService getInstance() {
        if (instance == null) {
            instance = new GoogleAdsACService();
        }

        return instance;
    }

    public void adToAction(Activity context, ActionCallback callback) {
        AsyncTask.execute(() -> {
            try {
                AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
                String adId = adInfo != null ? adInfo.getId() : null;
                if (adId != null) {
                    this.getAdInfoCloudFunction(adId, adInfo.isLimitAdTrackingEnabled())
                            .addOnCompleteListener(task -> {
                                if (!task.isSuccessful()) {
                                    Exception e = task.getException();
                                    if (e instanceof FirebaseFunctionsException) {
                                        FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                                        FirebaseFunctionsException.Code code = ffe.getCode();
                                        Log.w(TAG, "adToAction:onFailure: " +
                                                "details: "+ ffe.getDetails() + "; code: "+ code);
                                    } else {
                                        Log.w(TAG, "adToAction:onFailure", e);
                                    }
                                    return;
                                }

                                Map<String, Object> result = task.getResult();
                                Log.w(TAG, "adToAction: " + result);

                                if(result.get("action") != null) {
                                    callback.commit(result.get("action"));
                                }

                            });
                }

            } catch (IOException | GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException exception) {
                Log.w(TAG, "Get User Attribution Failed: ", exception);
            }
        });

    }

    private Task<Map<String, Object>> getAdInfoCloudFunction(String advertisingId, boolean lat) {
        // Create the arguments to the callable function, which are two integers
        Map<String, Object> data = new HashMap<>();
        data.put("advertisingId", advertisingId);
        data.put("lat", lat ? 1 : 0);
        // Call the function and extract the operation from the result
        return mFunctions
                .getHttpsCallable(AD_TO_ACTION)
                .withTimeout(1, TimeUnit.SECONDS)
                .call(data)
                .continueWith(task -> (Map<String, Object>) task.getResult().getData());

    }

    public interface ActionCallback {
        void commit(Object action);
    }
}
