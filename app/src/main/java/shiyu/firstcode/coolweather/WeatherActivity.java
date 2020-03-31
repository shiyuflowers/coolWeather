package shiyu.firstcode.coolweather;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.IOException;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import shiyu.firstcode.coolweather.gson.Forecast;
import shiyu.firstcode.coolweather.gson.Weather;
import shiyu.firstcode.coolweather.util.HttpUtil;
import shiyu.firstcode.coolweather.util.Utility;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView mScrollView_weather;
    private TextView mTextView_titleCity;
    private TextView mTextView_titleUpdateTime;
    private TextView mTextView_degree;
    private TextView mTextView_weatherInfo;
    private LinearLayout mLinearLayout_forecast;
    private TextView mTextView_aqi;
    private TextView mTextView_pm25;
    private TextView mTextView_comfort;
    private TextView mTextView_carWash;
    private TextView mTextView_sport;
    private ImageView mImageView_bigPicImg;
    public SwipeRefreshLayout mSwipeRefreshLayout;
    private String mWeatherId;
    public DrawerLayout mDrawerLayout;
    private Button mButton_navigator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT>=21)
        {
            View decorView=getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        //初始化各控件
        mScrollView_weather=findViewById(R.id.scrollView);
        mTextView_titleCity=findViewById(R.id.textView_titleCity);
        mTextView_titleUpdateTime=findViewById(R.id.textView_titleUpdateTime);
        mTextView_degree=findViewById(R.id.textView_degreeText);
        mTextView_weatherInfo=findViewById(R.id.textView_weatherInfo);
        mLinearLayout_forecast=findViewById(R.id.linearLayout_forecast);
        mTextView_aqi=findViewById(R.id.textView_aqi);
        mTextView_pm25=findViewById(R.id.textView_pm25);
        mTextView_comfort=findViewById(R.id.textView_comfort);
        mTextView_carWash=findViewById(R.id.textView_carWash);
        mTextView_sport=findViewById(R.id.textView_sport);
        mImageView_bigPicImg=findViewById(R.id.imageView_bingPicImg);
        mSwipeRefreshLayout=findViewById(R.id.swipeRefresh);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        mDrawerLayout=findViewById(R.id.drawerLayout);
        mButton_navigator=findViewById(R.id.button_navigator);


        SharedPreferences sharedPreferences = getWeatherSharedPreferences();

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getWeatherSharedPreferences();
                requestWeather(mWeatherId);
            }
        });

        String bingPicImgString=sharedPreferences.getString("bing_pic",null);
        if(bingPicImgString!=null)
        {
            Glide.with(this).load(bingPicImgString).into(mImageView_bigPicImg);
        }

        mButton_navigator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
        });

    }

    private SharedPreferences getWeatherSharedPreferences() {
        SharedPreferences sharedPreferences= PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString=sharedPreferences.getString("weather",null);
        if(weatherString!=null)
        {
            //有缓存直接解析天气数据
            Weather weather= Utility.handleWeatherResponse(weatherString);
            mWeatherId=weather.basic.weatherId;
            showWeatherInfo(weather);
        }else
        {
            //无缓存去服务器查询天气
            mWeatherId=getIntent().getStringExtra("weather_id");
//            String weatherId=getIntent().getStringExtra("weather_id");
            mScrollView_weather.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }
        return sharedPreferences;
    }

    //根据天气id请求城市天气信息
    public void requestWeather(String weatherId) {
        String weatherUrl="http://guolin.tech/api/weather?cityid="
                +weatherId+"&key=4291bce145794de6967cfb0dd7008dea";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText=response.body().string();
                Log.d("****************",responseText);
                final Weather weather=Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather!=null&& "ok".equals(weather.status))
                        {
                            SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        }else
                        {
                            Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        }
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });

            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });

            }
        });
        loadBingPic();
    }

    //加载必应每日一图
    private void loadBingPic() {
        String requestBingPic="http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic=response.body().string();
                SharedPreferences.Editor editor=PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(mImageView_bigPicImg);
                    }
                });

            }
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }


        });
    }

    //处理并展示Weather实体类中的数据
    private void showWeatherInfo(Weather weather) {
        String cityName=weather.basic.cityName;
        String updateTime=weather.basic.update.updateTime.split(" ")[1];
        String degree=weather.now.temperature+"摄氏度";
        String weatherInfo=weather.now.more.info;
        mTextView_titleCity.setText(cityName);
        mTextView_titleUpdateTime.setText(updateTime);
        mTextView_degree.setText(degree);
        mTextView_weatherInfo.setText(weatherInfo);
        mLinearLayout_forecast.removeAllViews();
        for(Forecast forecast:weather.forecastList)
        {
            View view= LayoutInflater.from(this).inflate(R.layout.forecast_item,mLinearLayout_forecast,false);
            TextView textView_date=view.findViewById(R.id.textView_date);
            TextView textView_info=view.findViewById(R.id.textView_info);
            TextView textView_max=view.findViewById(R.id.textView_max);
            TextView textView_min=view.findViewById(R.id.textView_min);
            textView_date.setText(forecast.date);
            textView_info.setText(forecast.more.info);
            textView_max.setText(forecast.temperature.max);
            textView_min.setText(forecast.temperature.min);
            mLinearLayout_forecast.addView(view);
        }

        if(weather.aqi!=null)
        {
            mTextView_aqi.setText(weather.aqi.city.api);
            mTextView_pm25.setText(weather.aqi.city.pm25);
        }

        String comfort="舒适度:"+weather.suggestion.comfort.info;
        String carWash="洗车指数:"+weather.suggestion.carWash.info;
        String sport="运动建议:"+weather.suggestion.sport.info;
        mTextView_comfort.setText(comfort);
        mTextView_carWash.setText(carWash);
        mTextView_sport.setText(sport);
        mScrollView_weather.setVisibility(View.VISIBLE);

    }
}
