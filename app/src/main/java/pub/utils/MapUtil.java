package pub.utils;

import android.content.Context;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.services.core.LatLonPoint;

/**
 * Created by Fussen on 2016/10/31.
 */

public class MapUtil implements AMapLocationListener {
    private AMapLocationClient locationClient = null;  // 定位
    private AMapLocationClientOption locationOption = null;  // 定位设置

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        mLonLatListener.getLonLat(aMapLocation);
        locationClient.stopLocation();
        locationClient.onDestroy();
        locationClient = null;
        locationOption = null;
    }

    private LonLatListener mLonLatListener;

    public void getLonLat(Context context, LonLatListener lonLatListener) {
        locationClient = new AMapLocationClient(context);
        locationOption = new AMapLocationClientOption();
        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);// 设置定位模式为高精度模式
        locationClient.setLocationListener(this);// 设置定位监听
        locationOption.setOnceLocation(false); // 单次定位 每隔2秒定位一次
        locationOption.setNeedAddress(true);//返回地址信息
        mLonLatListener = lonLatListener;//接口
        locationClient.setLocationOption(locationOption);// 设置定位参数
        locationClient.startLocation(); // 启动定位
    }

    public interface LonLatListener {
        void getLonLat(AMapLocation aMapLocation);
    }


    /**
     * 把LatLng对象转化为LatLonPoint对象
     */
    public static LatLonPoint convertToLatLonPoint(LatLng latlon) {
        return new LatLonPoint(latlon.latitude, latlon.longitude);
    }

    /**
     * 把LatLonPoint对象转化为LatLon对象
     */
    public static LatLng convertToLatLng(LatLonPoint latLonPoint) {
        return new LatLng(latLonPoint.getLatitude(), latLonPoint.getLongitude());
    }
}
