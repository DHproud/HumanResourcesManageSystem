package HumanManage.modules.sys.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import HumanManage.modules.sys.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}