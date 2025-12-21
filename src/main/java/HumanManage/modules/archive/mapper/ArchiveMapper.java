package HumanManage.modules.archive.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import HumanManage.modules.archive.entity.Archive;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArchiveMapper extends BaseMapper<Archive> {
    /**
     * 自定义分页查询（显式声明在Mapper层）
     * 使用 MyBatis-Plus 的 Wrapper 进行动态 SQL 组装，但通过 Mapper 方法暴露
     */
    IPage<Archive> selectArchivePage(IPage<Archive> page, @Param(Constants.WRAPPER) QueryWrapper<Archive> wrapper);
}