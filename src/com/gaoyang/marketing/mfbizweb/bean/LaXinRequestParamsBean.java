package com.gaoyang.marketing.mfbizweb.bean;

import java.io.Serializable;

/**
 * Created by zhanghui on 2018-12-3.
 * @desc 拉新前端请求web端参数实体
 */
public class LaXinRequestParamsBean implements Serializable {

    private static final long serialVersionUID = 2933375574213303647L;
    //标识拉新渠道 对应sourceId
    private String tag;
    //支付宝uid
    private String userId;
    //卡券id
    private String voucherId;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(String voucherId) {
        this.voucherId = voucherId;
    }
}
