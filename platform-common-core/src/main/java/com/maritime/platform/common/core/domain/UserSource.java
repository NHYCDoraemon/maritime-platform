package com.maritime.platform.common.core.domain;

public enum UserSource {
    /** 三定编内人员，数据中台同步（含借调/调动/挂职）。 */
    SYNC,
    /** 编外人员，管理员手工创建（外包/派遣/临聘/专家顾问）。 */
    EXTERNAL,
    /** 系统内置超管，初始化自动创建。 */
    BUILTIN,
    /** 测试用户，管理员手工创建。 */
    MANUAL
}
