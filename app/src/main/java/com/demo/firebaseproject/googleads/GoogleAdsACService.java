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
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GoogleAdsACService {
    private static final String TAG = "GoogleAdsACService";
    public static final String GOOGLE_ADS_CONVERSION_RESULT = "googleAdsConversionResult";
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private FirebaseFunctions mFunctions;
    private static GoogleAdsACService instance = null;

    public static GoogleAdsACService getInstance() {
        if (instance == null) {
            instance = new GoogleAdsACService();
        }

        return instance;
    }

    private GoogleAdsACService() {
         mFunctions = FirebaseFunctions.getInstance();
         mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
    }

    public void getUserAttribution(Activity context, ConversionDataCallback callback) {
        AsyncTask.execute(() -> {
            try {
                AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
                String adId = adInfo != null ? adInfo.getId() : null;
                if (adId != null) {
                    this.getAdInfoCloudFunction(adId,adInfo.isLimitAdTrackingEnabled())
                            .addOnCompleteListener(task -> {
                                if (!task.isSuccessful()) {
                                    Exception e = task.getException();
                                    if (e instanceof FirebaseFunctionsException) {
                                        FirebaseFunctionsException ffe = (FirebaseFunctionsException) e;
                                        FirebaseFunctionsException.Code code = ffe.getCode();
                                        Log.w(TAG, "googleAdsConversionResult:onFailure: " +
                                                "details: "+ ffe.getDetails() + "; code: "+ code);
                                    } else {
                                        Log.w(TAG, "googleAdsConversionResult:onFailure", e);
                                    }
                                    return;
                                }

                                Map<String, Object> result = task.getResult();
                                Log.w(TAG, "googleAdsConversionResult: " + result);
                                Map<String, Object> adEvent = extractAdEvent(result);

                                if (!adEvent.isEmpty()) {
                                    this.fetchActionFromRemoteConfig(context, adEvent, callback);
                                }

                            });
                }

            } catch (IOException | GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException exception) {
                Log.w(TAG, "Get User Attribution Failed: ", exception);
            }
        });

    }

    private Map<String, Object> extractAdEvent(Map<String, Object> s2sResult) {
        if(!(boolean) s2sResult.get("attributed")) {
            return new HashMap<>();
        }

        return ((List<Map<String, Object>>)s2sResult.get("ad_events")).get(0);
    }


    private void fetchActionFromRemoteConfig(Activity context, Map<String, Object> adEvents,
                                             ConversionDataCallback callback) {
        mFirebaseRemoteConfig.setConfigSettingsAsync(
                new FirebaseRemoteConfigSettings.Builder()
                        .setMinimumFetchIntervalInSeconds(0)
                        .setFetchTimeoutInSeconds(2)
                        .build());
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(context, task -> {
                    if (task.isSuccessful()) {
                        Boolean updated = task.getResult();
                        Log.d(TAG, "Config params updated: " + updated);
                    }

                    String adToAction = mFirebaseRemoteConfig.getString("ad_to_action");
                    try {
                        Map<String, Map<String, String>> adToActionMap = new Gson().fromJson(adToAction,
                                new TypeToken<Map<String, Map>>() {
                                }.getType());

                        String action = extractAction(adEvents, adToActionMap);

                        if(action != null) {
                            Log.d(TAG, "Committing Action: " + action);
                            callback.commitAction(action);
                        }

                    } catch (NullPointerException e) {
                        Log.w(TAG, "Illegal Remote Config value for key 'ad_to_action': " + adToAction);
                    }
                });

    }

    private String extractAction(Map<String, Object> adEvents, Map<String, Map<String, String>> adToActionMap) {
        Integer adGroupId  = (Integer) adEvents.get("ad_group_id");
        String adGroupIdStr = adGroupId == null ? "" : adGroupId.toString();
        String action = adToActionMap.get("adGroupIds") != null ? adToActionMap.get("adGroupIds").get(adGroupIdStr) : null;

        if(action == null) {
            Integer campaignId  = (Integer) adEvents.get("campaign_id");
            String campaignIdStr = campaignId == null ? "" : campaignId.toString();
            action = adToActionMap.get("campaignIds") != null ? adToActionMap.get("campaignIds").get(campaignIdStr) : null;
        }
        return action;
    }

    private Task<Map<String, Object>> getAdInfoCloudFunction(String advertisingId, boolean lat) {
        // Create the arguments to the callable function, which are two integers
        Map<String, Object> data = new HashMap<>();
        data.put("advertisingId", advertisingId);
        data.put("lat", lat ? 1 : 0);
        // Call the function and extract the operation from the result
        return mFunctions
                .getHttpsCallable(GOOGLE_ADS_CONVERSION_RESULT)
                .call(data)
                .continueWith(task -> (Map<String, Object>) task.getResult().getData());

    }

    public interface ConversionDataCallback {
        void commitAction(Object action);
    }
}
