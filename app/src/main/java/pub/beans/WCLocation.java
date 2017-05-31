package pub.beans;

/**
 * Created by able on 2017/5/27.
 */

public class WCLocation {
    private double longitude;
    private double latitude;
    /**
     * 厕所类型，0 公共，1 收费，2 分享
     */
    private int type_fee;
    /**
     * 性别  0 女，1 男，2 男女
     */
    private int sex;
    private String contact;
    private String remark;
    //米
    private double distance;

    private String title;
    private String address;

    //region getter setter
    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public int getType_fee() {
        return type_fee;
    }

    public void setType_fee(int type_fee) {
        this.type_fee = type_fee;
    }

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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    //endregion

    public String getSexStr(){
        String str = "";
        switch (getSex()){
            case 0:
                str = "女";
                break;
            case 1:
                str = "男";
                break;
            case 2:
                str = "男女";
                break;
        }
        return str;
    }
    public String getTypeFeeStr(){
        String str = "";
        switch (getType_fee()){
            case 0:
                str = "免费";
                break;
            case 1:
                str = "收费";
                break;
            case 2:
                str = "分享";
                break;
        }
        return str;
    }
}
