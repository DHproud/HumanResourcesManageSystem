package HumanManage.modules.archive.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("hr_archive")
public class Archive implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    // 档案编号 (系统自动生成)
    private String archiveCode;

    // --- 机构信息 (冗余存储名称，用于快照) ---
    private Long firstLevelOrgId;
    private String firstLevelOrgName;
    private Long secondLevelOrgId;
    private String secondLevelOrgName;
    private Long thirdLevelOrgId;
    private String thirdLevelOrgName;

    // --- 基础信息 ---
    private String name;
    private String gender;
    private String idCard;
    private String email;
    private String mobile;
    private String address;
    private String photoUrl;

    // --- 职位薪酬 ---
    private String positionName;
    private String jobTitleName;
    private Long salaryStandardId;
    private String salaryStandardName;

    // --- 详细信息 ---
    private String nationality;
    private String birthplace;
    private String education;
    private String major;
    private String politicalStatus;

    // --- 状态与审计 ---
    // 0:待复核 1:正常 2:已删除
    private Integer status;
    private String registrant; // 登记人
    private LocalDateTime registTime;
    private String reviewer;   // 复核人
    private LocalDateTime reviewTime;

    // --- 系统字段 ---
    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    // 以下字段仅用于接收前端查询参数（非数据库字段）
    @TableField(exist = false)
    private Integer page;

    @TableField(exist = false)
    private Integer size;

    @TableField(exist = false)
    private String startTime;

    @TableField(exist = false)
    private String endTime;
}