package com.gaoyang.marketing.mfbizweb.service.baseService;/**
 * Created by zhanghui on 2018-9-25.
 */

import com.alibaba.dubbo.rpc.proxy.TraceIdUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gaoyang.marketing.maccbasecore.facade.AccountBalanceBizServiceFacade;
import com.gaoyang.marketing.maccbasecore.facade.AccountBizServiceFacade;
import com.gaoyang.marketing.maccbasecore.facade.model.ResponseData;
import com.gaoyang.marketing.maccbasecore.facade.model.request.QueryAccountBalanceListData;
import com.gaoyang.marketing.maccbasecore.facade.model.response.OutAccountBalanceModel;
import com.gaoyang.marketing.maccbasecore.facade.model.response.OutQueryAccountModel;
import com.gaoyang.marketing.mfbizweb.bean.BusiMonitorLoggerBean;
import com.gaoyang.marketing.mfbizweb.bean.HttpResponseBean;
import com.gaoyang.marketing.mfbizweb.util.DateUtil;
import com.gaoyang.marketing.mfbizweb.util.LogExceptionWapper;
import com.gaoyang.marketing.mfbizweb.util.PublicUtil;
import com.gaoyang.marketing.mfbizweb.util.ResponseEnums;
import com.gaoyang.marketing.mfbizweb.util.acm.MfbizWebAcmClient;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author zhanghui
 * @create 2018-9-25
 * @description 查询流量记录serv
 */
@Service
public class QueryAccountBalanceService {

    public final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Logger busiMonitorLogger = LoggerFactory.getLogger("busi-monitor");

    @Resource(name = "accountBizServiceFacade")
    AccountBizServiceFacade accountBizServiceFacade;

    @Resource(name = "accountBalanceBizServiceFacade")
    AccountBalanceBizServiceFacade accountBalanceBizServiceFacade;
    @Autowired
    QueryUseInfoService useInfoService;
    @Autowired
    MfbizWebAcmClient mfbizWebAcmClient;

    /**
     * @param userId
     * @param pageNum
     * @param pageSize
     * @param queryType
     * @return
     * @desc 查询流量记录
     */
    public HttpResponseBean queryAccountBalance(String userId, String pageNum, String pageSize, String queryType, String gyMemberId, String openDate, String bookFlag, String bookDate) {

        String traceId = TraceIdUtil.getTraceId();
        HttpResponseBean httpResponseBean = new HttpResponseBean();
        if (logger.isInfoEnabled()) {
            logger.info("查询流量记录QueryAccountBalanceService, traceId={},userId={},gyMemberId={},opendate={},bookFlag={},bookDate={}", traceId, userId, gyMemberId, openDate, bookFlag, bookDate);
        }
        try {
            String incomeTotalAmount = "0";
            String exchangeAmount = "0";
            //1、incomeTotalAmount 和exchangeAmount字段调用账户平台queryAccountInfo方法
            ResponseData<com.gaoyang.marketing.maccbasecore.facade.model.response.OutQueryAccountModel> responseData = useInfoService.queryAccountInfo(userId);
            if (!PublicUtil.isEmpty(responseData) && !PublicUtil.isEmpty(responseData.getObj())) {
                OutQueryAccountModel outQueryAccountModel = responseData.getObj();

                if (!PublicUtil.isEmpty(outQueryAccountModel.getBalanceCurTotal())) {

                    //20181030  add判断开卡时间在本月时 incomeTotalAmount=incomeTotalAmount+开卡赠送流量
                    if (!StringUtils.isBlank(gyMemberId) && !StringUtils.isBlank(openDate) &&
                            !"-1".equals(openDate) && !"-1".equals(gyMemberId)) {

                        String sendFlow = getRuleFlow(openDate, bookFlag, bookDate);
                        if (logger.isInfoEnabled()) {
                            logger.info("查询流量记录QueryAccountBalanceService,会员领卡叠加流量, traceId={},userId={},gyMemberId={},sendFlow={}"
                                    , traceId, userId, gyMemberId, sendFlow);
                        }
                        incomeTotalAmount = String.valueOf(outQueryAccountModel.getBalanceCurTotal().intValue() + Integer.valueOf(sendFlow));

                        if (logger.isInfoEnabled()) {
                            logger.info("查询流量记录QueryAccountBalanceService,该用户已经是高阳会员, traceId={},userId={},gyMemberId={},当月获取总流量={}"
                                    , traceId, userId, gyMemberId, incomeTotalAmount);
                        }
                    } else {
                        incomeTotalAmount = outQueryAccountModel.getBalanceCurTotal().toPlainString();
                    }

                }

                if (!PublicUtil.isEmpty(outQueryAccountModel.getBalanceExchanged())) {
                    exchangeAmount = outQueryAccountModel.getBalanceExchanged().toPlainString();
                }
            } else {
                BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
                busiMonitorLoggerBean.setConsumer("mfbizweb");
                busiMonitorLoggerBean.setProvider("maccbasecore");
                busiMonitorLoggerBean.setResult("F");
                busiMonitorLoggerBean.setBusiness("查询流量记录");
                busiMonitorLoggerBean.setError_desc("查询异常");
                busiMonitorLoggerBean.setTrace_id(traceId);
                busiMonitorLogger.warn(busiMonitorLoggerBean);

                httpResponseBean.setCode(ResponseEnums.TYPE_ACCOUNTBALANCE_FAIL.getCode());
                httpResponseBean.setMsg(ResponseEnums.TYPE_ACCOUNTBALANCE_FAIL.getDesc());
                String res = JSONObject.toJSONString(httpResponseBean);
                logger.warn("调用账户数据平台接口queryAccountInfo返回对象为空traceId={},userId={},res={}", traceId, userId, res);
                return httpResponseBean;
            }

            //2、expireAmount  queryAccountBalanceList   TYPE_EXPIRE(9, "过期记录")
            QueryAccountBalanceListData queryAccountBalanceListData = setQueryAccountBalanceList(userId, "1", "1", "9");
            ResponseData<List<OutAccountBalanceModel>> responseDataForExpireAmount = queryAccountBalanceList(queryAccountBalanceListData);
            if (PublicUtil.isEmpty(responseDataForExpireAmount)) {

                BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
                busiMonitorLoggerBean.setConsumer("mfbizweb");
                busiMonitorLoggerBean.setProvider("maccbasecore");
                busiMonitorLoggerBean.setResult("F");
                busiMonitorLoggerBean.setBusiness("查询流量记录");
                busiMonitorLoggerBean.setError_desc("查询异常");
                busiMonitorLoggerBean.setTrace_id(traceId);
                busiMonitorLogger.warn(busiMonitorLoggerBean);

                httpResponseBean.setCode(ResponseEnums.TYPE_ACCOUNTBALANCE_FAIL.getCode());
                httpResponseBean.setMsg(ResponseEnums.TYPE_ACCOUNTBALANCE_FAIL.getDesc());
                String res = JSONObject.toJSONString(httpResponseBean);
                logger.warn("调用账户数据平台接口queryAccountBalanceList查询过期流量失败,traceId={},userId={},res={}", traceId, userId, res);
                return httpResponseBean;
            }
            List<OutAccountBalanceModel> list = responseDataForExpireAmount.getObj();
            String expireAmount = "0";
            if (!PublicUtil.isEmpty(list)) {
                OutAccountBalanceModel outAccountBalanceModel = list.get(0);
                if (!PublicUtil.isEmpty(outAccountBalanceModel.getRealTradeAmount())) {
                    expireAmount = outAccountBalanceModel.getRealTradeAmount().toPlainString();
                }
            }

            Map<String, Object> totalMap = new HashMap<String, Object>();

            //查询特殊记账list(排除线上支付获得、领卡赠送)
           List<OutAccountBalanceModel>  specialAccBalanceList =querySpecialAccBalanceList(userId);

            if(!PublicUtil.isEmpty(specialAccBalanceList)){
                    long specialAccBalance=0;
                for(OutAccountBalanceModel outAccountBalanceModel:specialAccBalanceList){
                    specialAccBalance=specialAccBalance+outAccountBalanceModel.getRealTradeAmount().intValue();

                }
                if (logger.isInfoEnabled()) {
                    logger.info("用户traceId={},userId={},本月支付+开卡赠送={},特殊记账={}", traceId, userId, incomeTotalAmount,specialAccBalance);
                }
                BigDecimal b = new BigDecimal(incomeTotalAmount);
                incomeTotalAmount=String.valueOf(b.intValue()+specialAccBalance);
            }

            //flowTotalData
            totalMap.put("incomeTotalAmount", incomeTotalAmount);
            totalMap.put("exchangeAmount", exchangeAmount);
            totalMap.put("expireAmount", expireAmount);
            //组装返回给前端的响应参数
            Map<String, Object> resMap = new HashMap<String, Object>();
            resMap.put("queryType", queryType);
            resMap.put("flowTotalData", totalMap);
            //3、流量变更明细数据：flowDetailData   根据queryType 调用queryAccountBalanceList
            QueryAccountBalanceListData queryAccountBalanceList = setQueryAccountBalanceList(userId, pageNum, pageSize, queryType);
            ResponseData<List<OutAccountBalanceModel>> responseDataForAccountBalance = queryAccountBalanceList(queryAccountBalanceList);
            if (PublicUtil.isEmpty(responseDataForAccountBalance)) {
                BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
                busiMonitorLoggerBean.setConsumer("mfbizweb");
                busiMonitorLoggerBean.setProvider("maccbasecore");
                busiMonitorLoggerBean.setResult("F");
                busiMonitorLoggerBean.setBusiness("查询流量记录");
                busiMonitorLoggerBean.setError_desc("查询异常");
                busiMonitorLoggerBean.setTrace_id(traceId);
                busiMonitorLogger.warn(busiMonitorLoggerBean);

                httpResponseBean.setCode(ResponseEnums.TYPE_ACCOUNTBALANCE_FAIL.getCode());
                httpResponseBean.setMsg(ResponseEnums.TYPE_ACCOUNTBALANCE_FAIL.getDesc());
                String res = JSONObject.toJSONString(httpResponseBean);
                logger.warn("调用账户数据平台接口queryAccountBalanceListData查询流量失败,traceId={},userId={},res={}", traceId, userId, res);
                return httpResponseBean;
            }
            List detailsList = new ArrayList<Map>();
            List<OutAccountBalanceModel> outAccountBalanceList = responseDataForAccountBalance.getObj();
            //outAccountBalanceList为空 即流量记录为空
            if (PublicUtil.isEmpty(outAccountBalanceList)) {
                if (logger.isInfoEnabled()) {
                    logger.info("调用账户数据平台接口queryAccountBalanceListData查询流量记录为空,traceId={},userId={},queryType={}", traceId, userId, queryType);
                }
                resMap.put("count", "0");
            } else {
                for (OutAccountBalanceModel outAccountBalanceModel1 : outAccountBalanceList) {
                    Map<String, Object> detailsMap = new HashMap<String, Object>();
                    detailsMap.put("changeAmount", outAccountBalanceModel1.getRealTradeAmount());
                    detailsMap.put("changedOrder", outAccountBalanceModel1.getTradeRemark());
                    detailsMap.put("changeDatetime", outAccountBalanceModel1.getTradeTime());
                    detailsMap.put("balanceType", outAccountBalanceModel1.getBalanceType());
                    detailsList.add(detailsMap);
                }
                //将流量记录总数count返回给前端
                resMap.put("count", responseDataForAccountBalance.getAllCount());

            }
            resMap.put("flowDetails", detailsList);
            httpResponseBean.setCode(ResponseEnums.TYPE_SUCCESS.getCode());
            httpResponseBean.setMsg(ResponseEnums.TYPE_SUCCESS.getDesc());
            httpResponseBean.setData(resMap);
            String res = JSONObject.toJSONString(httpResponseBean);
            if (logger.isInfoEnabled()) {
                logger.info("流量记录查询接口service,traceId={},userId={},res={}", traceId,userId, res);
            }
            return httpResponseBean;
        } catch (Exception e) {
            BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("mfbizweb");
            busiMonitorLoggerBean.setProvider("maccbasecore");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("查询流量记录");
            busiMonitorLoggerBean.setError_desc("查询异常");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);

            httpResponseBean.setCode(ResponseEnums.TYPE_ACCOUNTBALANCE_FAIL.getCode());
            httpResponseBean.setMsg(ResponseEnums.TYPE_ACCOUNTBALANCE_FAIL.getDesc());
            String res = JSONObject.toJSONString(httpResponseBean);
            logger.error("traceId={},userId={},res={},调用账户数据平台接口异常e={}",
                    traceId, userId, res, LogExceptionWapper.getStackTrace(e));
            return httpResponseBean;

        }

    }

    /**
     * @param queryAccountBalanceListData
     * @return
     * @desc 调用帐户数据平台接口  查询用户账户余额变更记录
     */
    public ResponseData<List<OutAccountBalanceModel>> queryAccountBalanceList(QueryAccountBalanceListData queryAccountBalanceListData) {
        String traceId = TraceIdUtil.getTraceId();
        if (logger.isInfoEnabled()) {
            logger.info("调用账户数据平台接口queryAccountBalanceList,traceId={},queryAccountBalanceListData={}", traceId, JSONArray.toJSON(queryAccountBalanceListData));
        }
        try {
            ResponseData<List<OutAccountBalanceModel>> responseData = accountBalanceBizServiceFacade.queryAccountBalanceList(queryAccountBalanceListData);

            if (!PublicUtil.isEmpty(responseData)) {
                String code = responseData.getCode();
                //判断接口返回code
                if (!ResponseEnums.RPC_TYPE_SUCCESS.getCode().equals(code)) {
                    logger.warn("调用账户数据平台接口queryAccountBalanceList,查询失败,traceId={},code={},userId={},type={}", traceId, code, queryAccountBalanceListData.getUserId(), queryAccountBalanceListData.getQueryType());
                    return null;
                }
                if (logger.isInfoEnabled()) {
                    logger.info("调用账户数据平台接口queryAccountBalanceList返回实体,traceId={},responseData={}", traceId, JSONObject.toJSON(responseData));
                }
                return responseData;
            }
        } catch (Exception e) {
            BusiMonitorLoggerBean busiMonitorLoggerBean = new BusiMonitorLoggerBean();
            busiMonitorLoggerBean.setConsumer("mfbizweb");
            busiMonitorLoggerBean.setProvider("maccbasecore");
            busiMonitorLoggerBean.setResult("F");
            busiMonitorLoggerBean.setBusiness("查询流量记录");
            busiMonitorLoggerBean.setError_desc("查询异常");
            busiMonitorLoggerBean.setTrace_id(traceId);
            busiMonitorLogger.warn(busiMonitorLoggerBean);

            logger.error("traceId={},调用账户数据平台接口queryAccountBalanceList异常e={}",
                    traceId, LogExceptionWapper.getStackTrace(e));
            e.printStackTrace();

        }
        return null;
    }

    public QueryAccountBalanceListData setQueryAccountBalanceList(String userId, String pageNum, String pageSize, String queryType) {
        String traceId = TraceIdUtil.getTraceId();
        if (logger.isInfoEnabled()) {
            logger.info("设置查询账户信息请求list setQueryAccountBalanceList,traceId={},userId={},queryType={}", traceId, userId, queryType);
        }
        TimeZone.getTimeZone("GMT");
        //获取本月第一天和最后一天零点零分
        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();
        startCal.add(Calendar.MONTH, -2);
        //设置为1号,当前日期既为本月第一天
        startCal.set(Calendar.DAY_OF_MONTH, 1);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);

        endCal.set(Calendar.DAY_OF_MONTH, 1);
        endCal.add(Calendar.MONTH, 1);
        endCal.set(Calendar.HOUR_OF_DAY, 0);
        endCal.set(Calendar.MINUTE, 0);
        endCal.set(Calendar.SECOND, 0);
        endCal.set(Calendar.MILLISECOND, 0);

        Date startDate = startCal.getTime();
        Date endDate = endCal.getTime();
        //Integer startRow = Integer.valueOf(pageSize) * (Integer.valueOf(pageNum) - Integer.valueOf("1") );
        int ii = Integer.parseInt(pageSize) * (Integer.parseInt(pageNum) - 1);
        String type = "";
        //根据传入 queryType判断类别，默认类别：TYPE_EXPIRE(9, "过期记录") 查询expireAmount
        //TYPE_ALL_RECORD(10, "额度内所有记录");
        // TYPE_NORMAL_INTER_INCOME(6, "接口额度内正常入账记录，不包含超限、冲正的记录"),
        //TYPE_EXCHANGE_WITH_ROLLBACK(4, "兑换记录，包含冲正"),
        // TYPE_EXPIRE(9, "过期记录"),
        switch (queryType) {
            case "ALL":
                type = "10";
                break;
            case "INCOME":
                type = "6";
                break;
            case "EXCHANGE":
                type = "4";
                break;
            default:
                type = "9";
        }
        QueryAccountBalanceListData queryAccountBalanceListData = new QueryAccountBalanceListData();
        queryAccountBalanceListData.setQueryType(Integer.valueOf(type));
        queryAccountBalanceListData.setUserId(userId);
        queryAccountBalanceListData.setPageSize(Integer.valueOf(pageSize));
        queryAccountBalanceListData.setStartRow(ii);
        queryAccountBalanceListData.setQueryBeginTime(startDate);
        queryAccountBalanceListData.setQueryEndTime(endDate);
        return queryAccountBalanceListData;

    }

    /**
     * @param openDate
     * @return
     * @desc 根据会员卡号判断是否当月开卡，当月开卡赠送多少流量
     * 2018  11 24 增加 判断预约赠送流量
     */
    private String getRuleFlow(String openDate, String bookFlag, String bookDate) {

        String traceId = TraceIdUtil.getTraceId();
        String sendFlow = "0";
        // 获取当前真实月份
        SimpleDateFormat format = new SimpleDateFormat("yyyyMM");
        String realCurrMonthStr = format.format(new Date(System.currentTimeMillis()));
        //创建会员卡月份 yyyyMMddHHmmss
        String createMonthSt = openDate.substring(0, 6);
        //1判断是否当月创建会员
        if (!realCurrMonthStr.equals(createMonthSt)) {
            if (logger.isInfoEnabled()) {
                logger.info("查询流量记录service,非本月创建会员卡,需叠加的流量数值为0,traceId={},openDate={}", traceId, openDate);
            }
            return sendFlow;
        }
        // 读取配置并发送，读取配置失败默认待叠加获取流量值为0
        String jsonRules = mfbizWebAcmClient.getPorpertiesValue("createMemeberSendFlowRule");
        if (logger.isInfoEnabled()) {
            logger.info("查询流量记录service,读取acm配置开卡赠送规则,traceId={},openDate={},rule={}", traceId, openDate, jsonRules);
        }
        if (StringUtils.isBlank(jsonRules) || "null".equals(jsonRules)) {
            sendFlow = "1000";
        } else {
            //开卡赠送流量
            sendFlow = PublicUtil.getSendFlow(jsonRules);
        }
        //先判断是不是预约用户
        String bookFlow = "0";
        if (logger.isInfoEnabled()) {
            logger.info("查询流量记录service,traceId={},开卡时间={},预约标识={},预约时间={}", traceId, openDate, bookFlag, bookDate);
        }
        if ("0".equals(bookFlag)) {

            //判断开卡时间与预约时间间隔
            Long bookDateMillis = DateUtil.parseDateFromStr(bookDate).getTime();
            Long openDateMillis = DateUtil.parseDateFromStr(openDate).getTime();

            Long cruTime = System.currentTimeMillis();
            //预约时间与开卡时间间隔15天以上则不赠送预约流量
            if (bookDateMillis + 15 * 24 * 60 * 60 * 1000 >= openDateMillis) {
                bookFlow = mfbizWebAcmClient.getPorpertiesValue("bookMemeberSendFlowRule");

                if (StringUtils.isBlank(bookFlow) || "null".equals(bookFlow)) {
                    bookFlow = "1000";
                }

            }
            int totalFlow = Integer.valueOf(bookFlow) + Integer.valueOf(sendFlow);
            return Integer.toString(totalFlow);
        }
        return sendFlow;
    }
    /**
     * @param userId
     * @return
     * @desc  查询某个用户当前月份特殊记账记录，用于当前余额显示，排除：正常记账获得、领卡赠送
     */
    public List<OutAccountBalanceModel>  querySpecialAccBalanceList(String userId) {
        String traceId = TraceIdUtil.getTraceId();
        if (logger.isInfoEnabled()) {
            logger.info("调用账户数据平台接口查询特殊记账querySpecialAccBalanceList,traceId={},userId={}", traceId, userId);
        }
        try {
            ResponseData<List<OutAccountBalanceModel>> responseData = accountBalanceBizServiceFacade.querySpecialAccBalanceList(userId);

            if (!PublicUtil.isEmpty(responseData)) {
                String code = responseData.getCode();
                //判断接口返回code
                if (!ResponseEnums.RPC_TYPE_SUCCESS.getCode().equals(code)) {
                    logger.warn("调用账户数据平台接口查询特殊记账querySpecialAccBalanceList,查询失败,traceId={},userId={}", traceId,userId);
                    return null;
                }
                if (logger.isInfoEnabled()) {
                    logger.info("调用账户数据平台接口查询特殊记账querySpecialAccBalanceList返回实体,traceId={},responseData={}", traceId, JSONObject.toJSON(responseData));
                }
                return responseData.getObj();
            }
        } catch (Exception e) {

            logger.error("traceId={},调用账户数据平台接口查询特殊记账querySpecialAccBalanceList异常e={}",
                    traceId, LogExceptionWapper.getStackTrace(e));
            e.printStackTrace();

        }
        return null;
    }

}
