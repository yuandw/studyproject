package com.gaoyang.marketing.mfbizweb.controller.baseController;/**
 * Created by zhanghui on 2018-9-20.
 */

import com.alibaba.dubbo.rpc.proxy.TraceIdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gaoyang.marketing.log.util.LogUtil;
import com.gaoyang.marketing.log.util.MethodCallResultEnum;
import com.gaoyang.marketing.mfbizweb.bean.HttpResponseBean;
import com.gaoyang.marketing.mfbizweb.service.baseService.QueryUseInfoService;
import com.gaoyang.marketing.mfbizweb.util.PublicUtil;
import com.gaoyang.marketing.mfbizweb.util.ResponseEnums;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author zhanghui
 * @create 2018-9-20
 * @description 获取用户信息入口
 */
@Controller
public class QueryUseInfoController {
    public final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String CONTROLLER_NAME="[获取用户信息controller]";
    @Autowired
    QueryUseInfoService useInfoService;
    @Autowired
    private LogUtil logUtil;

    @RequestMapping(value="/query_userInfo",method= RequestMethod.GET)
    @ResponseBody
    public String getUseInfo(HttpServletRequest request,
                             HttpServletResponse response){
        String traceId= TraceIdUtil.getTraceId();
        long startTime = System.currentTimeMillis();
        if (logger.isInfoEnabled()) {
            logger.info(CONTROLLER_NAME + "traceId={}", traceId);
        }
        HttpResponseBean responseBean=new HttpResponseBean();
        Map<String,String> userInfo=(Map<String, String>)request.getSession().getAttribute("userInfo");
        //对session中的用户信息进行校验
        if(PublicUtil.isEmpty(userInfo)){
            responseBean.setCode(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getCode());
            responseBean.setMsg(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getDesc());
            String returnStr= JSON.toJSONString(responseBean);
            logger.warn(CONTROLLER_NAME+"traceId={},returnStr={}",traceId,returnStr);
            logUtil.log(new Throwable(),CONTROLLER_NAME, null, MethodCallResultEnum.FAIL, "获取用户信息校验session为空", startTime);
            return returnStr;
        }
        String uid=userInfo.get("uid");
        String userId=userInfo.get("userId");
        String gyMemberId = userInfo.get("gyMemberId");
        String iphone=userInfo.get("iphone");
        if (logger.isInfoEnabled()) {
            logger.info(CONTROLLER_NAME + "traceId={},userId={},gyMemberId={},iphone={},userInfo={}", traceId, userId, gyMemberId, iphone,JSONObject.toJSONString(userInfo));
        }
        responseBean=useInfoService.getUserInfo(uid,userId,gyMemberId,iphone);
        String res = JSONObject.toJSONString(responseBean);
        if (logger.isInfoEnabled()) {
            logger.info(CONTROLLER_NAME + "traceId={},userId={},returnStr={}", traceId, userId, res);
        }

        if(ResponseEnums.TYPE_SUCCESS.getCode().equals(responseBean.getCode())){
            logUtil.log(new Throwable(),CONTROLLER_NAME, null, MethodCallResultEnum.SUCCESS, "获取用户信息成功", startTime);
        }else{
            logUtil.log(new Throwable(),CONTROLLER_NAME , null, MethodCallResultEnum.FAIL, "获取用户信息失败", startTime);
        }
        return res;
    }


}

