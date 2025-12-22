package HumanManage.modules.pay.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("hr_pay_run")
public class PayRun implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String runCode;

    private Long firstLevelOrgId;
    private String firstLevelOrgName;
    private Long secondLevelOrgId;
    private String secondLevelOrgName;
    private Long thirdLevelOrgId;
    private String thirdLevelOrgName;

    private Integer totalCount;
    private BigDecimal totalBasic; // 基本薪酬总额

    private Integer status; // 0: 待登记

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private Integer isDeleted;
}