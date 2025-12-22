package HumanManage.modules.archive.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import HumanManage.common.result.Result;
import HumanManage.modules.archive.entity.ArchiveEditRequest;
import HumanManage.modules.archive.service.ArchiveEditRequestService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;


/**
 * 专员提交修改申请、经理复核
 */
@RestController
@RequestMapping("/api/archive/editRequest")
public class ArchiveEditRequestController {

    @Resource
    private ArchiveEditRequestService requestService;

    /**
     * 专员提交修改申请
     * 请求体：{ archiveId:123, newData: "... JSON ..." , requester: 'name' }
     */
    @PostMapping("/submit")
    public Result<String> submit(@RequestBody ArchiveEditRequest req, HttpServletRequest request) {
        // 前端应仅允许 SPECIALIST 提交，后端再校验一次
        String roleHeader = request.getHeader("Role");
        if (roleHeader == null) roleHeader = request.getHeader("role");
        if (roleHeader == null || !"SPECIALIST".equalsIgnoreCase(roleHeader.trim())) {
            return Result.error("权限不足：只有人事专员可以提交修改申请");
        }
        // 记录 requester（如果请求体没传或不同步）
        if (req.getRequester() == null || req.getRequester().trim().isEmpty()) {
            String user = (String) request.getSession().getAttribute("username");
            if (user == null) user = "unknown";
            req.setRequester(user);
        }
        requestService.submitRequest(req);
        return Result.success("修改申请已提交，等待人事经理复核");
    }

    /**
     * 经理分页查询申请
     * GET /api/archive/editRequest/list?page=1&size=10&status=0
     */
    @GetMapping("/list")
    public Result<IPage<ArchiveEditRequest>> list(@RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer size,
                                                  @RequestParam(required = false) Integer status,
                                                  HttpServletRequest request) {
        // 只有 MANAGER 可查看复核列表（可根据需求放宽）
        String roleHeader = request.getHeader("Role");
        if (roleHeader == null) roleHeader = request.getHeader("role");
        if (roleHeader == null || !"MANAGER".equalsIgnoreCase(roleHeader.trim())) {
            return Result.error("权限不足：只有人事经理可以查看复核列表");
        }
        IPage<ArchiveEditRequest> p = requestService.pageRequests(new Page<>(page, size), status);
        return Result.success(p);
    }

    /**
     * 经理批准
     */
    @PostMapping("/approve/{id}")
    public Result<String> approve(@PathVariable Long id, HttpServletRequest request) {
        String roleHeader = request.getHeader("Role");
        if (roleHeader == null) roleHeader = request.getHeader("role");
        if (roleHeader == null || !"MANAGER".equalsIgnoreCase(roleHeader.trim())) {
            return Result.error("权限不足：只有人事经理可以审批");
        }
        String reviewer = (String) request.getSession().getAttribute("username");
        if (reviewer == null) reviewer = "manager";
        try {
            requestService.approveRequest(id, reviewer);
            return Result.success("审批通过，档案已更新");
        } catch (Exception e) {
            return Result.error("审批失败: " + e.getMessage());
        }
    }

    /**
     * 经理拒绝（body 中可带 remark）
     */
    @PostMapping("/reject/{id}")
    public Result<String> reject(@PathVariable Long id, @RequestBody(required = false) ArchiveEditRequest body, HttpServletRequest request) {
        String roleHeader = request.getHeader("Role");
        if (roleHeader == null) roleHeader = request.getHeader("role");
        if (roleHeader == null || !"MANAGER".equalsIgnoreCase(roleHeader.trim())) {
            return Result.error("权限不足：只有人事经理可以审批");
        }
        String reviewer = (String) request.getSession().getAttribute("username");
        if (reviewer == null) reviewer = "manager";
        String remark = (body != null) ? body.getRemark() : null;
        try {
            requestService.rejectRequest(id, reviewer, remark);
            return Result.success("已拒绝该修改申请");
        } catch (Exception e) {
            return Result.error("操作失败: " + e.getMessage());
        }
    }
}