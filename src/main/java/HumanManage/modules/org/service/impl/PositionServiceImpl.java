package HumanManage.modules.org.service.impl;

import HumanManage.modules.org.entity.Position;
import HumanManage.modules.org.mapper.PositionMapper;
import HumanManage.modules.org.service.PositionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class PositionServiceImpl extends ServiceImpl<PositionMapper, Position> implements PositionService {

    @Override
    public void addPosition(Position position) {
        // 1. 校验必填项
        if (!StringUtils.hasText(position.getName())) {
            throw new RuntimeException("职位名称不能为空");
        }
        if (position.getOrgId() == null) {
            throw new RuntimeException("必须选择所属机构");
        }

        // 2. 查重逻辑：同一个机构下，不能有两个重名的职位
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Position::getOrgId, position.getOrgId())
                .eq(Position::getName, position.getName());

        long count = this.count(wrapper);
        if (count > 0) {
            throw new RuntimeException("该机构下已存在同名职位，请勿重复添加");
        }

        // 3. 执行保存
        this.save(position);
    }

    // 【核心修正】实现了接口定义的 listByOrgId 方法，解决报错
    @Override
    public List<Position> listByOrgId(Long orgId) {
        LambdaQueryWrapper<Position> wrapper = new LambdaQueryWrapper<>();
        // 如果传了 orgId 就查特定的，没传就查所有
        if (orgId != null && orgId != 0) {
            wrapper.eq(Position::getOrgId, orgId);
        }
        wrapper.orderByDesc(Position::getCreateTime);
        return this.list(wrapper);
    }
}