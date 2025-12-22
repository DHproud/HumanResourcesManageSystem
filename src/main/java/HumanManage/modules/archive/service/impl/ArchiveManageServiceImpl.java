package HumanManage.modules.archive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import HumanManage.modules.archive.entity.Archive;
import HumanManage.modules.archive.mapper.ArchiveMapper;
import HumanManage.modules.archive.service.ArchiveManageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ArchiveManageServiceImpl implements ArchiveManageService {

    @Resource
    private ArchiveMapper archiveMapper;

    @Override
    public IPage<Archive> queryArchivePage(Archive param) {
        int pageNum = (param.getPage() == null || param.getPage() <= 0) ? 1 : param.getPage();
        int pageSize = (param.getSize() == null || param.getSize() <= 0) ? 10 : param.getSize();

        Page<Archive> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Archive> wrapper = new QueryWrapper<>();

        // 状态筛选：若前端传 status 则使用，否则默认查询正常档案 status=1
        if (param.getStatus() != null) {
            wrapper.eq("status", param.getStatus());
        } else {
            wrapper.eq("status", 1);
        }

        if (param.getFirstLevelOrgId() != null) wrapper.eq("first_level_org_id", param.getFirstLevelOrgId());
        if (param.getSecondLevelOrgId() != null) wrapper.eq("second_level_org_id", param.getSecondLevelOrgId());
        if (param.getThirdLevelOrgId() != null) wrapper.eq("third_level_org_id", param.getThirdLevelOrgId());

        if (StringUtils.isNotBlank(param.getPositionName())) wrapper.eq("position_name", param.getPositionName());

        LocalDateTime start = parseDate(param.getStartTime(), true);
        LocalDateTime end = parseDate(param.getEndTime(), false);
        if (start != null) wrapper.ge("regist_time", start);
        if (end != null) wrapper.le("regist_time", end);

        wrapper.orderByDesc("regist_time");
        return archiveMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArchive(Archive archive) {
        if (archive.getId() == null) throw new IllegalArgumentException("档案ID不能为空");
        archiveMapper.updateById(archive);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markDeleted(Long id) {
        Archive a = archiveMapper.selectById(id);
        if (a == null) throw new RuntimeException("档案不存在");
        if (a.getStatus() != null && a.getStatus() == 0) throw new RuntimeException("待复核的档案不能删除");
        if (a.getStatus() != null && a.getStatus() == 2) throw new RuntimeException("档案已处于已删除状态");
        a.setStatus(2);
        a.setUpdateTime(LocalDateTime.now());
        archiveMapper.updateById(a);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverArchive(Long id) {
        Archive a = archiveMapper.selectById(id);
        if (a == null) throw new RuntimeException("档案不存在");
        if (a.getStatus() == null || a.getStatus() != 2) throw new RuntimeException("仅允许恢复已删除的档案");
        a.setStatus(1);
        a.setUpdateTime(LocalDateTime.now());
        archiveMapper.updateById(a);
    }

    @Override
    public List<Archive> queryByStatus(Integer status) {
        QueryWrapper<Archive> wrapper = new QueryWrapper<>();
        wrapper.eq("status", status);
        return archiveMapper.selectList(wrapper);
    }

    private LocalDateTime parseDate(String text, boolean isStart) {
        if (!StringUtils.isNotBlank(text)) return null;
        try {
            if (text.length() == 10) {
                String t = isStart ? text + " 00:00:00" : text + " 23:59:59";
                return LocalDateTime.parse(t, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else {
                return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception e) {
            return null;
        }
    }
}