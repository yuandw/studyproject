package com.gaoyang.marketing.mfbizweb.util;/**
 * Created by zhanghui on 2018-10-8.
 */

/**
 * @author zhanghui
 * @create 2018-10-8
 * @description
 */
public enum ResponseEnums {

    //返回给前端信息
    TYPE_SUCCESS("0000","成功") ,
    TYPE_USERINFO_ISNOTEXIST("0998","用户信息不存在"),
    TYPE_USERINFO_EXCEPYION("0999","用户信息异常"),
    TYPE_SYSTEMBUSY_EXCEPYION("0999","系统繁忙"),
    TYPE_ACCOUNTBALANCE_FAIL("0999","查询失败"),
    TYPE_INTERFACE_EXCEPYION("0999","接口调用异常"),
    TYPE_FLOWCHARGE_SUCCESS("2","充值中，请耐心等待"),
    TYPE_FLOWCHARGE10G_FAIL("301","请重新发起兑换"),
    TYPE_FLOWCHARGENOTENOUGH_FAIL("302","请重新发起兑换"),
    TYPE_FLOWCHARGE_FAIL("3","请重新发起兑换"),
    TYPE_FLOWCHARGEFLAG_CLOSE("4","兑换服务已关闭"),
    TYPE_CREATEGYMEMEBER_FAIL("0999","领取失败"),

    TYPE_CREATEGYMEMEBERNOTMOBLIE_FAIL("0997","非移动用户"),
    TYPE_CREATEGYMEMEBERNOTPERSONAL_FAIL("0995","公司类账户"),
    TYPE_CREATEGYMEMEBERFREEZE_FAIL("0996","被冻结用户"),
    TYPE_CREATEGYMEMEBER_NOT_PHONE_FAIL("0994","支付宝未绑定手机号"),


    TYPE_APPOINTMENT_MORE_FAIL("21608","已预约"),
    TYPE_IS_MEMBER_FAIL("21609","已经是高阳会员"),
    TYPE_APPOINTMENT_OUT_TIME("21610","超出预约有效时间"),

    //联名卡新人大礼包需求
    TYPE_RES_SUCCESS("10000","success") ,
    TYPE_FAIL("20000","FAIL") ,
    //验签失败
    TYPE_CHECK_SIGN_FAIL("20000","fail") ,

    //非会员失败
    TYPE_NOT_MEMBER_FAIL("20001","fail") ,
    //重复记账
    TYPE_CHARGE_ACCOUNT_MORE("20002","fail") ,
   //特殊记账失败
    TYPE_CHARGE_ACCOUNT_FAIL("20003","fail") ,





    //RPC接口返回相关type

    RPC_TYPE_SUCCESS("10000","成功") ,
    RPC_TYPE_FAIL("20000","成功") ,
    //账户数据平台返回
    RPC_TYPE_QUERYACCOUNTINFO_FAIL("40007","账户不存在") ,
    RPC_TYPE_PRODUCT_FAIL("40009","货架不存在") ,
    RPC_TYPE_TRADE_CREATE_FAIL("40010","交易创建失败") ,
    //核心兑换返回
    RPC_TYPE_ACC_QUERY_FAIL("40011","账户不存在") ,
    RPC_ACC_BALANCE_NOT_ENG("40012","账户余额不足") ,
    RPC_TYPE_EXCHANGE_OUT_LIMIT("40013","账户兑换超限") ,
    RPC_TYPE_CONVERTFLOW_FAIL("40004","系统繁忙，请稍后再试"),
    // 预约卡开返回
    RPC_TYPE_GET_UID_ILLEGAL("21607","获取UID失败"),
    RPC_TYPE_APPOINTMENT_MORE("21608","已预约"),
    RPC_TYPE_IS_MEMBER("21609","已经是会员卡用户"),
    //联名卡新人大礼包
    RPC_TYPE_QUERYACCBYCONDITION_ISNULL("40007","查询成功结果集为空") ,

    ;



    ResponseEnums(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    private String code;
    private String desc;

    public String getCode() {
        return code;
    }



    public String getDesc() {
        return desc;
    }


    @Override
    public String toString() {
        return super.toString();
    }
}
