package com.gaoyang.marketing.mfbizweb.mq.exchangeVoucher;/**
 * Created by zhanghui on 2018-12-5.
 */

import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.transaction.LocalTransactionExecuter;
import com.aliyun.openservices.ons.api.transaction.TransactionStatus;
import com.gaoyang.marketing.constant.MsgEnvConst;
import com.gaoyang.marketing.maccbasecore.facade.model.ResponseData;
import com.gaoyang.marketing.maccbasecore.facade.model.response.OutSpecialChargeAccountModel;
import com.gaoyang.marketing.mfbizweb.bean.BusiMonitorLoggerBean;
import com.gaoyang.marketing.mfbizweb.service.LaXinExchangeVoucherService;
import com.gaoyang.marketing.mfbizweb.util.LogExceptionWapper;
import com.gaoyang.marketing.mfbizweb.util.PublicUtil;
import com.gaoyang.marketing.mfbizweb.util.ResponseEnums;
import com.gaoyang.marketing.protocol.ExchangeVoucherProtocol;
import com.gaoyang.marketing.rocketmq.producer.TransactionProducerAgent;
import com.gaoyang.marketing.util.MsgProtocolConstUtil;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author zhanghui
 * @create 2018-12-5
 * @description
 */
@Component
public class ExchangeVoucherExecute implements LocalTransactionExecuter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExchangeVoucherExecute.class);
    private final Logger busiMonitorLogger = LoggerFactory.getLogger("busi-monitor");
    @Value("${rocketmq.env}")
    String msgQueueEnv;
    @Autowired
    TransactionProducerAgent transactionProducerAgent;
    @Autowired
    LaXinExchangeVoucherService laXinExchangeVoucherService;
    @Resource(name = "exchangeVoucherChecker")
    ExchangeVoucherChecker exchangeVoucherChecker;

    @PostConstruct
    public void init() {
        // 初始化领取会员卡赠送流量提供者
        transactionProducerAgent.init(
                exchangeVoucherChecker,
                MsgProtocolConstUtil.protocol(msgQueueEnv, MsgEnvConst.EXCHANGE_VOUCHER_PROTOCOL).getProducerId()).start();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[用户特殊记账并核销卡券服务]-卡券核销事务消息[生产者]specialChargeAccProducerAgent初始化完成,当前环境为-{}", msgQueueEnv);
        }

    }

    @Override
    public TransactionStatus execute(Message msg, Object arg) {
        String message = new String(msg.getBody());
        ExchangeVoucherProtocol protocol = new ExchangeVoucherProtocol(msgQueueEnv);
        protocol.decode(message);
        return null;
    }

    public boolean sendMsg(String userId, String uid, String memberId,
                           String tradeFlowAmount, String voucherId, String traceId) {
        String tradeId = voucherId;
        boolean[] specialChargeAccountRes = new boolean[1];
        ExchangeVoucherProtocol protocol = new ExchangeVoucherProtocol(msgQueueEnv);
        protocol.setTradeId(tradeId);
        protocol.setUid(uid);
        protocol.setVoucherId(voucherId);
        protocol.setTraceId(traceId);
        protocol.setUserId(userId);
        String sendMsg = protocol.encode();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[用户特殊记账并核销卡券服务]-sendMsg,uid={},userId={},traceId={},tradeId={}", uid, userId, traceId, tradeId);
        }

        try {
            transactionProducerAgent
                    .send(
                            new Message(
                                    MsgProtocolConstUtil.protocol(msgQueueEnv, MsgEnvConst.EXCHANGE_VOUCHER_PROTOCOL).getMsgTopic(),
                                    MsgProtocolConstUtil.protocol(msgQueueEnv, MsgEnvConst.EXCHANGE_VOUCHER_PROTOCOL).getMsgTag(),
                                    MsgProtocolConstUtil.protocol(msgQueueEnv, MsgEnvConst.EXCHANGE_VOUCHER_PROTOCOL).getProducerId(),
                                    sendMsg.getBytes("UTF-8")), (msg, arg) -> {
                                ResponseData<OutSpecialChargeAccountModel> responseData = laXinExchangeVoucherService.specialChargeAccount(userId, uid, memberId, tradeId, tradeFlowAmount);
                                if (LOGGER.isInfoEnabled()) {
                                    LOGGER.info("拉新特殊记账返回,traceId={},uid={},userId={},responseData={}", traceId, uid, userId, JSON.toJSONString(responseData));
                                }
                                if (PublicUtil.isEmpty(responseData)) {
                                    LOGGER.warn("拉新特殊记账返回实体为空,specialChargeAccount未知,消息处理状态unkonw,traceId={},uid={},userId={}", traceId, uid, userId);
                                    specialChargeAccountRes[0] = false;
                                    return TransactionStatus.Unknow;
                                } else if (ResponseEnums.RPC_TYPE_SUCCESS.getCode().equals(responseData.getCode())) {

                                    if (LOGGER.isInfoEnabled()) {
                                        LOGGER.info("拉新特殊记账成功,specialChargeAccount消息处理状态成功,traceId={},uid={},userId={}", traceId, uid, userId);
                                    }
                                    specialChargeAccountRes[0] = true;
                                    return TransactionStatus.CommitTransaction;


                                } else {
                                    BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
                                    busiMonitorLoggerBean.setConsumer("mfbizweb");
                                    busiMonitorLoggerBean.setProvider("maccbasecore");
                                    busiMonitorLoggerBean.setResult("F");
                                    busiMonitorLoggerBean.setBusiness("新人大礼包拉新特殊记账");
                                    busiMonitorLoggerBean.setError_desc("新人大礼包拉新特殊记账失败");
                                    busiMonitorLoggerBean.setTrace_id(traceId);
                                    busiMonitorLogger.warn(busiMonitorLoggerBean);

                                    LOGGER.warn("拉新特殊记账失败,specialChargeAccount未知,消息处理状态回滚traceId={},uid={},userId={}", traceId, uid, userId);
                                    specialChargeAccountRes[0] = false;
                                    return TransactionStatus.RollbackTransaction;
                                }
                            }, null);

        } catch (Exception e) {
            BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("mfbizweb");
            busiMonitorLoggerBean.setProvider("maccbasecore");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("新人大礼包拉新特殊记账");
            busiMonitorLoggerBean.setError_desc("新人大礼包拉新特殊记账发送卡券核销消息异常");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);

            LOGGER.error("用户特殊记账并核销卡券服务异常,traceId={},uid={},userId={},异常={}", traceId, uid, userId, LogExceptionWapper.getStackTrace(e));
            return false;
        }

        return specialChargeAccountRes[0];


    }
}
