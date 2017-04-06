package pub.wc;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptor;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.CameraPosition;
import com.amap.api.maps2d.model.Circle;
import com.amap.api.maps2d.model.CircleOptions;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.VisibleRegion;
import com.amap.api.services.core.LatLonPoint;

import pub.beans.PositionInfo;
import pub.utils.SensorEventHelper;
import pub.utils.ToastUtil;

public class MainActivity extends AppCompatActivity implements LocationSource,AMapLocationListener,AMap.OnCameraChangeListener,View.OnClickListener {

    private MapView mapView;
    private AMap aMap;
    private ImageButton ibtnLocation;
    private ImageButton ibtnAddLocation;
    private LocationSource.OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    private AMapLocation currentAMapLocation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ibtnLocation = (ImageButton) findViewById(R.id.ibtn_position);
        ibtnLocation.setOnClickListener(this);
        ibtnAddLocation = (ImageButton) findViewById(R.id.ibtn_add_position);
        ibtnAddLocation.setOnClickListener(this);
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写
        init();
    }
    /**
     * 初始化AMap对象
     */
    private void init() {
        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.setLocationSource(this);// 设置定位监听
            //aMap.getUiSettings().setMyLocationButtonEnabled(false);// 设置默认定位按钮是否显示
            aMap.getUiSettings().setZoomControlsEnabled(false);
            //地图比例尺的开启
            aMap.getUiSettings().setScaleControlsEnabled(true);
            aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
            //设置地图默认的定位按钮是否显示
            //aMap.setOnCameraChangeListener(this);// 对amap添加移动地图事件监听器
            //重力感应
            mSensorHelper = new SensorEventHelper(this);
            mSensorHelper.registerSensorListener();
        }
    }


    //region Activity Life
    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    //endregion

    //region  implements LocationSource
    /**
     * 激活定位
     */
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
        ToastUtil.show(this,"激活定位");
        if (mlocationClient == null) {
            mlocationClient = new AMapLocationClient(this);
            mLocationOption = new AMapLocationClientOption();
            //设置定位监听
            mlocationClient.setLocationListener(this);
            //设置为高精度定位模式
            mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            //设置定位参数
            mlocationClient.setLocationOption(mLocationOption);
            // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
            // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
            // 在定位结束后，在合适的生命周期调用onDestroy()方法
            // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
            mlocationClient.startLocation();
        }
    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        ToastUtil.show(this,"停止定位");
        mListener = null;
        if (mlocationClient != null) {
            mlocationClient.stopLocation();
            mlocationClient.onDestroy();
        }
        mlocationClient = null;
    }
    //endregion

    //region  implements AMapLocationListener
    /**
     * 定位成功后回调函数
     */
    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if (mListener != null && aMapLocation != null) {
            if (aMapLocation != null && aMapLocation.getErrorCode() == 0) {
                ToastUtil.show(this,"定位成功");
                addSensorMaker(aMapLocation);
            } else {
                String errText = "定位失败," + aMapLocation.getErrorCode()+ ": " + aMapLocation.getErrorInfo();
                ToastUtil.show(this,errText);
            }
        }
    }

    private SensorEventHelper mSensorHelper;
    private boolean mFirstFix = false;
    private Marker mLocMarker;
    private Circle mCircle;
    private void addSensorMaker(AMapLocation amapLocation){
        currentAMapLocation = amapLocation;
        LatLng latLng = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
        if (!mFirstFix) {
            mFirstFix = true;
            addCircle(latLng, amapLocation.getAccuracy());//添加定位精度圆
            addMarker(latLng);//添加定位图标
            mSensorHelper.setCurrentMarker(mLocMarker);//定位图标旋转
            //这只放大缩小的级别
            //mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
            //aMap.moveCamera(CameraUpdateFactory.zoomTo(18));
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18));
        } else {
            mCircle.setCenter(latLng);
            mCircle.setRadius(amapLocation.getAccuracy());
            mLocMarker.setPosition(latLng);
        }
    }
    private static final int STROKE_COLOR = Color.argb(180, 3, 145, 255);
    private static final int FILL_COLOR = Color.argb(10, 0, 0, 180);
    private void addCircle(LatLng latlng, double radius) {
        CircleOptions options = new CircleOptions();
        options.strokeWidth(1f);
        options.fillColor(FILL_COLOR);
        options.strokeColor(STROKE_COLOR);
        options.center(latlng);
        options.radius(radius);
        mCircle = aMap.addCircle(options);
    }

    private void addMarker(LatLng latlng) {
        if (mLocMarker != null) {
            return;
        }
        Bitmap bMap = BitmapFactory.decodeResource(this.getResources(), R.mipmap.navi_map_gps_locked);
        BitmapDescriptor des = BitmapDescriptorFactory.fromBitmap(bMap);

//		BitmapDescriptor des = BitmapDescriptorFactory.fromResource(R.drawable.navi_map_gps_locked);
        MarkerOptions options = new MarkerOptions();
        options.icon(des);
        options.anchor(0.5f, 0.5f);
        options.position(latlng);
        mLocMarker = aMap.addMarker(options);
        mLocMarker.setTitle("wc");
    }
    //endregion

    //region  implements OnCameraChangeListener
    /**
     * 对正在移动地图事件回调
     */
    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        ToastUtil.show(this,"onCameraChange:" + cameraPosition.toString());

    }

    /**
     * 对移动地图结束事件回调
     */
    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        ToastUtil.show(this,"onCameraChangeFinish:" + cameraPosition.toString());
        VisibleRegion visibleRegion = aMap.getProjection().getVisibleRegion(); // 获取可视区域、
//        LatLngBounds latLngBounds = visibleRegion.latLngBounds;// 获取可视区域的Bounds
//        boolean isContain = latLngBounds.contains(Constants.SHANGHAI);// 判断上海经纬度是否包括在当前地图可见区域
//        if (isContain) {
//            ToastUtil.show(EventsActivity.this, "上海市在地图当前可见区域内");
//        } else {
//            ToastUtil.show(EventsActivity.this, "上海市超出地图当前可见区域");
//        }
    }

    //endregion

    //regionm View.OnClickListener
    @Override
    public void onClick(View v) {
        if (v == ibtnLocation) {
            if(currentAMapLocation != null){
                //回到当前位置
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentAMapLocation.getLatitude(), currentAMapLocation.getLongitude()), 18));
            }
        }else if(v == ibtnAddLocation){
            Intent intent = new Intent();
            PositionInfo positionInfo = new PositionInfo();
            positionInfo.setCityCode(currentAMapLocation.getCityCode());
            positionInfo.setLatLonPoint(new LatLonPoint(currentAMapLocation.getLatitude(), currentAMapLocation.getLongitude()));
            positionInfo.setAddress(currentAMapLocation.getAddress());
            positionInfo.setAddressJoin(currentAMapLocation.getProvince() + "-" + currentAMapLocation.getCity() + "-" + currentAMapLocation.getDistrict() + "-" + currentAMapLocation.getStreet() + "-" + currentAMapLocation.getStreetNum());
            positionInfo.setAoiName(currentAMapLocation.getAoiName());
            intent.putExtra("positioninfo",positionInfo);//实现Parcelable接口的对象
            intent.setClass(this, PositionActivity.class);
            startActivity(intent);
        }
    }

    //endregion
}
