package io.dataease.dto.userQuery;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

import java.util.List;

@Data
public class UserInfoDTO {
    public Long userId;
    public Long uid;
    public String country;
    public String product;
    public Long startTime;
    public Long endTime;
    public String sort;
    public Integer pageNo;
    public String date;
    public List<JSONObject> eventList;
    public List<JSONObject> userList;
}
