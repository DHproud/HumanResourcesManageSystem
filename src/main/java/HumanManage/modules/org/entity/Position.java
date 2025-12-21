package HumanManage.modules.org.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_position")
public class Position implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // 职位名称 (如：前端工程师)
    @TableField("position_name")
    private String name;

    // 职位分类 (如：技术、管理、市场，可选)
    private String category;

    // 关联的三级机构ID (该职位属于哪个部门)
    private Long orgId;

    // 冗余字段：机构名称 (方便前端显示：技术中心/研发部/前端组)
    private String orgNamePath;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}