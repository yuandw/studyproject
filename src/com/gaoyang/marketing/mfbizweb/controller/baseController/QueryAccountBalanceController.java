package com.gaoyang.marketing.mfbizweb.controller.baseController;/**
 * Created by zhanghui on 2018-9-25.
 */

import com.alibaba.dubbo.rpc.proxy.TraceIdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gaoyang.marketing.log.util.LogUtil;
import com.gaoyang.marketing.log.util.MethodCallResultEnum;
import com.gaoyang.marketing.mfbizweb.bean.HttpResponseBean;
import com.gaoyang.marketing.mfbizweb.service.baseService.QueryAccountBalanceService;
import com.gaoyang.marketing.mfbizweb.util.PublicUtil;
import com.gaoyang.marketing.mfbizweb.util.ResponseEnums;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author zhanghui
 * @create 2018-9-25
 * @description 流量记录controller
 */
@Controller
public class QueryAccountBalanceController {
    private final static String controllerName = "[查询流量记录controller]";
    public final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    QueryAccountBalanceService queryAccountBalanceService;
    @Autowired
    private LogUtil logUtil;

    @RequestMapping(value = "/query-account-balance", method = RequestMethod.GET)
    @ResponseBody
    public String queryAccountBalance(@RequestParam("pageNum") String pageNum,
                                      @RequestParam("pageSize") String pageSize,
                                      @RequestParam("queryType") String queryType,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        long startTime = System.currentTimeMillis();
        String traceId = TraceIdUtil.getTraceId();
        HttpResponseBean responseBean = new HttpResponseBean();
        Map<String, String> userInfo = (Map<String, String>) request.getSession().getAttribute("userInfo");

        if (logger.isInfoEnabled()) {
            logger.info(controllerName + "traceId={},pageNum={},pageSize={},queryType={}", traceId, pageNum, pageSize,queryType);
        }
        //增加 pageSize pageNum 数字判断
        if (!NumberUtils.isDigits(pageSize) || !NumberUtils.isDigits(pageNum)) {
            responseBean.setCode(ResponseEnums.TYPE_SYSTEMBUSY_EXCEPYION.getCode());
            responseBean.setMsg(ResponseEnums.TYPE_SYSTEMBUSY_EXCEPYION.getDesc());
            String returnStr = JSON.toJSONString(responseBean);
            logger.warn(controllerName + "traceId={},returnStr={}", traceId, returnStr);
            logUtil.log(new Throwable(), controllerName, null, MethodCallResultEnum.FAIL, "获取流量记录controller，pageSize或者pageNum不是数字", startTime);
            return returnStr;
        }

        //对session中的用户信息进行校验
        if (PublicUtil.isEmpty(userInfo)) {
            responseBean.setCode(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getCode());
            responseBean.setMsg(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getDesc());
            String returnStr = JSON.toJSONString(responseBean);
            logger.warn(controllerName + "traceId={},returnStr={}", traceId, returnStr);
            logUtil.log(new Throwable(), controllerName, null, MethodCallResultEnum.FAIL, "获取流量记录controller获取session为空", startTime);
            return returnStr;
        }
        String userId = userInfo.get("userId");
        String gyMemberId = userInfo.get("gyMemberId");
        String openDate = userInfo.get("openDate");
        String bookFlag=userInfo.get("bookFlag");
        String bookDate=userInfo.get("bookDate");

        //对session中的用户信息进行校验
        if (PublicUtil.isEmpty(userId)) {
            responseBean.setCode(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getCode());
            responseBean.setMsg(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getDesc());
            String returnStr = JSON.toJSONString(responseBean);
            logger.warn(controllerName + "traceId={},returnStr={}", traceId, returnStr);
            logUtil.log(new Throwable(), controllerName, null, MethodCallResultEnum.FAIL, "获取流量记录controller获取session中userid为空", startTime);
            return returnStr;
        }
        responseBean = queryAccountBalanceService.queryAccountBalance(userId, pageNum, pageSize, queryType, gyMemberId, openDate,bookFlag,bookDate);
        String returnStr = JSONObject.toJSONString(responseBean);
        if (logger.isInfoEnabled()) {
            logger.info(controllerName + "traceId={},userId={},res={}", traceId, userId, returnStr);
        }
        if (ResponseEnums.TYPE_SUCCESS.getCode().equals(responseBean.getCode())) {
            logUtil.log(new Throwable(), controllerName, null, MethodCallResultEnum.SUCCESS, "获取流量记录成功", startTime);
        } else {
            logUtil.log(new Throwable(), controllerName, null, MethodCallResultEnum.FAIL, "获取流量记录失败", startTime);
        }


        return returnStr;


    }

}
