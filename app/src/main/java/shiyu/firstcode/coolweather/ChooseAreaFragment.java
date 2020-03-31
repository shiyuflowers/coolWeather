package shiyu.firstcode.coolweather;

//import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import shiyu.firstcode.coolweather.db.City;
import shiyu.firstcode.coolweather.db.Country;
import shiyu.firstcode.coolweather.db.Province;
import shiyu.firstcode.coolweather.util.HttpUtil;
import shiyu.firstcode.coolweather.util.Utility;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE=0;
    public static final int LEVEL_CITY=1;
    public static final int LEVEL_COUNTRY=2;
    private ProgressDialog mProgressDialog;
    private TextView mTextView_titleText;
    private Button mButton_back;
    private ListView mListView;
    private ArrayAdapter<String> mArrayAdapter;
    private List<String> mStringList_data=new ArrayList<>();

    //省列表
    private List<Province> mProvinceList;

    //市列表
    private List<City> mCityList;

    //县列表
    private List<Country> mCountryList;

    //选中的省份
    private  Province selectedProvince;

    //选中的城市
    private City selectedCity;

    //当前选中的级别
    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.choose_area,container,false);
        mTextView_titleText=view.findViewById(R.id.textView_titleText);
        mButton_back=view.findViewById(R.id.button_back);
        mListView=view.findViewById(R.id.listView);
        mArrayAdapter=new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,mStringList_data);
        mListView.setAdapter(mArrayAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel==LEVEL_PROVINCE)
                {
                    selectedProvince=mProvinceList.get(position);
                    queryCites();
                }else if(currentLevel==LEVEL_CITY)
                {
                    selectedCity=mCityList.get(position);
                    queryCountries();
                }else if(currentLevel==LEVEL_COUNTRY)
                {
                    String weatherId=mCountryList.get(position).getWeatherId();
                    if(getActivity() instanceof MainActivity)
                    {
                        Intent intent=new Intent(getActivity(),WeatherActivity.class);
                        intent.putExtra("weather_id",weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity)
                    {
                        WeatherActivity weatherActivity=(WeatherActivity)getActivity();
                        weatherActivity.mDrawerLayout.closeDrawers();
                        weatherActivity.mSwipeRefreshLayout.setRefreshing(true);
                        weatherActivity.requestWeather(weatherId);
                    }


                }
            }
        });
        mButton_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel==LEVEL_COUNTRY)
                {
                    queryCites();
                }else if(currentLevel==LEVEL_CITY)
                {
                    queryProvinces();
                }
            }


        });
        queryProvinces();
    }

    //查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询
    private void queryProvinces() {
        mTextView_titleText.setText("中国");
        mButton_back.setVisibility(View.GONE);
        mProvinceList= LitePal.findAll(Province.class);
        if(mProvinceList.size()>0)
        {
            mStringList_data.clear();
            for(Province province:mProvinceList)
            {
                mStringList_data.add(province.getProvinceName());
            }
            mArrayAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel=LEVEL_PROVINCE;
        }else
        {
            String address="http://guolin.tech/api/china";
            queryFromSever(address,"province");
        }
        
    }



    //查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询
    private void queryCites() {
        mTextView_titleText.setText(selectedProvince.getProvinceName());
        mButton_back.setVisibility(View.VISIBLE);
        mCityList=LitePal.where("provinceId=?",String.valueOf(selectedProvince.getId())).find(City.class);
        if(mCityList.size()>0)
        {
            mStringList_data.clear();
            for(City city:mCityList)
            {
                mStringList_data.add(city.getCityName());
            }
            mArrayAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel=LEVEL_CITY;
        }else
        {
            int proviceCode=selectedProvince.getProvinceCode();
            String address="http://guolin.tech/api/china/"+proviceCode;
            queryFromSever(address,"city");
        }
    }

    //查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询
    private void queryCountries() {
        mTextView_titleText.setText(selectedCity.getCityName());
        mButton_back.setVisibility(View.VISIBLE);
        mCountryList=LitePal.where("cityId=?",String.valueOf(selectedCity.getId())).find(Country.class);
        if(mCountryList.size()>0)
        {
            mStringList_data.clear();
            for(Country country:mCountryList)
            {
                mStringList_data.add(country.getCountryName());
            }
            mArrayAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel=LEVEL_COUNTRY;
        }else
        {
            int provinceCode=selectedProvince.getProvinceCode();
            int cityCode=selectedCity.getCityCode();
            String address="http://guolin.tech/api/china/"+provinceCode+"/"+cityCode;
            queryFromSever(address,"county");
        }
    }

    //根据传入的地址和类型从服务器上查询省市县数据
    private void queryFromSever(String address,final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //通过runOnUiThread()方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText=response.body().string();
                boolean result=false;
                if("province".equals(type))
                {
                    result= Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type))
                {
                    result=Utility.handleCityResponse(responseText,selectedProvince.getProvinceCode());
                }else if("county".equals(type))
                {
                    result=Utility.handleCountryResponse(responseText,selectedCity.getId());
                }

                if(result)
                {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type))
                            {
                                queryProvinces();
                            }else if("city".equals(type))
                            {
                                queryCites();
                            }else if("county".equals(type))
                            {
                                queryCountries();
                            }

                        }
                    });

                }

            }
        });
    }

    //显示进度对话框
    private void showProgressDialog() {
        if(mProgressDialog==null)
        {
            mProgressDialog=new ProgressDialog(getActivity());
            mProgressDialog.setMessage("正在加载......");
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.show();

    }

    //关闭进度对话框
    private void closeProgressDialog() {
        if(mProgressDialog!=null)
        {
            mProgressDialog.dismiss();
        }
    }


}
