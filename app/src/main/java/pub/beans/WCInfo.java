package pub.beans;

import android.os.Parcel;
import android.os.Parcelable;

import com.amap.api.services.core.LatLonPoint;

import pub.utils.StringUtil;

/**
 * Created by able on 2017/4/6.
 */

public class WCInfo implements Parcelable {

    //region Fields
    /**
     * 性别  0 女，1 男，2 男女
     */
    private int sex = 2;
    /**
     * 联系方式
     */
    private String contact;
    /**
     * 厕所类型，0 公共，1 收费，2 分享
     */
    private int typeFee = 0;
    //endregion

    //region Properties


    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public int getTypeFee() {
        return typeFee;
    }

    public void setTypeFee(int typeFee) {
        this.typeFee = typeFee;
    }

    //endregion


    public WCInfo() {
    }

    //region CREATOR
    protected WCInfo(Parcel in) {
        sex = in.readInt();
        contact = in.readString();
        typeFee = in.readInt();

    }

    public static final Creator<WCInfo> CREATOR = new Creator<WCInfo>() {
        @Override
        public WCInfo createFromParcel(Parcel in) {
            return new WCInfo(in);
        }

        @Override
        public WCInfo[] newArray(int size) {
            return new WCInfo[size];
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
        dest.writeInt(sex);
        dest.writeString(contact);
        dest.writeInt(typeFee);
    }
    //endregion

    //region public method

    public void setSex(String name){
        if(StringUtil.isEmpty(name)){
            return;
        }
        if(name.equals("男女")){
            setSex(2);
        }else if(name.equals("男")){
            setSex(1);
        }else if(name.equals("女")){
            setSex(0);
        }
    }
    public void setTypeFee(String name){
        if(StringUtil.isEmpty(name)){
            return;
        }
        if(name.equals("免费")){
            setSex(0);
        }else if(name.equals("收费")){
            setSex(1);
        }else if(name.equals("分享")){
            setSex(2);
        }
    }
    //endregion
}
