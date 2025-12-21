package HumanManage.modules.archive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import HumanManage.modules.archive.entity.Archive;
import HumanManage.modules.archive.entity.ArchiveQueryVo;
import HumanManage.modules.archive.mapper.ArchiveMapper;
import HumanManage.modules.archive.service.ArchiveManageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 说明：
 * - 不使用 org.springframework.util.StringUtils
 * - 使用 MP 的 StringUtils 或纯手写非空判断
 * - 不改动 ArchiveMapper（沿用 BaseMapper 的内置 selectPage/updateById）
 */
@Service
public class ArchiveManageServiceImpl implements ArchiveManageService {

    @Resource
    private ArchiveMapper archiveMapper;

    @Override
    public IPage<Archive> queryArchivePage(ArchiveQueryVo vo) {
        Page<Archive> page = new Page<>(vo.getPage(), vo.getSize());
        QueryWrapper<Archive> wrapper = new QueryWrapper<>();

        // 默认查询已复核通过的档案（如需包含全部，可移除该条件）
        wrapper.eq("status", 1);

        // 机构三级联动（与关系）
        if (vo.getFirstLevelOrgId() != null) {
            wrapper.eq("first_level_org_id", vo.getFirstLevelOrgId());
        }
        if (vo.getSecondLevelOrgId() != null) {
            wrapper.eq("second_level_org_id", vo.getSecondLevelOrgId());
        }
        if (vo.getThirdLevelOrgId() != null) {
            wrapper.eq("third_level_org_id", vo.getThirdLevelOrgId());
        }

        // 职位名称（下拉选择 => 精确匹配；若改为输入框可使用 like）
        if (StringUtils.isNotBlank(vo.getPositionName())) {
            wrapper.eq("position_name", vo.getPositionName());
        }

        // 建档时间范围（支持两种格式）
        LocalDateTime start = parseDate(vo.getStartTime(), true);
        LocalDateTime end = parseDate(vo.getEndTime(), false);
        if (start != null) {
            wrapper.ge("regist_time", start);
        }
        if (end != null) {
            wrapper.le("regist_time", end);
        }

        // 排序：按建档时间倒序
        wrapper.orderByDesc("regist_time");

        // 使用 BaseMapper 内置分页，不需要 XML
        return archiveMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArchive(Archive archive) {
        if (archive.getId() == null) {
            throw new IllegalArgumentException("档案ID不能为空");
        }
        // 直接调用 BaseMapper 内置更新（不改动原 ArchiveService/Impl）
        archiveMapper.updateById(archive);
    }

    /**
     * 解析日期字符串：
     * - "yyyy-MM-dd" => 补足为当天 00:00:00 或 23:59:59
     * - "yyyy-MM-dd HH:mm:ss" => 直接解析
     */
    private LocalDateTime parseDate(String text, boolean isStart) {
        if (!StringUtils.isNotBlank(text)) return null;

        // 纯手写非空判断也可以：
        // if (text == null || text.trim().isEmpty()) return null;

        try {
            if (text.length() == 10) {
                // 只有日期
                String t = isStart ? text + " 00:00:00" : text + " 23:59:59";
                return LocalDateTime.parse(t, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else {
                return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception e) {
            // 非法格式直接忽略该条件
            return null;
        }
    }
}