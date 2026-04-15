package com.maritime.platform.common.core.util;

import java.util.Objects;

/**
 * 分片键计算工具类 — 纯 Java，无框架依赖，可在 domain 层使用。
 *
 * <p>所有写入 iam_user / iam_user_org_rel / iam_user_role 的代码必须调用
 * {@link #compute(String)} 计算 shard_key。
 * 所有查询这三张表的代码必须带 shard_key 条件做分区裁剪。</p>
 *
 * <p>SHARD_COUNT 是红线常量，修改需要 DDL 重分区。</p>
 */
public final class IamShardKeyUtil {

    /** 分片数量 — 红线常量，修改需要 DDL 重分区 */
    public static final int SHARD_COUNT = 4;

    private IamShardKeyUtil() {
    }

    /**
     * 根据 userId 计算分片键。
     *
     * @param userId 用户标识，不能为 null
     * @return 0 ~ (SHARD_COUNT - 1) 范围内的整数
     */
    public static int compute(String userId) {
        Objects.requireNonNull(userId, "userId required");
        return Math.floorMod(userId.hashCode(), SHARD_COUNT);
    }
}
