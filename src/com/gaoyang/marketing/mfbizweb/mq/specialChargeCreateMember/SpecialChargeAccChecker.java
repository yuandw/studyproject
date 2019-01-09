package com.gaoyang.marketing.mfbizweb.mq.specialChargeCreateMember;/**
 * Created by zhanghui on 2018-10-29.
 */

import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.transaction.LocalTransactionChecker;
import com.aliyun.openservices.ons.api.transaction.TransactionStatus;
import com.gaoyang.marketing.mfbizweb.bean.BusiMonitorLoggerBean;
import com.gaoyang.marketing.mfbizweb.service.baseService.QueryUserAuthService;
import com.gaoyang.marketing.mfbizweb.util.LogExceptionWapper;
import com.gaoyang.marketing.mfbizweb.util.PublicUtil;
import com.gaoyang.marketing.muserbasecore.userinfo.facade.model.response.UserInfoModel;
import com.gaoyang.marketing.protocol.SpecialChargeAccProtocol;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author zhanghui
 * @create 2018-10-29
 * @description  领取会员卡赠送1g回查 领取会员卡是否成功
 */
@Component(value = "specialChargeAccChecker")
public class SpecialChargeAccChecker implements LocalTransactionChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpecialChargeAccChecker.class);
    private  final Logger busiMonitorLogger = LoggerFactory.getLogger("busi-monitor");
    @Autowired
    QueryUserAuthService queryUserAuthService;

    @Value("${rocketmq.env}")
    String msgQueueEnv;
    @Override
    public TransactionStatus check(Message msg) {
        String message = new String(msg.getBody());
        SpecialChargeAccProtocol protocol = new SpecialChargeAccProtocol(msgQueueEnv);

        protocol.decode(message);
        // 解析参数
        String uid = protocol.getUid();
        String userId = protocol.getUserId();
        String traceId = protocol.getTraceId();
        String msgId = msg.getMsgID();

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("traceId={},msgId={},userId={},uid={},领取会员卡specialChargeAccChecker回查开始",
                    traceId, msgId, userId, uid);
        }
        try{
            // 查询创建会员卡是否成功
            UserInfoModel userInfoModel =
                    queryUserAuthService.getUserInfo(uid,"");
            if (userInfoModel == null|| PublicUtil.isEmpty(userInfoModel.getGyMemberId())||"-1".equals(userInfoModel.getGyMemberId())) {

                BusiMonitorLoggerBean busiMonitorLoggerBean =new BusiMonitorLoggerBean();
                busiMonitorLoggerBean.setConsumer("mfbizweb");
                busiMonitorLoggerBean.setProvider("muserbasecore");
                busiMonitorLoggerBean.setResult("F");
                busiMonitorLoggerBean.setBusiness("领卡");
                busiMonitorLoggerBean.setError_desc("领卡未生成高阳会员卡号,回滚赠送流量消息");
                busiMonitorLoggerBean.setTrace_id(traceId);
                busiMonitorLogger.warn(busiMonitorLoggerBean);

                LOGGER.warn("traceId={},msgId={},uid={},userId={},specialChargeAccChecker未生成高阳会员卡号,回滚赠送流量消息,回查用户信息={}",
                        traceId, msgId, uid, userId, JSON.toJSONString(userInfoModel));
                return TransactionStatus.RollbackTransaction;
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("traceId={},msgId={},uid={},userId={},specialChargeAccChecker已生成高阳会员卡号,发送扣款流量消息提交,回查用户信息={}",
                        traceId, msgId, uid, userId, JSON.toJSONString(userInfoModel));
            }
            return TransactionStatus.CommitTransaction;
        }catch(Exception e){
            BusiMonitorLoggerBean busiMonitorLoggerBean =new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("mfbizweb");
            busiMonitorLoggerBean.setProvider("muserbasecore");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("领卡");
            busiMonitorLoggerBean.setError_desc("领卡异常");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);

            LOGGER.warn("traceId={},msgId={},uid={},userId={},调用查询用户信息失败e={}",
                    traceId, msgId, uid, userId, LogExceptionWapper.getStackTrace(e));
            return TransactionStatus.Unknow;
        }




    }
}

