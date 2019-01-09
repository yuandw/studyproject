package com.gaoyang.marketing.mfbizweb.mq.exchangeVoucher;/**
 * Created by zhanghui on 2018-12-5.
 */

import com.alibaba.dubbo.rpc.proxy.TraceIdUtil;
import com.alibaba.fastjson.JSONObject;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.transaction.LocalTransactionChecker;
import com.aliyun.openservices.ons.api.transaction.TransactionStatus;
import com.gaoyang.marketing.maccbasecore.facade.model.ResponseData;
import com.gaoyang.marketing.maccbasecore.facade.model.response.OutAccountBalanceModel;
import com.gaoyang.marketing.mfbizweb.bean.BusiMonitorLoggerBean;
import com.gaoyang.marketing.mfbizweb.service.LaXinExchangeVoucherService;
import com.gaoyang.marketing.mfbizweb.util.LogExceptionWapper;
import com.gaoyang.marketing.mfbizweb.util.PublicUtil;
import com.gaoyang.marketing.mfbizweb.util.ResponseEnums;
import com.gaoyang.marketing.protocol.ExchangeVoucherProtocol;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author zhanghui
 * @create 2018-12-5
 * @description 特殊记账发送核销券事务消息
 */
@Component(value = "exchangeVoucherChecker")
public class ExchangeVoucherChecker implements LocalTransactionChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeVoucherChecker.class);
    private final Logger busiMonitorLogger = LoggerFactory.getLogger("busi-monitor");
    @Autowired
    LaXinExchangeVoucherService laXinExchangeVoucherService;

    @Value("${rocketmq.env}")
    String msgQueueEnv;

    @Override
    public TransactionStatus check(Message msg) {
        String message = new String(msg.getBody());
        ExchangeVoucherProtocol protocol = new ExchangeVoucherProtocol(msgQueueEnv);
        protocol.decode(message);
        // 解析参数
        String uid = protocol.getUid();
        String userId = protocol.getUserId();
        String traceId = protocol.getTraceId();
        String msgId = msg.getMsgID();
        String tradeId = protocol.getTradeId();
        TraceIdUtil.setTraceId(traceId);
        try {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("traceId={},msgId={},userId={},uid={},trade={},特殊记账发送核销卡券回查开始",
                        traceId, msgId, userId, uid, tradeId);
            }
            ResponseData<OutAccountBalanceModel> responseData = laXinExchangeVoucherService.queryAccBalanceByCondition(userId, tradeId);
            if (PublicUtil.isEmpty(responseData)) {
                LOGGER.warn("查询拉新特殊记账返回实体为空,回滚事务消息,traceId={},uid={},userId={},tradeId={}", traceId, uid, userId,tradeId);
                return TransactionStatus.Unknow;
            }
            //返回成功 说明已经特殊记账
            if (ResponseEnums.RPC_TYPE_SUCCESS.getCode().equals(responseData.getCode())) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("traceId={},msgId={},userId={},uid={},trade={},特殊记账发送核销卡券回查成功,提交事务消息",
                            traceId, msgId, userId, uid, tradeId);
                }
                return TransactionStatus.CommitTransaction;

            } else if (ResponseEnums.RPC_TYPE_QUERYACCBYCONDITION_ISNULL.getCode().equals(responseData.getCode())) {
                //结果集为空情况 特殊记账失败 回滚消息
                LOGGER.warn("查询拉新特殊记账结果集为空,回滚事务消息,traceId={},uid={},userId={},tradeId={},responseData={}",
                        traceId, uid, userId,tradeId, JSONObject.toJSONString(responseData));
                return TransactionStatus.RollbackTransaction;
            } else {
                LOGGER.warn("查询拉新特殊记账失败,回滚事务消息,traceId={},uid={},userId={},tradeId={},responseData={}",
                        traceId, uid, userId, tradeId,JSONObject.toJSONString(responseData));
                return TransactionStatus.Unknow;
            }
        } catch (Exception e) {
            BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("mfbizweb");
            busiMonitorLoggerBean.setProvider("maccbasecore");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("新人大礼包拉新特殊记账");
            busiMonitorLoggerBean.setError_desc("新人大礼包拉新特殊记账失败");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);
            LOGGER.warn("traceId={},msgId={},uid={},userId={},拉新特殊记账核销卡券异常失败e={}",
                    traceId, msgId, uid, userId, LogExceptionWapper.getStackTrace(e));
            return TransactionStatus.Unknow;
        }

    }
}
