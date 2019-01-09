package com.gaoyang.marketing.mfbizweb.controller.baseController;/**
 * Created by zhanghui on 2018-9-20.
 */

import com.gaoyang.marketing.mfbizweb.service.baseService.QueryUserAuthService;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * @author zhanghui
 * @create 2018-9-20
 * @description 获取用户信息入口  此接口停用
 */
@Controller
public class QueryUserAuthController {
    public final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String CONTROLLER_NAME="[获取用户授权controller]";

    @Autowired
    QueryUserAuthService queryUserAuthService;

    @RequestMapping(value="/query_uid",method= RequestMethod.GET)
    @ResponseBody
    public String getUserAuth(@RequestParam("authcode") String authCode,HttpServletRequest request){
        /*String traceId= TraceIdUtil.getTraceId();
        logger.info("调用用户授权平台getUserAuth接口,traceId={},contrlllerName={},authCode={}",traceId,controllerName,authCode);
        HttpResponseBean responseBean=new HttpResponseBean();
        HttpSession session=request.getSession();
        Map<String,String> userInfo;
        Map<String,String> retMap=new HashMap<>();

        if(null==session.getAttribute("userInfo")){
            responseBean=queryUserAuthService.getUserAuth(authCode);
            if(!"0000".equals(responseBean.getCode())) {
                String returnStr=JSON.toJSONString(responseBean);
                logger.error("调用用户授权平台getUserAuth接口,授权失败,traceId={},contrlllerName={},authCode={},returnStr={}",traceId,controllerName,authCode,returnStr);
                Map<String,String> sessionInfo=(HashMap<String,String>)request.getSession().getAttribute("userInfo");
                logger.info("session信息,sessionInfo={}", JSONObject.toJSONString(sessionInfo));
                return returnStr;
            }
            session.setMaxInactiveInterval(300);
            userInfo=(HashMap<String,String>)responseBean.getData();
            session.setAttribute("userInfo", userInfo);
            logger.info("调用用户授权平台getUserAuth接口,授权成功,traceId={},contrlllerName={},authCode={},userInfo={}",traceId,controllerName,authCode,JSONObject.toJSONString(userInfo));
            retMap.put("phone",userInfo.get("iphone"));
        }
        userInfo=(HashMap<String,String>)session.getAttribute("userInfo");
        retMap.put("phone",userInfo.get("iphone"));
        responseBean.setCode("0000");
        responseBean.setMsg("");
        responseBean.setData(retMap);
        String returnStr=JSON.toJSONString(responseBean);
        logger.info("用户已授权,traceId={},contrlllerName={},authCode={},returnStr={}",traceId,controllerName,authCode,returnStr);
        return returnStr;*/
        return "";
    }
}

