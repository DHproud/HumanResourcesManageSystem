package HumanManage.modules.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import HumanManage.modules.salary.entity.SalaryProject;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SalaryProjectMapper extends BaseMapper<SalaryProject> {
    // 如需自定义 SQL，可在此添加方法并在 XML 中实现（目前无需）
}