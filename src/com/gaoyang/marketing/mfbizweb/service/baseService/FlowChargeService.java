package com.gaoyang.marketing.mfbizweb.service.baseService;/**
 * Created by zhanghui on 2018-9-27.
 */

import com.alibaba.dubbo.rpc.proxy.TraceIdUtil;
import com.alibaba.fastjson.JSONObject;
import com.gaoyang.marketing.mfbizcore.facade.UserBizServiceFacade;
import com.gaoyang.marketing.mfbizcore.facade.model.ResponseData;
import com.gaoyang.marketing.mfbizcore.facade.model.request.ConvertFlowData;
import com.gaoyang.marketing.mfbizcore.facade.model.response.ConvertFlowModel;
import com.gaoyang.marketing.mfbizweb.bean.BusiMonitorLoggerBean;
import com.gaoyang.marketing.mfbizweb.bean.HttpResponseBean;
import com.gaoyang.marketing.mfbizweb.util.LogExceptionWapper;
import com.gaoyang.marketing.mfbizweb.util.PublicUtil;
import com.gaoyang.marketing.mfbizweb.util.ResponseEnums;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhanghui
 * @create 2018-9-27
 * @description 兑换流量service
 */
@Service
public class FlowChargeService {
    public final Logger logger = LoggerFactory.getLogger(this.getClass());
    private  final Logger busiMonitorLogger = LoggerFactory.getLogger("busi-monitor");

    @Resource(name="userBizServiceFacade")
    UserBizServiceFacade userBizServiceFacade;
    @Autowired
    QueryUserAuthService queryUserAuthService;
    public HttpResponseBean flowCharge(String uid,String userId,String poiId,String iphone){
        HttpResponseBean responseBean=new HttpResponseBean();
        Map map=new HashMap<>();
        String traceId= TraceIdUtil.getTraceId();
        if (logger.isInfoEnabled()) {
            logger.info("兑换流量service, traceId={}, uid={},userId={},poiId={},iphone={}", traceId, uid, userId, poiId, iphone);
        }
        ConvertFlowData convertFlowData=new ConvertFlowData();
        convertFlowData.setZfbUserId(uid);
        convertFlowData.setUserId(userId);
        convertFlowData.setPhoneNum(iphone);
        convertFlowData.setPoiId(poiId);

        ResponseData<ConvertFlowModel> responseData= convertFlow(convertFlowData);
        if(PublicUtil.isEmpty(responseData)){
            logger.warn("兑换流量service调用核心业务平台convertFlow异常，返回实体为空, traceId={}, uid={},userId={},iphone={},poiId={},code={}", traceId, uid, userId,iphone,poiId);
            responseBean.setCode(ResponseEnums.TYPE_SUCCESS.getCode());
            map.put("tradeStatus",ResponseEnums.TYPE_FLOWCHARGE_FAIL.getCode());
            map.put("bizMsg",ResponseEnums.TYPE_FLOWCHARGE_FAIL.getDesc());
            responseBean.setData(map);
            return responseBean;
        }
        String code=responseData.getCode();
        if(PublicUtil.isEmpty(code)||ResponseEnums.RPC_TYPE_PRODUCT_FAIL.getCode().equals(code)||
                ResponseEnums.RPC_TYPE_TRADE_CREATE_FAIL.getCode().equals(code)||
                ResponseEnums.RPC_TYPE_ACC_QUERY_FAIL.getCode().equals(code)||
                ResponseEnums.RPC_TYPE_CONVERTFLOW_FAIL.getCode().equals(code)){

            BusiMonitorLoggerBean busiMonitorLoggerBean =new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("mfbizweb");
            busiMonitorLoggerBean.setProvider("mfbizcore");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("兑换流量");
            busiMonitorLoggerBean.setError_desc("兑换流量失败");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);

            logger.warn("兑换流量service调用核心业务平台convertFlow失败, traceId={}, uid={},userId={},iphone={},poiId={},code={}", traceId, uid, userId,iphone,poiId,code);
            responseBean.setCode(ResponseEnums.TYPE_SUCCESS.getCode());
            map.put("tradeStatus",ResponseEnums.TYPE_FLOWCHARGE_FAIL.getCode());
            map.put("bizMsg",ResponseEnums.TYPE_FLOWCHARGE_FAIL.getDesc());
            responseBean.setData(map);
            return responseBean;
        }
        //账户余额不足 RPC_ACC_BALANCE_NOT_ENG
        if(ResponseEnums.RPC_ACC_BALANCE_NOT_ENG.getCode().equals(code)){
            logger.warn("兑换流量service调用核心业务平台convertFlow返回账户余额不足, traceId={}, uid={},userId={},iphone={},poiId={},code={}", traceId, uid, userId,iphone,poiId,code);
            responseBean.setCode(ResponseEnums.TYPE_SUCCESS.getCode());
            map.put("tradeStatus",ResponseEnums.TYPE_FLOWCHARGENOTENOUGH_FAIL.getCode());
            map.put("bizMsg",ResponseEnums.TYPE_FLOWCHARGENOTENOUGH_FAIL.getDesc());
            responseBean.setData(map);
            return responseBean;
        }
        //本月兑换超过10g RPC_TYPE_EXCHANGE_OUT_LIMIT
        if(ResponseEnums.RPC_TYPE_EXCHANGE_OUT_LIMIT.getCode().equals(code)){
            logger.warn("兑换流量service调用核心业务平台convertFlow返回本月兑换超过10G, traceId={}, uid={},userId={},iphone={},poiId={},code={}", traceId, uid, userId,iphone,poiId,code);
            responseBean.setCode(ResponseEnums.TYPE_SUCCESS.getCode());
            map.put("tradeStatus",ResponseEnums.TYPE_FLOWCHARGE10G_FAIL.getCode());
            map.put("bizMsg",ResponseEnums.TYPE_FLOWCHARGE10G_FAIL.getDesc());
            responseBean.setData(map);
            return responseBean;
        }
        //兑换成功
        if(ResponseEnums.RPC_TYPE_SUCCESS.getCode().equals(code)){
            if (logger.isInfoEnabled()) {
            logger.info("兑换流量service调用核心业务平台convertFlow兑换成功, traceId={}, uid={},userId={},iphone={},poiId={},code={}", traceId, uid, userId,iphone,poiId, code);
            }
            responseBean.setCode(ResponseEnums.TYPE_SUCCESS.getCode());
            map.put("tradeStatus",ResponseEnums.TYPE_FLOWCHARGE_SUCCESS.getCode());
            map.put("bizMsg",ResponseEnums.TYPE_FLOWCHARGE_SUCCESS.getDesc());
            responseBean.setData(map);
            return responseBean;
        }
        return responseBean;
    }

    /**
     * @desc   调用调用核心模块兑换流量
     * @param convertFlowData
     * @return
     */
    public ResponseData<ConvertFlowModel> convertFlow(ConvertFlowData convertFlowData){
        String traceId= TraceIdUtil.getTraceId();
        if (logger.isInfoEnabled()) {
        logger.info("调用核心平台接口convertFlow传入参数, traceId={}, uid={},userId={},iphone={},poiId={}",traceId,convertFlowData.getZfbUserId(),convertFlowData.getUserId(),convertFlowData.getPhoneNum(),convertFlowData.getPoiId());
        }
        try {
            //测试异常日志
          ResponseData<ConvertFlowModel> responseData=userBizServiceFacade.convertFlow(convertFlowData);
            if (logger.isInfoEnabled()) {
            logger.info("调用核心平台接口convertFlow,查询成功,traceId={},uid={},userId={},poiId={},responseData={}", traceId, convertFlowData.getZfbUserId(),convertFlowData.getUserId(),convertFlowData.getPoiId(),JSONObject.toJSONString(responseData));
            }
            //判断接口返回code
            if(PublicUtil.isEmpty(responseData)){

                BusiMonitorLoggerBean busiMonitorLoggerBean =new BusiMonitorLoggerBean();
                busiMonitorLoggerBean.setConsumer("mfbizweb");
                busiMonitorLoggerBean.setProvider("mfbizcore");
                busiMonitorLoggerBean.setResult("F");
                busiMonitorLoggerBean.setBusiness("兑换流量");
                busiMonitorLoggerBean.setError_desc("兑换流量返回对象为空");
                busiMonitorLoggerBean.setTrace_id(traceId);
                busiMonitorLogger.warn(busiMonitorLoggerBean);

                logger.warn("调用核心平台接口convertFlow返回对象为空,查询失败,traceId={},uid={},userId={},iphone={},poiId={}",traceId,convertFlowData.getZfbUserId(),convertFlowData.getUserId(),convertFlowData.getPhoneNum(),convertFlowData.getPoiId());
                return null;
            }

            return responseData;
        } catch (Exception e) {
            BusiMonitorLoggerBean busiMonitorLoggerBean =new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("mfbizweb");
            busiMonitorLoggerBean.setProvider("mfbizcore");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("兑换流量");
            busiMonitorLoggerBean.setError_desc("兑换流量异常");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);

            logger.error("traceId={},uid={},userId={},iphone={},poiId={},调用核心平台接口convertFlow异常e={}"
                    ,traceId,convertFlowData.getZfbUserId(),convertFlowData.getUserId(),convertFlowData.getPhoneNum(),convertFlowData.getPoiId(),LogExceptionWapper.getStackTrace(e));
            return null;
        }

    }


    /**
     * 兑换流量前校验手机号
     * @param uid
     * @param userId
     * @param poiId
     * @param iphone
     * @return
     */
    public HttpResponseBean checkIphone(String uid,String userId,String poiId,String iphone){
        HttpResponseBean responseBean=new HttpResponseBean();
        Map map=new HashMap<>();
        String traceId= TraceIdUtil.getTraceId();
        if (logger.isInfoEnabled()) {
            logger.info("兑换流量校验手机号service, traceId={}, uid={},userId={},poiId={},iphone={}", traceId, uid, userId, poiId, iphone);
        }
        ConvertFlowData reqData=new ConvertFlowData();
        reqData.setZfbUserId(uid);
        reqData.setUserId(userId);
        reqData.setPhoneNum(iphone);
        reqData.setPoiId(poiId);

        ResponseData<ConvertFlowModel> responseData= convertFlow(reqData);
        if(PublicUtil.isEmpty(responseData)){
            logger.warn("兑换流量校验手机号service调用核心业务平台convertFlow异常，返回实体为空, traceId={}, uid={},userId={},iphone={},poiId={},code={}", traceId, uid, userId,iphone,poiId);
            responseBean.setCode(ResponseEnums.TYPE_SUCCESS.getCode());
            map.put("tradeStatus",ResponseEnums.TYPE_FLOWCHARGE_FAIL.getCode());
            map.put("bizMsg",ResponseEnums.TYPE_FLOWCHARGE_FAIL.getDesc());
            responseBean.setData(map);
            return responseBean;
        }
        String code=responseData.getCode();
        if(PublicUtil.isEmpty(code)||ResponseEnums.RPC_TYPE_PRODUCT_FAIL.getCode().equals(code)||
                ResponseEnums.RPC_TYPE_TRADE_CREATE_FAIL.getCode().equals(code)||
                ResponseEnums.RPC_TYPE_ACC_QUERY_FAIL.getCode().equals(code)||
                ResponseEnums.RPC_TYPE_CONVERTFLOW_FAIL.getCode().equals(code)){

            BusiMonitorLoggerBean busiMonitorLoggerBean =new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("mfbizweb");
            busiMonitorLoggerBean.setProvider("mfbizcore");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("兑换流量校验手机号");
            busiMonitorLoggerBean.setError_desc("兑换流量校验手机号失败");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);

            logger.warn("兑换流量校验手机号service调用核心业务平台convertFlow失败, traceId={}, uid={},userId={},iphone={},poiId={},code={}", traceId, uid, userId,iphone,poiId,code);
            responseBean.setCode(ResponseEnums.TYPE_SUCCESS.getCode());
            map.put("tradeStatus",ResponseEnums.TYPE_FLOWCHARGE_FAIL.getCode());
            map.put("bizMsg",ResponseEnums.TYPE_FLOWCHARGE_FAIL.getDesc());
            responseBean.setData(map);
            return responseBean;
        }

        //校验结果
        if(ResponseEnums.RPC_TYPE_SUCCESS.getCode().equals(code)){
            if (logger.isInfoEnabled()) {
                logger.info("兑换流量service调用核心业务平台convertFlow兑换成功, traceId={}, uid={},userId={},iphone={},poiId={},code={}", traceId, uid, userId,iphone,poiId, code);
            }
            responseBean.setCode(ResponseEnums.TYPE_SUCCESS.getCode());
            map.put("tradeStatus",ResponseEnums.TYPE_FLOWCHARGE_SUCCESS.getCode());
            map.put("bizMsg",ResponseEnums.TYPE_FLOWCHARGE_SUCCESS.getDesc());
            responseBean.setData(map);
            return responseBean;
        }
        return responseBean;
    }
}
