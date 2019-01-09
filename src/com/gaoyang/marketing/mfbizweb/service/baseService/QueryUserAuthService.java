package com.gaoyang.marketing.mfbizweb.service.baseService;/**
 * Created by zhanghui on 2018-9-20.
 */

import com.alibaba.dubbo.rpc.proxy.TraceIdUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gaoyang.marketing.mfbizweb.bean.BusiMonitorLoggerBean;
import com.gaoyang.marketing.mfbizweb.util.LogExceptionWapper;
import com.gaoyang.marketing.mfbizweb.util.PublicUtil;
import com.gaoyang.marketing.mfbizweb.util.ResponseEnums;
import com.gaoyang.marketing.muserbasecore.userauth.facade.UserAuthFacade;
import com.gaoyang.marketing.muserbasecore.userauth.facade.model.AlipaySystemOauthTokenModel;
import com.gaoyang.marketing.muserbasecore.userinfo.facade.UserInfoFacade;
import com.gaoyang.marketing.muserbasecore.userinfo.facade.model.ResponseData;
import com.gaoyang.marketing.muserbasecore.userinfo.facade.model.response.UserInfoModel;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author gongzelin
 * @create 2018-9-20
 * @description获取用户授权信息service 用户进入小程序页面初始化获取用户信息service
 */
@Service("queryUserAuthService")
public class QueryUserAuthService {
    public final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Resource(name="userAuthFacade")
    UserAuthFacade userAuthFacade;

    @Resource(name="userInfoFacade")
    UserInfoFacade userInfoFacade;

    /**
     * @desc 调用用户授权平台和用户数据平台接口 　完成用户授权
     * @param authCode
     * @return Map
     */
    public Map getUserAuth(String authCode,boolean isCteateMember,String sourceId){
        String traceId= TraceIdUtil.getTraceId();
        Map<String,String> retMap=new HashMap<String,String>();
        //判断接口返回code
        try{
            if(PublicUtil.isEmpty(authCode)){
                logger.warn("调用获取用户授权信息service，authCode为空，traceId={}",traceId);
                return null;
            }
            AlipaySystemOauthTokenModel alipaySystemOauthTokenModel=getAccessToken(authCode);
            if(PublicUtil.isEmpty(alipaySystemOauthTokenModel)){
                logger.warn("调用用户授权平台接口getAlipayAuthToken返回对象为空,traceId={},authCode={}",traceId,authCode);
                return null;
            }
            if(PublicUtil.isEmpty(alipaySystemOauthTokenModel.getZfbUid())||PublicUtil.isEmpty(alipaySystemOauthTokenModel.getAccessToken())){
                logger.warn("调用用户授权平台接口getAlipayAuthToken返回zfbUid或accessToken为空" +
                        ",traceId={},authCode={}",traceId,authCode);
                return null;
            }
            retMap.put("uid",alipaySystemOauthTokenModel.getZfbUid());
            //针对是否创建会员卡的authcode 区别存放token
            if(isCteateMember){
                retMap.put("accessTokenForCreateMember",alipaySystemOauthTokenModel.getAccessToken());
                retMap.put("accessToken",alipaySystemOauthTokenModel.getAccessToken());
            }else{
                retMap.put("accessToken",alipaySystemOauthTokenModel.getAccessToken());
            }
            //20181017 增加sourceId 用户来源
            UserInfoModel userInfoModel=getUserInfo(alipaySystemOauthTokenModel.getZfbUid(),sourceId);
            if(PublicUtil.isEmpty(userInfoModel)){
                logger.warn("调用用户授权平台接口queryUserInfo返回对象为空,traceId={},uid={}",traceId,alipaySystemOauthTokenModel.getZfbUid());
                return null;
            }
            if(PublicUtil.isEmpty(userInfoModel.getUserId())){
                logger.warn("调用用户授权平台接口queryUserInfo返回userId为空,traceId={},uid={}",traceId,alipaySystemOauthTokenModel.getZfbUid());
                return null;
            }
            retMap.put("userId",userInfoModel.getUserId());
            //新增预约标识  计入缓存
            retMap.put("bookFlag",userInfoModel.getRegisterStatus());
            //预约时间计入缓存
            retMap.put("bookDate",userInfoModel.getReserveTime());
            //脱敏的手机号 和原手机号都记缓存
            //手机号-1时不展示
            String iphone="";
            if(!PublicUtil.isEmpty(userInfoModel.getUserBindPhone())&&(userInfoModel.getUserBindPhone().length()==11)){
                iphone=PublicUtil.getSercetiphone(userInfoModel.getUserBindPhone());
                retMap.put("realIphone",userInfoModel.getUserBindPhone());
            }
            retMap.put("iphone",iphone);
            String gyMemberId="";
            String openDate=userInfoModel.getOpendate();
            retMap.put("openDate",openDate);
            //会员卡号为-1不记缓存
            if(PublicUtil.isEmpty(userInfoModel.getGyMemberId())||"-1".equals(userInfoModel.getGyMemberId())){
                retMap.put("gyMemberId",gyMemberId);
            }else{
                retMap.put("gyMemberId",userInfoModel.getGyMemberId());
            }
            if (logger.isInfoEnabled()) {
                logger.info("调用用户授权平台和用户数据平台接口getAlipayAuthToken、getAlipayUserInfoShare查询成功！traceId={},authCode={},sourceId={}", traceId, authCode, sourceId);
                logger.info("记录缓存中的用户信息,traceId={},map={}", traceId,JSONObject.toJSONString(retMap));
            }
            return retMap;
        } catch(Exception e){
            logger.error("traceId={},authCode={},获取用户信息异常e={}",
                    traceId,authCode, LogExceptionWapper.getStackTrace(e));
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @desc 调用用户授权平台接口  获取支付宝accessToken uid
     * @param authCode
     * @return
     */
    public AlipaySystemOauthTokenModel getAccessToken(String authCode){
        String traceId= TraceIdUtil.getTraceId();
        if (logger.isInfoEnabled()) {
            logger.info("调用用户授权平台接口getAlipayAuthToken,traceId={},authCode={}", traceId, authCode);
        }
        try {
            ResponseData<AlipaySystemOauthTokenModel> responseData=userAuthFacade.getAlipayAuthToken(authCode);

            //判断接口返回code
            if(PublicUtil.isEmpty(responseData)){
                logger.warn("调用用户授权平台接口getAlipayAuthToken查询失败，返回responseData为空,traceId={},authCode={}" ,traceId, authCode);
                return null;
            }
            if(!ResponseEnums.RPC_TYPE_SUCCESS.getCode().equals(responseData.getCode())){
                logger.warn("调用用户授权平台接口getAlipayAuthToken查询失败,traceId={},authCode={},responseData={},code={}" ,traceId, authCode, JSONArray.toJSONString(responseData),responseData.getCode());
                return null;
            }
            AlipaySystemOauthTokenModel alipaySystemOauthTokenModel=responseData.getObj();
            logger.info("调用用户授权平台接口getAlipayAuthToken查询成功,traceId={},authCode={},uid={},code={},responseData={}",
                    traceId, authCode,alipaySystemOauthTokenModel.getZfbUid(),responseData.getCode() , JSONArray.toJSONString(responseData));
            return alipaySystemOauthTokenModel;
        } catch (Exception e) {

            logger.error("traceId={},authCode={},调用用户授权平台接口getAlipayAuthToken异常e={}",
                    traceId,authCode, LogExceptionWapper.getStackTrace(e));
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @desc 调用用户数据平台接口  获取用户信息
     * @param uid
     * @return
     */
    public UserInfoModel getUserInfo(String uid,String sourceId){
        String traceId= TraceIdUtil.getTraceId();
        if (logger.isInfoEnabled()) {
            logger.info("调用用户数据平台接口queryUserInfo,traceId={},uid={},sourceId={}", traceId, uid, sourceId);
        }
        try {
            ResponseData<UserInfoModel> responseData=userInfoFacade.queryUserInfoIsExist(uid,sourceId);
            if (logger.isInfoEnabled()) {
                logger.info("rpc调用用户模块queryUserInfoIsExist，responseData={}", JSONObject.toJSONString(responseData));
            }
            //判断接口返回code
            if(!PublicUtil.isEmpty(responseData)&&!ResponseEnums.RPC_TYPE_SUCCESS.getCode().equals(responseData.getCode())){
                logger.warn("调用用户数据平台接口queryUserInfoIsExsit查询失败,traceId={},uid={},code={}" ,traceId, uid,responseData.getCode());
                return null;
            }
            UserInfoModel userInfoModel=responseData.getObj();
            if (logger.isInfoEnabled()) {
                logger.info("调用用户数据平台接口queryUserInfoIsExsit查询成功,traceId={},uid={},userInfoModel={}", traceId, uid, JSONObject.toJSONString(userInfoModel));
            }
            return userInfoModel;
        } catch (Exception e) {
            logger.error("traceId={},uid={},调用用户数据平台接口queryUserInfoIsExsit异常e={}",
                    traceId,uid, LogExceptionWapper.getStackTrace(e));
            e.printStackTrace();
            return null;
        }
    }
}
