package com.coolweather.android;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.coolweather.android.db.County;
import com.coolweather.android.gson.Forecast;
import com.coolweather.android.gson.Weather;
import com.coolweather.android.service.AutoUpdateService;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    public DrawerLayout drawerLayout;

    public SwipeRefreshLayout swipeRefresh;

    private ScrollView weatherLayout;

    private Button navButton;
    private Button shareButton;
    private TextView titleCity;
    private TextView titleUpdateTime;

    private TextView degreeText;

    private TextView weatherInfoText;

    private LinearLayout forecastLayout;

    private TextView aqiText;

    private TextView pm25Text;

    private TextView comfortText;

    private TextView carWashText;

    private TextView sportText;

    private ImageView bingPicImg;

    private String mWeatherId;
    private Button addressButton;
    private List<County> countyList;
    private String WeatherId;
    private String a;
    private String b;
    private String c;
    private String d;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);

        bingPicImg = (ImageView) findViewById(R.id.bing_pic_img);
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navButton = (Button) findViewById(R.id.nav_button);
        shareButton=(Button) findViewById(R.id.share_button);
        addressButton=(Button)findViewById(R.id.address_button);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String weatherString = prefs.getString("weather", null);
        final String weatherString1 = prefs.getString("weather", null);
        if (weatherString != null) {
            // 有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            // 无缓存时去服务器查询天气
            mWeatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Weather weather = Utility.handleWeatherResponse(weatherString1);
                Intent wechatIntent = new Intent(Intent.ACTION_SEND);
                wechatIntent.setType("text/plain");
                for (Forecast forecast : weather.forecastList){
                    a = forecast.date;
                    b = forecast.more.info;
                    c = forecast.temperature.max;
                    d = forecast.temperature.min;
                }
                /*wechatIntent.putExtra(Intent.EXTRA_TEXT, "城市名："+ weather.basic.cityName);
                wechatIntent.putExtra(Intent.EXTRA_TEXT, "更新时间："+ weather.basic.update.updateTime.split("")[1]);
                wechatIntent.putExtra(Intent.EXTRA_TEXT, "温度："+ weather.now.temperature+"℃");
                wechatIntent.putExtra(Intent.EXTRA_TEXT, "天气："+ weather.now.more.info);*/
                wechatIntent.putExtra(Intent.EXTRA_TEXT, "城市名："+ weather.basic.cityName+"   "+"天气："+ weather.now.more.info+"  "+"预报："+a+b+"最高温度："+c+"℃"+
                        "最低温度:"+d+"℃");
                /*wechatIntent.putExtra(Intent.EXTRA_TEXT, "天气情况：" + weather.forecastList );
                wechatIntent.putExtra(Intent.EXTRA_TEXT, "建议：" + weather.suggestion);*/
                startActivity(wechatIntent);



            }
        });

        addressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                countyList = DataSupport.findAll(County.class);
                if(countyList.size() >0){
                    for(County county : countyList){
                        if(county.getCountyName().equals("长沙")){
                            WeatherId = county.getWeatherId();
                        }
                    }
                    requestWeather(WeatherId);
                }

            }
        });

        String bingPic = prefs.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }
    }

    /**
     * 根据天气id请求城市天气信息。
     */
    public void requestWeather(final String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=3438935be958444aa839ad667d7391c1";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            mWeatherId = weather.basic.weatherId;
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
        loadBingPic();
    }

    /**
     * 加载必应每日一图
     */
    private void loadBingPic() {
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 处理并展示Weather实体类中的数据。
     */
    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews();
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.date_text);
            TextView infoText = (TextView) view.findViewById(R.id.info_text);
            TextView maxText = (TextView) view.findViewById(R.id.max_text);
            TextView minText = (TextView) view.findViewById(R.id.min_text);
            ImageView infoimage=(ImageView) view.findViewById(R.id.info_image);
            if(forecast.more.info.equals("晴"))
            {
                infoimage.setImageResource(R.drawable.sun);
            }

            //雨有关的天气
            if(forecast.more.info.equals("阵雨"))
            {
                infoimage.setImageResource(R.drawable.zhy);
            }
            if(forecast.more.info.equals("雷阵雨 "))
            {
                infoimage.setImageResource(R.drawable.lzy);
            }
            if(forecast.more.info.equals("小雨"))
            {
                infoimage.setImageResource(R.drawable.xy);
            }
            if(forecast.more.info.equals("中雨"))
            {
                infoimage.setImageResource(R.drawable.zy);
            }
            if(forecast.more.info.equals("大雨"))
            {
                infoimage.setImageResource(R.drawable.dy);
            }
            if(forecast.more.info.equals("暴雨"))
            {
                infoimage.setImageResource(R.drawable.by);
            }
            if(forecast.more.info.equals("大暴雨"))
            {
                infoimage.setImageResource(R.drawable.dby);
            }
            if(forecast.more.info.equals("特大暴雨"))
            {
                infoimage.setImageResource(R.drawable.tdby);
            }
            if(forecast.more.info.equals("小到中雨"))
            {
                infoimage.setImageResource(R.drawable.x_to_zyu);
            }
            if(forecast.more.info.equals("中到大雨"))
            {
                infoimage.setImageResource(R.drawable.z_to_dyu);
            }
            if(forecast.more.info.equals("大到暴雨"))
            {
                infoimage.setImageResource(R.drawable.d_to_byu);
            }
            if(forecast.more.info.equals("暴雨到大暴雨"))
            {
                infoimage.setImageResource(R.drawable.b_to_tbyu);
            }
            if(forecast.more.info.equals("大暴雨到特大暴雨"))
            {
                infoimage.setImageResource(R.drawable.d_to_tdyu);
            }
            //雪
            if(forecast.more.info.equals("阵雪"))
            {
                infoimage.setImageResource(R.drawable.zhx);
            }
            if(forecast.more.info.equals("小雪"))
            {
                infoimage.setImageResource(R.drawable.xx);
            }
            if(forecast.more.info.equals("中雪"))
            {
                infoimage.setImageResource(R.drawable.zx);
            }
            if(forecast.more.info.equals("暴雪"))
            {
                infoimage.setImageResource(R.drawable.bx);
            }


            if(forecast.more.info.equals("雾"))
            {
                infoimage.setImageResource(R.drawable.wu);
            }
            if(forecast.more.info.equals("多云"))
            {
                infoimage.setImageResource(R.drawable.clound);
            }
            if(forecast.more.info.equals("阴"))
            {
                infoimage.setImageResource(R.drawable.y);
            }
            if(forecast.more.info.equals("雨夹雪"))
            {
                infoimage.setImageResource(R.drawable.y_x);
            }
            if(forecast.more.info.equals("小到中雪"))
            {
                infoimage.setImageResource(R.drawable.x_to_zx);
            }
            if(forecast.more.info.equals("中到大雪"))
            {
                infoimage.setImageResource(R.drawable.z_to_dx);
            }
            if(forecast.more.info.equals("大到暴雪 "))
            {
                infoimage.setImageResource(R.drawable.d_to_bx);
            }

            if(forecast.more.info.equals("雷阵雨伴有冰雹 "))
            {
                infoimage.setImageResource(R.drawable.lzy_bb);
            }
            if(forecast.more.info.equals("浮尘"))
            {
                infoimage.setImageResource(R.drawable.fz);
            }
            if(forecast.more.info.equals("扬沙"))
            {
                infoimage.setImageResource(R.drawable.ys);
            }
            if(forecast.more.info.equals("沙尘暴"))
            {
                infoimage.setImageResource(R.drawable.scb);
            }
            if(forecast.more.info.equals("强沙尘暴"))
            {
                infoimage.setImageResource(R.drawable.qscb);
            }
            if(forecast.more.info.equals("特强沙尘暴"))
            {
                infoimage.setImageResource(R.drawable.tqscb);
            }
            if(forecast.more.info.equals("冻雨"))
            {
                infoimage.setImageResource(R.drawable.dongy);
            }
            if(forecast.more.info.equals("轻雾"))
            {
                infoimage.setImageResource(R.drawable.qw);
            }
            if(forecast.more.info.equals("浓雾"))
            {
                infoimage.setImageResource(R.drawable.nw);
            }
            if(forecast.more.info.equals("强浓雾"))
            {
                infoimage.setImageResource(R.drawable.qnw);
            }
            if(forecast.more.info.equals("轻度霾"))
            {
                infoimage.setImageResource(R.drawable.qdm);
            }
            if(forecast.more.info.equals("轻微霾"))
            {
                infoimage.setImageResource(R.drawable.qwm);
            }
            if(forecast.more.info.equals("中度霾"))
            {
                infoimage.setImageResource(R.drawable.zdm);
            }
            if(forecast.more.info.equals("重度霾"))
            {
                infoimage.setImageResource(R.drawable.zhdm);
            }
            if(forecast.more.info.equals("特强霾"))
            {
                infoimage.setImageResource(R.drawable.tqm);
            }
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }
        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运行建议：" + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }

}
