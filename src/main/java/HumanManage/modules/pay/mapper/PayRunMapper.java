package HumanManage.modules.pay.mapper;

import HumanManage.modules.pay.entity.PayRun;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PayRunMapper extends BaseMapper<PayRun> {
    // 你可以在这里添加自定义查询方法
}