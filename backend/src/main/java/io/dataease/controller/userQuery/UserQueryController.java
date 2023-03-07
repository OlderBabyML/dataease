package io.dataease.controller.userQuery;

import com.alibaba.fastjson.JSONObject;
import com.github.xiaoymin.knife4j.annotations.ApiSupport;
import io.dataease.dto.userQuery.UserInfoDTO;
import io.dataease.service.userQuery.UserQueryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@Api(tags = "用户细查")
@ApiSupport(order = 230)
@RestController
@RequestMapping("/user/query")
public class UserQueryController {
    @Resource
    private UserQueryService userQueryService;

    @ApiOperation("获取用户信息")
    @PostMapping("userInfo")
    public List<JSONObject> getUserInfo(@RequestBody UserInfoDTO userInfoDTO) throws Exception {
        return userQueryService.getUserInfo(userInfoDTO);
    }

    @ApiOperation("获取用户列表")
    @PostMapping("userList")
    public List<JSONObject> getUserList(@RequestBody UserInfoDTO userInfoDTO) throws Exception {
        return userQueryService.getUserList(userInfoDTO);
    }

    @ApiOperation("获取用户事件数量")
    @PostMapping("userEventNum")
    public JSONObject getUserEventNum(@RequestBody UserInfoDTO userInfoDTO) throws Exception {
        return userQueryService.getUserEventNum(userInfoDTO);
    }

    @ApiOperation("获取用户事件明细")
    @PostMapping("userEventDetail")
    public List<JSONObject> getUserEventDetail(@RequestBody UserInfoDTO userInfoDTO) throws Exception {
        return userQueryService.getUserEventDetail(userInfoDTO);
    }

    @ApiOperation("获取用户事件明细按天")
    @PostMapping("userEventDetailForDate")
    public List<JSONObject> getUserEventDetailForDate(@RequestBody UserInfoDTO userInfoDTO) throws Exception {
        return userQueryService.getUserEventDetailForDate(userInfoDTO);
    }

    @ApiOperation("获取筛选项")
    @PostMapping("options")
    public List<JSONObject> getOptions(@RequestBody UserInfoDTO userInfoDTO) throws Exception {
        return userQueryService.getOptions(userInfoDTO);
    }
}
