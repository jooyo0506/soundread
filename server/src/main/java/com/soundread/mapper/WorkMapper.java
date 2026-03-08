package com.soundread.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soundread.model.entity.Work;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkMapper extends BaseMapper<Work> {
}
