package pub.wc;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.CameraPosition;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.core.SuggestionCity;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;

import java.util.ArrayList;
import java.util.List;

import pub.adapters.AddressAdapter;
import pub.beans.PositionInfo;
import pub.beans.SearchAddressInfo;
import pub.utils.Constants;
import pub.utils.ToastUtil;

import static pub.utils.MapUtil.convertToLatLng;
import static pub.utils.MapUtil.convertToLatLonPoint;

public class SelectActivity extends AppCompatActivity implements PoiSearch.OnPoiSearchListener,GeocodeSearch.OnGeocodeSearchListener,AMap.OnCameraChangeListener,AMap.OnMapClickListener,AdapterView.OnItemClickListener, View.OnClickListener {

    //extra
    private PositionInfo positionInfo;

    //ui ele
    private MapView mapView;
    private ListView lvAddress;
    private ImageView ivCenterMaker;
    private ImageButton ibtnPosition;
    private ImageView ivSearch;
    private TextView tvOk;
    private ImageView ivBack;

    //list
    private ArrayList<SearchAddressInfo> addressList = new ArrayList<>();
    private AddressAdapter addressAdapter;

    //anim
    private Animation animCenterPositionMaker;

    //map
    private AMap aMap;
    private LatLng mFinalChoosePosition;
    //map camera
    //处理拖拽地图和点击地址列表某个地址。
    private boolean isHandDrag = true;
    //第一次加载
    private boolean isFirstLoad = true;
    //暂时没用
    private boolean isBackFromSearch = false;

    //map geo_search
    private GeocodeSearch geocoderSearch;
    public SearchAddressInfo mAddressInfoFirst = null;

    //map poi_search
    private int currentPage = 0;// 当前页面，从0开始计数
    private PoiSearch.Query query;// Poi查询条件类
    private PoiSearch poiSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);

        //extra
        positionInfo = (PositionInfo)getIntent().getParcelableExtra("positioninfo");

        //ui ele
        mapView = (MapView) findViewById(R.id.mapview);
        lvAddress = (ListView) findViewById(R.id.lv_address);
        ivCenterMaker = (ImageView) findViewById(R.id.iv_center_maker);
        ibtnPosition = (ImageButton) findViewById(R.id.ibtn_position);
        ivSearch = (ImageView) findViewById(R.id.iv_search);
        tvOk = (TextView) findViewById(R.id.tv_ok);
        ivBack = (ImageView) findViewById(R.id.iv_back);

        ivSearch.setOnClickListener(this);
        tvOk.setOnClickListener(this);
        ivBack.setOnClickListener(this);
        ibtnPosition.setOnClickListener(this);

        mapView.onCreate(savedInstanceState);
        //anim
        animCenterPositionMaker = AnimationUtils.loadAnimation(this, R.anim.center_position_maker);

        //list
        addressAdapter = new AddressAdapter(this, addressList);
        lvAddress.setAdapter(addressAdapter);
        lvAddress.setOnItemClickListener(this);
        //
        initMap();
    }

    //region Activity Life

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }
    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        mapView.onSaveInstanceState(outState);
    }
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }
    //endregion

    private void initMap() {

        if (aMap == null) {
            aMap = mapView.getMap();

            //地图比例尺的开启
            aMap.getUiSettings().setScaleControlsEnabled(true);

            //关闭地图缩放按钮 就是那个加号 和减号
            aMap.getUiSettings().setZoomControlsEnabled(false);

            //暂时没用
            aMap.setOnMapClickListener(this);

            //对amap添加移动地图事件监听器
            aMap.setOnCameraChangeListener(this);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 3;//缩小1/3
            Marker markerCurrent = aMap.addMarker(new MarkerOptions()
                    .anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.marker_current,options)))
                    .position(convertToLatLng(positionInfo.getLatLonPoint())));

            //拿到地图中心的经纬度
            mFinalChoosePosition = markerCurrent.getPosition();

            geocoderSearch = new GeocodeSearch(getApplicationContext());
            //设置逆地理编码监听
            geocoderSearch.setOnGeocodeSearchListener(this);
        }
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(convertToLatLng(positionInfo.getLatLonPoint()), Constants.ZOOM_LEVEL));
    }

    //region implements AMap.OnCameraChangeListener
    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

    }

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        //每次移动结束后地图中心的经纬度
        mFinalChoosePosition = cameraPosition.target;
        ivCenterMaker.startAnimation(animCenterPositionMaker);
        if (isHandDrag || isFirstLoad) {//手动去拖动地图

            // 开始进行poi搜索
            getAddressFromLonLat(cameraPosition.target);
            doSearchQueryByPosition();

        } else if (isBackFromSearch) {
            //搜索地址返回后 拿到选择的位置信息继续搜索附近的兴趣点
            isBackFromSearch = false;
            doSearchQueryByPosition();
        } else {
            addressAdapter.notifyDataSetChanged();
        }
        isHandDrag = true;
        isFirstLoad = false;
    }
    /**
     * 开始进行poi搜索
     * 通过经纬度获取附近的poi信息
     * <p>
     * 1、keyword 传 ""
     * 2、poiSearch.setBound(new PoiSearch.SearchBound(lpTemp, 5000, true)); 根据
     */
    protected void doSearchQueryByPosition() {

        currentPage = 0;
        query = new PoiSearch.Query("", "", positionInfo.getCityCode());// 第一个参数表示搜索字符串，第二个参数表示poi搜索类型，第三个参数表示poi搜索区域（空字符串代表全国）
        query.setPageSize(20);// 设置每页最多返回多少条poiitem
        query.setPageNum(currentPage);// 设置查第一页

        LatLonPoint latLonPoint = convertToLatLonPoint(mFinalChoosePosition);

        if (latLonPoint != null) {
            poiSearch = new PoiSearch(this, query);
            poiSearch.setOnPoiSearchListener(this);  // 实现  onPoiSearched  和  onPoiItemSearched
            poiSearch.setBound(new PoiSearch.SearchBound(latLonPoint, Constants.POI_SEARCH_RANGE, true));//
            // 设置搜索区域为以lpTemp点为圆心，其周围5000米范围
            poiSearch.searchPOIAsyn();// 异步搜索,触发回调方法
        }
    }
    /**
     * 根据经纬度得到地址
     */
    public void getAddressFromLonLat(final LatLng latLonPoint) {
        // 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
        RegeocodeQuery query = new RegeocodeQuery(convertToLatLonPoint(latLonPoint), Constants.GEO_SEARCH_RANGE, GeocodeSearch.AMAP);
        geocoderSearch.getFromLocationAsyn(query);// 设置同步逆地理编码请求,然后触发回调方法。
    }
    //endregion

    //region implements AMap.OnMapClickListener
    @Override
    public void onMapClick(LatLng latLng) {
        Toast.makeText(this, "latitude" + String.valueOf(latLng.latitude), Toast.LENGTH_SHORT).show();
    }
    //endregion

    //region implements View.OnClickListener
    @Override
    public void onClick(View v) {
        if (v == ibtnPosition) {
            //回到当前位置
            aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(convertToLatLng(positionInfo.getLatLonPoint()), Constants.ZOOM_LEVEL));
        } else if (v == ivBack) {
            finish();
        } else if (v == ivSearch) {

            //Intent intent = new Intent(this, SearchAddressActivity.class);
            //intent.putExtra("maker_center", mFinalChoosePosition);
            //intent.putExtra("city", city);
            //startActivityForResult(intent, SEARCH_ADDDRESS);
            //isBackFromSearch = false;

        } else if (v == tvOk) {

            sendLocation();

        }
    }
    private void sendLocation() {
        SearchAddressInfo addressInfo = null;
        for (SearchAddressInfo info : addressList) {
            if (info.isChoose) {
                addressInfo = info;
            }
        }
        if (addressInfo != null) {
            Intent intent = new Intent();
            intent.putExtra("address", addressInfo);
            setResult(Constants.REQ_CODE_POSITION, intent);
            finish();
        } else {
            ToastUtil.show(this, "请选择地址");
        }
    }

    //endregion

    //region implements AdapterView.OnItemClickListener
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mFinalChoosePosition = convertToLatLng(addressList.get(position).latLonPoint);
        for (int i = 0; i < addressList.size(); i++) {
            addressList.get(i).isChoose = false;
        }
        addressList.get(position).isChoose = true;

        isHandDrag = false;

        // 点击之后，改变了地图中心位置， onCameraChangeFinish 也会调用
        // 只要地图发生改变，就会调用 onCameraChangeFinish ，不是说非要手动拖动屏幕才会调用该方法
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mFinalChoosePosition.latitude, mFinalChoosePosition.longitude), Constants.ZOOM_LEVEL));

    }

    //endregion

    //region implements GeocodeSearch.OnGeocodeSearchListener
    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
        if (i == 1000) {//转换成功
            if (regeocodeResult != null && regeocodeResult.getRegeocodeAddress() != null && regeocodeResult.getRegeocodeAddress().getFormatAddress() != null) {
                //拿到详细地址
                String addressName = regeocodeResult.getRegeocodeAddress().getFormatAddress(); // 逆转地里编码不是每次都可以得到对应地图上的opi

                //条目中第一个地址 也就是当前你所在的地址
                mAddressInfoFirst = new SearchAddressInfo(addressName, addressName, false, convertToLatLonPoint(mFinalChoosePosition));

                //其实也是可以在这就能拿到附近的兴趣点的

            } else {
                ToastUtil.show(this, "没有搜到");
            }
        } else {
            ToastUtil.showerror(this, i);
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }

    //endregion

    //region implements PoiSearch.OnPoiSearchListener

    /**
     * poi没有搜索到数据，返回一些推荐城市的信息
     */
    private void showSuggestCity(List<SuggestionCity> cities) {
        String infomation = "推荐城市\n";
        for (int i = 0; i < cities.size(); i++) {
            infomation += "城市名称:" + cities.get(i).getCityName() + "城市区号:"
                    + cities.get(i).getCityCode() + "城市编码:"
                    + cities.get(i).getAdCode() + "\n";
        }
        ToastUtil.show(this, infomation);
    }
    @Override
    public void onPoiSearched(PoiResult poiResult, int rcode) {
        if (rcode == 1000) {
            if (poiResult != null && poiResult.getQuery() != null) {// 搜索poi的结果
                if (poiResult.getQuery().equals(query)) {// 是否是同一条
                    List<PoiItem> poiItems = poiResult.getPois();// 取得第一页的poiitem数据，页数从数字0开始

                    List<SuggestionCity> suggestionCities = poiResult.getSearchSuggestionCitys();// 当搜索不到poiitem数据时，会返回含有搜索关键字的城市信息

                    //搜索到数据
                    if (poiItems != null && poiItems.size() > 0) {
                        addressList.clear();
                        //先将 逆地理编码过的当前地址 也就是条目中第一个地址 放到集合中
                        addressList.add(mAddressInfoFirst);
                        SearchAddressInfo addressInfo = null;
                        for (PoiItem poiItem : poiItems) {
                            addressInfo = new SearchAddressInfo(poiItem.getTitle(), poiItem.getSnippet(), false, poiItem.getLatLonPoint());
                            addressList.add(addressInfo);
                        }
                        if (isHandDrag) {
                            addressList.get(0).isChoose = true;
                        }
                        addressAdapter.notifyDataSetChanged();

                    } else if (suggestionCities != null && suggestionCities.size() > 0) {
                        showSuggestCity(suggestionCities);
                    } else {
                        ToastUtil.show(SelectActivity.this, "对不起，没有搜索到相关数据");
                    }
                }
            } else {
                Toast.makeText(this, "对不起，没有搜索到相关数据！", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }
    //endregion
}
