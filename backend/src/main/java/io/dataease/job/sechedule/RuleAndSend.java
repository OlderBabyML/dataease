package io.dataease.job.sechedule;

import io.dataease.plugins.common.base.domain.SysUser;

import java.io.Serializable;
import java.util.List;

public class RuleAndSend implements Serializable {
    private String type;
    private String operate;
    private String value;
    private String color;
    private Object send;
    private List<SysUser> users;
    private String link;
    private String condition;
    private String conditionValue;

    private Integer numDay;

    public RuleAndSend() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOperate() {
        return operate;
    }

    public void setOperate(String operate) {
        this.operate = operate;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public List<SysUser> getUsers() {
        return users;
    }

    public void setUsers(List<SysUser> users) {
        this.users = users;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getNumDay() {
        return numDay;
    }

    public void setNumDay(Integer numDay) {
        this.numDay = numDay;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getConditionValue() {
        return conditionValue;
    }

    public void setConditionValue(String conditionValue) {
        this.conditionValue = conditionValue;
    }

    public Object getSend() {
        return send;
    }

    public void setSend(Object send) {
        this.send = send;
    }
}
