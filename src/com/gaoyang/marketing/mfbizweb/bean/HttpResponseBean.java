package com.gaoyang.marketing.mfbizweb.bean;/**
 * Created by zhanghui on 2018-9-21.
 */

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * @author zhanghui
 * @create 2018-9-21
 * @description
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class HttpResponseBean implements Serializable {
    private static final long serialVersionUID = 3990273656099723203L;
    private String code;
    private String msg;
    private String exmsg;
    private Object data;
    private String exurl;

    public String getExurl() {
        return exurl;
    }

    public void setExurl(String exurl) {
        this.exurl = exurl;
    }

    public String getExmsg() {
        return exmsg;
    }

    public void setExmsg(String exmsg) {
        this.exmsg = exmsg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
