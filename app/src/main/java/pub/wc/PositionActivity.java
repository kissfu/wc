package pub.wc;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pub.beans.PositionInfo;
import pub.beans.SearchAddressInfo;
import pub.beans.WCInfo;
import pub.managers.ReqCallBack;
import pub.managers.RequestManager;
import pub.utils.Constants;
import pub.utils.MapUtil;
import pub.utils.ThreadUtil;
import pub.utils.ToastUtil;

import static pub.managers.RequestManager.TYPE_POST_FORM;
import static pub.managers.RequestManager.TYPE_POST_JSON;

public class PositionActivity extends AppCompatActivity  implements View.OnClickListener  {

    private Button btnSelect;
    private TextView tvAddress;
    private TextView tvAddressJoin;
    private TextView tvPoi;
    private TextView tvLon;
    private TextView tvLat;
    RadioGroup rgSex;
    RadioGroup rgTypeFee;
    EditText etRemark;
    EditText etContact;
    private Button btnAdd;

    private PositionInfo positionInfo;
    private GeocodeSearch geocoderSearch;
    private WCInfo wcInfo = new WCInfo();
    private ProgressDialog dialogProgress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_position);

        positionInfo = (PositionInfo)getIntent().getParcelableExtra("positioninfo");
        positionInfo.setWcInfo(wcInfo);
        btnSelect = (Button) findViewById(R.id.btn_select);
        btnSelect.setOnClickListener(this);
        tvAddress = (TextView) findViewById(R.id.tv_address);
        tvAddressJoin = (TextView) findViewById(R.id.tv_address_join);
        tvPoi = (TextView) findViewById(R.id.tv_poi);
        tvLon = (TextView) findViewById(R.id.tv_lon);
        tvLat = (TextView) findViewById(R.id.tv_lat);
        rgSex = (RadioGroup)this.findViewById(R.id.rg_sex);
        rgSex.setOnCheckedChangeListener(rgListener);
        rgTypeFee = (RadioGroup)this.findViewById(R.id.rg_typeFee);
        rgTypeFee.setOnCheckedChangeListener(rgListener);
        etContact = (EditText) findViewById(R.id.et_contact);
        etRemark = (EditText) findViewById(R.id.et_remark);
        btnAdd = (Button) findViewById(R.id.btn_add);
        btnAdd.setOnClickListener(this);

        getAddresses();
        init();
    }
    //region ProgressDialog
    private void init() {
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

    //region RadioGroup.OnCheckedChangeListener
    RadioGroup.OnCheckedChangeListener rgListener = new RadioGroup.OnCheckedChangeListener() {
         @Override
         public void onCheckedChanged(RadioGroup arg0, int arg1) {
            //获取变更后的选中项的ID
             int radioButtonId = arg0.getCheckedRadioButtonId();
            //根据ID获取RadioButton的实例
            RadioButton rb = (RadioButton)PositionActivity.this.findViewById(radioButtonId);
            //更新文本内容，以符合选中项
             if(arg0 == rgSex){
                 wcInfo.setSex(rb.getText().toString());
             }else if(arg0 == rgTypeFee){
                 wcInfo.setTypeFee(rb.getText().toString());
             }

         }
    };
    //endregion

    //region Handler Message
    private Handler msgHandler = new Handler(){
        public void handleMessage(Message msg) {
            if(msg.what == 1){
                initData();
            }else{
                ToastUtil.showerror(PositionActivity.this, msg.arg1);
            }
            dismissProgressDialog();
        }
    };
    //endregion

    //region implements View.OnClickListener

    @Override
    public void onClick(View v) {
        if (v == btnSelect) {
            Intent intent = new Intent();
            intent.putExtra("positioninfo",positionInfo);//实现Parcelable接口的对象
            intent.setClass(this, SelectActivity.class);
            startActivityForResult(intent, Constants.REQ_CODE_POSITION);
            //startActivity(intent);
        }else if(v == btnAdd){
            wcInfo.setContact(etContact.getText().toString());
            positionInfo.setRemark(etRemark.getText().toString());
            addAddressToServer();
        }
    }
    //endregion

    //region override Activity

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQ_CODE_POSITION && resultCode == Constants.REQ_CODE_POSITION) {
            SearchAddressInfo info = (SearchAddressInfo) data.getParcelableExtra("address");
            positionInfo.setLatLonPoint(info.latLonPoint);
            getAddresses();
        }
    }
    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(PositionActivity.this);
        builder.setTitle("返回确认")
                .setMessage("当前填写信息可能会被替换！")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {ToastUtil.show(PositionActivity.this,"已经取消");}})
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {PositionActivity.super.onBackPressed();}
                });
        builder.show();
    }

    //endregion

    //region private Methods

    /**
     * add position to server
     */
    private void addAddressToServer() {
        showProgressDialog();
        ThreadUtil.getCachedThreadPool().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    RequestManager.getInstance(PositionActivity.this).requestAsyn("index.php",TYPE_POST_FORM,positionInfo.toMap(),reqCallBack);
                } catch (Exception e) {
                    Message msg = msgHandler.obtainMessage();
                    msg.what = 0;
                    msgHandler.sendMessage(msg);
                }
            }
        });
    }

    ReqCallBack reqCallBack = new ReqCallBack() {
        @Override
        public void onReqSuccess(Object result) {
            Log.i(Constants.LOG_TAG,"ok,"+result);
            dismissProgressDialog();
        }

        @Override
        public void onReqFailed(String errorMsg) {
            Log.i(Constants.LOG_TAG,"no,"+errorMsg);
            dismissProgressDialog();
        }
    };

    /**
     * 响应逆地理编码的批量请求
     */
    private void getAddresses() {
        if (geocoderSearch == null) {
            geocoderSearch = new GeocodeSearch(this);
        }
        showProgressDialog();
        ThreadUtil.getCachedThreadPool().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    // 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
                    RegeocodeQuery query = new RegeocodeQuery(positionInfo.getLatLonPoint(), Constants.GEO_SEARCH_RANGE, GeocodeSearch.AMAP);
                    RegeocodeAddress result = geocoderSearch.getFromLocation(query);// 设置同步逆地理编码请求
                    if (result != null && result.getFormatAddress() != null) {
                        Message msg = msgHandler.obtainMessage();
                        msg.what = 1;
                        positionInfo.setCityCode(result.getCityCode());
                        //positionInfo.getLatLonPoint()不改变
                        positionInfo.setAddress(result.getFormatAddress());
                        positionInfo.setAddressJoin(result.getProvince() + "-" + result.getCity() + "-" + result.getDistrict() + "-" + result.getStreetNumber().getStreet()+ "-" + result.getStreetNumber().getNumber());
                        if(result.getAois().size()>0) {
                            positionInfo.setAoiName(result.getAois().get(0).getAoiName());
                        }
                        msgHandler.sendMessage(msg);
                    }
                } catch (AMapException e) {
                    Message msg = msgHandler.obtainMessage();
                    msg.what = 0;
                    msg.arg1 = e.getErrorCode();
                    msgHandler.sendMessage(msg);
                }
            }
        });
    }

    private void initData(){
        try{
            Log.i(Constants.LOG_TAG,"获取地址信息。");
            tvAddress.setText("当前地址：" + positionInfo.getAddress());
            tvAddressJoin.setText("当前城市：" + positionInfo.getAddressJoin());
            tvPoi.setText("当前位置："+positionInfo.getAoiName());
            tvLon.setText("当前经度：" + positionInfo.getLatLonPoint().getLongitude());
            tvLat.setText("当前纬度：" + positionInfo.getLatLonPoint().getLatitude());
        }catch (Exception e){
            Log.i(Constants.LOG_TAG,"获取地址信息。"+e.getMessage());
        }

    }

    //endregion
}
