package com.example.feedbackpro;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Feedback extends AppCompatActivity {

    TextView tv2;
    EditText et1;
    Button bt1;
    String url1;
    private DatabaseReference databaseReference;

    class GetWeather extends AsyncTask<String, Void, String> {
        private final String cityName;

        // Constructor to pass city name
        public GetWeather(String cityName) {
            this.cityName = cityName;
        }

        @Override
        protected String doInBackground(String... url1) {
            StringBuilder result = new StringBuilder();
            try {
                URL url = new URL(url1[0]);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));

                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
                return result.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d("WeatherData", "Response: " + result);
            if (result != null) {
                try {
                    JSONObject jsonObject = new JSONObject(result);
                    if (jsonObject.has("cod")) {
                        int responseCode = jsonObject.getInt("cod");
                        if (responseCode != 200) {
                           
                            String errorMessage = jsonObject.optString("message", "Unknown error");
                            tv2.setText("Error: " + errorMessage);
                            return;
                        }
                    }

                    JSONObject main = jsonObject.optJSONObject("main");
                    if (main == null) {
                        Log.e("WeatherData", "Main object is null");
                        tv2.setText("Error: Main weather data not found.");
                        return;
                    }

                    // Extracting temperature
                    double temp = main.optDouble("temp", Double.NaN);
                    if (Double.isNaN(temp)) {
                        Log.e("WeatherData", "Temperature data is missing");
                        tv2.setText("Error: Temperature data not found.");
                        return;
                    }


                    temp -= 273.15;

                    // Extracting weather description
                    JSONArray weatherArray = jsonObject.optJSONArray("weather");
                    String description = "";
                    if (weatherArray != null && weatherArray.length() > 0) {
                        JSONObject weather = weatherArray.optJSONObject(0);
                        if (weather != null) {
                            description = weather.optString("description", "No description");
                        } else {
                            Log.e("WeatherData", "Weather object is null");
                            tv2.setText("Error: Weather description not found.");
                            return;
                        }
                    } else {
                        Log.e("WeatherData", "Weather array is null or empty");
                        tv2.setText("Error: Weather data not found.");
                        return;
                    }
                    // Displaying the parsed information
                    String weatherInfo = String.format("City: %s\nTemperature: %.2f °C\nDescription: %s", cityName, temp, description);
                    tv2.setText(weatherInfo);

                    // Saving to Firebase
                    saveWeatherDataToFirebase(cityName, temp, description);

                } catch (Exception e) {
                    Log.e("WeatherData", "Parsing error: " + e.getMessage());
                    e.printStackTrace();
                    tv2.setText("Unable to parse weather data.");
                }
            } else {
                tv2.setText("No response received from server.");
            }
        }


        private void saveWeatherDataToFirebase(String city, double temperature, String description) {
            // Creating a unique ID for each entry based on timestamp
            String key = databaseReference.push().getKey();
            if (key != null) {

                WeatherInfo weatherInfo = new WeatherInfo(city, temperature, description);

                databaseReference.child(key).setValue(weatherInfo)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(Feedback.this, "Weather data saved successfully", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(Feedback.this, "Failed to save weather data", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }
    public static class WeatherInfo {
        public String city;
        public double temperature;
        public String description;


        public WeatherInfo(String city, double temperature, String description) {
            this.city = city;
            this.temperature = temperature;
            this.description = description;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feedback);

        tv2 = findViewById(R.id.tv2);
        et1 = findViewById(R.id.et1);
        bt1 = findViewById(R.id.bt1);
        databaseReference = FirebaseDatabase.getInstance().getReference("WeatherData");

        bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = et1.getText().toString();
                if (!city.isEmpty()) {
                    Toast.makeText(Feedback.this, "Searching", Toast.LENGTH_SHORT).show();
                    url1 = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=59df33f8049c196c42b93f021cafacb9";
                    GetWeather task = new GetWeather(city);
                    task.execute(url1);
                } else {
                    Toast.makeText(Feedback.this, "Enter city", Toast.LENGTH_SHORT).show();
                }
            }
        });





        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}