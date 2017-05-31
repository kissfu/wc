package pub.wc;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import pub.beans.PositionInfo;
import pub.beans.WCLocation;
import pub.managers.ReqCallBack;
import pub.managers.RequestManager;
import pub.utils.Constants;
import pub.utils.SensorEventHelper;
import pub.utils.ThreadUtil;
import pub.utils.ToastUtil;

import static pub.managers.RequestManager.TYPE_POST_FORM;

public class MainActivity extends AppCompatActivity implements LocationSource,AMapLocationListener, AMap.OnCameraChangeListener,View.OnClickListener,AMap.OnMapClickListener,AMap.OnMarkerClickListener {

    private RelativeLayout rlMakerDetail;
    private TextView tvTitle;
    private TextView tvAddress;
    private MapView mapView;
    private AMap aMap;
    private ImageButton ibtnLocation;
    private ImageButton ibtnAddLocation;
    private LocationSource.OnLocationChangedListener mListener;
    private AMapLocationClient mlocationClient;
    private AMapLocationClientOption mLocationOption;
    private AMapLocation currentAMapLocation;
    private GeocodeSearch geocoderSearch;
    private Marker selectMarker;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        rlMakerDetail = (RelativeLayout) findViewById(R.id.rl_maker_detail);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        tvAddress = (TextView) findViewById(R.id.tv_address);
        ibtnLocation = (ImageButton) findViewById(R.id.ibtn_position);
        ibtnLocation.setOnClickListener(this);
        ibtnAddLocation = (ImageButton) findViewById(R.id.ibtn_add_position);
        ibtnAddLocation.setOnClickListener(this);
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);// 此方法必须重写
        init();
        initProgressDialog();
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
            aMap.setOnMapClickListener(this);
            aMap.setOnMarkerClickListener(this);
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
        //每次界面显示的时候刷附近厕所
        getWcAddresses();
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
                //Log.i(Constants.LOG_TAG, "定位成功");
                addSensorMaker(aMapLocation);
            } else {
                String errText = "定位失败," + aMapLocation.getErrorCode()+ ": " + aMapLocation.getErrorInfo();
                Log.i(Constants.LOG_TAG,errText);
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
            getWcAddresses();
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
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bMap = BitmapFactory.decodeResource(this.getResources(), R.mipmap.navi_map_gps_locked,options);
        BitmapDescriptor des = BitmapDescriptorFactory.fromBitmap(bMap);

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.icon(des);
        markerOptions.anchor(0.5f, 0.5f);
        markerOptions.position(latlng);
        mLocMarker = aMap.addMarker(markerOptions);
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

    //region View.OnClickListener,AMap.OnMapClickListener,AMap.OnMarkerClickListener
    @Override
    public void onClick(View v) {
        PositionInfo positionInfo = getPositionInfo();
        if(positionInfo == null){return;}
        if (v == ibtnLocation) {
            //回到当前位置
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(positionInfo.getLatLonPoint().getLatitude(), positionInfo.getLatLonPoint().getLongitude()), 18));
        }else if(v == ibtnAddLocation){
            Intent intent = new Intent();
            intent.putExtra("positioninfo",positionInfo);//实现Parcelable接口的对象
            intent.setClass(this, PositionActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        isShowMakerDetail(false);
        returnToOriginalMaker(null);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.getObject() != null) {
            isShowMakerDetail(true);
            try {
                WCLocation obj = (WCLocation) marker.getObject();
                if (selectMarker == null) {
                    selectMarker = marker;
                } else {
                    //将之前被点击的marker置为原来的状态
                    returnToOriginalMaker(marker);
                    selectMarker = marker;
                }
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.marker_pressed)));
                tvTitle.setText(obj.getTitle()+"");
                tvAddress.setText(obj.getAddress());
            } catch (Exception e) {
                Log.e(Constants.LOG_TAG,"error,"+e);
            }
        }else {
            isShowMakerDetail(false);
            returnToOriginalMaker(marker);
        }
        return true;
    }
    //endregion

    //region private methods

    private void returnToOriginalMaker(Marker marker){
        if(selectMarker == null)return;
        if(selectMarker.equals(marker))return;
        WCLocation obj = (WCLocation) selectMarker.getObject();
        if(obj == null)return;
        setMakerIcon(selectMarker,obj.getSex());
    }
    private void setMakerIcon(Marker marker,int sex){
        switch (sex){
            case 0:
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.marker_0)));
                break;
            case 1:
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.marker_1)));
                break;
            case 2:
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.marker_2)));
                break;
        }
    }
    private void isShowMakerDetail(boolean isShow){
        if (isShow) {
            rlMakerDetail.setVisibility(View.VISIBLE);
        } else {
            rlMakerDetail.setVisibility(View.GONE);
        }
    }

    private PositionInfo getPositionInfo(){
        if(currentAMapLocation == null){
            return null;
        }
        PositionInfo positionInfo = new PositionInfo();
        positionInfo.setCityCode(currentAMapLocation.getCityCode());
        positionInfo.setLatLonPoint(new LatLonPoint(currentAMapLocation.getLatitude(), currentAMapLocation.getLongitude()));
        positionInfo.setAddress(currentAMapLocation.getAddress());
        positionInfo.setAddressJoin(currentAMapLocation.getProvince() + "-" + currentAMapLocation.getCity() + "-" + currentAMapLocation.getDistrict() + "-" + currentAMapLocation.getStreet() + "-" + currentAMapLocation.getStreetNum());
        positionInfo.setAoiName(currentAMapLocation.getAoiName());
        return positionInfo;
    }

    private void getWcAddresses(){
        final PositionInfo positionInfo = getPositionInfo();
        if(positionInfo == null){return;}
        //showProgressDialog();
        ThreadUtil.getCachedThreadPool().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    HashMap<String, String> hashMap = positionInfo.toMap();
                    hashMap.put(Constants.REQ_ACTION_KEY,Constants.REQ_ACTION_GET);
                    RequestManager.getInstance(MainActivity.this).requestAsyn(Constants.REQ_API_PHP,TYPE_POST_FORM,hashMap,reqCallBack);
                } catch (Exception e) {
                    Log.e(Constants.LOG_TAG,"error,"+e);
                    dismissProgressDialog();
                }
            }
        });
    }

    ArrayList<WCLocation> listScreen = new ArrayList<WCLocation>();
    private boolean isContain(WCLocation info){
        for (final WCLocation item:listScreen){
            if(info.getLongitude() == item.getLongitude() && info.getLatitude() == item.getLatitude()){
                return true;
            }
        }
        return false;
    }
    private synchronized void setMarkerDetail(RegeocodeAddress result,WCLocation row){
        MarkerOptions markerOps = new MarkerOptions().position(new LatLng(row.getLatitude(), row.getLongitude())).title(result.getFormatAddress());
        StringBuilder sb = new StringBuilder();
        sb.append(result.getStreetNumber().getStreet()+result.getStreetNumber().getNumber());
        sb.append("-"+row.getSexStr());
        sb.append("-"+row.getTypeFeeStr());
        sb.append("-"+row.getContact());
        row.setTitle(sb.toString());
        row.setAddress(result.getFormatAddress());
        Marker marker = aMap.addMarker(markerOps);
        marker.setObject(row);
        setMakerIcon(marker,row.getSex());
    }
    private void addWcMarkers(JsonArray jarr){
        if (geocoderSearch == null) { geocoderSearch = new GeocodeSearch(this); }

        Gson gson = new Gson();
        for(JsonElement item : jarr ){
            final WCLocation row = gson.fromJson( item , WCLocation.class);
            if(isContain(row))continue;

            ThreadUtil.getSingleThreadPool().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        // 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
                        RegeocodeQuery query = new RegeocodeQuery(new LatLonPoint(row.getLatitude(), row.getLongitude()), Constants.GEO_SEARCH_RANGE, GeocodeSearch.AMAP);
                        RegeocodeAddress result = geocoderSearch.getFromLocation(query);// 设置同步逆地理编码请求
                        if (result != null && result.getFormatAddress() != null) {
                            setMarkerDetail(result, row);
                        }
                    } catch (Exception e) {
                        dismissProgressDialog();
                    }
                }
            });
            listScreen.add(row);
        }
    }

    ReqCallBack reqCallBack = new ReqCallBack() {
        @Override
        public void onReqSuccess(Object result) {
            Log.i(Constants.LOG_TAG,"ok,"+result);
            JsonObject json = new JsonParser().parse(result.toString()).getAsJsonObject();
            JsonArray jarr = json.getAsJsonArray("data");
            addWcMarkers(jarr);
            dismissProgressDialog();
        }

        @Override
        public void onReqFailed(String errorMsg) {
            Log.i(Constants.LOG_TAG,"no,"+errorMsg);
            dismissProgressDialog();
        }
    };
    //endregion

    //region ProgressDialog

    private ProgressDialog dialogProgress;

    private void initProgressDialog() {
        if(null == dialogProgress) {
            dialogProgress = new ProgressDialog(this);
            dialogProgress.setCanceledOnTouchOutside(false);
        }
    }
    public void showProgressDialog() {
        if(dialogProgress!=null && !dialogProgress.isShowing()) {
            dialogProgress.setMessage("正在加载...");
            dialogProgress.show();
        }
    }

    public void dismissProgressDialog() {
        if(dialogProgress!=null && dialogProgress.isShowing()){
            dialogProgress.dismiss();
        }
    }
    //endregion
}
