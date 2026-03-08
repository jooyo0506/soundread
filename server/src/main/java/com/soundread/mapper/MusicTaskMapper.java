package com.soundread.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soundread.model.entity.MusicTask;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 音乐任务 Mapper
 */
@Mapper
public interface MusicTaskMapper extends BaseMapper<MusicTask> {
}
