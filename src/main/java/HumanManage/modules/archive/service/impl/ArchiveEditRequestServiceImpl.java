package HumanManage.modules.archive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import HumanManage.modules.archive.entity.Archive;
import HumanManage.modules.archive.entity.ArchiveEditRequest;
import HumanManage.modules.archive.mapper.ArchiveEditRequestMapper;
import HumanManage.modules.archive.mapper.ArchiveMapper;
import HumanManage.modules.archive.service.ArchiveEditRequestService;
import HumanManage.modules.archive.service.ArchiveManageService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * 实现：提交/列出/审批/拒绝
 */
@Service
public class ArchiveEditRequestServiceImpl implements ArchiveEditRequestService {

    @Resource
    private ArchiveEditRequestMapper requestMapper;

    @Resource
    private ArchiveManageService archiveManageService;

    // 用于在审批“通过新建档案”时插入到 archive 表
    @Resource
    private ArchiveMapper archiveMapper;

    @Override
    public void submitRequest(ArchiveEditRequest req) {
        if (req.getArchiveId() == null) {
            // 新建申请时 archiveId 可以为空或 0
        }
        req.setRequestTime(LocalDateTime.now());
        req.setStatus(0); // 待复核
        requestMapper.insert(req);
    }

    @Override
    public IPage<ArchiveEditRequest> pageRequests(Page<ArchiveEditRequest> page, Integer status) {
        QueryWrapper<ArchiveEditRequest> wrapper = new QueryWrapper<>();
        if (status != null) wrapper.eq("status", status);
        wrapper.orderByDesc("request_time");
        return requestMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveRequest(Long id, String reviewer) {
        ArchiveEditRequest req = requestMapper.selectById(id);
        if (req == null) {
            throw new RuntimeException("申请不存在");
        }
        if (req.getStatus() != null && req.getStatus() != 0) {
            throw new RuntimeException("申请不处于待复核状态");
        }

        try {
            // 使用支持 Java8 时间类型的 ObjectMapper 以避免 LocalDateTime 反序列化错误
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            Archive newArchive = mapper.readValue(req.getNewData(), Archive.class);

            if (req.getArchiveId() == null || req.getArchiveId() == 0) {
                // 新建档案：插入到 hr_archive 表
                newArchive.setId(null);
                newArchive.setCreateTime(LocalDateTime.now());
                newArchive.setUpdateTime(LocalDateTime.now());
                if (newArchive.getStatus() == null) newArchive.setStatus(1);
                archiveMapper.insert(newArchive);

                // 将申请的 archiveId 更新为新插入的 id（便于追溯）
                req.setArchiveId(newArchive.getId());
            } else {
                // 修改已有档案：调用 ArchiveManageService.updateArchive
                archiveManageService.updateArchive(newArchive);
            }

            // 更新申请状态为“通过”
            req.setStatus(1);
            req.setReviewer(reviewer);
            req.setReviewTime(LocalDateTime.now());
            requestMapper.updateById(req);
        } catch (Exception e) {
            throw new RuntimeException("处理审批失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRequest(Long id, String reviewer, String remark) {
        ArchiveEditRequest req = requestMapper.selectById(id);
        if (req == null) throw new RuntimeException("申请不存在");
        if (req.getStatus() != null && req.getStatus() != 0) throw new RuntimeException("申请不处于待复核状态");

        req.setStatus(2);
        req.setReviewer(reviewer);
        req.setReviewTime(LocalDateTime.now());
        req.setRemark(remark);
        requestMapper.updateById(req);
    }
}