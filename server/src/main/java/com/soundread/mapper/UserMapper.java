package com.soundread.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.soundread.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 原子激活 VIP（防并发覆盖）
     *
     * <p>
     * 使用 SQL GREATEST 避免 select→set→update 的读写竞争：
     * 到期时间从当前时间或已有到期时间（取更晚的）开始叠加天数，
     * VIP 等级取当前等级和新等级的较大值（不降级）。
     * </p>
     *
     * @return 影响行数（正常为 1）
     */
    @Update("UPDATE user SET " +
            "  vip_expire_time = DATE_ADD(GREATEST(COALESCE(vip_expire_time, NOW()), NOW()), INTERVAL #{durationDays} DAY), "
            +
            "  vip_level = GREATEST(COALESCE(vip_level, 0), #{vipLevel}), " +
            "  tier_code = #{tierCode}, " +
            "  updated_at = NOW() " +
            "WHERE id = #{userId}")
    int atomicActivateVip(@Param("userId") Long userId,
            @Param("durationDays") int durationDays,
            @Param("vipLevel") int vipLevel,
            @Param("tierCode") String tierCode);
}
