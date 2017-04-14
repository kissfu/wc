package pub.beans;

import android.os.Parcel;
import android.os.Parcelable;

import com.amap.api.services.core.LatLonPoint;

/**
 * Created by able on 2017/4/6.
 */

public class PositionInfo  implements Parcelable {

    //region Fields
    private String cityCode;
    private String address;
    private String addressJoin;
    private  String aoiName;
    private LatLonPoint latLonPoint;
    /**
     * 摘要；附注;注意
     */
    private String remark;
    private WCInfo wcInfo;
    //endregion

    //region Properties

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddressJoin() {
        return addressJoin;
    }

    public void setAddressJoin(String addressJoin) {
        this.addressJoin = addressJoin;
    }

    public String getAoiName() {
        return aoiName;
    }

    public void setAoiName(String aoiName) {
        this.aoiName = aoiName;
    }

    public LatLonPoint getLatLonPoint() {
        return latLonPoint;
    }

    public void setLatLonPoint(LatLonPoint latLonPoint) {
        this.latLonPoint = latLonPoint;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public WCInfo getWcInfo() {
        return wcInfo;
    }

    public void setWcInfo(WCInfo wcInfo) {
        this.wcInfo = wcInfo;
    }

    //endregion


    public PositionInfo() {
    }

    //region CREATOR
    protected PositionInfo(Parcel in) {

        cityCode = in.readString();
        address = in.readString();
        addressJoin = in.readString();
        aoiName = in.readString();
        latLonPoint = in.readParcelable(LatLonPoint.class.getClassLoader());
        remark = in.readString();
        wcInfo = in.readParcelable(WCInfo.class.getClassLoader());
    }

    public static final Creator<PositionInfo> CREATOR = new Creator<PositionInfo>() {
        @Override
        public PositionInfo createFromParcel(Parcel in) {
            return new PositionInfo(in);
        }

        @Override
        public PositionInfo[] newArray(int size) {
            return new PositionInfo[size];
        }
    };
    //endregion

    //region implements Parcelable
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(cityCode);
        dest.writeString(address);
        dest.writeString(addressJoin);
        dest.writeString(aoiName);
        dest.writeParcelable(latLonPoint,flags);
        dest.writeString(remark);
        dest.writeParcelable(wcInfo,flags);
    }
    //endregion
}
