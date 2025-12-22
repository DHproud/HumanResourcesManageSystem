package HumanManage.modules.salary.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 薪酬标准主表（包含适用职位、三险一金开关与复核字段）
 */
@Data
@TableName("hr_salary_standard")
public class SalaryStandard {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String standardCode;
    private String standardName;
    private String author;
    private String registrant;
    private LocalDateTime registerTime;

    /**
     * 适用职位 id（引用职位表）
     */
    private Long applicablePositionId;

    /**
     * 适用职位冗余文本（便于显示）
     */
    private String applicablePosition;

    /**
     * 三险一金开关（0/1）
     */
    private Integer includePension;       // 养老
    private Integer includeMedical;      // 医疗
    private Integer includeUnemployment; // 失业
    private Integer includeHousing;      // 住房公积金

    /**
     * 复核相关字段
     * reviewStatus: 0=待复核, 1=通过, 2=拒绝
     * reviewer: 复核人用户名
     * reviewTime: 复核时间
     * reviewComment: 复核意见（长文本）
     */
    private Integer reviewStatus;
    private String reviewer;
    private LocalDateTime reviewTime;
    private String reviewComment;

    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;
}