package io.ona.kujaku.sample.activities;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import io.ona.kujaku.KujakuLibrary;
import io.ona.kujaku.callables.AsyncTaskCallable;
import io.ona.kujaku.domain.Point;
import io.ona.kujaku.helpers.MapBoxStyleStorage;
import io.ona.kujaku.helpers.MapBoxWebServiceApi;
import io.ona.kujaku.listeners.OnFinishedListener;
import io.ona.kujaku.sample.BuildConfig;
import io.ona.kujaku.sample.MyApplication;
import io.ona.kujaku.sample.R;
import io.ona.kujaku.services.MapboxOfflineDownloaderService;
import io.ona.kujaku.tasks.GenericAsyncTask;
import io.ona.kujaku.utils.Constants;
import io.ona.kujaku.utils.Permissions;

import static io.ona.kujaku.utils.Constants.MAP_ACTIVITY_REQUEST_CODE;
import static io.ona.kujaku.utils.Constants.NEW_FEATURE_POINTS_JSON;

public class MainActivity extends BaseNavigationDrawerActivity {

    private EditText topLeftLatEd;
    private EditText topLeftLngEd;
    private EditText bottomRightLatEd;
    private EditText bottomRightLngEd;
    private EditText mapNameEd;
    private EditText topRightLatEd;
    private EditText topRightLngEd;
    private EditText bottomLeftLatEd;
    private EditText bottomLeftLngEd;

    private static final String SAMPLE_JSON_FILE_NAME = "2017-nov-27-kujaku-metadata.json";
    private static final int PERMISSIONS_REQUEST_CODE = 9823;
    private String[] basicPermissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private int lastNotificationId = 200;
    private final static String TAG = MainActivity.class.getSimpleName();

    private List<Point> points;

    private Activity mainActivity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestBasicPermissions();

        bottomRightLatEd = findViewById(R.id.edt_mainActivity_bottomRightLatitude);
        bottomRightLngEd = findViewById(R.id.edt_mainActivity_bottomRightLongitude);
        topLeftLatEd = findViewById(R.id.edt_mainActivity_topLeftLatitude);
        topLeftLngEd = findViewById(R.id.edt_mainActivity_topLeftLongitude);
        bottomLeftLatEd = findViewById(R.id.edt_mainActivity_bottomLeftLatitude);
        bottomLeftLngEd = findViewById(R.id.edt_mainActivity_bottomLeftLongitude);
        topRightLatEd = findViewById(R.id.edt_mainActivity_topRightLatitude);
        topRightLngEd = findViewById(R.id.edt_mainActivity_topRightLongitude);

        mapNameEd = findViewById(R.id.edt_mainActivity_mapName);

        Button startOfflineDownload = findViewById(R.id.btn_mainActivity_startOfflineDownload);
        startOfflineDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadMap();
            }
        });

        final EditText mapBoxStyleUrl = (EditText) findViewById(R.id.edt_mainActivity_mapboxStyleURL);
        mapBoxStyleUrl.setText("mapbox://styles/ona/cj9jueph7034i2rphe0gp3o6m");
        Button btnDownloadMapBoxStyle = (Button) findViewById(R.id.btn_mainActivity_downloadMapboxStyle);
        btnDownloadMapBoxStyle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadMapBoxStyle(mapBoxStyleUrl.getText().toString());
            }
        });

        setTitle(R.string.main_activity_title);

        // Fetch previously dropped points
        final OnFinishedListener onPointsFetchFinishedListener = new OnFinishedListener() {
            @Override
            public void onSuccess(Object[] objects) {
                points = (List<Point>) objects[0];
                KujakuLibrary.getInstance().launchMapActivity(mainActivity, points, true);
            }
            @Override
            public void onError(Exception e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        };

        Button btnLaunchKujakuMap = findViewById(R.id.btn_mainActivity_launchKujakuMap);
        btnLaunchKujakuMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: will need to figure out how to get new points added after initial MainActivity instantiation
                if (points == null || points.size() == 0) {
                    fetchDroppedPoints(onPointsFetchFinishedListener);
                } else {
                    KujakuLibrary.getInstance().launchMapActivity(mainActivity, points, true);
                }
            }
        });
        registerLocalBroadcastReceiver();

        Button btnOpenMapActivity = findViewById(R.id.btn_mainActivity_openMapActivity);
        btnOpenMapActivity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: will need to figure out how to get new points added after initial MainActivity instantiation
                if (points == null || points.size() == 0) {
                    fetchDroppedPoints(onPointsFetchFinishedListener);
                } else {
                    KujakuLibrary.getInstance().launchMapActivity(mainActivity, points, true);
                }
            }
        });
    }


    private void fetchDroppedPoints(OnFinishedListener onFinishedListener) {
        GenericAsyncTask genericAsyncTask = new GenericAsyncTask(new AsyncTaskCallable() {
            @Override
            public Object[] call() throws Exception {
                List<Point> droppedPoints = MyApplication.getInstance().getPointsRepository().getAllPoints();
                return new Object[]{droppedPoints};
            }
        });
        genericAsyncTask.setOnFinishedListener(onFinishedListener);
        genericAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    protected int getContentView() {
        return R.layout.activity_main;
    }

    @Override
    protected int getSelectedNavigationItem() {
        return R.id.nav_main_activity;
    }

    private void downloadMap() {
        double topLeftLat = 37.7897;
        double topLeftLng = -119.5073;
        double bottomRightLat = 37.6744;
        double bottomRightLng = -119.6815;
        double topRightLat = 37.7897;
        double topRightLng = -119.6815;
        double bottomLeftLat = 37.6744;
        double bottomLeftLng = -119.5073;

        String tllatE = topLeftLatEd.getText().toString();
        String tllngE = topLeftLngEd.getText().toString();
        String brlatE = bottomRightLatEd.getText().toString();
        String brlngE = bottomRightLngEd.getText().toString();
        String trLatE = topRightLatEd.getText().toString();
        String trLngE = topRightLngEd.getText().toString();
        String blLatE = bottomLeftLatEd.getText().toString();
        String blLngE = bottomLeftLngEd.getText().toString();

        String mapName = mapNameEd.getText().toString();
        if (mapName.isEmpty()) {
            Toast.makeText(this, "Please enter a Map Name!", Toast.LENGTH_LONG)
                    .show();
            return;
        }

        if (isValidDouble(tllatE) && isValidDouble(tllngE) && isValidDouble(brlatE) && isValidDouble(brlngE)
                && isValidDouble(trLatE) && isValidDouble(trLngE) && isValidDouble(blLatE) && isValidDouble(blLngE)) {
            topLeftLat = Double.valueOf(tllatE);
            topLeftLng = Double.valueOf(tllngE);
            bottomRightLat = Double.valueOf(brlatE);
            bottomRightLng = Double.valueOf(brlngE);
            topRightLat = Double.valueOf(trLatE);
            topRightLng = Double.valueOf(trLngE);
            bottomLeftLat = Double.valueOf(blLatE);
            bottomLeftLng = Double.valueOf(blLngE);
        } else {
            Toast.makeText(this, "Invalid Lat or Lng! Reverting to default values", Toast.LENGTH_LONG)
                    .show();
        }

        Intent mapDownloadIntent = new Intent(this, MapboxOfflineDownloaderService.class);
        mapDownloadIntent.putExtra(Constants.PARCELABLE_KEY_MAPBOX_ACCESS_TOKEN, BuildConfig.MAPBOX_SDK_ACCESS_TOKEN);
        mapDownloadIntent.putExtra(Constants.PARCELABLE_KEY_SERVICE_ACTION, MapboxOfflineDownloaderService.SERVICE_ACTION.DOWNLOAD_MAP);
        mapDownloadIntent.putExtra(Constants.PARCELABLE_KEY_STYLE_URL, "mapbox://styles/ona/cj9jueph7034i2rphe0gp3o6m");
        mapDownloadIntent.putExtra(Constants.PARCELABLE_KEY_MAP_UNIQUE_NAME, mapName);
        mapDownloadIntent.putExtra(Constants.PARCELABLE_KEY_MAX_ZOOM, 20.0);
        mapDownloadIntent.putExtra(Constants.PARCELABLE_KEY_MIN_ZOOM, 0.0);
        mapDownloadIntent.putExtra(Constants.PARCELABLE_KEY_TOP_LEFT_BOUND, new LatLng(topLeftLat, topLeftLng));
        mapDownloadIntent.putExtra(Constants.PARCELABLE_KEY_TOP_RIGHT_BOUND, new LatLng(topRightLat, topRightLng));
        mapDownloadIntent.putExtra(Constants.PARCELABLE_KEY_BOTTOM_RIGHT_BOUND, new LatLng(bottomRightLat, bottomRightLng));
        mapDownloadIntent.putExtra(Constants.PARCELABLE_KEY_BOTTOM_LEFT_BOUND, new LatLng(bottomLeftLat, bottomLeftLng));

        startService(mapDownloadIntent);
    }

    private boolean isValidDouble(String doubleString) {
        String doubleRegex = "[+-]{0,1}[0-9]*.{0,1}[0-9]*";
        return (!doubleString.isEmpty() && doubleString.matches(doubleRegex));
    }

    private void registerLocalBroadcastReceiver() {
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        Bundle bundle = intent.getExtras();
                        if (bundle != null) {
                            Log.i("KUJAKU SAMPLE APP TAG", intent.getExtras().toString());
                            if (bundle.containsKey(MapboxOfflineDownloaderService.KEY_RESULT_STATUS)
                                    && bundle.containsKey(MapboxOfflineDownloaderService.KEY_RESULT_MESSAGE)
                                    && bundle.containsKey(MapboxOfflineDownloaderService.KEY_RESULTS_PARENT_ACTION)
                                    && bundle.containsKey(Constants.PARCELABLE_KEY_MAP_UNIQUE_NAME)) {

                                String mapUniqueName = bundle.getString(Constants.PARCELABLE_KEY_MAP_UNIQUE_NAME);
                                String resultStatus = bundle.getString(MapboxOfflineDownloaderService.KEY_RESULT_STATUS);
                                MapboxOfflineDownloaderService.SERVICE_ACTION serviceAction = (MapboxOfflineDownloaderService.SERVICE_ACTION) bundle.get(MapboxOfflineDownloaderService.KEY_RESULTS_PARENT_ACTION);

                                if (resultStatus.equals(MapboxOfflineDownloaderService.SERVICE_ACTION_RESULT.FAILED.name())) {
                                    String message = bundle.getString(MapboxOfflineDownloaderService.KEY_RESULT_MESSAGE);
                                    showInfoNotification("Error occurred " + mapUniqueName + ":" + serviceAction.name(), message);
                                }
                            }
                        } else {
                            Log.i("KUJAKU SAMPLE APP TAG", "Broadcast message has null Extras");
                        }
                    }
                }, new IntentFilter(Constants.INTENT_ACTION_MAP_DOWNLOAD_SERVICE_STATUS_UPDATES));
    }

    private void downloadMapBoxStyle(String mapboxStyleUrl) {
        MapBoxWebServiceApi mapBoxWebServiceApi = new MapBoxWebServiceApi(this, BuildConfig.MAPBOX_SDK_ACCESS_TOKEN);
        mapBoxWebServiceApi.retrieveStyleJSON(mapboxStyleUrl, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Toast.makeText(MainActivity.this, response, Toast.LENGTH_SHORT)
                        .show();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Error downloading MapBox Style JSON : " + error.getMessage(), Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case MAP_ACTIVITY_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    if (data.hasExtra(NEW_FEATURE_POINTS_JSON)) {
                        saveDroppedPoints(data);
                    }
                }
                break;
            default:
                break;
        }
    }

    private void saveDroppedPoints(Intent data) {
        GenericAsyncTask genericAsyncTask = new GenericAsyncTask(new AsyncTaskCallable() {
            @Override
            public Object[] call() throws Exception {
                List<String> geoJSONFeatures = data.getStringArrayListExtra(NEW_FEATURE_POINTS_JSON);
                for (String geoJSONFeature : geoJSONFeatures) {
                    try {
                        JSONObject featurePoint = new JSONObject(geoJSONFeature);
                        JSONArray coordinates = featurePoint.getJSONObject("geometry").getJSONArray("coordinates");
                        MyApplication.getInstance().getPointsRepository().addOrUpdate(new Point(null, (double) coordinates.get(1), (double) coordinates.get(0)));
                    } catch (Exception e) {
                        Log.e(TAG, "JsonArray parse error occured");
                    }
                }
                return null;
            }
        });
        genericAsyncTask.setOnFinishedListener(null);
        genericAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  
    private void confirmSampleStyleAvailable() {
        MapBoxStyleStorage mapBoxStyleStorage = new MapBoxStyleStorage();
        String style = mapBoxStyleStorage.readStyle("file:///sdcard/Dukto/2017-nov-27-kujaku-metadata.json");
        if (TextUtils.isEmpty(style)) {
            //Write the file to storage
            String sampleStyleString = readAssetFile(SAMPLE_JSON_FILE_NAME);
            mapBoxStyleStorage.writeToFile("Dukto", SAMPLE_JSON_FILE_NAME, sampleStyleString);
        }
    }

    public String readAssetFile(String inFile) {
        String fileStringContents = "";

        try {
            InputStream stream = getAssets().open(inFile);

            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();
            fileStringContents = new String(buffer);
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }

        return fileStringContents;
    }

    private void requestBasicPermissions() {
        ArrayList<String> notGivenPermissions = new ArrayList<>();

        for (String permission : basicPermissions) {
            if (!Permissions.check(this, permission)) {
                notGivenPermissions.add(permission);
            }
        }

        if (notGivenPermissions.size() > 0) {
            Permissions.request(this, notGivenPermissions.toArray(new String[notGivenPermissions.size()]), PERMISSIONS_REQUEST_CODE);
        } else {
            confirmSampleStyleAvailable();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            requestBasicPermissions();
        }
    }

    private void showInfoNotification(String title, String content) {
        lastNotificationId++;
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_dialog_info);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(lastNotificationId, notificationBuilder.build());
    }
}
