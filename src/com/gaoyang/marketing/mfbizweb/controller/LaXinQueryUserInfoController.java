package com.gaoyang.marketing.mfbizweb.controller;/**
 * Created by zhanghui on 2018-12-3.
 */

import com.alibaba.dubbo.rpc.proxy.TraceIdUtil;
import com.alibaba.fastjson.JSON;
import com.gaoyang.marketing.log.util.LogUtil;
import com.gaoyang.marketing.log.util.MethodCallResultEnum;
import com.gaoyang.marketing.mfbizweb.bean.HttpResponseBean;
import com.gaoyang.marketing.mfbizweb.bean.LaXinRequestParamsBean;
import com.gaoyang.marketing.mfbizweb.bean.UserInfoBean;
import com.gaoyang.marketing.mfbizweb.service.LaXinExchangeVoucherService;
import com.gaoyang.marketing.mfbizweb.service.baseService.QueryUseInfoService;
import com.gaoyang.marketing.mfbizweb.util.Constants;
import com.gaoyang.marketing.mfbizweb.util.PublicUtil;
import com.gaoyang.marketing.mfbizweb.util.ResponseEnums;
import com.gaoyang.marketing.mfbizweb.util.SignUtils;
import com.gaoyang.marketing.mfbizweb.util.acm.MfbizWebAcmClient;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Map;

/**
 * @author zhanghui
 * @create 2018-12-3
 * @description
 */
@Controller
public class LaXinQueryUserInfoController {
    public final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String CONTROLLER_NAME = "[拉新获取用户信息controller]";
    @Autowired
    LaXinExchangeVoucherService laXinExchangeVoucherService;
    @Autowired
    private LogUtil logUtil;
    @Autowired
    private SignUtils signUtils;
    @Autowired
    QueryUseInfoService useInfoService;
    @Autowired
    MfbizWebAcmClient mfbizWebAcmClient;
    @RequestMapping(value = "/query_laXinUserInfo", method = RequestMethod.GET)
    @ResponseBody
    public String getUseInfo(HttpServletRequest request,
                             HttpServletResponse response, @RequestParam("params") String params) {
        String traceId = TraceIdUtil.getTraceId();
        long startTime = System.currentTimeMillis();
        if (logger.isInfoEnabled()) {
            logger.info(CONTROLLER_NAME + "traceId={},params={}", traceId, params);
        }
        HttpResponseBean responseBean = new HttpResponseBean();
        HttpResponseBean responseBean_useInfo = new HttpResponseBean();
        String returnStr = "";
        Map<String, String> userInfo = (Map<String, String>) request.getSession().getAttribute("userInfo");
        if (PublicUtil.isEmpty(userInfo)) {
            responseBean.setCode(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getCode());
            responseBean.setMsg(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getDesc());
            returnStr = JSON.toJSONString(responseBean);
            logger.warn(CONTROLLER_NAME + "traceId={},returnStr={}", traceId, returnStr);
            logUtil.log(new Throwable(), CONTROLLER_NAME, null, MethodCallResultEnum.FAIL, "拉新获取用户信息失败", startTime);
            return returnStr;
        }

        String userId = userInfo.get("userId");
        String memberId = userInfo.get("gyMemberId");
        String iphone = userInfo.get("iphone");
        String uid = userInfo.get("uid");
        //查询用户信息
        responseBean_useInfo = useInfoService.getUserInfo(uid, userId, memberId, iphone);

        if (logger.isInfoEnabled()) {
            logger.info(CONTROLLER_NAME + "获取拉新用户帐户信息，traceId={},responseBean_useInfo={}", traceId, JSON.toJSONString(responseBean_useInfo));
        }

        if (!ResponseEnums.TYPE_SUCCESS.getCode().equals(responseBean_useInfo.getCode())) {
            if (logger.isInfoEnabled()) {
                logger.info(CONTROLLER_NAME + "拉新用户查询用户账户信息失败,traceId={},responseBean_useInfo={}", traceId, JSON.toJSONString(responseBean_useInfo));
            }
            responseBean.setCode(ResponseEnums.TYPE_USERINFO_EXCEPYION.getCode());
            responseBean.setMsg(ResponseEnums.TYPE_USERINFO_EXCEPYION.getDesc());
            returnStr = JSON.toJSONString(responseBean);
            if (logger.isInfoEnabled()) {
                logger.info(CONTROLLER_NAME + "traceId={},returnStr={}", traceId, returnStr);
            }
            logUtil.log(new Throwable(), CONTROLLER_NAME, null, MethodCallResultEnum.SUCCESS, "拉新用户查询用户账户信息失败", startTime);
            return returnStr;
        }

        //1、判断是否是会员，非会员领取会员卡；会员 进行验签。
        if (!PublicUtil.isMember(userInfo.get("gyMemberId"))) {
            if (logger.isInfoEnabled()) {
                logger.info(CONTROLLER_NAME + "拉新用户非会员需要创建会员卡,traceId={},用户信息={}", traceId, JSON.toJSONString(userInfo));
            }
            responseBean.setCode(ResponseEnums.TYPE_NOT_MEMBER_FAIL.getCode());
            responseBean.setMsg(ResponseEnums.TYPE_NOT_MEMBER_FAIL.getDesc());
            responseBean.setData(responseBean_useInfo.getData());
            returnStr = JSON.toJSONString(responseBean);
            if (logger.isInfoEnabled()) {
                logger.info(CONTROLLER_NAME + "traceId={},returnStr={}", traceId, returnStr);
            }
            logUtil.log(new Throwable(), CONTROLLER_NAME, null, MethodCallResultEnum.SUCCESS, "拉新用户非高阳会员,需要先领取会员卡", startTime);
            return returnStr;
        }
        String unescapeParams = StringEscapeUtils.unescapeHtml(params);
        logger.info(CONTROLLER_NAME + "traceId={},unescapeParams={}", traceId, unescapeParams);

        //2、验签
        LaXinRequestParamsBean laXinRequestParamsBean = signUtils.validateAndConvert(unescapeParams);
        logger.info(CONTROLLER_NAME + "traceId={},laXinRequestParamsBean={}", traceId, JSON.toJSONString(laXinRequestParamsBean));
        if (PublicUtil.isEmpty(laXinRequestParamsBean)) {

            logger.warn(CONTROLLER_NAME + "拉新用户验签失败,traceId={},用户信息={}", traceId, JSON.toJSONString(userInfo));

            responseBean.setCode(ResponseEnums.TYPE_CHECK_SIGN_FAIL.getCode());
            responseBean.setMsg(ResponseEnums.TYPE_CHECK_SIGN_FAIL.getDesc());
            responseBean.setData(responseBean_useInfo.getData());
            returnStr = JSON.toJSONString(responseBean);
            logger.warn(CONTROLLER_NAME + "traceId={},returnStr={}", traceId, returnStr);
            logUtil.log(new Throwable(), CONTROLLER_NAME, null, MethodCallResultEnum.FAIL, "拉新用户验签失败", startTime);
            return returnStr;
        }
        if (logger.isInfoEnabled()) {
            logger.info(CONTROLLER_NAME + "验签通过");
        }
        String voucherId = laXinRequestParamsBean.getVoucherId();

        String tag = laXinRequestParamsBean.getTag();
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

        responseBean = laXinExchangeVoucherService.laXinExchangeVoucher(voucherId, uid, userId, memberId, tag);

        if (ResponseEnums.TYPE_RES_SUCCESS.getCode().equals(responseBean.getCode())) {
            //卡券核销成功  将卡券面值+账户余额

            UserInfoBean userInfoBean=(UserInfoBean)responseBean_useInfo.getData();

            String balance=userInfoBean.getBalance();
            BigDecimal b = new BigDecimal(balance);

            String totalBalance=String.valueOf(b.intValue()+Integer.valueOf(tradeAmount));
            userInfoBean.setBalance(totalBalance);
            responseBean.setData(userInfoBean);
            returnStr = JSON.toJSONString(responseBean);

            if (logger.isInfoEnabled()) {
                logger.info(CONTROLLER_NAME + "traceId={},returnStr={}", traceId, returnStr);
            }
            logUtil.log(new Throwable(), CONTROLLER_NAME, null, MethodCallResultEnum.SUCCESS, "拉新券核销成功", startTime);
        } else {

            responseBean.setData(responseBean_useInfo.getData());
            returnStr = JSON.toJSONString(responseBean);

            if (logger.isInfoEnabled()) {
                logger.info(CONTROLLER_NAME + "traceId={},returnStr={}", traceId, returnStr);
            }
            logUtil.log(new Throwable(), CONTROLLER_NAME, null, MethodCallResultEnum.FAIL, "拉新券核销失败", startTime);
        }
        return returnStr;
    }

}
