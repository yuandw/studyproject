package com.gaoyang.marketing.mfbizweb.controller;/**
 * Created by zhanghui on 2018-12-5.
 */

import com.alibaba.dubbo.rpc.proxy.TraceIdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gaoyang.marketing.log.util.LogUtil;
import com.gaoyang.marketing.log.util.MethodCallResultEnum;
import com.gaoyang.marketing.mfbizweb.bean.HttpResponseBean;
import com.gaoyang.marketing.mfbizweb.bean.LaXinRequestParamsBean;
import com.gaoyang.marketing.mfbizweb.bean.UserInfoBean;
import com.gaoyang.marketing.mfbizweb.service.LaXinExchangeVoucherService;
import com.gaoyang.marketing.mfbizweb.service.baseService.CreateGyMemeberService;
import com.gaoyang.marketing.mfbizweb.util.Constants;
import com.gaoyang.marketing.mfbizweb.util.PublicUtil;
import com.gaoyang.marketing.mfbizweb.util.ResponseEnums;
import com.gaoyang.marketing.mfbizweb.util.SignUtils;
import com.gaoyang.marketing.mfbizweb.util.acm.MfbizWebAcmClient;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * @author zhanghui
 * @create 2018-12-5
 * @description
 */
@Controller
public class LaXinCreateMemberController {
    public final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String CONTROLLER_NAME = "[拉新用户创建会员卡controller]";
    @Autowired
    LaXinExchangeVoucherService laXinExchangeVoucherService;
    @Autowired
    CreateGyMemeberService createGyMemeberService;
    @Autowired
    private LogUtil logUtil;
    @Autowired
    private SignUtils signUtils;

    @Autowired
    MfbizWebAcmClient mfbizWebAcmClient;

    @RequestMapping(value = "/laXinCreateMemeber", method = RequestMethod.GET)
    @ResponseBody
    public String getUseInfo(HttpServletRequest request,
                             HttpServletResponse response, @RequestParam("params") String params) {
        String traceId = TraceIdUtil.getTraceId();
        long startTime = System.currentTimeMillis();
        if (logger.isInfoEnabled()) {
            logger.info(CONTROLLER_NAME + "traceId={}", traceId);
        }
        HttpResponseBean responseBean = new HttpResponseBean();
        HttpResponseBean responseBeanCreateMemeber = new HttpResponseBean();
        String returnStr = "";
        Map<String, String> userInfo = (Map<String, String>) request.getSession().getAttribute("userInfo");
        if (PublicUtil.isEmpty(userInfo)) {
            responseBean.setCode(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getCode());
            responseBean.setMsg(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getDesc());
            returnStr = JSON.toJSONString(responseBean);
            logger.warn(CONTROLLER_NAME + "traceId={},returnStr={}", traceId, returnStr);
            logUtil.log(new Throwable(), CONTROLLER_NAME, null, MethodCallResultEnum.FAIL, "拉新用户创建会员卡失败", startTime);
            return returnStr;
        }
        String uid = userInfo.get("uid");
        String userId = userInfo.get("userId");
        String accessToken = userInfo.get("accessTokenForCreateMember");
        String bookFlag = userInfo.get("bookFlag");
        String bookDate = userInfo.get("bookDate");
        //对session中的用户信息进行校验
        if (PublicUtil.isEmpty(uid) || PublicUtil.isEmpty(userId) || PublicUtil.isEmpty(accessToken)) {
            responseBean.setCode(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getCode());
            responseBean.setMsg(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getDesc());
            returnStr = JSON.toJSONString(responseBean);
            logUtil.log(new Throwable(), CONTROLLER_NAME, null, MethodCallResultEnum.FAIL, "拉新领取会员卡controller获取session中uid、userid、token信息为空为空", startTime);
            logger.warn(CONTROLLER_NAME + "traceId={},returnStr={}", traceId, returnStr);
            return returnStr;
        }
        //取tag信息
        String tag = PublicUtil.getTag(params);
        if (StringUtils.isBlank(tag)) {
            tag="cmcclaxin";
        }


        responseBeanCreateMemeber = createGyMemeberService.createGYMemeber(userId, accessToken, uid, bookFlag, bookDate, tag);

        if (ResponseEnums.TYPE_SUCCESS.getCode().equals(responseBeanCreateMemeber.getCode())) {
            //拉新用户创建会员卡成功 将会员卡信息计入缓存
            UserInfoBean userInfoBean = (UserInfoBean) responseBeanCreateMemeber.getData();
            userInfo.put("gyMemberId", userInfoBean.getMciGyId());
            userInfo.put("openDate", userInfoBean.getOpenDate());
            userInfo.put("iphone", userInfoBean.getPhone());
            userInfo.put("realIphone", userInfoBean.getRealIphone());
            if (logger.isInfoEnabled()) {
                logger.info(CONTROLLER_NAME + "将手机号，会员卡号计入缓存,traceId={},userinfo={},拉新用户创建会员卡成功"
                        , traceId, JSONObject.toJSONString(userInfo));
            }
            request.getSession().setAttribute("userInfo", userInfo);
            //设置有效期
            HttpSession session = request.getSession();
            session.setMaxInactiveInterval(300);
            //创建会员卡成功
            //验签
            LaXinRequestParamsBean laXinRequestParamsBean = signUtils.validateAndConvert(params);
            if (PublicUtil.isEmpty(laXinRequestParamsBean)) {

                logger.warn(CONTROLLER_NAME + "拉新用户创建会员卡成功，验签失败,traceId={},用户信息={}", traceId, JSON.toJSONString(userInfo));
                responseBean.setCode(ResponseEnums.TYPE_CHECK_SIGN_FAIL.getCode());
                responseBean.setMsg(ResponseEnums.TYPE_CHECK_SIGN_FAIL.getDesc());
                returnStr = JSON.toJSONString(responseBean);
                logger.warn(CONTROLLER_NAME + "traceId={},returnStr={}", traceId, returnStr);
                logUtil.log(new Throwable(), CONTROLLER_NAME, null, MethodCallResultEnum.FAIL, "拉新用户创建会员卡成功，验签失败", startTime);
                return returnStr;
            }
            String voucherId = laXinRequestParamsBean.getVoucherId();
            String memberId = userInfo.get("gyMemberId");
            tag = laXinRequestParamsBean.getTag();
            String tradeAmount = "0";
            String  laxinchannel="";
            if (StringUtils.isBlank(laxinchannel)) {
                laxinchannel = mfbizWebAcmClient.getPorpertiesValue("laxinchannel");
                if (StringUtils.isBlank(laxinchannel) || "null".equals(laxinchannel)) {
                    laxinchannel = "cmcclaxin";
                }
            }
            if (laxinchannel.contains(tag)) {
                tradeAmount = Constants.CMCC_LAXIN_FLOW;
            }
            //传给前端sendFlow +卡券核销的流量值
            UserInfoBean userInfoBeanCreateMemeber = (UserInfoBean) responseBeanCreateMemeber.getData();
            String sendFlow = userInfoBeanCreateMemeber.getSendFlow();
            userInfoBeanCreateMemeber.setSendFlow(String.valueOf(Integer.valueOf(sendFlow) + Integer.valueOf(tradeAmount)));

            responseBean = laXinExchangeVoucherService.laXinExchangeVoucher(voucherId, uid, userId, memberId, tag);
            responseBean.setData(userInfoBeanCreateMemeber);
            returnStr = JSON.toJSONString(responseBean);

        } else if (ResponseEnums.TYPE_CREATEGYMEMEBERFREEZE_FAIL.getCode().equals(responseBeanCreateMemeber.getCode())) {
            returnStr = JSON.toJSONString(responseBeanCreateMemeber);
            if (logger.isInfoEnabled()) {
                logger.info(CONTROLLER_NAME + "冻结账户创建会员卡失败traceId={},uid={},userId={},returnStr={}", traceId, uid, userId, returnStr);
            }

        } else if (ResponseEnums.TYPE_CREATEGYMEMEBERNOTPERSONAL_FAIL.getCode().equals(responseBeanCreateMemeber.getCode())) {
            returnStr = JSON.toJSONString(responseBeanCreateMemeber);
            if (logger.isInfoEnabled()) {
                logger.info(CONTROLLER_NAME + "公司类账户创建会员卡失败traceId={},uid={},userId={},returnStr={}", traceId, uid, userId, returnStr);
            }
        } else if (ResponseEnums.TYPE_CREATEGYMEMEBERNOTMOBLIE_FAIL.getCode().equals(responseBeanCreateMemeber.getCode())) {
            returnStr = JSON.toJSONString(responseBeanCreateMemeber);
            if (logger.isInfoEnabled()) {
                logger.info(CONTROLLER_NAME + "非移动用户创建会员卡失败traceId={},uid={},userId={},returnStr={}", traceId, uid, userId, returnStr);
            }
        } else if (ResponseEnums.TYPE_CREATEGYMEMEBER_NOT_PHONE_FAIL.getCode().equals(responseBeanCreateMemeber.getCode())) {
            returnStr = JSON.toJSONString(responseBeanCreateMemeber);
            if (logger.isInfoEnabled()) {
                logger.info(CONTROLLER_NAME + "支付宝用户未绑定手机号开卡失败traceId={},uid={},userId={},returnStr={}", traceId, uid, userId, returnStr);
            }
        } else {
            //领取会员卡失败
            responseBean.setCode(ResponseEnums.TYPE_NOT_MEMBER_FAIL.getCode());
            responseBean.setMsg(ResponseEnums.TYPE_NOT_MEMBER_FAIL.getDesc());
            returnStr = JSON.toJSONString(responseBean);
            logger.warn(CONTROLLER_NAME + "traceId={},returnStr={}", traceId, returnStr);
            logUtil.log(new Throwable(), CONTROLLER_NAME, null, MethodCallResultEnum.FAIL, "拉新用户创建会员卡失败", startTime);
            return returnStr;

        }


        if (logger.isInfoEnabled()) {
            logger.info(CONTROLLER_NAME + "traceId={},uid={},userId={},returnStr={}", traceId, uid, userId, returnStr);
        }

        if (ResponseEnums.TYPE_RES_SUCCESS.getCode().equals(responseBean.getCode())) {
            logUtil.log(new Throwable(), CONTROLLER_NAME, null, MethodCallResultEnum.SUCCESS, "拉新券核销成功", startTime);
        } else {
            logUtil.log(new Throwable(), CONTROLLER_NAME, null, MethodCallResultEnum.FAIL, "拉新券核销失败", startTime);
        }
        return returnStr;
    }

}
