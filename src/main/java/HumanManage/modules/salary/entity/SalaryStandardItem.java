package HumanManage.modules.salary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 薪酬标准条目（每个薪酬项目对应的金额或系统生成项）
 */
@Data
@TableName("hr_salary_standard_item")
public class SalaryStandardItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联主表
     */
    private Long standardId;

    /**
     * 对应管理员维护的薪酬项目 id（允许为空以保留历史或系统项）
     */
    private Long projectId;

    /**
     * 项目编号（冗余或系统项代码）
     */
    private String projectCode;

    /**
     * 项目名称（冗余）
     */
    private String projectName;

    /**
     * 金额（两位小数）
     */
    private BigDecimal amount;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}