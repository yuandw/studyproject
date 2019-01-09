package com.gaoyang.marketing.mfbizweb.bean;/**
 * Created by zhanghui on 2018-11-20.
 */

import java.io.Serializable;

/**
 * @author zhanghui
 * @create 2018-11-20
 * @description   业务监控日志实体
 */
public class BusiMonitorLoggerBean implements Serializable {
        private static final long serialVersionUID = -6294447928838113971L;
        //调用者
        private String consumer;
        //被调用者
        private String provider;
        //业务描述
        private String business;
        //异常描述
        private String error_desc;
        // 返回结果 T/F
        private String result;

        private String trace_id;

        public String getConsumer() {
                return consumer;
        }

        public void setConsumer(String consumer) {
                this.consumer = consumer;
        }

        public String getProvider() {
                return provider;
        }

        public void setProvider(String provider) {
                this.provider = provider;
        }

        public String getBusiness() {
                return business;
        }

        public void setBusiness(String business) {
                this.business = business;
        }

        public String getError_desc() {
                return error_desc;
        }

        public void setError_desc(String error_desc) {
                this.error_desc = error_desc;
        }

        public String getResult() {
                return result;
        }

        public void setResult(String result) {
                this.result = result;
        }

        public String getTrace_id() {
                return trace_id;
        }

        public void setTrace_id(String trace_id) {
                this.trace_id = trace_id;
        }
}
