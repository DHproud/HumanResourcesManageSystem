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
import java.util.List;

@Data
@TableName("hr_pay_record")
public class PayRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long runId;

    private Long employeeId;
    private String employeeNo;
    private String employeeName;
    private String position;

    private BigDecimal basicSalary;
    private BigDecimal reward;
    private BigDecimal deduction;
    private BigDecimal totalPayable;

    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer isDeleted;

    // 非数据库字段：明细项
    private transient List<PayRecordItem> items;
}