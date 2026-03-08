package com.soundread.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soundread.model.entity.UserStorage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户存储计量 Mapper
 *
 * @author SoundRead
 */
@Mapper
public interface UserStorageMapper extends BaseMapper<UserStorage> {

        /**
         * 原子性地增加存储用量
         *
         * @param userId     用户ID
         * @param deltaBytes 增量字节数（正数增加，负数释放）
         * @param deltaFiles 文件数量增量
         */
        @Update("INSERT INTO user_storage (user_id, used_bytes, file_count, last_calculated_at) " +
                        "VALUES (#{userId}, GREATEST(0, #{deltaBytes}), GREATEST(0, #{deltaFiles}), NOW()) " +
                        "ON DUPLICATE KEY UPDATE " +
                        "used_bytes = GREATEST(0, used_bytes + #{deltaBytes}), " +
                        "file_count = GREATEST(0, file_count + #{deltaFiles}), " +
                        "last_calculated_at = NOW()")
        void updateStorageDelta(@Param("userId") Long userId,
                        @Param("deltaBytes") long deltaBytes,
                        @Param("deltaFiles") int deltaFiles);
}
