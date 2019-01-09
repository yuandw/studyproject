package com.gaoyang.marketing.mfbizweb.service.baseService;/**
 * Created by zhanghui on 2018-9-24.
 */

import com.alibaba.dubbo.rpc.proxy.TraceIdUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gaoyang.marketing.mfbizweb.bean.BusiMonitorLoggerBean;
import com.gaoyang.marketing.mfbizweb.bean.HttpResponseBean;
import com.gaoyang.marketing.mfbizweb.bean.UserInfoBean;
import com.gaoyang.marketing.mfbizweb.mq.specialChargeCreateMember.SpecialChargeAccService;
import com.gaoyang.marketing.mfbizweb.util.DateUtil;
import com.gaoyang.marketing.mfbizweb.util.LogExceptionWapper;
import com.gaoyang.marketing.mfbizweb.util.PublicUtil;
import com.gaoyang.marketing.mfbizweb.util.ResponseEnums;
import com.gaoyang.marketing.mfbizweb.util.acm.MfbizWebAcmClient;
import com.gaoyang.marketing.muserbasecore.membercard.facade.MemberCardFacade;
import com.gaoyang.marketing.muserbasecore.membercard.facade.model.response.CreateMemberCardModel;
import com.gaoyang.marketing.muserbasecore.userauth.facade.UserAuthFacade;
import com.gaoyang.marketing.muserbasecore.userauth.facade.model.AlipaySystemOauthTokenModel;
import com.gaoyang.marketing.muserbasecore.userauth.facade.model.AlipayUserInfoShareModel;
import com.gaoyang.marketing.muserbasecore.userinfo.facade.UserInfoFacade;
import com.gaoyang.marketing.muserbasecore.userinfo.facade.model.ResponseData;
import com.gaoyang.marketing.muserbasecore.userinfo.facade.model.request.UpdateUserInfoData;
import com.gaoyang.marketing.muserbasecore.userinfo.facade.model.response.UpdateUserInfoModel;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author zhanghui
 * @create 2018-9-24
 * @description 创建会员卡service
 */
@Service
public class CreateGyMemeberService {
    public final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Logger busiMonitorLogger = LoggerFactory.getLogger("busi-monitor");

    @Resource(name = "userAuthFacade")
    UserAuthFacade userAuthFacade;

    @Resource(name = "memberCardFacade")
    MemberCardFacade memberCardFacade;

    @Resource(name = "userInfoFacade")
    UserInfoFacade userInfoFacade;
    @Resource(name = "specialChargeAccService")
    SpecialChargeAccService specialChargeAccService;
    @Autowired
    MfbizWebAcmClient mfbizWebAcmClient;

    /**
     * @param userId
     * @param bookFlag 预约标识
     *                 bookDate  预约时间
     *                 sourceId    来源
     * @return
     * @desc 调用用户接口平台创建会员卡
     * @update 20181217  增加特殊号卡标识
     */
    public HttpResponseBean createGYMemeber(String userId, String accessToken, String uid, String bookFlag, String bookDate,String sourceId) {
        String traceId = TraceIdUtil.getTraceId();
        HttpResponseBean httpResponseBean = new HttpResponseBean();
        UserInfoBean userInfoBean = new UserInfoBean();
        try {

            //1 调用用户平台数据，查询支付宝用户信息
            AlipayUserInfoShareModel alipayUserInfoShareModel = getAlipayUserInfoShare(accessToken);
            if (PublicUtil.isEmpty(alipayUserInfoShareModel)
                    || PublicUtil.isEmpty(alipayUserInfoShareModel.getUserId())) {

                BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
                busiMonitorLoggerBean.setConsumer("mfbizweb");
                busiMonitorLoggerBean.setProvider("muserbasecore");
                busiMonitorLoggerBean.setResult("F");
                busiMonitorLoggerBean.setBusiness("领卡");
                busiMonitorLoggerBean.setError_desc("查询支付宝用户信息返回对象为空或userid、电话号为空");
                busiMonitorLoggerBean.setTrace_id(traceId);
                busiMonitorLogger.warn(busiMonitorLoggerBean);

                httpResponseBean.setCode(ResponseEnums.TYPE_CREATEGYMEMEBER_FAIL.getCode());
                httpResponseBean.setMsg(ResponseEnums.TYPE_CREATEGYMEMEBER_FAIL.getDesc());
                logger.warn("调用用户数据平台接口getAlipayUserInfoShare返回对象为空或userid为空,traceId={},accessTokenForCreateMember={}", traceId, accessToken);
                return httpResponseBean;
            }
            //获取手机号为空说明 支付宝账户未绑定手机号，不能开卡
            if (PublicUtil.isEmpty(alipayUserInfoShareModel.getMobile())) {

                httpResponseBean.setCode(ResponseEnums.TYPE_CREATEGYMEMEBER_NOT_PHONE_FAIL.getCode());
                httpResponseBean.setMsg(ResponseEnums.TYPE_CREATEGYMEMEBER_NOT_PHONE_FAIL.getDesc());
                logger.warn("调用用户数据平台接口getAlipayUserInfoShare返回手机号为空,不能开卡，traceId={},accessTokenForCreateMember={}", traceId, accessToken);
                return httpResponseBean;
            }


            //针对手机号进行移动判断
            String iphone = alipayUserInfoShareModel.getMobile();
           boolean moblieFlag = isChinaMobilePhoneNum(iphone);

            // 2 更新用户信息----非移动用户也会更新用户信息
            UpdateUserInfoModel updateUserInfoModel = updateUserInfo(alipayUserInfoShareModel, userId, moblieFlag,sourceId);

            if (logger.isInfoEnabled()) {
                logger.info("更新用户信息,traceId={},uid={},iphone={},moblieFlag={},updateUserInfoModel={}", traceId, uid, iphone, moblieFlag, JSONObject.toJSONString(updateUserInfoModel));
            }
            if (PublicUtil.isEmpty(updateUserInfoModel)) {

                BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
                busiMonitorLoggerBean.setConsumer("mfbizweb");
                busiMonitorLoggerBean.setProvider("muserbasecore");
                busiMonitorLoggerBean.setResult("F");
                busiMonitorLoggerBean.setBusiness("领卡");
                busiMonitorLoggerBean.setError_desc("更新用户信息返回实体Model为空");
                busiMonitorLoggerBean.setTrace_id(traceId);
                busiMonitorLogger.warn(busiMonitorLoggerBean);

                httpResponseBean.setCode(ResponseEnums.TYPE_CREATEGYMEMEBER_FAIL.getCode());
                httpResponseBean.setMsg(ResponseEnums.TYPE_CREATEGYMEMEBER_FAIL.getDesc());
                logger.warn("调用用户数据平台接口updateUserInfo更新用户信息失败或返回实体Model为空,traceId={}, userId={}, uid={}", traceId, userId, uid);
                return httpResponseBean;
            }

            if (!moblieFlag) {
                String provinceName = "";
                //创建会员卡时省份测试开关配置
                String provinceTest = mfbizWebAcmClient.getPorpertiesValue("provinceTest");
                if (StringUtils.isBlank(provinceTest) || "null".equals(provinceTest)) {
                    //acm异常获取不到地址默认不跳转
                    provinceTest = "1";
                }
                //判断省份信息是否测试
                if ("0".equals(provinceTest)) {
                    provinceName = mfbizWebAcmClient.getPorpertiesValue("provinceName");
                    if (StringUtils.isBlank(provinceName) || "null".equals(provinceName)) {
                        //acm异常获取不到地址默认不跳转
                        provinceName = "其他";
                    }
                } else {
                    //20181109针对省份信息为空的非移动号码，跳转链接为其他
                    if (PublicUtil.isEmpty(alipayUserInfoShareModel.getProvince())) {
                        provinceName = "其他";
                    } else {
                        provinceName = URLDecoder.decode(alipayUserInfoShareModel.getProvince(), "UTF-8");
                    }
                }
                if (logger.isInfoEnabled()) {
                    logger.info("创建会员卡手机号省份acm测试配置,traceId={}, userId={}, uid={},provinceTest={},provinceName={}", traceId, userId, uid, provinceTest, provinceName);
                }
                //根据省份选择相关跳转外链
                String extraUrl = getExtraUrl(provinceName);
                httpResponseBean.setCode(ResponseEnums.TYPE_CREATEGYMEMEBERNOTMOBLIE_FAIL.getCode());
                httpResponseBean.setMsg(ResponseEnums.TYPE_CREATEGYMEMEBERNOTMOBLIE_FAIL.getDesc());
                httpResponseBean.setExmsg(provinceName);
                httpResponseBean.setExurl(extraUrl);
                if (logger.isInfoEnabled()) {
                    logger.info("非移动用户，无法创建会员卡,traceId={},uid={},iphone={},provinceName={},extraUrl={}", traceId, uid, iphone, provinceName, extraUrl);
                }
                return httpResponseBean;
            }
            //判断用户状态和用户类别
            String userStatus = alipayUserInfoShareModel.getUserStatus();
            String userType = alipayUserInfoShareModel.getUserType();
            //userStatus=B 被冻结用户
            if ("B".equals(userStatus)) {
                httpResponseBean.setCode(ResponseEnums.TYPE_CREATEGYMEMEBERFREEZE_FAIL.getCode());
                httpResponseBean.setMsg(ResponseEnums.TYPE_CREATEGYMEMEBERFREEZE_FAIL.getDesc());
                if (logger.isInfoEnabled()) {
                    logger.info("被冻结用户，无法创建会员卡,traceId={},uid={}", traceId, uid);
                }
                return httpResponseBean;
            }
            // userType=1 为公司类用户
            if ("1".equals(userType)) {
                httpResponseBean.setCode(ResponseEnums.TYPE_CREATEGYMEMEBERNOTPERSONAL_FAIL.getCode());
                httpResponseBean.setMsg(ResponseEnums.TYPE_CREATEGYMEMEBERNOTPERSONAL_FAIL.getDesc());
                if (logger.isInfoEnabled()) {
                    logger.info("公司类账户，无法创建会员卡,traceId={},uid={}", traceId, uid);
                }
                return httpResponseBean;
            }

            //4调用判断是否需要赠送流量
            // 不需要赠送流量  调用用户数据平台rpc，创建会员卡；需要赠送流量  发送事物消息 创建会员卡赠送流量
            // 读取配置
            List<CreateMemberCardModel> createMemberCardModelList = new ArrayList<>();
            //1·、判断是否预约用户
            //2、判断开卡时间是否在预约时间15天内
            //预约送流量默认是0
            String sendBookFlow = "0";
            if ("0".equals(bookFlag)) {

                //判断开卡时间与预约时间间隔
                Long bookDateMillis = DateUtil.parseDateFromStr(bookDate).getTime();
                Long cruTime = System.currentTimeMillis();
                //预约时间与开卡时间间隔15天以上则不赠送预约流量
                if (bookDateMillis + 15 * 24 * 60 * 60 * 1000 >= cruTime) {
                    sendBookFlow = mfbizWebAcmClient.getPorpertiesValue("bookMemeberSendFlowRule");
                    if (logger.isInfoEnabled()) {
                        logger.info("领取会员卡service,读取acm配置，预约开卡赠送流量规则,traceId={},uid={},userId={},rule={}", traceId, uid, userId, sendBookFlow);
                    }
                    if (StringUtils.isBlank(sendBookFlow) || "null".equals(sendBookFlow)) {
                        sendBookFlow = "1000";
                    }
                }
            }

            String jsonRules = mfbizWebAcmClient.getPorpertiesValue("createMemeberSendFlowRule");
            if (logger.isInfoEnabled()) {
                logger.info("领取会员卡service,读取acm配置，开卡赠送流量规则,traceId={},uid={},userId={},rule={}", traceId, uid, userId, jsonRules);
            }
            String sendFlow = "0";
            //开卡成功将totalFlow返回给前端  开卡赠送+预约赠送
            int totalFlow = 0;
            //acm 没有配置 默认开卡赠送1000M流量
            if (StringUtils.isBlank(jsonRules) || "null".equals(jsonRules)) {
                totalFlow = 1000 + Integer.valueOf(sendBookFlow);
                createMemberCardModelList = specialChargeAccService.sendMsg(uid, accessToken, userId, traceId, Integer.toString(totalFlow),sourceId);

            } else {
                sendFlow = PublicUtil.getSendFlow(jsonRules);
                //没有配置开卡送流量 则直接调用rpc开卡
                if ("0".equals(sendFlow) && "0".equals(sendBookFlow)) {
                    if (logger.isInfoEnabled()) {
                        logger.info("领取会员卡service,在acm配置有效时间内未获取到赠送规则，开卡不赠送流量,直接调用rpc创建会员卡接口,traceId={},uid={},userId={},rule={}", traceId, uid, userId, jsonRules);
                    }
                    ResponseData<CreateMemberCardModel> responseData = createMemberCardAddMsiId(uid, accessToken, userId,sourceId);
                    createMemberCardModelList.add(responseData.getObj());
                } else {
                    totalFlow = Integer.valueOf(sendFlow) + Integer.valueOf(sendBookFlow);
                    if (logger.isInfoEnabled()) {
                        logger.info("领取会员卡service,traceId={},uid={},userId={},开卡赠送流量={},预热赠送流量={}", traceId, uid, userId, sendFlow, sendBookFlow);
                    }
                    createMemberCardModelList = specialChargeAccService.sendMsg(uid, accessToken, userId, traceId, Integer.toString(totalFlow),sourceId);
                }

            }


            if (PublicUtil.isEmpty(createMemberCardModelList)
                    || PublicUtil.isEmpty(createMemberCardModelList.get(0))
                    || PublicUtil.isEmpty(createMemberCardModelList.get(0).getGyMemberId())
                    || "-1".equals(createMemberCardModelList.get(0).getGyMemberId())
                    || "-1".equals(alipayUserInfoShareModel.getMobile())) {

                BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
                busiMonitorLoggerBean.setConsumer("mfbizweb");
                busiMonitorLoggerBean.setProvider("muserbasecore");
                busiMonitorLoggerBean.setResult("F");
                busiMonitorLoggerBean.setBusiness("领卡");
                busiMonitorLoggerBean.setError_desc("创建会员卡返回对象为空");
                busiMonitorLoggerBean.setTrace_id(traceId);
                busiMonitorLogger.warn(busiMonitorLoggerBean);


                httpResponseBean.setCode(ResponseEnums.TYPE_CREATEGYMEMEBER_FAIL.getCode());
                httpResponseBean.setMsg(ResponseEnums.TYPE_CREATEGYMEMEBER_FAIL.getDesc());
                logger.warn("调用用户数据平台接口createMemberCard返回对象为空或会员卡号为空或会员卡号为-1或手机号为-1,traceId={}, userId={}, uid={}", traceId, userId, uid);
                return httpResponseBean;
            }
            //增加开卡时间
            userInfoBean.setOpenDate(createMemberCardModelList.get(0).getOpendate());
            userInfoBean.setMciGyId(createMemberCardModelList.get(0).getGyMemberId());
            userInfoBean.setSendFlow(Integer.toString(totalFlow));
            //手机号脱敏
            String getSercetiphone = PublicUtil.getSercetiphone(alipayUserInfoShareModel.getMobile());
            userInfoBean.setPhone(getSercetiphone);
            userInfoBean.setRealIphone(alipayUserInfoShareModel.getMobile());
            httpResponseBean.setCode(ResponseEnums.TYPE_SUCCESS.getCode());
            httpResponseBean.setData(userInfoBean);
            if (logger.isInfoEnabled()) {
                logger.info("创建会员卡接口service,traceId={},userId={},uid={},res={}", traceId, userId, uid, JSONArray.toJSONString(httpResponseBean));
            }
            return httpResponseBean;
        } catch (Exception e) {
            httpResponseBean.setCode(ResponseEnums.TYPE_USERINFO_EXCEPYION.getCode());
            httpResponseBean.setMsg(ResponseEnums.TYPE_USERINFO_EXCEPYION.getDesc());

            BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("mfbizweb");
            busiMonitorLoggerBean.setProvider("muserbasecore");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("领卡");
            busiMonitorLoggerBean.setError_desc("领卡异常");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);

            logger.error("traceId={},uid={},userId={},创建会员卡异常e={}",
                    traceId, uid, userId, LogExceptionWapper.getStackTrace(e));
            e.printStackTrace();
            return httpResponseBean;
        }
    }

    /**
     * @param authCode
     * @return
     * @desc 调用用户数据平台接口查询accessToken uid
     */

    public AlipaySystemOauthTokenModel getAlipayAuthToken(String authCode) {
        String traceId = TraceIdUtil.getTraceId();
        if (logger.isInfoEnabled()) {
            logger.info("调用用户数据平台接口getAlipayAuthToken, traceId={}, authCode={}", traceId, authCode);
        }
        try {

            ResponseData<AlipaySystemOauthTokenModel> responseData = userAuthFacade.getAlipayAuthToken(authCode);
            if (!PublicUtil.isEmpty(responseData)) {
                String code = responseData.getCode();
                if (logger.isInfoEnabled()) {
                    logger.info("调用用户数据平台接口getAlipayAuthToken, traceId={}, authCode={},responseData={}", traceId, authCode, responseData);
                }
                //判断接口返回code
                if (!ResponseEnums.RPC_TYPE_SUCCESS.getCode().equals(code)) {
                    logger.warn("调用用户数据平台接口getAlipayAuthToken，查询失败, traceId={}, authCode={},code={}", traceId, authCode, code);
                    return null;
                }
                AlipaySystemOauthTokenModel alipaySystemOauthTokenModel = responseData.getObj();
                return alipaySystemOauthTokenModel;
            } else {
                logger.warn("调用用户数据平台接口getAlipayAuthToken获取responseData为空, traceId={}, responseData={}", traceId, JSONObject.toJSONString(responseData));
            }
        } catch (Exception e) {

            BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("mfbizweb");
            busiMonitorLoggerBean.setProvider("muserbasecore");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("领卡");
            busiMonitorLoggerBean.setError_desc("获取支付宝accessToken异常");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);

            logger.error("traceId={},authCode={},调用用户数据平台接口getAlipayAuthToken异常e={}",
                    traceId, authCode, LogExceptionWapper.getStackTrace(e));
            e.printStackTrace();

        }
        return null;
    }


    /**
     * @param accessToken
     * @return AlipayUserInfoShareModel
     * @desc 通过accessToken获取支付宝用户信息
     */
    public AlipayUserInfoShareModel getAlipayUserInfoShare(String accessToken) {
        String traceId = TraceIdUtil.getTraceId();
        logger.info("调用用户数据平台接口getAlipayUserInfoShare, traceId={},accessTokenForCreateMember={}", traceId, accessToken);
        try {
            ResponseData<AlipayUserInfoShareModel> responseData = userAuthFacade.getAlipayUserInfoShare(accessToken);
            if (logger.isInfoEnabled()) {
                logger.info("调用用户数据平台接口getAlipayUserInfoShare,查询返回结果, traceId={}, responseData={}",
                        traceId, JSONObject.toJSONString(responseData));
            }
            if (!PublicUtil.isEmpty(responseData)) {
                String code = responseData.getCode();
                //判断接口返回code
                if (!ResponseEnums.RPC_TYPE_SUCCESS.getCode().equals(code)) {
                    logger.warn("调用用户数据平台接口getAlipayUserInfoShare,查询失败, traceId={}, accessTokenForCreateMember={},code={}", traceId, accessToken, code);
                    return null;
                }
                AlipayUserInfoShareModel alipayUserInfoShareModel = responseData.getObj();
                if (logger.isInfoEnabled()) {
                    logger.info("调用用户数据平台接口getAlipayUserInfoShare,查询成功, traceId={}, accessTokenForCreateMember={},code={},userId={},iphone={},userStatus={},userType={}",
                            traceId, accessToken, code, alipayUserInfoShareModel.getUserId(), alipayUserInfoShareModel.getMobile(),
                            alipayUserInfoShareModel.getUserStatus(), alipayUserInfoShareModel.getUserType());
                }
                return alipayUserInfoShareModel;
            }
        } catch (Exception e) {
            BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("mfbizweb");
            busiMonitorLoggerBean.setProvider("muserbasecore");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("领卡");
            busiMonitorLoggerBean.setError_desc("accessToken获取支付宝用户信息异常");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);

            logger.error("traceId={},调用用户数据平台接口getAlipayUserInfoShare异常e={}",
                    traceId, LogExceptionWapper.getStackTrace(e));


        }
        return null;
    }

    /**
     * @param alipayUserInfoShareModel moblieFlag ture 移动 false非移动
     * @return
     * @desc 调用用户数据平台更新用户新
     */
    public UpdateUserInfoModel updateUserInfo(AlipayUserInfoShareModel alipayUserInfoShareModel, String userId, boolean moblieFlag,String sourceId) {
        String traceId = TraceIdUtil.getTraceId();
        String uid = alipayUserInfoShareModel.getUserId();
        String avatar = alipayUserInfoShareModel.getAvatar();
        String province = alipayUserInfoShareModel.getProvince();
        String city = alipayUserInfoShareModel.getCity();
        String nickName = alipayUserInfoShareModel.getNickName();
        String isStudentCertified = alipayUserInfoShareModel.getIsStudentCertified();
        String userType = alipayUserInfoShareModel.getUserType();
        String userStatus = alipayUserInfoShareModel.getUserStatus();
        String gender = alipayUserInfoShareModel.getGender();
        String isCrtified = alipayUserInfoShareModel.getIsCertified();
        String phone = alipayUserInfoShareModel.getMobile();
        String isp = "9";
        if (moblieFlag) {
            isp = "1";
        }

        try {
            UpdateUserInfoData updateUserInfoData = new UpdateUserInfoData();
            updateUserInfoData.setUserId(userId);
            updateUserInfoData.setZfbUid(uid);
            updateUserInfoData.setUserAvatarUrl(avatar);
            updateUserInfoData.setUserProvince(province);
            updateUserInfoData.setUserCity(city);
            updateUserInfoData.setUserNickName(nickName);
            updateUserInfoData.setUserIsStudentCert(isStudentCertified);
            updateUserInfoData.setZfbUserType(userType);
            updateUserInfoData.setZfbUserStatus(userStatus);
            updateUserInfoData.setUserGender(gender);
            updateUserInfoData.setUserIsCert(isCrtified);
            updateUserInfoData.setUserBindPhone(phone);
            updateUserInfoData.setIspType(isp);
            updateUserInfoData.setMsiId(sourceId);
            if (logger.isInfoEnabled()) {
                logger.info("调用用户数据平台接口updateUserInfo,traceId={}, uid={},userId={},updateUserInfoData={}", traceId, uid, userId, JSONObject.toJSON(updateUserInfoData));
            }
            ResponseData<UpdateUserInfoModel> responseData = userInfoFacade.updateUserInfo(updateUserInfoData);
            if (logger.isInfoEnabled()) {
                logger.info("调用用户数据平台接口updateUserInfo，更新用户信息,traceId={}, uid={},responseData={}", traceId, uid, JSONObject.toJSONString(responseData));
            }
            if (!PublicUtil.isEmpty(responseData)) {

                String code = responseData.getCode();
                //判断接口返回code
                if (!ResponseEnums.RPC_TYPE_SUCCESS.getCode().equals(code)) {
                    logger.warn("调用用户数据平台接口updateUserInfo，更新用户信息失败, traceId={}, uid={},code={}", traceId, uid, code);
                    return null;
                }
                UpdateUserInfoModel updateUserinfoModel = responseData.getObj();
                return updateUserinfoModel;
            }
        } catch (Exception e) {

            BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("mfbizweb");
            busiMonitorLoggerBean.setProvider("muserbasecore");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("领卡");
            busiMonitorLoggerBean.setError_desc("更新用户信息异常");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);

            logger.error("traceId={},uid={},调用用户数据平台接口updateUserInfo异常e={}",
                    traceId, LogExceptionWapper.getStackTrace(e));
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param uid,accessToken,userId
     * @return
     * @desc 调用用户数据平台接口createMemberCard创建会员卡
     */
    public ResponseData<CreateMemberCardModel> createMemberCardAddMsiId(String uid, String accessToken, String userId,String sourceId) {
        String traceId = TraceIdUtil.getTraceId();
        if (logger.isInfoEnabled()) {
            logger.info("调用用户数据平台接口createMemberCard, traceId={}, uid={},accessToken={}, userId={},sourceId={}", traceId, uid, accessToken, userId,sourceId);
        }
        try {

            ResponseData<CreateMemberCardModel> responseData = memberCardFacade.createMemberCard(userId, uid, accessToken,sourceId);
            if (PublicUtil.isEmpty(responseData)) {
                logger.warn("调用用户数据平台接口createMemberCard返回结果为空，查询失败, traceId={},uid={},userId={},sourceId={}",
                        traceId, uid,userId,sourceId);
                return null;
            }

            if (logger.isInfoEnabled()) {
                logger.info("调用用户数据平台接口createMemberCard,成功, traceId={}, uid={},sourceId={},responseData={}",
                        traceId, uid, JSONObject.toJSONString(responseData),sourceId);
            }
            return responseData;
        } catch (Exception e) {
            BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("mfbizweb");
            busiMonitorLoggerBean.setProvider("muserbasecore");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("领卡");
            busiMonitorLoggerBean.setError_desc("领卡异常");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);

            logger.error("traceId={},uid={},调用用户数据平台接口createMemberCard异常e={}",
                    traceId, LogExceptionWapper.getStackTrace(e));
            e.printStackTrace();
            return null;
        }

    }






    /**
     * @param uid,accessToken,userId
     * @return
     * @desc 调用用户数据平台接口createMemberCard创建会员卡     暂不使用
     */
    public ResponseData<CreateMemberCardModel> createMemberCard(String uid, String accessToken, String userId) {
        String traceId = TraceIdUtil.getTraceId();
        if (logger.isInfoEnabled()) {
            logger.info("调用用户数据平台接口createMemberCard, traceId={}, uid={},accessToken={}, userId={}", traceId, uid, accessToken, userId);
        }
        try {
            //测试监控日志
            ResponseData<CreateMemberCardModel> responseData = memberCardFacade.createMemberCard(userId, uid, accessToken);
            if (PublicUtil.isEmpty(responseData)) {
                logger.warn("调用用户数据平台接口createMemberCard返回结果为空，查询失败, traceId={},uid={}", traceId, uid);
                return null;
            }
            // String code=responseData.getCode();
            //判断接口返回code--20181029  判断接口返回值放在消息send内部类判断
          /*  if(!ResponseEnums.RPC_TYPE_SUCCESS.getCode().equals(code)){
                logger.error("调用用户数据平台接口createMemberCard查询失败, traceId={}, uid={},code={}",traceId,uid,code);
                return null;
            }
            CreateMemberCardModel createMemberCardModel=responseData.getObj();
            if(PublicUtil.isEmpty(createMemberCardModel)){

                logger.error("调用用户数据平台接口createMemberCard,返回model为空, traceId={}, uid={},code={}", traceId, uid, code);
                return null;
            }*/

            if (logger.isInfoEnabled()) {
                logger.info("调用用户数据平台接口createMemberCard,成功, traceId={}, uid={},responseData={}", traceId, uid, JSONObject.toJSONString(responseData));
            }
            return responseData;
        } catch (Exception e) {
            BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("mfbizweb");
            busiMonitorLoggerBean.setProvider("muserbasecore");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("领卡");
            busiMonitorLoggerBean.setError_desc("领卡异常");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);

            logger.error("traceId={},uid={},调用用户数据平台接口createMemberCard异常e={}",
                    traceId, LogExceptionWapper.getStackTrace(e));
            e.printStackTrace();
            return null;
        }

    }

    public boolean isChinaMobilePhoneNum(String mobile) {
        String regex = mfbizWebAcmClient.getPorpertiesValue("iphone.prefix");
        if (StringUtils.isBlank(regex) || "null".equals(regex)) {
            regex = "^(139|138|137|136|135|134|147|150|151|152|157|158|159|170|172|178|182|183|184|187|188|198|175)\\d{8}$";
        }
        Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(mobile);
        return m.matches();
    }

    public String getExtraUrl(String provinceName) {
        //判断支付宝返回省份信息是否包含“省”“市”
        String provinceUrl = "";
        if (provinceName.endsWith("省") || provinceName.endsWith("市")) {
            provinceUrl = "url_" + provinceName.substring(0, provinceName.length() - 1);
        } else {
            provinceUrl = "url_" + provinceName;
        }
        String extraUrl = mfbizWebAcmClient.getPorpertiesValue(provinceUrl);
        if (StringUtils.isBlank(extraUrl) || "null".equals(extraUrl)) {
            //acm异常获取不到地址默认不跳转
            extraUrl = "";
        }
        return extraUrl;
    }


}
