package HumanManage.modules.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import HumanManage.modules.salary.entity.SalaryProject;

public interface SalaryProjectService extends IService<SalaryProject> {
    // 可在此扩展额外方法，例如按 code 查找等
    SalaryProject getByProjectCode(String projectCode);
}