package com.gaoyang.marketing.mfbizweb.service.baseService;/**
 * Created by zhanghui on 2018-9-20.
 */

import com.alibaba.dubbo.rpc.proxy.TraceIdUtil;import com.alibaba.fastjson.JSONObject;
import com.gaoyang.marketing.mfbizweb.bean.HttpResponseBean;
import com.gaoyang.marketing.mfbizweb.bean.ProductInfoBean;
import com.gaoyang.marketing.mfbizweb.util.LogExceptionWapper;
import com.gaoyang.marketing.mfbizweb.util.PublicUtil;
import com.gaoyang.marketing.mfbizweb.util.ResponseEnums;
import com.gaoyang.marketing.mprodbasecore.facade.ProductBizServiceFacade;
import com.gaoyang.marketing.mprodbasecore.facade.model.ResponseData;
import com.gaoyang.marketing.mprodbasecore.facade.model.request.ProductOnlineInfoData;
import com.gaoyang.marketing.mprodbasecore.facade.model.response.ProductOnlineInfoModel;
import com.jiexun.transaction.common.log.Logger;
import com.jiexun.transaction.common.log.LoggerFactory;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;


/**
 * @author zhanghui
 * @create 2018-9-20
 * @description获取商品信息service
 */
@Service
public class QueryProductInfoService {
    public final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource(name="productBizServiceFacade")
    ProductBizServiceFacade productBizServiceFacade;

    /**
     * @param merchantSubId
     * @return
     * @desc 商品信息平台前端返回信息
     */
    public HttpResponseBean getProductInfo(String merchantSubId){
        String traceId= TraceIdUtil.getTraceId();
        // 商品数据平台接口queryProductList 查询商品列表
        HttpResponseBean responseBean=new HttpResponseBean();
        //判断接口返回code
        try{
            if(PublicUtil.isEmpty(merchantSubId)){
                responseBean.setCode(ResponseEnums.TYPE_SYSTEMBUSY_EXCEPYION.getCode());
                responseBean.setMsg(ResponseEnums.TYPE_SYSTEMBUSY_EXCEPYION.getDesc());
                logger.warn("调用商品数据平台接口queryProductList子商户ID为空,traceId={},merchantSubId={},returnStr={}",traceId, merchantSubId, JSONObject.toJSONString(responseBean));
                return responseBean;
            }
            List<ProductOnlineInfoModel> productList=queryProductList(merchantSubId);
            List<ProductInfoBean> returnList=new ArrayList<>();
            if(PublicUtil.isEmpty(productList)){
                responseBean.setCode(ResponseEnums.TYPE_SYSTEMBUSY_EXCEPYION.getCode());
                responseBean.setMsg(ResponseEnums.TYPE_SYSTEMBUSY_EXCEPYION.getDesc());
                logger.warn("调用商品数据平台接口queryProductList返回对象为空,traceId={},merchantSubId={},returnStr={}",traceId,merchantSubId,JSONObject.toJSONString(responseBean));
                return responseBean;
            }
            for(ProductOnlineInfoModel product:productList) {
                if(PublicUtil.isEmpty(product.getOlProductId())||
                        PublicUtil.isEmpty((product).getOlProductViewName())||
                        PublicUtil.isEmpty((product).getOlFlowConsume())){

                    responseBean.setCode(ResponseEnums.TYPE_SYSTEMBUSY_EXCEPYION.getCode());
                    responseBean.setMsg(ResponseEnums.TYPE_SYSTEMBUSY_EXCEPYION.getDesc());
                    logger.warn("调用商品数据平台接口queryProductList返回olProductId、olProductViewName、olFlowConsume为空,traceId={},merchantSubId={},returnStr={}",traceId,merchantSubId,JSONObject.toJSONString(responseBean));
                    return responseBean;
                }
                returnList.add(new ProductInfoBean(product));
            }

            responseBean.setCode(ResponseEnums.TYPE_SUCCESS.getCode());
            responseBean.setData(returnList);
            if (logger.isInfoEnabled()) {
                logger.info("调用商品数据平台接口queryProductList查询成功！,traceId={},merchantSubId={},returnStr={}", traceId, merchantSubId, JSONObject.toJSONString(responseBean));
            }
            return responseBean;
        }catch(Exception e){
            responseBean.setCode(ResponseEnums.TYPE_SYSTEMBUSY_EXCEPYION.getCode());
            responseBean.setMsg(ResponseEnums.TYPE_SYSTEMBUSY_EXCEPYION.getDesc());
            logger.error("traceId={},merchantSubId={},调用商品数据平台接口queryProductList查询异常e={}",
                    traceId,merchantSubId, LogExceptionWapper.getStackTrace(e));
            e.printStackTrace();
            return responseBean;
        }
    }

    /**
     * @desc 调用商品数据平台接口  查询商品列表
     * @param merchantSubId
     * @return
     */
    public List<ProductOnlineInfoModel> queryProductList(String merchantSubId){
        String traceId= TraceIdUtil.getTraceId();
        if (logger.isInfoEnabled()) {
            logger.info("调用商品数据平台接口queryProductList,traceId={},merchantSubId={}", traceId, merchantSubId);
        }
        try {
            ProductOnlineInfoData productOnlineInfoData=new ProductOnlineInfoData();
            productOnlineInfoData.setMerchantSubId(merchantSubId);
            ResponseData<List<ProductOnlineInfoModel>> responseData=productBizServiceFacade.queryProductList(productOnlineInfoData);
            if (logger.isInfoEnabled()) {
                logger.info("调用商品数据平台接口queryProductList,traceId={},merchantSubId={},responseData={}", traceId, merchantSubId, JSONObject.toJSONString(responseData));
            }
            if(!PublicUtil.isEmpty(responseData)) {
                //判断接口返回code
                if (!ResponseEnums.RPC_TYPE_SUCCESS.getCode().equals(responseData.getCode())) {
                    logger.warn("调用商品数据平台接口queryProductList查询失败,traceId={},merchantSubId={},code={}", traceId, merchantSubId, responseData.getCode());
                    return null;
                }
                List<ProductOnlineInfoModel> productList = responseData.getObj();
                return productList;
            }
        } catch (Exception e) {
            logger.error("traceId={},merchantSubId={},调用商品数据平台接口queryProductList异常e={}",
                    traceId,merchantSubId, LogExceptionWapper.getStackTrace(e));
            e.printStackTrace();

        }
        return null;
    }
}
