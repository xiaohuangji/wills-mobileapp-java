package com.renren.mobile.mcp.api.command.user;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.renren.mobile.mcp.api.command.AbstractApiCommand;
import com.renren.mobile.mcp.api.entity.ApiCommandContext;
import com.renren.mobile.mcp.api.entity.ApiResult;
import com.renren.mobile.mcp.api.entity.ApiResultCode;
import com.renren.mobile.mcp.utils.McpUtils;
import com.tg.service.UserService;

public class UserChangeUserInfoCommand extends AbstractApiCommand {

	private static final Log logger = LogFactory.getLog(UserChangeUserInfoCommand.class);

    private UserService userService;

    @Override
    public ApiResult onExecute(ApiCommandContext context) {

        // 取参数
       int userId = context.getUserId();
        Map<String, String> stringParams = context.getStringParams();

        String headUrl=stringParams.get("headUrl");
        String userName=stringParams.get("userName");
        String gender=stringParams.get("gender");
      
        Object result = null;
        ApiResult apiResult = null;

        // 执行RPC调用       
        try {
            long t = System.currentTimeMillis();
            result = userService.changeUserInfo(userId, userName, Integer.valueOf(gender), headUrl);
            McpUtils.rpcTimeCost(t, "user.changeUserInfo");
        } catch (Exception e) {
            // 异常记录日志， 返回错误信息
            logger.error("RPC error ", e);
            apiResult = new ApiResult(ApiResultCode.E_SYS_RPC_ERROR);
            return apiResult;
        }

        // 正常返回接口数据
        apiResult = new ApiResult(ApiResultCode.SUCCESS, result);
        return apiResult;
    }

	public void setUserService(UserService userService) {
		this.userService = userService;
	}
}
