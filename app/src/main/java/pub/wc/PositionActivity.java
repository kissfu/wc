package pub.wc;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;

import pub.utils.MapUtil;
import pub.utils.ToastUtil;

public class PositionActivity extends AppCompatActivity  implements View.OnClickListener  {

    private Button send;
    private TextView tv_city;
    private TextView tv_lon;
    private TextView tv_lat;
    private TextView tv_location;
    private TextView tv_poi;

    private double longitude;
    private double latitude;
    private String cityCode;
    private AMapLocation currentAMapLocation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_position);

        currentAMapLocation = (AMapLocation)getIntent().getParcelableExtra("amap");

        send = (Button) findViewById(R.id.btn_send);

        tv_city = (TextView) findViewById(R.id.city);
        tv_lon = (TextView) findViewById(R.id.lon);
        tv_lat = (TextView) findViewById(R.id.lat);
        tv_location = (TextView) findViewById(R.id.location);
        tv_poi = (TextView) findViewById(R.id.poi);

        send.setOnClickListener(this);

        initData();
    }

    //region intent extras

    private void initData(){
        if(currentAMapLocation == null){
            return;
        }
        ToastUtil.show(PositionActivity.this,"定位成功，获取地址信息。");
        currentAMapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
        latitude = currentAMapLocation.getLatitude();//获取纬度
        longitude = currentAMapLocation.getLongitude();//获取经度
        cityCode = currentAMapLocation.getCityCode();
        tv_lat.setText("当前纬度：" + latitude);
        tv_lon.setText("当前经度：" + longitude);
        tv_location.setText("当前位置：" + currentAMapLocation.getAddress());
        tv_city.setText("当前城市：" + currentAMapLocation.getProvince() + "-" + currentAMapLocation.getCity() + "-" + currentAMapLocation.getDistrict() + "-" + currentAMapLocation.getStreet() + "-" + currentAMapLocation.getStreetNum());
        tv_poi.setText("当前位置："+currentAMapLocation.getAoiName());
        currentAMapLocation.getAccuracy();//获取精度信息
    }
    //endregion

    //region implements View.OnClickListener
    @Override
    public void onClick(View v) {
        if (v == send) {
            Intent intent = new Intent();
            intent.putExtra("longitude", longitude);
            intent.putExtra("latitude", latitude);
            intent.putExtra("cityCode", cityCode);
            //intent.setClass(this, ShareLocationActivity.class);
            //startActivity(intent);
        }
    }
    //endregion
}
