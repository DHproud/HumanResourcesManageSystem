package HumanManage.modules.pay.service.impl;

import HumanManage.modules.archive.entity.Archive;
import HumanManage.modules.archive.mapper.ArchiveMapper;
import HumanManage.modules.pay.entity.PayRecord;
import HumanManage.modules.pay.entity.PayRecordItem;
import HumanManage.modules.pay.entity.PayRun;
import HumanManage.modules.pay.mapper.PayRecordItemMapper;
import HumanManage.modules.pay.mapper.PayRecordMapper;
import HumanManage.modules.pay.mapper.PayRunMapper;
import HumanManage.modules.pay.service.PayRecordService;
import HumanManage.modules.pay.service.PayRunService;
import HumanManage.modules.salary.entity.SalaryStandardItem;
import HumanManage.modules.salary.mapper.SalaryStandardItemMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PayRunServiceImpl implements PayRunService {

    @Resource
    private PayRunMapper payRunMapper;

    @Resource
    private PayRecordMapper payRecordMapper;

    @Resource
    private PayRecordItemMapper payRecordItemMapper;

    @Resource
    private ArchiveMapper archiveMapper;

    @Resource
    private SalaryStandardItemMapper salaryStandardItemMapper;

    @Resource
    private PayRecordService payRecordService;

    private static final DateTimeFormatter RUN_CODE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");


    @Override
    public Map<String, Object> pagePendingRuns(long page, long size, String name, Integer status, String runCode, String startDate, String endDate) {
        Page<PayRun> p = new Page<>(page, size);
        QueryWrapper<PayRun> w = new QueryWrapper<>();
        w.eq("is_deleted", 0);
        if (status != null) {
            w.eq("status", status);
        }
        if (runCode != null && !runCode.trim().isEmpty()) {
            w.like("run_code", runCode.trim());
        }
        if (name != null && !name.trim().isEmpty()) {
            String kw = name.trim();
            w.and(q -> q.like("first_level_org_name", kw).or().like("second_level_org_name", kw).or().like("third_level_org_name", kw).or().like("run_code", kw));
        }
        // 时间范围（create_time）
        try {
            if (startDate != null && !startDate.trim().isEmpty()) {
                LocalDateTime s = LocalDate.parse(startDate.trim(), DATE_FMT).atStartOfDay();
                w.ge("create_time", s);
            }
            if (endDate != null && !endDate.trim().isEmpty()) {
                LocalDateTime e = LocalDate.parse(endDate.trim(), DATE_FMT).atTime(23,59,59);
                w.le("create_time", e);
            }
        } catch (Exception ex) {
            // 解析失败则忽略时间过滤
            ex.printStackTrace();
        }

        w.orderByDesc("create_time");
        Page<PayRun> res = payRunMapper.selectPage(p, w);

        List<Map<String,Object>> rows = new ArrayList<>();
        for (PayRun r : res.getRecords()) {
            Map<String,Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("runCode", r.getRunCode());
            m.put("orgName", r.getFirstLevelOrgName() + (r.getSecondLevelOrgName() != null ? "/" + r.getSecondLevelOrgName() : "") + (r.getThirdLevelOrgName() != null ? "/" + r.getThirdLevelOrgName() : ""));
            m.put("totalCount", r.getTotalCount() == null ? 0 : r.getTotalCount());
            m.put("totalBasic", r.getTotalBasic() == null ? BigDecimal.ZERO : r.getTotalBasic());
            m.put("createTime", r.getCreateTime());
            rows.add(m);
        }

        Map<String,Object> data = new HashMap<>();
        data.put("records", rows);
        data.put("total", res.getTotal());
        data.put("current", res.getCurrent());
        data.put("size", res.getSize());
        return data;
    }

    @Override
    public PayRun getById(Long id) {
        return payRunMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateRecordsForRun(Long runId) {
        PayRun run = payRunMapper.selectById(runId);
        if (run == null) throw new RuntimeException("发放单不存在");

        Long thirdOrgId = run.getThirdLevelOrgId();
        QueryWrapper<Archive> aw = new QueryWrapper<>();
        aw.eq("is_deleted", 0).eq("status", 1);
        if (thirdOrgId != null) aw.eq("third_level_org_id", thirdOrgId);
        List<Archive> emps = archiveMapper.selectList(aw);
        if (emps == null) emps = Collections.emptyList();

        // 删除旧记录及明细
        QueryWrapper<PayRecord> prw = new QueryWrapper<>();
        prw.eq("run_id", runId);
        List<PayRecord> existRecs = payRecordMapper.selectList(prw);
        if (existRecs != null && !existRecs.isEmpty()) {
            List<Long> existIds = existRecs.stream().map(PayRecord::getId).collect(Collectors.toList());
            QueryWrapper<PayRecordItem> priw = new QueryWrapper<>();
            priw.in("record_id", existIds);
            payRecordItemMapper.delete(priw);
            payRecordMapper.delete(prw);
        }

        List<PayRecord> toSaveRecords = new ArrayList<>();
        int totalCount = 0;
        BigDecimal totalBasic = BigDecimal.ZERO;

        for (Archive a : emps) {
            totalCount++;
            PayRecord rec = new PayRecord();
            rec.setRunId(runId);
            rec.setEmployeeId(a.getId());
            rec.setEmployeeNo(null);
            rec.setEmployeeName(a.getName());
            rec.setPosition(a.getPositionName());
            rec.setReward(BigDecimal.ZERO);
            rec.setDeduction(BigDecimal.ZERO);
            rec.setStatus(0);
            rec.setCreateTime(LocalDateTime.now());
            rec.setUpdateTime(LocalDateTime.now());

            Long stdId = a.getSalaryStandardId();
            BigDecimal basicForEmp = BigDecimal.ZERO;
            if (stdId != null) {
                QueryWrapper<SalaryStandardItem> sw = new QueryWrapper<>();
                sw.eq("standard_id", stdId);
                List<SalaryStandardItem> items = salaryStandardItemMapper.selectList(sw);
                for (SalaryStandardItem ssi : items) {
                    if (basicForEmp.compareTo(BigDecimal.ZERO) == 0) {
                        String pn = ssi.getProjectName() != null ? ssi.getProjectName() : "";
                        String pc = ssi.getProjectCode() != null ? ssi.getProjectCode() : "";
                        if (pn.contains("基本") || "S001".equalsIgnoreCase(pc)) {
                            basicForEmp = ssi.getAmount() != null ? ssi.getAmount() : BigDecimal.ZERO;
                        }
                    }
                }
            }
            rec.setBasicSalary(basicForEmp);
            rec.setTotalPayable(basicForEmp);
            toSaveRecords.add(rec);

            totalBasic = totalBasic.add(basicForEmp != null ? basicForEmp : BigDecimal.ZERO);
        }

        for (PayRecord r : toSaveRecords) {
            payRecordMapper.insert(r);
            Long stdId = null;
            Archive a = archiveMapper.selectById(r.getEmployeeId());
            if (a != null) stdId = a.getSalaryStandardId();
            if (stdId != null) {
                QueryWrapper<SalaryStandardItem> sw = new QueryWrapper<>();
                sw.eq("standard_id", stdId);
                List<SalaryStandardItem> items = salaryStandardItemMapper.selectList(sw);
                for (SalaryStandardItem ssi : items) {
                    PayRecordItem it = new PayRecordItem();
                    it.setRecordId(r.getId());
                    it.setProjectId(ssi.getProjectId());
                    it.setProjectCode(ssi.getProjectCode());
                    it.setProjectName(ssi.getProjectName());
                    it.setAmount(ssi.getAmount() != null ? ssi.getAmount() : BigDecimal.ZERO);
                    it.setIsSystem(ssi.getProjectCode() != null && ssi.getProjectCode().toUpperCase().startsWith("SYS") ? 1 : 0);
                    it.setCreateTime(LocalDateTime.now());
                    it.setUpdateTime(LocalDateTime.now());
                    payRecordItemMapper.insert(it);
                }
            }
        }

        PayRun upd = new PayRun();
        upd.setId(runId);
        upd.setTotalCount(totalCount);
        upd.setTotalBasic(totalBasic);
        upd.setUpdateTime(LocalDateTime.now());
        payRunMapper.updateById(upd);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForReview(Long runId) {
        PayRun run = payRunMapper.selectById(runId);
        if (run == null) throw new RuntimeException("发放单不存在");
        run.setStatus(1);
        run.setUpdateTime(LocalDateTime.now());
        payRunMapper.updateById(run);

        QueryWrapper<PayRecord> qw = new QueryWrapper<>();
        qw.eq("run_id", runId);
        List<PayRecord> recs = payRecordMapper.selectList(qw);
        for (PayRecord r : recs) {
            r.setStatus(1);
            r.setUpdateTime(LocalDateTime.now());
            payRecordMapper.updateById(r);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrUpdateRunForOrg(Long firstLevelOrgId, String firstLevelOrgName,
                                        Long secondLevelOrgId, String secondLevelOrgName,
                                        Long thirdLevelOrgId, String thirdLevelOrgName) {
        if (thirdLevelOrgId == null) {
            return null;
        }

        QueryWrapper<PayRun> qw = new QueryWrapper<>();
        qw.eq("is_deleted", 0).eq("third_level_org_id", thirdLevelOrgId).eq("status", 0);
        List<PayRun> existing = payRunMapper.selectList(qw);
        if (existing != null && !existing.isEmpty()) {
            PayRun run = existing.get(0);
            generateRecordsForRun(run.getId());
            return run.getId();
        }

        PayRun run = new PayRun();
        run.setRunCode("PR" + LocalDateTime.now().format(RUN_CODE_FMT));
        run.setFirstLevelOrgId(firstLevelOrgId);
        run.setFirstLevelOrgName(firstLevelOrgName);
        run.setSecondLevelOrgId(secondLevelOrgId);
        run.setSecondLevelOrgName(secondLevelOrgName);
        run.setThirdLevelOrgId(thirdLevelOrgId);
        run.setThirdLevelOrgName(thirdLevelOrgName);
        run.setStatus(0);
        run.setCreateTime(LocalDateTime.now());
        run.setUpdateTime(LocalDateTime.now());
        run.setIsDeleted(0);
        payRunMapper.insert(run);

        generateRecordsForRun(run.getId());
        return run.getId();
    }

    @Override
    public void generateForAllActiveThirdOrgs() {
        QueryWrapper<Archive> qw = new QueryWrapper<>();
        qw.eq("is_deleted", 0).eq("status", 1).isNotNull("third_level_org_id");
        List<Archive> emps = archiveMapper.selectList(qw);
        if (emps == null || emps.isEmpty()) return;
        Map<Long, Archive> map = new HashMap<>();
        for (Archive a : emps) {
            if (a.getThirdLevelOrgId() != null && !map.containsKey(a.getThirdLevelOrgId())) {
                map.put(a.getThirdLevelOrgId(), a);
            }
        }
        for (Map.Entry<Long, Archive> e : map.entrySet()) {
            Archive sample = e.getValue();
            createOrUpdateRunForOrg(sample.getFirstLevelOrgId(), sample.getFirstLevelOrgName(),
                    sample.getSecondLevelOrgId(), sample.getSecondLevelOrgName(),
                    sample.getThirdLevelOrgId(), sample.getThirdLevelOrgName());
        }
    }

    /**
     * 复核实现
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewRun(Long runId, String action, String comment, List<Map<String, Object>> records) {
        PayRun run = payRunMapper.selectById(runId);
        if (run == null) throw new RuntimeException("发放单不存在");

        // 1) 如果前端传来了 records（id/reward/deduction），先更新这些值
        if (records != null && !records.isEmpty()) {
            payRecordService.saveRewardsAndDeductions(runId, records);
        }

        // 2) 根据 action 更新状态
        if ("approve".equalsIgnoreCase(action)) {
            // 标记发放单为已发放/待支付 (2)
            run.setStatus(2);
            run.setUpdateTime(LocalDateTime.now());
            payRunMapper.updateById(run);

            // 更新对应 pay_record 为已复核/已通过 (2)
            QueryWrapper<PayRecord> qw = new QueryWrapper<>();
            qw.eq("run_id", runId);
            List<PayRecord> recs = payRecordMapper.selectList(qw);
            for (PayRecord r : recs) {
                r.setStatus(2);
                r.setUpdateTime(LocalDateTime.now());
                payRecordMapper.updateById(r);
            }
        } else if ("reject".equalsIgnoreCase(action)) {
            // 拒绝：退回到待登记（0）
            run.setStatus(0);
            run.setUpdateTime(LocalDateTime.now());
            payRunMapper.updateById(run);

            QueryWrapper<PayRecord> qw = new QueryWrapper<>();
            qw.eq("run_id", runId);
            List<PayRecord> recs = payRecordMapper.selectList(qw);
            for (PayRecord r : recs) {
                r.setStatus(0);
                r.setUpdateTime(LocalDateTime.now());
                payRecordMapper.updateById(r);
            }
        } else {
            throw new RuntimeException("未知的复核动作: " + action);
        }
    }
}