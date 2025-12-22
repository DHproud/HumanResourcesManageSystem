package HumanManage.modules.archive.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import HumanManage.modules.archive.entity.ArchiveEditRequest;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArchiveEditRequestMapper extends BaseMapper<ArchiveEditRequest> {
    // 标准的 CRUD 足够；若需分页/高级查询可在 service 层实现
}