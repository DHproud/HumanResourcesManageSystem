package HumanManage.modules.org.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import HumanManage.modules.org.entity.Organization;
import HumanManage.modules.org.mapper.OrganizationMapper;
import HumanManage.modules.org.service.OrganizationService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrganizationServiceImpl extends ServiceImpl<OrganizationMapper, Organization> implements OrganizationService {

    @Override
    public List<Organization> listByParent(Long parentId) {
        return this.list(new LambdaQueryWrapper<Organization>()
                .eq(Organization::getParentId, parentId)
                .orderByAsc(Organization::getSort)); // 按排序字段排序
    }
}