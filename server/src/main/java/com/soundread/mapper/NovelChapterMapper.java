package com.soundread.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soundread.model.entity.NovelChapter;
import org.apache.ibatis.annotations.Mapper;

/**
 * 有声书章节 Mapper
 *
 * @author SoundRead
 */
@Mapper
public interface NovelChapterMapper extends BaseMapper<NovelChapter> {
}
