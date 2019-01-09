package com.gaoyang.marketing.mfbizweb.controller.baseController;/**
 * Created by zhanghui on 2018-9-27.
 */

import com.alibaba.dubbo.rpc.proxy.TraceIdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gaoyang.marketing.log.util.LogUtil;
import com.gaoyang.marketing.log.util.MethodCallResultEnum;
import com.gaoyang.marketing.mfbizweb.bean.HttpResponseBean;
import com.gaoyang.marketing.mfbizweb.service.baseService.FlowChargeService;
import com.gaoyang.marketing.mfbizweb.util.PublicUtil;
import com.gaoyang.marketing.mfbizweb.util.ResponseEnums;
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
import java.util.HashMap;
import java.util.Map;

/**
 * @author zhanghui
 * @create 2018-9-27
 * @description 流量兑换controller
 */
@Controller
public class FlowChargeController {
    private static final String CONTROLLER_NAME="[流量兑换controller]";
    private static final String FLOW_CHARGE_FLAG = "flowChargeFlag";        // web后台流量兑换开关
    private static final String FLOW_CHARGE_TEST = "flowChargeTest";        // web后台流量兑换开关

    public final Logger logger = LoggerFactory.getLogger(this.getClass());


    @Autowired
    FlowChargeService flowChargeService;

    @Autowired
    private LogUtil logUtil;

    @Autowired
    MfbizWebAcmClient mfbizWebAcmClient;
    private String flowChargeFlag;

    @RequestMapping(value="/flow_charge",method= RequestMethod.GET)
    @ResponseBody
    public String flowCharge(@RequestParam("poiId") String poiId,@RequestParam("poiFlowConsume") String poiFlowConsume,@RequestParam("poiViewName") String poiViewName,
                             HttpServletRequest request,
                             HttpServletResponse response){
        flowChargeFlag = mfbizWebAcmClient.getPorpertiesValue(FLOW_CHARGE_FLAG);
        if(StringUtils.isBlank(flowChargeFlag)||"null".equals(flowChargeFlag)){
            //默认开通服务
            flowChargeFlag="0";
        }
        long startTime = System.currentTimeMillis();
        String traceId= TraceIdUtil.getTraceId();
        HttpResponseBean responseBean=new HttpResponseBean();
        //20181022 增加流量兑换开关
        if("0".equals(flowChargeFlag)){

            Map<String,String> userInfo=(Map<String, String>)request.getSession().getAttribute("userInfo");
            if(PublicUtil.isEmpty(userInfo)){
                responseBean.setCode(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getCode());
                responseBean.setMsg(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getDesc());
                String returnStr= JSON.toJSONString(responseBean);
                logger.warn(CONTROLLER_NAME+"traceId={},returnStr={}",traceId,returnStr);
                logUtil.log(new Throwable(),CONTROLLER_NAME , null, MethodCallResultEnum.FAIL, "流量兑换controller获取session为空", startTime);
                return returnStr;
            }

            if (logger.isInfoEnabled()) {
                logger.info(CONTROLLER_NAME + "兑换流量开关打开,traceId={},flowChargeFlag={}", traceId, flowChargeFlag);
            }
            String uid=userInfo.get("uid");
            String userId=userInfo.get("userId");
            if (logger.isInfoEnabled()) {
                logger.info(CONTROLLER_NAME + "traceId={},userId={},poiId={}", traceId, userId, poiId);
            }
            //增加测试开关 0 测试
            String flowChargeTest=mfbizWebAcmClient.getPorpertiesValue(FLOW_CHARGE_TEST);
            String iphone="";
            if("0".equals(flowChargeTest)){
                iphone="13810910389";
            }else{
               iphone=userInfo.get("realIphone");
            }
            if (logger.isInfoEnabled()) {
                logger.info(CONTROLLER_NAME + "获取session信息,traceId={},uid={},userId={},iphone={}", traceId, uid, userId, iphone);
            }
            //对session中的用户信息进行校验
            if(PublicUtil.isEmpty(uid)||PublicUtil.isEmpty(userId)||PublicUtil.isEmpty(iphone)){
                responseBean.setCode(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getCode());
                responseBean.setMsg(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getDesc());
                String returnStr= JSON.toJSONString(responseBean);
                logger.warn(CONTROLLER_NAME + "traceId={},returnStr={}", traceId, returnStr);
                logUtil.log(new Throwable(), CONTROLLER_NAME, null, MethodCallResultEnum.FAIL, "流量兑换controller获取session中uid、userid、iphone为空", startTime);
                return returnStr;
            }
            responseBean=flowChargeService.flowCharge(uid,userId,poiId,iphone);
            String returnStr = JSONObject.toJSONString(responseBean);
            if (logger.isInfoEnabled()) {
                logger.info(CONTROLLER_NAME + "traceId={},res={}", traceId, returnStr);
            }
            Map map= (Map) responseBean.getData();
            if(ResponseEnums.TYPE_FLOWCHARGE_SUCCESS.getCode().equals(map.get("tradeStatus"))){
                logUtil.log(new Throwable(),CONTROLLER_NAME , null, MethodCallResultEnum.SUCCESS, "流量兑换成功", startTime);
            }else{
                logUtil.log(new Throwable(),CONTROLLER_NAME , null, MethodCallResultEnum.FAIL, "流量兑换失败", startTime);
            }
            return returnStr;

        }else{
            Map map=new HashMap<>();
            if (logger.isInfoEnabled()) {
                logger.info(CONTROLLER_NAME + "兑换流量开关关闭,traceId={},flowChargeFlag={}", traceId, flowChargeFlag);
            }
            responseBean.setCode(ResponseEnums.TYPE_SUCCESS.getCode());
            map.put("tradeStatus",ResponseEnums.TYPE_FLOWCHARGEFLAG_CLOSE.getCode());
            map.put("bizMsg",ResponseEnums.TYPE_FLOWCHARGEFLAG_CLOSE.getDesc());
            responseBean.setData(map);
            String returnStr = JSONObject.toJSONString(responseBean);
            if (logger.isInfoEnabled()) {
                logger.info(CONTROLLER_NAME + "traceId={},returnStr={}", traceId, returnStr);
            }
            logUtil.log(new Throwable(),CONTROLLER_NAME , null, MethodCallResultEnum.FAIL, "流量兑换失败，流量兑换开关已关闭", startTime);
            return returnStr;
        }

    }

}
