package lk.jiat.bonsaimart.network;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import lk.jiat.bonsaimart.model.WeatherModel;

public class WeatherApiService {

    private static final String TAG = "WeatherApi";

    // ============================================================
    // ⚠️ ඔයාගේ API KEY එක මෙතනට දාන්න
    // ============================================================
    private static final String API_KEY = "YOUR_OPENWEATHER_API_KEY_HERE";

    private static final String BASE_URL =
            "https://api.openweathermap.org/data/2.5/weather";

    // ============================================================
    // CALLBACK INTERFACE
    // ============================================================
    public interface WeatherCallback {
        void onSuccess(WeatherModel weather);
        void onError(String message);
    }

    // ============================================================
    // GET WEATHER BY CITY NAME
    // ============================================================
    public static void getWeather(Context context,
                                  String cityName,
                                  WeatherCallback callback) {

        String url = BASE_URL
                + "?q=" + cityName
                + "&appid=" + API_KEY
                + "&units=metric";  // Celsius

        Log.d(TAG, "Requesting: " + url);

        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        WeatherModel weather = parseWeatherResponse(response);
                        weather.setSuccess(true);
                        callback.onSuccess(weather);

                    } catch (Exception e) {
                        Log.e(TAG, "Parse error", e);
                        callback.onError("Parse error: " + e.getMessage());
                    }
                },
                error -> {
                    Log.e(TAG, "Volley error", error);
                    String message = "Network error";
                    if (error.networkResponse != null) {
                        message += " (Code: " + error.networkResponse.statusCode + ")";
                    }
                    callback.onError(message);
                }
        );

        queue.add(request);
    }

    // ============================================================
    // PARSE JSON RESPONSE
    // ============================================================
    private static WeatherModel parseWeatherResponse(JSONObject json) throws Exception {

        WeatherModel weather = new WeatherModel();

        // City name
        weather.setCityName(json.getString("name"));

        // Main data (temperature, humidity)
        JSONObject main = json.getJSONObject("main");
        weather.setTemperature(main.getDouble("temp"));
        weather.setFeelsLike(main.getDouble("feels_like"));
        weather.setHumidity(main.getInt("humidity"));

        // Wind
        JSONObject wind = json.getJSONObject("wind");
        weather.setWindSpeed(wind.getDouble("speed") * 3.6); // m/s to km/h

        // Weather description
        if (json.has("weather") && json.getJSONArray("weather").length() > 0) {
            JSONObject weatherObj = json.getJSONArray("weather").getJSONObject(0);
            weather.setDescription(weatherObj.getString("description"));
            weather.setIconCode(weatherObj.getString("icon"));
        }

        return weather;
    }
}