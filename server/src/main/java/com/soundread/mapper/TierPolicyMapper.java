package com.soundread.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soundread.model.entity.SysTierPolicy;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TierPolicyMapper extends BaseMapper<SysTierPolicy> {
}
