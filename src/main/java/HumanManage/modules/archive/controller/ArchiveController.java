package HumanManage.modules.archive.controller;

import HumanManage.common.result.Result;
import HumanManage.modules.archive.entity.Archive;
import HumanManage.modules.archive.service.ArchiveService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/archive")
public class ArchiveController {

    @Autowired
    private ArchiveService archiveService;

    // 1. 档案登记 (人事专员用)
    @PostMapping("/add")
    public Result<String> add(@RequestBody Archive archive) {
        try {
            // 设置当前登记人
            archive.setRegistrant("人事专员");
            archiveService.registerArchive(archive);
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

    // 4. 新增：按 ID 获取档案明细（GET /api/archive/{id}）
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

    // 5. 新增：档案变更（PUT /api/archive/update）
    //    权限校验说明：
    //    - 推荐从 Authorization token 解码获取当前用户角色并做校验（如果你有 JwtUtils，请在此处使用）
    //    - 当前实现：若请求头中存在 'Role'（或 'role'）值，则仅允许该值为 'SPECIALIST' 时更新；
    //      若请求头不携带 role，则出于兼容性允许更新（建议尽快改为从 token 解码并强制校验）
    @PutMapping("/update")
    public Result<String> update(@RequestBody Archive archive, HttpServletRequest request) {
        try {
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
                // 没有 role header —— 如果你有 JWT 解码工具，请在这里解析 Authorization token
                // String authHeader = request.getHeader("Authorization");
                // // 示例：使用 JwtUtils.parseToken(authHeader) -> 获取角色 -> 校验
                // // if (roleFromToken不是SPECIALIST) return Result.error("权限不足");
                // 目前处于兼容性考虑，允许继续（但建议尽快改为 token 校验）
            }

            // 执行更新（使用 MyBatis-Plus 提供的 updateById）
            boolean ok = archiveService.updateById(archive);
            if (ok) {
                return Result.success("档案变更成功");
            } else {
                return Result.error("档案变更失败（可能档案不存在）");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("变更失败：" + e.getMessage());
        }
    }
}