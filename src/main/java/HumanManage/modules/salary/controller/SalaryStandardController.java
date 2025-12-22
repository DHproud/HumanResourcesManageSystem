package HumanManage.modules.salary.controller;

import HumanManage.common.result.Result;
import HumanManage.modules.salary.entity.SalaryStandard;
import HumanManage.modules.salary.entity.SalaryStandardItem;
import HumanManage.modules.salary.service.SalaryStandardItemService;
import HumanManage.modules.salary.service.SalaryStandardService;
import HumanManage.modules.salary.util.SalaryCodeGenerator;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 薪酬标准管理 Controller（包含三险一金、适用职位与复核功能）
 *
 * API:
 *  - GET  /api/salary/standard/page
 *  - GET  /api/salary/standard/{id}
 *  - POST /api/salary/standard/add
 *  - PUT  /api/salary/standard/{id}
 *  - DELETE /api/salary/standard/delete/{id}
 *  - GET  /api/salary/standard/review/list
 *  - POST /api/salary/standard/review/{id}
 */
@RestController
@RequestMapping("/api/salary/standard")
public class SalaryStandardController {

    @Resource
    private SalaryStandardService salaryStandardService;

    @Resource
    private SalaryStandardItemService salaryStandardItemService;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 计算三险一金（保留两位小数，四舍五入）
     */
    private Map<String, BigDecimal> calcInsurances(BigDecimal basic) {
        Map<String, BigDecimal> ret = new HashMap<>();
        if (basic == null) basic = BigDecimal.ZERO;
        BigDecimal pension = basic.multiply(new BigDecimal("0.08"));
        BigDecimal medical = basic.multiply(new BigDecimal("0.02")).add(new BigDecimal("3"));
        BigDecimal unemployment = basic.multiply(new BigDecimal("0.005"));
        BigDecimal housing = basic.multiply(new BigDecimal("0.08"));

        ret.put("pension", pension.setScale(2, RoundingMode.HALF_UP));
        ret.put("medical", medical.setScale(2, RoundingMode.HALF_UP));
        ret.put("unemployment", unemployment.setScale(2, RoundingMode.HALF_UP));
        ret.put("housing", housing.setScale(2, RoundingMode.HALF_UP));
        return ret;
    }

    /**
     * 分页查询（包含每条标准的条目）
     *
     * 增强查询条件：
     *  - standardCode (模糊匹配 standard_code)
     *  - keyword (模糊匹配 standard_name, author, registrant, reviewer)
     *  - startTime / endTime (范围匹配 register_time，格式 "yyyy-MM-dd HH:mm:ss")
     *
     * 未传入的条件不生效。
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String standardCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime
    ) {
        Page<SalaryStandard> p = new Page<>(page, size);
        QueryWrapper<SalaryStandard> w = new QueryWrapper<>();
        w.eq("is_deleted", 0);

        // 标准编号模糊查询
        if (standardCode != null && !standardCode.trim().isEmpty()) {
            w.like("standard_code", standardCode.trim());
        }

        // 关键字匹配多个字段：标准名称、制定人、登记人（变更人）、复核人
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            w.and(wrapper -> wrapper
                    .like("standard_name", kw)
                    .or()
                    .like("author", kw)
                    .or()
                    .like("registrant", kw)
                    .or()
                    .like("reviewer", kw)
            );
        }

        // 登记时间范围
        try {
            if (startTime != null && !startTime.trim().isEmpty()) {
                LocalDateTime s = LocalDateTime.parse(startTime.trim(), DT_FMT);
                w.ge("register_time", s);
            }
        } catch (Exception e) {
            // 解析失败则忽略该条件
        }
        try {
            if (endTime != null && !endTime.trim().isEmpty()) {
                LocalDateTime eTime = LocalDateTime.parse(endTime.trim(), DT_FMT);
                w.le("register_time", eTime);
            }
        } catch (Exception e) {
            // ignore
        }

        w.orderByDesc("create_time");
        IPage<SalaryStandard> res = salaryStandardService.page(p, w);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (SalaryStandard s : res.getRecords()) {
            Map<String, Object> m = new HashMap<>();
            m.put("standard", s);
            List<SalaryStandardItem> items = salaryStandardItemService.list(new QueryWrapper<SalaryStandardItem>().eq("standard_id", s.getId()));
            m.put("items", items);
            // expose also a string id if front-end needs it
            m.put("standardIdStr", s.getId() != null ? String.valueOf(s.getId()) : null);
            rows.add(m);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", rows);
        data.put("total", res.getTotal());
        data.put("current", res.getCurrent());
        data.put("size", res.getSize());
        return Result.success(data);
    }

    /**
     * 获取单条（含条目）
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> get(@PathVariable Long id) {
        SalaryStandard s = salaryStandardService.getById(id);
        if (s == null || (s.getIsDeleted() != null && s.getIsDeleted() == 1)) {
            return Result.error("记录不存在");
        }
        List<SalaryStandardItem> items = salaryStandardItemService.list(new QueryWrapper<SalaryStandardItem>().eq("standard_id", id));
        Map<String, Object> data = new HashMap<>();
        data.put("standard", s);
        data.put("items", items);
        return Result.success(data);
    }

    /**
     * 待复核列表（仅 review_status = 0 的项）
     */
    @GetMapping("/review/list")
    public Result<Map<String, Object>> reviewList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String name
    ) {
        Page<SalaryStandard> p = new Page<>(page, size);
        QueryWrapper<SalaryStandard> w = new QueryWrapper<>();
        w.eq("is_deleted", 0);
        w.eq("review_status", 0);
        if (name != null && !name.trim().isEmpty()) {
            w.like("standard_name", name.trim());
        }
        w.orderByDesc("create_time");
        IPage<SalaryStandard> res = salaryStandardService.page(p, w);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (SalaryStandard s : res.getRecords()) {
            Map<String, Object> m = new HashMap<>();
            m.put("standard", s);
            rows.add(m);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", rows);
        data.put("total", res.getTotal());
        data.put("current", res.getCurrent());
        data.put("size", res.getSize());
        return Result.success(data);
    }

    /**
     * 新增薪酬标准（登记）
     */
    @PostMapping("/add")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> add(@RequestBody Map<String, Object> params,
                              @RequestHeader(value = "Username", required = false) String usernameHeader) {

        Object nameObj = params.get("standardName");
        Object authorObj = params.get("author");

        if (nameObj == null || nameObj.toString().trim().isEmpty()) {
            return Result.error("薪酬标准名称不能为空");
        }
        if (authorObj == null || authorObj.toString().trim().isEmpty()) {
            return Result.error("制定人不能为空");
        }

        String registrant;
        Object registrantObj = params.get("registrant");
        if (registrantObj != null && registrantObj.toString().trim().length() > 0) {
            registrant = registrantObj.toString().trim();
        } else if (usernameHeader != null && usernameHeader.trim().length() > 0) {
            registrant = usernameHeader;
        } else {
            registrant = authorObj.toString().trim();
        }

        Integer includePension = getIntFlag(params.get("includePension"));
        Integer includeMedical = getIntFlag(params.get("includeMedical"));
        Integer includeUnemployment = getIntFlag(params.get("includeUnemployment"));
        Integer includeHousing = getIntFlag(params.get("includeHousing"));

        Long applicablePositionId = params.get("applicablePositionId") != null ? tryParseLong(params.get("applicablePositionId")) : null;
        String applicablePositionText = params.get("applicablePosition") != null ? params.get("applicablePosition").toString() : null;

        SalaryStandard std = new SalaryStandard();
        std.setStandardCode(SalaryCodeGenerator.generate());
        std.setStandardName(nameObj.toString().trim());
        std.setAuthor(authorObj.toString().trim());
        std.setRegistrant(registrant);
        std.setRegisterTime(LocalDateTime.now());
        std.setApplicablePositionId(applicablePositionId);
        std.setApplicablePosition(applicablePositionText);
        std.setIncludePension(includePension);
        std.setIncludeMedical(includeMedical);
        std.setIncludeUnemployment(includeUnemployment);
        std.setIncludeHousing(includeHousing);
        std.setReviewStatus(0); // 新增默认待复核
        std.setStatus(1);
        std.setCreateTime(LocalDateTime.now());
        std.setUpdateTime(LocalDateTime.now());
        std.setIsDeleted(0);

        // 保存主表
        salaryStandardService.save(std);

        // 处理条目并识别基本工资
        BigDecimal basicSalary = BigDecimal.ZERO;
        Long basicProjectId = params.get("basicProjectId") != null ? tryParseLong(params.get("basicProjectId")) : null;

        Object itemsObj = params.get("items");
        if (itemsObj instanceof List) {
            List<?> li = (List<?>) itemsObj;
            for (Object o : li) {
                if (!(o instanceof Map)) continue;
                Map<?, ?> m = (Map<?, ?>) o;
                Object pidObj = m.get("projectId");
                if (pidObj == null) continue;
                Long projectId = tryParseLong(pidObj);
                BigDecimal amount = parseAmount(m.get("amount"));

                SalaryStandardItem item = new SalaryStandardItem();
                item.setStandardId(std.getId());
                item.setProjectId(projectId);
                item.setProjectCode(m.get("projectCode") != null ? m.get("projectCode").toString() : null);
                item.setProjectName(m.get("projectName") != null ? m.get("projectName").toString() : null);
                item.setAmount(amount);
                item.setCreateTime(LocalDateTime.now());
                item.setUpdateTime(LocalDateTime.now());
                salaryStandardItemService.save(item);

                // 识别基本工资
                if (basicSalary.compareTo(BigDecimal.ZERO) == 0) {
                    if (basicProjectId != null && projectId != null && basicProjectId.equals(projectId)) {
                        basicSalary = amount;
                    } else {
                        String pname = item.getProjectName() != null ? item.getProjectName() : "";
                        String pcode = item.getProjectCode() != null ? item.getProjectCode() : "";
                        if (pname.contains("基本") || "S001".equalsIgnoreCase(pcode)) {
                            basicSalary = amount;
                        }
                    }
                }
            }
        }

        // 计算并保存系统条目（三险一金）
        Map<String, BigDecimal> ins = calcInsurances(basicSalary);
        if (includePension != null && includePension == 1) {
            saveSystemItem(std.getId(), null, "养老保险", ins.get("pension"), "SYS_PENSION");
        }
        if (includeMedical != null && includeMedical == 1) {
            saveSystemItem(std.getId(), null, "医疗保险", ins.get("medical"), "SYS_MEDICAL");
        }
        if (includeUnemployment != null && includeUnemployment == 1) {
            saveSystemItem(std.getId(), null, "失业保险", ins.get("unemployment"), "SYS_UNEMPLOY");
        }
        if (includeHousing != null && includeHousing == 1) {
            saveSystemItem(std.getId(), null, "住房公积金", ins.get("housing"), "SYS_HOUSING");
        }

        return Result.success("新增成功");
    }

    /**
     * 更新（覆盖条目：删除旧条目再插入新条目，包括系统三险一金条目）
     */
    @PutMapping("/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> update(@PathVariable Long id, @RequestBody Map<String, Object> params) {
        SalaryStandard db = salaryStandardService.getById(id);
        if (db == null || (db.getIsDeleted() != null && db.getIsDeleted() == 1)) {
            return Result.error("记录不存在");
        }

        Object nameObj = params.get("standardName");
        Object authorObj = params.get("author");
        Object registrantObj = params.get("registrant");
        if (nameObj == null || nameObj.toString().trim().isEmpty()) {
            return Result.error("薪酬标准名称不能为空");
        }
        if (authorObj == null || authorObj.toString().trim().isEmpty()) {
            return Result.error("制定人不能为空");
        }
        if (registrantObj == null || registrantObj.toString().trim().isEmpty()) {
            return Result.error("登记人不能为空");
        }

        Integer includePension = getIntFlag(params.get("includePension"));
        Integer includeMedical = getIntFlag(params.get("includeMedical"));
        Integer includeUnemployment = getIntFlag(params.get("includeUnemployment"));
        Integer includeHousing = getIntFlag(params.get("includeHousing"));

        Long applicablePositionId = params.get("applicablePositionId") != null ? tryParseLong(params.get("applicablePositionId")) : db.getApplicablePositionId();
        String applicablePositionText = params.get("applicablePosition") != null ? params.get("applicablePosition").toString() : db.getApplicablePosition();

        db.setStandardName(nameObj.toString().trim());
        db.setAuthor(authorObj.toString().trim());
        db.setRegistrant(registrantObj.toString().trim());
        db.setApplicablePositionId(applicablePositionId);
        db.setApplicablePosition(applicablePositionText);
        db.setIncludePension(includePension);
        db.setIncludeMedical(includeMedical);
        db.setIncludeUnemployment(includeUnemployment);
        db.setIncludeHousing(includeHousing);
        db.setUpdateTime(LocalDateTime.now());
        // 更新后恢复为待复核状态（如需不同逻辑可调整）
        db.setReviewStatus(0);
        db.setReviewer(null);
        db.setReviewTime(null);
        db.setReviewComment(null);
        salaryStandardService.updateById(db);

        // 删除旧条目
        salaryStandardItemService.remove(new QueryWrapper<SalaryStandardItem>().eq("standard_id", id));

        // 重新插入前端传入条目并识别基本工资
        BigDecimal basicSalary = BigDecimal.ZERO;
        Long basicProjectId = params.get("basicProjectId") != null ? tryParseLong(params.get("basicProjectId")) : null;

        Object itemsObj = params.get("items");
        if (itemsObj instanceof List) {
            List<?> li = (List<?>) itemsObj;
            for (Object o : li) {
                if (!(o instanceof Map)) continue;
                Map<?, ?> m = (Map<?, ?>) o;
                Object pidObj = m.get("projectId");
                if (pidObj == null) continue;
                Long projectId = tryParseLong(pidObj);
                BigDecimal amount = parseAmount(m.get("amount"));

                SalaryStandardItem item = new SalaryStandardItem();
                item.setStandardId(db.getId());
                item.setProjectId(projectId);
                item.setProjectCode(m.get("projectCode") != null ? m.get("projectCode").toString() : null);
                item.setProjectName(m.get("projectName") != null ? m.get("projectName").toString() : null);
                item.setAmount(amount);
                item.setCreateTime(LocalDateTime.now());
                item.setUpdateTime(LocalDateTime.now());
                salaryStandardItemService.save(item);

                if (basicSalary.compareTo(BigDecimal.ZERO) == 0) {
                    if (basicProjectId != null && projectId != null && basicProjectId.equals(projectId)) {
                        basicSalary = amount;
                    } else {
                        String pname = item.getProjectName() != null ? item.getProjectName() : "";
                        String pcode = item.getProjectCode() != null ? item.getProjectCode() : "";
                        if (pname.contains("基本") || "S001".equalsIgnoreCase(pcode)) {
                            basicSalary = amount;
                        }
                    }
                }
            }
        }

        // 计算并保存三险一金条目（如果被勾选）
        Map<String, BigDecimal> ins = calcInsurances(basicSalary);
        if (includePension != null && includePension == 1) {
            saveSystemItem(db.getId(), null, "养老保险", ins.get("pension"), "SYS_PENSION");
        }
        if (includeMedical != null && includeMedical == 1) {
            saveSystemItem(db.getId(), null, "医疗保险", ins.get("medical"), "SYS_MEDICAL");
        }
        if (includeUnemployment != null && includeUnemployment == 1) {
            saveSystemItem(db.getId(), null, "失业保险", ins.get("unemployment"), "SYS_UNEMPLOY");
        }
        if (includeHousing != null && includeHousing == 1) {
            saveSystemItem(db.getId(), null, "住房公积金", ins.get("housing"), "SYS_HOUSING");
        }

        return Result.success("更新成功");
    }

    /**
     * 管理员复核接口
     */
    @PostMapping("/review/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> review(@PathVariable Long id, @RequestBody Map<String, Object> params,
                                 @RequestHeader(value = "Username", required = false) String usernameHeader) {
        SalaryStandard s = salaryStandardService.getById(id);
        if (s == null || (s.getIsDeleted() != null && s.getIsDeleted() == 1)) {
            return Result.error("记录不存在");
        }

        Object actionObj = params.get("action");
        Object commentObj = params.get("comment");
        if (actionObj == null) {
            return Result.error("action 必填（approve 或 reject）");
        }
        String action = actionObj.toString().trim().toLowerCase();
        String comment = commentObj != null ? commentObj.toString().trim() : "";

        int newStatus;
        if ("approve".equals(action) || "pass".equals(action) || "通过".equals(action)) {
            newStatus = 1;
        } else if ("reject".equals(action) || "拒绝".equals(action) || "deny".equals(action)) {
            newStatus = 2;
        } else {
            return Result.error("action 值不正确，应为 approve 或 reject");
        }

        s.setReviewStatus(newStatus);
        s.setReviewer(usernameHeader != null ? usernameHeader : "admin");
        s.setReviewTime(LocalDateTime.now());
        s.setReviewComment(comment);
        s.setUpdateTime(LocalDateTime.now());
        salaryStandardService.updateById(s);

        return Result.success(newStatus == 1 ? "复核通过" : "复核拒绝");
    }

    /**
     * 删除（逻辑删除主表，并删除条目）
     */
    @DeleteMapping("/delete/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> delete(@PathVariable Long id) {
        SalaryStandard s = salaryStandardService.getById(id);
        if (s == null) {
            return Result.error("记录不存在");
        }
        // 删除条目
        salaryStandardItemService.remove(new QueryWrapper<SalaryStandardItem>().eq("standard_id", id));
        // 逻辑删除主表
        s.setIsDeleted(1);
        s.setUpdateTime(LocalDateTime.now());
        salaryStandardService.updateById(s);
        return Result.success("删除成功");
    }

    // -------------------- 辅助方法 --------------------

    private void saveSystemItem(Long standardId, Long projectId, String projectName, BigDecimal amount, String projectCode) {
        SalaryStandardItem it = new SalaryStandardItem();
        it.setStandardId(standardId);
        it.setProjectId(projectId); // 系统条目通常为 null
        it.setProjectCode(projectCode);
        it.setProjectName(projectName);
        it.setAmount(amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2));
        it.setCreateTime(LocalDateTime.now());
        it.setUpdateTime(LocalDateTime.now());
        salaryStandardItemService.save(it);
    }

    private Integer getIntFlag(Object flag) {
        if (flag == null) return 0;
        if (flag instanceof Number) return ((Number) flag).intValue() == 1 ? 1 : 0;
        String s = flag.toString().trim().toLowerCase();
        if ("1".equals(s) || "true".equals(s) || "yes".equals(s)) return 1;
        return 0;
    }

    private BigDecimal parseAmount(Object amtObj) {
        try {
            if (amtObj == null) return BigDecimal.ZERO.setScale(2);
            BigDecimal bd = new BigDecimal(amtObj.toString());
            return bd.setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO.setScale(2);
        }
    }

    private Long tryParseLong(Object o) {
        try {
            if (o == null) return null;
            if (o instanceof Number) return ((Number) o).longValue();
            return Long.valueOf(o.toString());
        } catch (Exception e) {
            return null;
        }
    }
}