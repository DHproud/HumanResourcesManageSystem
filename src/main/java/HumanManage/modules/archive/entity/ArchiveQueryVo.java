package HumanManage.modules.archive.entity;

import lombok.Data;
import java.io.Serializable;

/**
 * 档案查询条件视图对象
 */
@Data
public class ArchiveQueryVo implements Serializable {
    // 分页参数
    private Integer page = 1;
    private Integer size = 10;

    // --- 查询条件 ---
    private Long firstLevelOrgId;   // 一级机构
    private Long secondLevelOrgId;  // 二级机构
    private Long thirdLevelOrgId;   // 三级机构
    private String positionName;    // 职位名称

    // 建档时间范围 (字符串格式，方便前端传递)
    private String startTime;
    private String endTime;
}
