package HumanManage.modules.salary.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import HumanManage.modules.salary.entity.SalaryProject;
import HumanManage.modules.salary.mapper.SalaryProjectMapper;
import HumanManage.modules.salary.service.SalaryProjectService;
import org.springframework.stereotype.Service;

@Service
public class SalaryProjectServiceImpl extends ServiceImpl<SalaryProjectMapper, SalaryProject> implements SalaryProjectService {

    @Override
    public SalaryProject getByProjectCode(String projectCode) {
        return lambdaQuery()
                .eq(SalaryProject::getProjectCode, projectCode)
                .eq(SalaryProject::getIsDeleted, 0)
                .one();
    }
}