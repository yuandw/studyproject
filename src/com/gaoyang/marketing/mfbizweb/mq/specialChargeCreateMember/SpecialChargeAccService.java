package com.gaoyang.marketing.mfbizweb.mq.specialChargeCreateMember;/**
 * Created by zhanghui on 2018-10-29.
 */

import com.alibaba.fastjson.JSON;
import com.aliyun.openservices.ons.api.Message;
import com.aliyun.openservices.ons.api.transaction.LocalTransactionExecuter;
import com.aliyun.openservices.ons.api.transaction.TransactionStatus;
import com.gaoyang.marketing.constant.MsgEnvConst;
import com.gaoyang.marketing.maccbasecore.facade.model.AccExtEntity;
import com.gaoyang.marketing.mfbizweb.bean.BusiMonitorLoggerBean;
import com.gaoyang.marketing.mfbizweb.service.baseService.CreateGyMemeberService;
import com.gaoyang.marketing.mfbizweb.util.LogExceptionWapper;
import com.gaoyang.marketing.mfbizweb.util.PublicUtil;
import com.gaoyang.marketing.mfbizweb.util.ResponseEnums;
import com.gaoyang.marketing.mfbizweb.util.acm.MfbizWebAcmClient;
import com.gaoyang.marketing.muserbasecore.membercard.facade.model.response.CreateMemberCardModel;
import com.gaoyang.marketing.muserbasecore.userinfo.facade.model.ResponseData;
import com.gaoyang.marketing.protocol.SpecialChargeAccProtocol;
import com.gaoyang.marketing.rocketmq.producer.TransactionProducerAgent;
import com.gaoyang.marketing.util.MsgProtocolConstUtil;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;


/**
 * @author zhanghui
 * @create 2018-10-29
 * @description 创建会员卡送1g流量业务执行器
 */
@Component
public class SpecialChargeAccService implements LocalTransactionExecuter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpecialChargeAccService.class);
    private  final Logger busiMonitorLogger = LoggerFactory.getLogger("busi-monitor");

    @Autowired
    TransactionProducerAgent transactionProducerAgent;
    @Value("${rocketmq.env}")
    String msgQueueEnv;
    @Autowired
    CreateGyMemeberService  createGyMemeberService;

    @Autowired
    MfbizWebAcmClient mfbizWebAcmClient;

    @Resource(name = "specialChargeAccChecker")
    SpecialChargeAccChecker specialChargeAccChecker;
    @PostConstruct
    public void init() {
        // 初始化领取会员卡赠送流量提供者
        transactionProducerAgent.init(
                specialChargeAccChecker,
                MsgProtocolConstUtil.protocol(msgQueueEnv, MsgEnvConst.SPECIAL_CHARGE_PROTOCOL).getProducerId()).start();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[用户领卡赠送流量服务]-领取会员卡完成事务消息[生产者]specialChargeAccProducerAgent初始化完成,当前环境为-{}", msgQueueEnv);
        }
    }
    @Override
    public TransactionStatus execute(Message msg, Object o) {
        String message = new String(msg.getBody());
        SpecialChargeAccProtocol protocol = new SpecialChargeAccProtocol(msgQueueEnv);
        protocol.decode(message);
        return null;
    }

    public  List<CreateMemberCardModel> sendMsg(String uid, String accessToken, String userId,String traceId,String sendFlow,String sourceId){

        SpecialChargeAccProtocol protocol = new SpecialChargeAccProtocol(msgQueueEnv);
        String tradeId="SPECIAL_"+userId;
      /*  String remark=mfbizWebAcmClient.getPorpertiesValue("createMemberRemark");
        if (StringUtils.isBlank(remark)||"null".equals(remark)){
            remark="领卡赠送";
        }*/

        AccExtEntity accExtEntity = new AccExtEntity();
        accExtEntity.setGyMemberId(null);
        accExtEntity.setZfbUid(uid);
        accExtEntity.setSceneId(null);
        String remark =accExtEntity.encode();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[用户领卡赠送流量服务]-sendMsg,uid={},userId={},traceId={},tradeId={},remark={}", uid,userId,traceId,tradeId,sendFlow);
        }
        protocol.setRemark(remark);
        protocol.setUid(uid);
        protocol.setUserId(userId);
        protocol.setTraceId(traceId);
        protocol.setTradeId(tradeId);
        protocol.setTradeflowAmount(sendFlow);
        String sendMsg=protocol.encode();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[用户领卡赠送流量服务]-sendMsg,uid={},userId={},traceId={},tradeId={},sendFlow={}", uid,userId,traceId,tradeId,sendFlow);
        }
        List<CreateMemberCardModel> createMemberCardModels = new ArrayList<>();
        try {
            transactionProducerAgent
                    .send(
                            new Message(
                                    MsgProtocolConstUtil.protocol(msgQueueEnv, MsgEnvConst.SPECIAL_CHARGE_PROTOCOL).getMsgTopic(),
                                    MsgProtocolConstUtil.protocol(msgQueueEnv, MsgEnvConst.SPECIAL_CHARGE_PROTOCOL).getMsgTag(),
                                    MsgProtocolConstUtil.protocol(msgQueueEnv, MsgEnvConst.SPECIAL_CHARGE_PROTOCOL).getProducerId(),
                    sendMsg.getBytes("UTF-8")),(msg, arg)->{
                ResponseData<CreateMemberCardModel>  responseData= createGyMemeberService.createMemberCardAddMsiId(uid,accessToken,userId,sourceId);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("领取会员卡createMemberCard返回,traceId={},uid={},userId={},responseData={}",traceId, uid,userId, JSON.toJSONString(responseData));
                }
                if(PublicUtil.isEmpty(responseData)
                        ||PublicUtil.isEmpty(responseData.getObj())||PublicUtil.isEmpty(responseData.getObj().getGyMemberId())|| "-1".equals(responseData.getObj().getGyMemberId())){

                    BusiMonitorLoggerBean busiMonitorLoggerBean =new BusiMonitorLoggerBean();
                    busiMonitorLoggerBean.setConsumer("mfbizweb");
                    busiMonitorLoggerBean.setProvider("muserbasecore");
                    busiMonitorLoggerBean.setResult("F");
                    busiMonitorLoggerBean.setBusiness("领卡");
                    busiMonitorLoggerBean.setError_desc("领取会员卡返回实体或返回会员卡号为空");
                    busiMonitorLoggerBean.setTrace_id(traceId);
                    busiMonitorLogger.warn(busiMonitorLoggerBean);

                    LOGGER.warn("领取会员卡异常返回实体或返回会员卡号为空,createMemberCard未知,消息处理状态unkonw,traceId={},uid={},userId={}", traceId, uid,userId);

                    return TransactionStatus.Unknow;
                }else if(!ResponseEnums.RPC_TYPE_SUCCESS.getCode().equals(responseData.getCode())){

                    BusiMonitorLoggerBean busiMonitorLoggerBean =new BusiMonitorLoggerBean();
                    busiMonitorLoggerBean.setConsumer("mfbizweb");
                    busiMonitorLoggerBean.setProvider("muserbasecore");
                    busiMonitorLoggerBean.setResult("F");
                    busiMonitorLoggerBean.setBusiness("领卡");
                    busiMonitorLoggerBean.setError_desc("领取会员卡失败");
                    busiMonitorLoggerBean.setTrace_id(traceId);
                    busiMonitorLogger.warn(busiMonitorLoggerBean);
                    LOGGER.warn("领取会员卡失败,createMemberCard,消息处理状态回滚traceId={},uid={},userId={}", traceId, uid,userId);

                    return TransactionStatus.RollbackTransaction;
                }else {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("领取会员卡成功,createMemberCard,消息处理状态成功,traceId={},uid={},userId={}", traceId, uid,userId);
                    }
                    createMemberCardModels.add(responseData.getObj());
                    return TransactionStatus.CommitTransaction;
                }
                },null);

               }catch (Exception e){

                BusiMonitorLoggerBean busiMonitorLoggerBean =new BusiMonitorLoggerBean();
                busiMonitorLoggerBean.setConsumer("mfbizweb");
                busiMonitorLoggerBean.setProvider("muserbasecore");
                busiMonitorLoggerBean.setResult("F");
                busiMonitorLoggerBean.setBusiness("领卡");
                busiMonitorLoggerBean.setError_desc("领卡异常");
                busiMonitorLoggerBean.setTrace_id(traceId);
                busiMonitorLogger.warn(busiMonitorLoggerBean);
                LOGGER.error("领取会员卡失败异常,traceId={},uid={},userId={},异常={}",  traceId, uid,userId, LogExceptionWapper.getStackTrace(e));
            return null;
        }

            return createMemberCardModels;

    }
}
