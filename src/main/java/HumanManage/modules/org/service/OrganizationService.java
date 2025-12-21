package HumanManage.modules.org.service;

import com.baomidou.mybatisplus.extension.service.IService;
import HumanManage.modules.org.entity.Organization;
import java.util.List;

public interface OrganizationService extends IService<Organization> {

    /**
     * 根据父级ID获取下级机构列表
     * @param parentId 父ID
     * @return 机构列表
     */
    List<Organization> listByParent(Long parentId);
}