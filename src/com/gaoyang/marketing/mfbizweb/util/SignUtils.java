package com.gaoyang.marketing.mfbizweb.util;/**
 * Created by zhanghui on 2018-12-3.
 */

import com.alibaba.dubbo.rpc.proxy.TraceIdUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.gaoyang.marketing.mfbizweb.bean.BusiMonitorLoggerBean;
import com.gaoyang.marketing.mfbizweb.bean.LaXinRequestParamsBean;
import com.gaoyang.marketing.mfbizweb.util.acm.MfbizWebAcmClient;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author zhanghui
 * @create 2018-12-3
 * @description
 */
@Component
public class SignUtils {
    private static final Logger logger = LoggerFactory.getLogger(SignUtils.class);
    private final Logger busiMonitorLogger = LoggerFactory.getLogger("busi-monitor");
    @Value("${rsa2.publicKey}")
    private String rsa2PublicKey;
    @Value("${rsa.publicKey}")
    private String rsaPublicKey;
    private static final String IS_TEST_NULL = "null";

    @Autowired
    MfbizWebAcmClient mfbizWebAcmClient;

    public LaXinRequestParamsBean validateAndConvert(String paramsContent) {
        LaXinRequestParamsBean laXinRequestParamsBean = new LaXinRequestParamsBean();
        String traceId = TraceIdUtil.getTraceId();
        if (logger.isInfoEnabled()) {
            logger.info("traceId={} 请求参数为 paramsContent={}", traceId, paramsContent);
        }
        String isLaXinTest = mfbizWebAcmClient.getPorpertiesValue("isLaXinTest");
        if (StringUtils.isBlank(isLaXinTest) || IS_TEST_NULL.equals(isLaXinTest)) {
            // 记账进行验签
            isLaXinTest = "false";
            logger.info("traceId={}当前不需要进行验签操作,isTest=[true],原因:ACM加载不到值,加载本地默认值", traceId);
        }
        try {
            String paramsDecode=URLDecoder.decode(paramsContent,"UTF-8");
            JSONObject response = JSON.parseObject(paramsDecode, Feature.OrderedField);
            boolean iSCheck = false;
            if (logger.isInfoEnabled()) {
                logger.info("traceId={},当前环境是否需要进行测试isTest={}，参数={}", traceId, isLaXinTest,response.toJSONString());
            }
            if (isLaXinTest.equals("true")) {
                iSCheck = true;
            } else {

                //检测非空
                if (StringUtils.isBlank(response.getString("sign")) ||
                        StringUtils.isBlank(response.getString("signType")) ||
                        StringUtils.isBlank(response.getString("charset")) ||
                        StringUtils.isBlank(response.getString("voucherId")) ||
                        StringUtils.isBlank(response.getString("userId"))|| StringUtils.isBlank(response.getString("tag"))
                        ) {
                    if (logger.isInfoEnabled()) {
                        logger.info("traceId={}，sign={},signType={},charset={},voucherId={},userId={}含有空值",
                                traceId,
                                response.getString("sign"),
                                response.getString("signType"),
                                response.getString("charset"),
                                response.getString("voucherId"),
                                response.getString("userId"),
                                response.getString("tag")
                        );
                    }
                    BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
                    busiMonitorLoggerBean.setConsumer("visitor");
                    busiMonitorLoggerBean.setProvider("mfbizweb");
                    busiMonitorLoggerBean.setResult("F");
                    busiMonitorLoggerBean.setBusiness("新人大礼包拉新验签");
                    busiMonitorLoggerBean.setError_desc("新人大礼包拉新传入必填参数为空");
                    busiMonitorLoggerBean.setTrace_id(traceId);
                    busiMonitorLogger.warn(busiMonitorLoggerBean);
                    return null;
                }
                //组装源串
                String signSource = getSignCheckContentV1(response);
                //验证签名
                iSCheck = checkSign(signSource, response);
            }
            if (iSCheck) {            //验证通过
                laXinRequestParamsBean.setTag(response.containsKey("tag")?response.getString("tag"):"");
                laXinRequestParamsBean.setUserId(response.containsKey("userId")?response.getString("userId"):"");
                laXinRequestParamsBean.setVoucherId(response.containsKey("voucherId")?response.getString("voucherId"):"");


                return laXinRequestParamsBean;
            } else {
                BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
                busiMonitorLoggerBean.setConsumer("visitor");
                busiMonitorLoggerBean.setProvider("mfbizweb");
                busiMonitorLoggerBean.setResult("F");
                busiMonitorLoggerBean.setBusiness("新人大礼包拉新验签");
                busiMonitorLoggerBean.setError_desc("新人大礼包拉新验签失败");
                busiMonitorLoggerBean.setTrace_id(traceId);
                busiMonitorLogger.warn(busiMonitorLoggerBean);
                return null;
            }
        } catch (UnsupportedEncodingException e) {

            BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("visitor");
            busiMonitorLoggerBean.setProvider("mfbizweb");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("新人大礼包拉新验签");
            busiMonitorLoggerBean.setError_desc("新人大礼包拉新验签异常");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);
          logger.error("traceId={},验签validateAndConvert异常e={}",
                    traceId, LogExceptionWapper.getStackTrace(e));
        }

        return null;
    }

    private String getSignCheckContentV1(JSONObject response) {
        String traceId = TraceIdUtil.getTraceId();
        String charset = response.getString("charset");
        Set<String> keySet = response.keySet();
        Map<String, String> params = new HashMap<>(keySet.size());
        for (String key : keySet) {
            String value = response.getString(key);
            if (StringUtils.isBlank(value)) {
                params.put(key, "");
            } else {
                try {
                    params.put(key, URLDecoder.decode(value, charset));
                } catch (UnsupportedEncodingException e) {
                    return null;
                }
            }
        }
        if (params == null || params.isEmpty()) {
            logger.info("签名源串为空，traceId={}",traceId);
            BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("visitor");
            busiMonitorLoggerBean.setProvider("mfbizweb");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("新人大礼包拉新验签");
            busiMonitorLoggerBean.setError_desc("新人大礼包拉新签名传为空");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);
            return null;
        } else {
            params.remove("sign");
            TreeMap<String, String> treeMap = new TreeMap(params);
            StringBuffer content = new StringBuffer();
            for (String key : treeMap.keySet()) {
                String value = (String) params.get(key);
                content.append(key + "=" + value + "&");
            }
            content.deleteCharAt(content.length() - 1);
            return content.toString();
        }
    }

    private  boolean checkSign(String signSource, JSONObject response) {
        String traceId = TraceIdUtil.getTraceId();
        try {
            if (logger.isInfoEnabled()) {
                logger.info("signSource = {},traceId={}", signSource,traceId);
            }
            String signType = response.getString("signType");
            boolean isCheckSign = false;
            if (signType.equalsIgnoreCase("rsa")) {
                isCheckSign = AlipaySignature.rsaCheck(signSource, response.getString("sign").replaceAll(" ","+"), rsaPublicKey, response.getString("charset"), "RSA");
            } else if (signType.equalsIgnoreCase("rsa2")) {
                isCheckSign = AlipaySignature.rsaCheck(signSource, response.getString("sign").replaceAll(" ","+"), rsa2PublicKey, response.getString("charset"), "RSA2");
            }
            return isCheckSign;
        } catch (AlipayApiException e) {
            BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("visitor");
            busiMonitorLoggerBean.setProvider("mfbizweb");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("新人大礼包拉新验签");
            busiMonitorLoggerBean.setError_desc("新人大礼包拉新验签异常");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);
            logger.error("traceId={},验签checkSign异常e={}",
                    traceId, LogExceptionWapper.getStackTrace(e));

            return false;
        }
    }

   /* public static void main(String[] args) {
        String sign="Ne64FE7IGNgokQrtO8rnQZXRuQ1oxsiI6GnPNc9OstOF2MutpqwouEZFjSYZ3fau82BiI1RgEDkjsV 0THAnejrOZiTtaPxdbvAiVnhY/Yp9SKWFDIxRdT30qs0JJIzvUaQbsV2Pt5n8AcufhYN/V l2DnaAsps4psxYKj/73Is=";
        String signSource="charset=UTF-8&signType=RSA&tag=cmcclaxin&userId=2088302125107333&voucherId=2018120700073002330702MDQG1B";
        try {
            AlipaySignature.rsaCheck(signSource,sign.replaceAll(" ","+"), "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCnxj/9qwVfgoUh/y2W89L6BkRAFljhNhgPdyPuBV64bfQNN1PjbCzkIM6qRdKBoLPXmKKMiFYnkd6rAoprih3/PrQEB/VsW8OoM8fxn67UDYuyBTqA23MML9q1+ilIZwBC2AQ2UBVOrFXfFl75p6/B5KsiNG9zpgmLCUYuLkxpLQIDAQAB", "UTF-8","RSA");
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
    }*/
}
