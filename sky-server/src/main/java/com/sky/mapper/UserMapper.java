package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户信息
     * @return
     */
    User getByOpenId();

    /**
     * 插入数据
     * @param user
     */
    void insert(User user);
}
