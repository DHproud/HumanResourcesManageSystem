package HumanManage.modules.org.service;

import HumanManage.modules.org.entity.Position;
import com.baomidou.mybatisplus.extension.service.IService;
import java.util.List;

public interface PositionService extends IService<Position> {

    // 添加职位（带业务逻辑校验）
    void addPosition(Position position);

    // 根据机构ID查询职位列表（用于下拉框或列表筛选）
    List<Position> listByOrgId(Long orgId);
}