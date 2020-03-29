package com.demo.firebaseproject.googleads;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;


import com.demo.firebaseproject.R;
import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.FirebaseFunctionsException;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class ConversionData {
    private static String TAG = "ConversionData";
    private FirebaseFunctions mFunctions = FirebaseFunctions.getInstance();

    private static ConversionData instance = null;

    public static ConversionData getInstance() {
        if (instance == null) {
            instance = new ConversionData();
        }

        return instance;
    }

    private ConversionData() {

    }

    public void getGoogleAdsInfo(Activity context, ConversionDataCallback callback) {
        AsyncTask.execute(() -> {
            try {
                AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
                String adId = adInfo != null && !adInfo.isLimitAdTrackingEnabled() ? adInfo.getId() : null;
                if (adId != null) {
                    this.getAdInfo(adId)
                            .addOnCompleteListener(task -> {
                                if (!task.isSuccessful()) {
                                    Exception e = task.getException();
                                    if (e instanceof FirebaseFunctionsException) {
                                        FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                                        FirebaseFunctionsException.Code code = ffe.getCode();
                                        Log.w(TAG, String.valueOf(ffe.getDetails()));
                                    } else {
                                        Log.w(TAG, "googleAdsConversionResult:onFailure", e);
                                    }
                                    return;
                                }

                                Map<String, Object> result = task.getResult();
                                Log.w(TAG, "googleAdsConversionResult: " + result);
                                Integer adGroupId = (Integer) result.get("ad_group_id");
                                if (adGroupId != null) {
                                    this.processGoogleAttribution(context, adGroupId, callback);
                                }

                            });
                }

            } catch (IOException | GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException exception) {
                // Error handling if needed
            }
        });

    }


    private void processGoogleAttribution(Activity context, Integer adGroupId,
                                          ConversionDataCallback callback) {
        FirebaseRemoteConfig mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(context, task -> {
                    if (task.isSuccessful()) {
                        boolean updated = task.getResult();
                        Log.d(TAG, "Config params updated: " + updated);
                    }

                    String adToAction = mFirebaseRemoteConfig.getString("ad_to_action");
                    try {

                        Map<String, Map<String, String>> adToActionMap = new Gson().fromJson(adToAction,
                                new TypeToken<Map<String, Map>>() {
                                }.getType());
                        Object action = adToActionMap.get("adGroupIds").get(String.valueOf(adGroupId));
                        if(action != null) {
                            callback.takeAction(action);
                        }

                    } catch (NullPointerException e) {
                        Log.w(TAG, "Illegal Remote Config Value: " + adToAction);

                    }
                });

    }

    private Task<Map<String, Object>> getAdInfo(String advertisingId) {
        // Create the arguments to the callable function, which are two integers
        Map<String, Object> data = new HashMap<>();
        data.put("advertisingId", advertisingId);
        // Call the function and extract the operation from the result
        return mFunctions
                .getHttpsCallable("googleAdsConversionResult")
                .call(data)
                .continueWith(task -> (Map<String, Object>) task.getResult().getData());

    }

    public interface ConversionDataCallback {
        void takeAction(Object action);
    }
}
