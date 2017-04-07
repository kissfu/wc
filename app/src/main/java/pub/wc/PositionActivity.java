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

import pub.beans.PositionInfo;
import pub.utils.MapUtil;
import pub.utils.ToastUtil;

public class PositionActivity extends AppCompatActivity  implements View.OnClickListener  {

    private Button send;
    private TextView tv_city;
    private TextView tv_lon;
    private TextView tv_lat;
    private TextView tv_location;
    private TextView tv_poi;

    private PositionInfo positionInfo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_position);

        positionInfo = (PositionInfo)getIntent().getParcelableExtra("positioninfo");

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
        ToastUtil.show(PositionActivity.this,"获取地址信息。");
        //currentAMapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
        tv_lat.setText("当前纬度：" + positionInfo.getLatLonPoint().getLatitude());
        tv_lon.setText("当前经度：" + positionInfo.getLatLonPoint().getLongitude());
        tv_location.setText("当前地址：" + positionInfo.getAddress());
        tv_city.setText("当前城市：" + positionInfo.getAddressJoin());
        tv_poi.setText("当前位置："+positionInfo.getAoiName());
        //currentAMapLocation.getAccuracy();//获取精度信息
    }
    //endregion

    //region implements View.OnClickListener
    @Override
    public void onClick(View v) {
        if (v == send) {
            Intent intent = new Intent();
            intent.putExtra("longitude", positionInfo.getLatLonPoint().getLongitude());
            intent.putExtra("latitude", positionInfo.getLatLonPoint().getLatitude());
            intent.putExtra("cityCode", positionInfo.getCityCode());
            intent.setClass(this, SelectActivity.class);
            startActivity(intent);
        }
    }
    //endregion
}
