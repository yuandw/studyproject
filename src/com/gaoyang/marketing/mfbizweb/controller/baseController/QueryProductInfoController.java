package com.gaoyang.marketing.mfbizweb.controller.baseController;/**
 * Created by zhanghui on 2018-9-20.
 */

import com.alibaba.dubbo.rpc.proxy.TraceIdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gaoyang.marketing.log.util.LogUtil;
import com.gaoyang.marketing.log.util.MethodCallResultEnum;
import com.gaoyang.marketing.mfbizweb.bean.HttpResponseBean;
import com.gaoyang.marketing.mfbizweb.service.baseService.QueryProductInfoService;
import com.gaoyang.marketing.mfbizweb.util.PublicUtil;
import com.gaoyang.marketing.mfbizweb.util.ResponseEnums;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * @author zhanghui
 * @create 2018-9-20
 * @description 获取用户信息入口
 */
@Controller
public class QueryProductInfoController {
    public final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String CONTROLLER_NAME="[获取商品信息controller]";

    @Autowired
    private QueryProductInfoService queryProductInfoService;
    @Autowired
    private LogUtil logUtil;
    @Value("${productInfo.merchantSubId}")
    private String merchantSubId;


    @RequestMapping(value="/query_prod_online",method= RequestMethod.GET)
    @ResponseBody
    public String getProductInfo(HttpServletRequest request){
        String traceId= TraceIdUtil.getTraceId();
        long startTime = System.currentTimeMillis();
        if (logger.isInfoEnabled()) {
            logger.info("调用商品信息平台getProductInfo接口,traceId={},contrlllerName={},merchantSubId={}", traceId, CONTROLLER_NAME, merchantSubId);
        }
        HttpResponseBean responseBean=new HttpResponseBean();

        if(PublicUtil.isEmpty(request.getSession().getAttribute("userInfo"))){
            responseBean.setCode(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getCode());
            responseBean.setMsg(ResponseEnums.TYPE_USERINFO_ISNOTEXIST.getDesc());
            String returnStr=JSON.toJSONString(responseBean);
            logUtil.log(new Throwable(),CONTROLLER_NAME , null, MethodCallResultEnum.FAIL, "获取商品信息controller获取session为空", startTime);
            logger.warn("调用商品信息平台getProductInfo接口,用户信息不存在,traceId={},contrlllerName={},merchantSubId={},returnStr={}", traceId, CONTROLLER_NAME,merchantSubId, returnStr);
            return returnStr;
        }

        responseBean=queryProductInfoService.getProductInfo(merchantSubId);
        if(ResponseEnums.TYPE_SUCCESS.getCode().equals(responseBean.getCode())){
            logUtil.log(new Throwable(),CONTROLLER_NAME , null, MethodCallResultEnum.SUCCESS, "获取商品信息controller成功", startTime);
        }else{
            logUtil.log(new Throwable(),CONTROLLER_NAME , null, MethodCallResultEnum.FAIL, "获取商品信息controller失败", startTime);
        }
        return JSONObject.toJSONString(responseBean);
    }
}

