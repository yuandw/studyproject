package com.gaoyang.marketing.mfbizweb.bean;

import com.gaoyang.marketing.mprodbasecore.facade.model.response.ProductOnlineInfoModel;

import java.math.BigDecimal;

/**
 * @author zhanghui
 * @create 2018-9-21
 * @description
 */

public class ProductInfoBean {
	//货架id
	private String poiId;
	//商品展示名称
	private String poiViewName;
	//兑换流量
	private BigDecimal poiFlowConsume;
	//token
	private String userToken;
	
	public ProductInfoBean(ProductOnlineInfoModel productOnlineInfoModel) {
		this.poiId=productOnlineInfoModel.getOlProductId();
		this.poiViewName=productOnlineInfoModel.getOlProductViewName();
		this.poiFlowConsume=productOnlineInfoModel.getOlFlowConsume();
	}

	public String getPoiId() {
		return poiId;
	}

	public void setPoiId(String poiId) {
		this.poiId = poiId;
	}

	public String getPoiViewName() {
		return poiViewName;
	}

	public void setPoiViewName(String poiViewName) {
		this.poiViewName = poiViewName;
	}

	public BigDecimal getPoiFlowConsume() {
		return poiFlowConsume;
	}

	public void setPoiFlowConsume(BigDecimal poiFlowConsume) {
		this.poiFlowConsume = poiFlowConsume;
	}

	public String getUserToken() {
		return userToken;
	}

	public void setUserToken(String userToken) {
		this.userToken = userToken;
	}
	
}
