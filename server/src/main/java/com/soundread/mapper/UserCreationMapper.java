package com.soundread.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soundread.model.entity.UserCreation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户创作记录 Mapper
 *
 * @author SoundRead
 */
@Mapper
public interface UserCreationMapper extends BaseMapper<UserCreation> {
}
