package HumanManage.modules.archive.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import HumanManage.common.result.Result;
import HumanManage.modules.archive.entity.Archive;
import HumanManage.modules.archive.service.ArchiveManageService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/archive")
public class ArchiveManageController {

    @Resource
    private ArchiveManageService archiveManageService;

    /**
     * 使用 Archive 参数（包含 page/size/status/startTime/endTime）进行查询
     * 权限增强：若请求来自 SPECIALIST，则强制禁止返回 status=2 的档案，
     * 实现方式为若 param.status == 2 且 role == SPECIALIST，则将 status 改为 1（或 null），防止查看已删除档案。
     */
    @PostMapping("/search")
    public Result<IPage<Archive>> search(@RequestBody Archive param, HttpServletRequest request) {
        String roleHeader = request.getHeader("Role");
        if (roleHeader == null) roleHeader = request.getHeader("role");
        boolean isSpecialist = roleHeader != null && "SPECIALIST".equalsIgnoreCase(roleHeader.trim());

        // 调试输出（可保留或换成日志）
        System.out.println("[DEBUG] /api/archive/search received Archive param = " + param + ", role = " + roleHeader);

        if (isSpecialist && param != null && param.getStatus() != null && param.getStatus() == 2) {
            // 专员试图查询已删除（2） -> 强制改回只查询正常（1）
            System.out.println("[SECURITY] SPECIALIST attempted to query status=2; overriding to status=1");
            param.setStatus(1);
        }

        IPage<Archive> page = archiveManageService.queryArchivePage(param);
        return Result.success(page);
    }

    @PostMapping("/markDeleted/{id}")
    public Result<String> markDeleted(@PathVariable Long id, HttpServletRequest request) {
        String roleHeader = request.getHeader("Role");
        if (roleHeader == null) roleHeader = request.getHeader("role");
        if (roleHeader == null || !"MANAGER".equalsIgnoreCase(roleHeader.trim())) {
            return Result.error("权限不足：只有人事经理可以执行删除操作");
        }
        try {
            archiveManageService.markDeleted(id);
            return Result.success("档案已标记为已删除");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/recover/{id}")
    public Result<String> recover(@PathVariable Long id, HttpServletRequest request) {
        String roleHeader = request.getHeader("Role");
        if (roleHeader == null) roleHeader = request.getHeader("role");
        if (roleHeader == null || !"MANAGER".equalsIgnoreCase(roleHeader.trim())) {
            return Result.error("权限不足：只有人事经理可以执行恢复操作");
        }
        try {
            archiveManageService.recoverArchive(id);
            return Result.success("档案已恢复为正常状态");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

}
