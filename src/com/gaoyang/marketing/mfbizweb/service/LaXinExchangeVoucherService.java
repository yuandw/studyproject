package com.gaoyang.marketing.mfbizweb.service;/**
 * Created by zhanghui on 2018-12-5.
 */

import com.alibaba.dubbo.rpc.proxy.TraceIdUtil;
import com.alibaba.fastjson.JSONObject;
import com.gaoyang.marketing.maccbasecore.facade.AccountBalanceBizServiceFacade;
import com.gaoyang.marketing.maccbasecore.facade.AccountBizServiceFacade;
import com.gaoyang.marketing.maccbasecore.facade.constant.AccBalanceConst;
import com.gaoyang.marketing.maccbasecore.facade.constant.AccountConst;
import com.gaoyang.marketing.maccbasecore.facade.model.AccExtEntity;
import com.gaoyang.marketing.maccbasecore.facade.model.ResponseData;
import com.gaoyang.marketing.maccbasecore.facade.model.request.QueryAccountBalanceData;
import com.gaoyang.marketing.maccbasecore.facade.model.request.SpecialChargeAccountData;
import com.gaoyang.marketing.maccbasecore.facade.model.response.OutAccountBalanceModel;
import com.gaoyang.marketing.maccbasecore.facade.model.response.OutSpecialChargeAccountModel;
import com.gaoyang.marketing.mfbizweb.bean.BusiMonitorLoggerBean;
import com.gaoyang.marketing.mfbizweb.bean.HttpResponseBean;
import com.gaoyang.marketing.mfbizweb.mq.exchangeVoucher.ExchangeVoucherExecute;
import com.gaoyang.marketing.mfbizweb.util.*;
import com.gaoyang.marketing.mfbizweb.util.acm.MfbizWebAcmClient;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;


/**
 * @author zhanghui
 * @create 2018-12-5
 * @description 联名卡新人大礼包 特殊记账 券核销ser
 */
@Service
public class LaXinExchangeVoucherService {
    public static final Logger logger = LoggerFactory.getLogger(LaXinExchangeVoucherService.class);
    private final Logger busiMonitorLogger = LoggerFactory.getLogger("busi-monitor");
    @Resource(name = "accountBalanceBizServiceFacade")
    AccountBalanceBizServiceFacade accountBalanceBizServiceFacade;

    @Resource(name = "accountBizServiceFacade")
    AccountBizServiceFacade accountBizServiceFacade;

    @Resource(name = "exchangeVoucherExecute")
    ExchangeVoucherExecute exchangeVoucherExecute;
    @Autowired
    MfbizWebAcmClient mfbizWebAcmClient;
    public HttpResponseBean laXinExchangeVoucher(String voucherId, String uid, String userId, String memberId, String tag) {
        String traceId = TraceIdUtil.getTraceId();
        HttpResponseBean httpResponseBean = new HttpResponseBean();
        //1、根据券id+uid拼接tradeId查询是否特殊记账,重复记账返回
        //2、特殊记账并发送事物消息券核销
        try {

            //1、根据voucherId+uid拼接tradeId查询是否特殊记账,重复记账返回
            String tradeId = voucherId;
            ResponseData<OutAccountBalanceModel> queryAccBalanceResponseData = queryAccBalanceByCondition(userId, tradeId);

            //返回为空or返回非成功且非结果集为空
            if (PublicUtil.isEmpty(queryAccBalanceResponseData)) {
                httpResponseBean.setCode(ResponseEnums.TYPE_INTERFACE_EXCEPYION.getCode());
                httpResponseBean.setMsg(ResponseEnums.TYPE_INTERFACE_EXCEPYION.getDesc());
                logger.warn("调用账户数据平台接口queryAccBalanceByCondition返回对象为空,traceId={},userId={},uid={}", traceId, userId, uid);
                return httpResponseBean;
            }

            //返回成功，即已存在特殊记账
            if (ResponseEnums.RPC_TYPE_SUCCESS.getCode().equals(queryAccBalanceResponseData.getCode())) {
                httpResponseBean.setCode(ResponseEnums.TYPE_CHARGE_ACCOUNT_MORE.getCode());
                httpResponseBean.setMsg(ResponseEnums.TYPE_CHARGE_ACCOUNT_MORE.getDesc());
                logger.warn("调用账户数据平台接口queryAccBalanceByCondition返回查询成功，说明已经特殊记账,traceId={},userId={},uid={}", traceId, userId, uid);
                return httpResponseBean;

            } else if (ResponseEnums.RPC_TYPE_QUERYACCBYCONDITION_ISNULL.getCode().equals(queryAccBalanceResponseData.getCode())) {
                //结果集为空未进行特殊记账

                String  laxinchannel="";
                String tradeAmount = "0";
                if (StringUtils.isBlank(laxinchannel)) {
                    laxinchannel = mfbizWebAcmClient.getPorpertiesValue("laxinchannel");
                    if (StringUtils.isBlank(laxinchannel) || "null".equals(laxinchannel)) {
                        laxinchannel = "cmcclaxin";
                    }
                }
                if (laxinchannel.contains(tag)) {
                    tradeAmount = Constants.CMCC_LAXIN_FLOW;
                }
                //进行特殊记账，记账成功后发送核销消息MQ
                Boolean isExchangeVoucher = exchangeVoucherExecute.sendMsg(userId, uid, memberId, tradeAmount, voucherId, traceId);
                if (isExchangeVoucher) {
                    httpResponseBean.setCode(ResponseEnums.TYPE_RES_SUCCESS.getCode());
                    httpResponseBean.setMsg(ResponseEnums.TYPE_RES_SUCCESS.getDesc());
                    if (logger.isInfoEnabled()) {
                        logger.info("联名卡新人大礼包特殊记账券核销成功,traceId={},userId={},uid={}", traceId, userId, uid);
                    }
                } else {
                    httpResponseBean.setCode(ResponseEnums.TYPE_CHARGE_ACCOUNT_FAIL.getCode());
                    httpResponseBean.setMsg(ResponseEnums.TYPE_CHARGE_ACCOUNT_FAIL.getDesc());

                    logger.warn("联名卡新人大礼包特殊记账券核销失败,traceId={},userId={},uid={}", traceId, userId, uid);

                }


                return httpResponseBean;
            } else {
                httpResponseBean.setCode(ResponseEnums.TYPE_INTERFACE_EXCEPYION.getCode());
                httpResponseBean.setMsg(ResponseEnums.TYPE_INTERFACE_EXCEPYION.getDesc());
                logger.warn("调用账户数据平台接口queryAccBalanceByCondition返回code,非成功非已特殊记账,traceId={},userId={},uid={}", traceId, userId, uid);
                return httpResponseBean;

            }

        } catch (Exception e) {

            BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("mfbizweb");
            busiMonitorLoggerBean.setProvider("maccbasecore");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("新人大礼包拉新");
            busiMonitorLoggerBean.setError_desc("新人大礼包拉新特殊记账异常");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);
            httpResponseBean.setCode(ResponseEnums.TYPE_USERINFO_EXCEPYION.getCode());
            httpResponseBean.setMsg(ResponseEnums.TYPE_USERINFO_EXCEPYION.getDesc());

            logger.error("traceId={},uid={},userId={},联名卡新人大礼包特殊记账券核销异常e={}",
                    traceId, uid, userId, LogExceptionWapper.getStackTrace(e));
            return httpResponseBean;
        }

    }

    /**
     * @param userId
     * @param tradeId
     * @return
     * @desc 查询用户是否有过特殊记账
     */
    public ResponseData<OutAccountBalanceModel> queryAccBalanceByCondition(String userId, String tradeId) {
        String traceId = TraceIdUtil.getTraceId();

        try {
            QueryAccountBalanceData queryAccountBalanceData =
                    new QueryAccountBalanceData();
            queryAccountBalanceData.setUserId(userId);
            queryAccountBalanceData.setTradeId(tradeId);
            queryAccountBalanceData.setTradeType(AccBalanceConst.ACC_BALANCE_TRADE_TYPE_IN);
            queryAccountBalanceData.setTradeSource(AccBalanceConst.ACC_BALANCE_TRADE_SOURCE_INTREFACE);
            if (logger.isInfoEnabled()) {
                logger.info("调用帐户数据平台接口queryAccBalanceByCondition,traceId={},userId={},传入实体queryAccountBalanceData={}",
                        traceId, userId, JSONObject.toJSONString(queryAccountBalanceData));
            }
            ResponseData<OutAccountBalanceModel> responseData = accountBalanceBizServiceFacade.queryAccBalanceByCondition(queryAccountBalanceData);
            if (PublicUtil.isEmpty(responseData)) {
                BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
                busiMonitorLoggerBean.setConsumer("mfbizweb");
                busiMonitorLoggerBean.setProvider("maccbasecore");
                busiMonitorLoggerBean.setResult("F");
                busiMonitorLoggerBean.setBusiness("新人大礼包拉新查询帐户记录");
                busiMonitorLoggerBean.setError_desc("新人大礼包拉新查询账户记录返回为空");
                busiMonitorLoggerBean.setTrace_id(traceId);
                busiMonitorLogger.warn(busiMonitorLoggerBean);
                logger.warn("调用帐户数据平台接口queryAccBalanceByCondition返回结果为空，查询失败, traceId={},userId={}", traceId, userId);
                return null;
            }
            if (logger.isInfoEnabled()) {
                logger.info("调用帐户数据平台接口queryAccBalanceByCondition返回结果,traceId={},userId={},返回={}",
                        traceId, userId, JSONObject.toJSONString(responseData));
            }

            return responseData;
        } catch (Exception e) {
            BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("mfbizweb");
            busiMonitorLoggerBean.setProvider("maccbasecore");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("新人大礼包拉新查询特殊记账");
            busiMonitorLoggerBean.setError_desc("新人大礼包拉新查询特殊记账异常");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);
            logger.error("traceId={},userId={},帐户数据平台接口queryAccBalanceByConditiondt异常e={}",
                    traceId, userId, LogExceptionWapper.getStackTrace(e));

        }
        return null;
    }

    /**
     * @param userId
     * @param tradeId
     * @return
     * @desc 拉新用户特殊记账
     */
    public ResponseData<OutSpecialChargeAccountModel> specialChargeAccount(String userId, String uid, String memberId,
                                                                           String tradeId, String tradeFlowAmount) {
        String traceId = TraceIdUtil.getTraceId();

        try {
            SpecialChargeAccountData specialChargeAccountData = new SpecialChargeAccountData();
            specialChargeAccountData.setTradeTime(DateUtil.formatDate(new Date()));
            specialChargeAccountData.setTradeId(tradeId);
            specialChargeAccountData.setTradeFlowAmount(tradeFlowAmount);
            specialChargeAccountData.setUserId(userId);
            AccExtEntity accExtEntity = new AccExtEntity();
            accExtEntity.setZfbUid(uid)
                    .setGyMemberId(memberId)
                    .setSceneId(AccountConst.VOURCHER_SPECIAL_CHARGE_ACC);
            String extMsg = accExtEntity.encode();
            specialChargeAccountData.setExtMsg(extMsg);

            if (logger.isInfoEnabled()) {
                logger.info("调用帐户数据平台接口特殊记账specialChargeAccount,traceId={},userId={},uid={},传入实体specialChargeAccountData={}",
                        traceId, userId, uid, JSONObject.toJSONString(specialChargeAccountData));
            }
            ResponseData<OutSpecialChargeAccountModel> responseData = accountBizServiceFacade.specialChargeAccount(specialChargeAccountData);
            if (PublicUtil.isEmpty(responseData)) {
                BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
                busiMonitorLoggerBean.setConsumer("mfbizweb");
                busiMonitorLoggerBean.setProvider("maccbasecore");
                busiMonitorLoggerBean.setResult("F");
                busiMonitorLoggerBean.setBusiness("新人大礼包拉新特殊记账");
                busiMonitorLoggerBean.setError_desc("新人大礼包拉新特殊记账返回实体为空");
                busiMonitorLoggerBean.setTrace_id(traceId);
                busiMonitorLogger.warn(busiMonitorLoggerBean);

                logger.warn("调用帐户数据平台接口特殊记账specialChargeAccount返回结果为空，查询失败, traceId={},userId={}", traceId, userId);
                return null;
            }
            if (logger.isInfoEnabled()) {
                logger.info("调用帐户数据平台接口特殊记账specialChargeAccount,traceId={},userId={},uid={},返回结果responseData={}",
                        traceId, userId, uid, JSONObject.toJSONString(responseData));
            }
            return responseData;
        } catch (Exception e) {
            BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("mfbizweb");
            busiMonitorLoggerBean.setProvider("maccbasecore");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("新人大礼包拉新特殊记账");
            busiMonitorLoggerBean.setError_desc("新人大礼包拉新特殊记账异常");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);
            logger.error("traceId={},userId={},uid={},帐户数据平台接口特殊记账specialChargeAccount异常e={}",
                    traceId, userId, uid, LogExceptionWapper.getStackTrace(e));

        }
        return null;
    }
}
