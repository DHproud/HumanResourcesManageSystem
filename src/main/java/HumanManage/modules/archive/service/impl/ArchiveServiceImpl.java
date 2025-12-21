package HumanManage.modules.archive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import HumanManage.modules.archive.entity.Archive;
import HumanManage.modules.archive.mapper.ArchiveMapper;
import HumanManage.modules.archive.service.ArchiveService;
import HumanManage.modules.org.entity.Organization;
import HumanManage.modules.org.service.OrganizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;

@Service
public class ArchiveServiceImpl extends ServiceImpl<ArchiveMapper, Archive> implements ArchiveService {

    @Autowired
    private OrganizationService organizationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerArchive(Archive archive) {
        // 1. 补全机构名称快照 (即使前端传了，为了安全最好后端再查一次)
        fillOrgNames(archive);

        // 2. 自动生成档案编号
        String code = generateArchiveCode(archive);
        archive.setArchiveCode(code);

        // 3. 设置初始状态
        archive.setStatus(0); // 0: 待复核
        archive.setRegistTime(LocalDateTime.now());
        // archive.setRegistrant("admin"); // TODO: 接入SpringSecurity后从Context获取

        // 4. 落库保存
        this.save(archive);
    }

    @Override
    public List<Archive> getPendingReviews() {
        // 查询 status = 0 的记录，按创建时间倒序
        return this.list(new LambdaQueryWrapper<Archive>()
                .eq(Archive::getStatus, 0)
                .orderByDesc(Archive::getCreateTime));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewPass(Archive archive) {
        // 复核时，只允许更新部分字段（基础信息、详细信息），不允许更新核心归属信息（机构、职位、编号）
        // 这里我们查出数据库里的旧数据
        Archive oldArchive = this.getById(archive.getId());
        if (oldArchive == null) {
            throw new RuntimeException("档案不存在");
        }

        // 更新允许修改的字段
        oldArchive.setName(archive.getName());
        oldArchive.setGender(archive.getGender());
        oldArchive.setIdCard(archive.getIdCard());
        oldArchive.setEmail(archive.getEmail());
        oldArchive.setMobile(archive.getMobile());
        oldArchive.setAddress(archive.getAddress());
        oldArchive.setJobTitleName(archive.getJobTitleName());
        oldArchive.setEducation(archive.getEducation());
        oldArchive.setMajor(archive.getMajor());
        oldArchive.setPoliticalStatus(archive.getPoliticalStatus());
        oldArchive.setNationality(archive.getNationality());

        // 核心状态流转
        oldArchive.setStatus(1); // 1: 正常 (复核通过)
        oldArchive.setReviewTime(LocalDateTime.now());
        oldArchive.setReviewer("人事经理"); // TODO: 获取当前登录用户

        this.updateById(oldArchive);
    }

    // --- 私有辅助方法 ---

    // 补全机构名称
    private void fillOrgNames(Archive archive) {
        if (archive.getFirstLevelOrgId() != null) {
            Organization o = organizationService.getById(archive.getFirstLevelOrgId());
            if (o != null) archive.setFirstLevelOrgName(o.getOrgName());
        }
        if (archive.getSecondLevelOrgId() != null) {
            Organization o = organizationService.getById(archive.getSecondLevelOrgId());
            if (o != null) archive.setSecondLevelOrgName(o.getOrgName());
        }
        if (archive.getThirdLevelOrgId() != null) {
            Organization o = organizationService.getById(archive.getThirdLevelOrgId());
            if (o != null) archive.setThirdLevelOrgName(o.getOrgName());
        }
    }

    // 生成编号: YYYY(4) + L1(2) + L2(2) + L3(2) + Seq(2)
    private String generateArchiveCode(Archive archive) {
        String year = String.valueOf(Year.now().getValue());

        String c1 = getOrgCode(archive.getFirstLevelOrgId());
        String c2 = getOrgCode(archive.getSecondLevelOrgId());
        String c3 = getOrgCode(archive.getThirdLevelOrgId());

        String prefix = year + c1 + c2 + c3;

        // 查询当前前缀下的最大编号
        // SELECT count(*) FROM hr_archive WHERE archive_code LIKE 'prefix%'
        long count = this.count(new LambdaQueryWrapper<Archive>()
                .likeRight(Archive::getArchiveCode, prefix));

        return prefix + String.format("%02d", count + 1);
    }

    private String getOrgCode(Long orgId) {
        if (orgId == null) return "00";
        Organization org = organizationService.getById(orgId);
        // 如果数据库org_code没维护好，默认给00防止报错
        return (org != null && org.getOrgCode() != null) ? org.getOrgCode() : "00";
    }
}