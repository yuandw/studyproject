package com.gaoyang.marketing.mfbizweb.bean;/**
 * Created by zhanghui on 2018-9-21.
 */

import java.io.Serializable;

/**
 * @author zhanghui
 * @create 2018-9-21
 * @description
 */
public class UserInfoBean implements Serializable {
    private static final long serialVersionUID = 5753729441786940225L;
    //账户余额
    private String balance;
    //用户本月兑换过的次数
    private String tradeTimesCur;
    //用户本月可以兑换的总次数 (固定为10次)
    private String tradeTimesTotal;
    //用户本月兑换过的流量
    private String tradeAmountCur;
    //用户本月可以兑换的总流量额度(固定为10G)
    private String tradeAmountTotal;
    //会员卡号
    private String mciGyId;
    //脱敏电话号码
    private String phone;
    //非脱敏电话号码
    private String realIphone;
    //与前端交互token
    private String userToken;
    //开卡赠送流量值
    private String sendFlow;
    //开卡时间
    private String openDate;

    public String getOpenDate() {
        return openDate;
    }

    public void setOpenDate(String openDate) {
        this.openDate = openDate;
    }

    public String getSendFlow() {
        return sendFlow;
    }

    public void setSendFlow(String sendFlow) {
        this.sendFlow = sendFlow;
    }

    public String getRealIphone() {
        return realIphone;
    }
    public void setRealIphone(String realIphone) {
        this.realIphone = realIphone;
    }
    public String getBalance() {
        return balance;
    }

    public void setBalance(String balance) {
        this.balance = balance;
    }

    public String getUserToken() {
        return userToken;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMciGyId() {
        return mciGyId;
    }

    public void setMciGyId(String mciGyId) {
        this.mciGyId = mciGyId;
    }

    public String getTradeAmountTotal() {
        return tradeAmountTotal;
    }

    public void setTradeAmountTotal(String tradeAmountTotal) {
        this.tradeAmountTotal = tradeAmountTotal;
    }

    public String getTradeAmountCur() {
        return tradeAmountCur;
    }

    public void setTradeAmountCur(String tradeAmountCur) {
        this.tradeAmountCur = tradeAmountCur;
    }

    public String getTradeTimesTotal() {
        return tradeTimesTotal;
    }

    public void setTradeTimesTotal(String tradeTimesTotal) {
        this.tradeTimesTotal = tradeTimesTotal;
    }

    public String getTradeTimesCur() {
        return tradeTimesCur;
    }

    public void setTradeTimesCur(String tradeTimesCur) {
        this.tradeTimesCur = tradeTimesCur;
    }


}
