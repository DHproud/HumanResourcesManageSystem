package HumanManage.modules.salary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 薪酬项目实体
 */
@Data
@TableName("hr_salary_project")
public class SalaryProject {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String projectCode;   // 项目编号，如 S001

    private String projectName;   // 项目名称，如 基本工资

    private String description;   // 项目说明

    private Integer status;       // 1=启用，0=禁用

    private String createdBy;     // 创建人

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Integer isDeleted;    // 0 未删除, 1 已删除
}