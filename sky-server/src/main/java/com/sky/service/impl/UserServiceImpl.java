package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    public static final String WX_LOGIN_URL = "https://api.weixin.qq.com/sns/jscode2session";

    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;


    /**
     * 微信登录
     *
     * @param userLoginDTO
     * @return
     */
    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {
        // 通过微信接口服务获取openid
        String openid = getOpenId(userLoginDTO.getCode());

        // 判断openid是否为空，如果为空表示登陆失败，抛出业务异常
        if (openid == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        // 判断用户是否为新用户
        User user = userMapper.getByOpenId();

        // 如果是新用户则自动注册
        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        // 返回用户对象
        return user;
    }

    /**
     * 访问微信接口服务，获取openid
     * @param code
     * @return
     */
    private String getOpenId(String code) {
        // 通过微信接口服务获取openid
        Map<String, String> params = new HashMap<>();
        params.put("appid", weChatProperties.getAppid());
        params.put("secret", weChatProperties.getSecret());
        params.put("js_code", code);
        params.put("grant_type", "authorization_code");

        // 发送GET请求到微信登录接口，获取返回的JSON字符串
        String json = HttpClientUtil.doGet(WX_LOGIN_URL, params);
        // 将返回的JSON字符串解析成JSON对象格式
        JSONObject jsonObject = JSON.parseObject(json);
        // 从JSON对象中获取openid字段的值
        String openid = jsonObject.getString("openid");
        return openid;
    }
}
