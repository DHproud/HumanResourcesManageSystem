package HumanManage.modules.archive.controller;

import HumanManage.common.result.Result;
import HumanManage.modules.archive.entity.Archive;
import HumanManage.modules.archive.service.ArchiveService;
import HumanManage.modules.pay.service.PayRunService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ArchiveController - 兼容前端薪酬标准下拉选择提交的字段
 *
 * 说明：
 * - 前端发送的薪酬标准可能为 salaryStandardIdStr (字符串)、salaryStandardCode、salaryStandardName
 * - Archive 实体中存在 salaryStandardId (Long) 与 salaryStandardName (String)
 * - 本控制器会优先解析 salaryStandardIdStr -> 转为 Long -> 写入 archive.salaryStandardId
 *   并把 salaryStandardName 也写入 archive.salaryStandardName
 *
 * 额外：在新增/更新档案后，会尝试触发发放单的创建/刷新（PayRun），以便自动维护待登记的薪酬发放单。
 */
@RestController
@RequestMapping("/api/archive")
public class ArchiveController {

    @Autowired
    private ArchiveService archiveService;

    @Autowired
    private PayRunService payRunService;

    // 忽略未知字段，避免因为前端多余字段导致 convertValue 抛异常
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // 1. 档案登记 (人事专员用)
    @PostMapping("/add")
    public Result<String> add(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            // 将 payload 映射到 Archive（忽略未知字段）
            Archive archive = mapper.convertValue(payload, Archive.class);

            // 处理薪酬标准 id（字符串形式） -> Archive.salaryStandardId (Long)
            Object sidStrObj = payload.get("salaryStandardIdStr");
            if (sidStrObj == null) sidStrObj = payload.get("salaryStandardId");
            Long sid = tryParseLong(sidStrObj);
            if (sid != null) {
                archive.setSalaryStandardId(sid);
            }

            // 处理薪酬标准名称（前端可能提交）
            Object sname = payload.get("salaryStandardName");
            if (sname != null) {
                archive.setSalaryStandardName(sname.toString());
            }

            // 设置登记人（优先使用请求头 Username）
            String usernameHeader = request.getHeader("Username");
            if (usernameHeader == null || usernameHeader.trim().isEmpty()) {
                usernameHeader = request.getHeader("username");
            }
            if (usernameHeader == null || usernameHeader.trim().isEmpty()) {
                usernameHeader = "人事专员";
            }
            archive.setRegistrant(usernameHeader);

            // 设置登记时间
            archive.setRegistTime(LocalDateTime.now());

            // 保存档案（该方法内部可能已有触发发放单的逻辑）
            archiveService.registerArchive(archive);

            // 为保证兼容性：若 registerArchive 中没有触发发放单，这里再尝试触发一次（容错）
            try {
                Long firstId = archive.getFirstLevelOrgId();
                Long secondId = archive.getSecondLevelOrgId();
                Long thirdId = archive.getThirdLevelOrgId();
                String firstName = archive.getFirstLevelOrgName();
                String secondName = archive.getSecondLevelOrgName();
                String thirdName = archive.getThirdLevelOrgName();
                if (thirdId != null) {
                    // createOrUpdateRunForOrg 内部已做事务与异常捕获策略（如果抛异常不会影响主流程）
                    payRunService.createOrUpdateRunForOrg(firstId, firstName, secondId, secondName, thirdId, thirdName);
                }
            } catch (Exception ex) {
                // 记录异常（不抛出），避免因发放单生成失败导致档案登记失败
                ex.printStackTrace();
            }

            return Result.success("档案登记成功，已提交复核");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("登记失败：" + e.getMessage());
        }
    }

    // 2. 获取待复核列表 (人事经理用)
    @GetMapping("/list/pending")
    public Result<List<Archive>> listPending() {
        List<Archive> list = archiveService.getPendingReviews();
        return Result.success(list);
    }

    // 3. 复核通过 (人事经理用)
    @PostMapping("/review")
    public Result<String> review(@RequestBody Archive archive) {
        try {
            if (archive.getId() == null) {
                return Result.error("档案ID不能为空");
            }
            archiveService.reviewPass(archive);
            return Result.success("复核通过，档案已生效");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("复核失败：" + e.getMessage());
        }
    }

    // 4. 按 ID 获取档案明细（GET /api/archive/{id}）
    @GetMapping("/{id}")
    public Result<Archive> getById(@PathVariable Long id) {
        try {
            Archive archive = archiveService.getById(id);
            if (archive != null) {
                return Result.success(archive);
            } else {
                return Result.error("档案不存在");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取档案失败：" + e.getMessage());
        }
    }

    // 5. 档案变更（PUT /api/archive/update）
    @PutMapping("/update")
    public Result<String> update(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            Archive archive = mapper.convertValue(payload, Archive.class);

            if (archive.getId() == null) {
                return Result.error("档案ID不能为空");
            }

            // 从请求头读取 role（兼容性做法）
            String roleHeader = request.getHeader("Role");
            if (roleHeader == null) roleHeader = request.getHeader("role");

            if (roleHeader != null && !"".equals(roleHeader.trim())) {
                // 如果有 role 字段，强制要求为 SPECIALIST 才能修改
                if (!"SPECIALIST".equalsIgnoreCase(roleHeader.trim())) {
                    return Result.error("权限不足：只有人事专员可以变更档案");
                }
            } else {
                // 没有 role header —— 兼容性允许继续（建议改为 token 校验）
            }

            // 处理薪酬标准字段（与 add 同步）
            Object sidStrObj = payload.get("salaryStandardIdStr");
            if (sidStrObj == null) sidStrObj = payload.get("salaryStandardId");
            Long sid = tryParseLong(sidStrObj);
            if (sid != null) {
                archive.setSalaryStandardId(sid);
            }

            Object sname = payload.get("salaryStandardName");
            if (sname != null) {
                archive.setSalaryStandardName(sname.toString());
            }

            boolean ok = archiveService.updateById(archive);
            if (ok) {
                // 更新成功后，尝试触发发放单的创建/刷新（若该档案包含三级机构信息）
                try {
                    Long firstId = archive.getFirstLevelOrgId();
                    Long secondId = archive.getSecondLevelOrgId();
                    Long thirdId = archive.getThirdLevelOrgId();
                    String firstName = archive.getFirstLevelOrgName();
                    String secondName = archive.getSecondLevelOrgName();
                    String thirdName = archive.getThirdLevelOrgName();
                    if (thirdId != null) {
                        payRunService.createOrUpdateRunForOrg(firstId, firstName, secondId, secondName, thirdId, thirdName);
                    }
                } catch (Exception ex) {
                    // 记录异常但不影响更新结果
                    ex.printStackTrace();
                }

                return Result.success("档案变更成功");
            } else {
                return Result.error("档案变更失败（可能档案不存在）");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("变更失败：" + e.getMessage());
        }
    }

    // -------------------- 辅助方法 --------------------
    private Long tryParseLong(Object o) {
        try {
            if (o == null) return null;
            if (o instanceof Number) return ((Number) o).longValue();
            String s = o.toString().trim();
            if (s.isEmpty()) return null;
            return Long.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }
}