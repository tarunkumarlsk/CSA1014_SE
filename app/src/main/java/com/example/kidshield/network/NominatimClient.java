package com.example.kidshield.network;

import android.os.Handler;
import android.os.Looper;
import com.example.kidshield.models.NominatimResult;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple Nominatim (OpenStreetMap) geocoding client — no API key required, with Android Geocoder fallback.
 */
public class NominatimClient {

    private static final ExecutorService pool = Executors.newSingleThreadExecutor();
    private static final Handler main = new Handler(Looper.getMainLooper());
    
    public interface Callback {
        void onResults(List<NominatimResult> results);
    }
    
    public static void search(android.content.Context ctx, String query, Callback callback) {
        pool.execute(() -> {
            List<NominatimResult> results = new ArrayList<>();
            try {
                if (ctx != null && android.location.Geocoder.isPresent()) {
                    android.location.Geocoder geocoder = new android.location.Geocoder(ctx, java.util.Locale.getDefault());
                    List<android.location.Address> addresses = geocoder.getFromLocationName(query, 5);
                    if (addresses != null && !addresses.isEmpty()) {
                        for (android.location.Address a : addresses) {
                            NominatimResult r = new NominatimResult();
                            // prioritize formatted address line
                            r.displayName = a.getAddressLine(0) != null ? a.getAddressLine(0) : a.getFeatureName();
                            r.lat = a.getLatitude();
                            r.lon = a.getLongitude();
                            results.add(r);
                        }
                    }
                }
            } catch (Exception ignored) {}
            
            // Fallback to OSM Network if GeoCoder fails/returns empty
            if (results.isEmpty()) {
                try {
                    String encoded = URLEncoder.encode(query, "UTF-8");
                    String urlStr = "https://nominatim.openstreetmap.org/search?q="
                            + encoded + "&format=json&limit=6&addressdetails=0";
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("User-Agent", "KidShieldApp/1.0 (baranas@example.com)");
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);

                    InputStream is = conn.getInputStream();
                    byte[] buf = new byte[8192];
                    StringBuilder sb = new StringBuilder();
                    int n;
                    while ((n = is.read(buf)) != -1) sb.append(new String(buf, 0, n));
                    conn.disconnect();

                    JSONArray arr = new JSONArray(sb.toString());
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        NominatimResult r = new NominatimResult();
                        r.displayName = obj.optString("display_name");
                        r.lat = obj.optDouble("lat");
                        r.lon = obj.optDouble("lon");
                        results.add(r);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            main.post(() -> callback.onResults(results));
        });
    }

    public static void search(String query, Callback callback) {
        search(null, query, callback);
    }

    public static void reverse(android.content.Context ctx, double lat, double lon, Callback callback) {
        pool.execute(() -> {
            List<NominatimResult> results = new ArrayList<>();
            try {
                if (ctx != null && android.location.Geocoder.isPresent()) {
                    android.location.Geocoder geocoder = new android.location.Geocoder(ctx, java.util.Locale.getDefault());
                    List<android.location.Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        android.location.Address a = addresses.get(0);
                        NominatimResult r = new NominatimResult();
                        r.displayName = a.getAddressLine(0) != null ? a.getAddressLine(0) : "Unknown Location";
                        r.lat = a.getLatitude();
                        r.lon = a.getLongitude();
                        results.add(r);
                    }
                }
            } catch (Exception ignored) {}

            if (results.isEmpty()) {
                try {
                    String urlStr = "https://nominatim.openstreetmap.org/reverse?lat="
                            + lat + "&lon=" + lon + "&format=json&addressdetails=1";
                    URL url = new URL(urlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestProperty("User-Agent", "KidShieldApp/1.0 (baranas@example.com)");
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);

                    InputStream is = conn.getInputStream();
                    byte[] buf = new byte[8192];
                    StringBuilder sb = new StringBuilder();
                    int n;
                    while ((n = is.read(buf)) != -1) sb.append(new String(buf, 0, n));
                    conn.disconnect();

                    JSONObject obj = new JSONObject(sb.toString().trim());
                    NominatimResult r = new NominatimResult();
                    r.displayName = obj.optString("display_name");
                    r.lat = obj.optDouble("lat");
                    r.lon = obj.optDouble("lon");
                    results.add(r);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            main.post(() -> callback.onResults(results));
        });
    }

    public static void reverse(double lat, double lon, Callback callback) {
        reverse(null, lat, lon, callback);
    }
}
