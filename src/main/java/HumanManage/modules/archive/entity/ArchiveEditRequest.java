package HumanManage.modules.archive.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 档案修改申请实体：保存人事专员提交的修改（JSON 格式）
 */
@Data
@TableName("hr_archive_edit_request")
public class ArchiveEditRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long archiveId;

    private String newData; // 存 JSON 字符串

    private String requester;
    private LocalDateTime requestTime;

    /**
     * 0 = 待复核, 1 = 通过, 2 = 拒绝
     */
    private Integer status;

    private String reviewer;
    private LocalDateTime reviewTime;
    private String remark;
}