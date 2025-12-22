package HumanManage.modules.pay.controller;

import HumanManage.common.result.Result;
import HumanManage.modules.pay.entity.PayRecord;
import HumanManage.modules.pay.entity.PayRun;
import HumanManage.modules.pay.service.PayRecordService;
import HumanManage.modules.pay.service.PayRunService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 发放单控制器（包含生成/保存/提交/复核等接口）
 */
@RestController
@RequestMapping("/api/pay/run")
public class PayRunController {

    @Resource
    private PayRunService payRunService;

    @Resource
    private PayRecordService payRecordService;

    /**
     * 分页查询：支持 runCode、name（关键字）、startDate、endDate、status
     * GET /api/pay/run/page?page=1&size=10&runCode=PR...&name=关键字&startDate=2025-12-01&endDate=2025-12-31&status=1
     */
    @GetMapping("/page")
    public Result<Map<String,Object>> page(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String runCode,
                                           @RequestParam(required = false) String name,
                                           @RequestParam(required = false) String startDate,
                                           @RequestParam(required = false) String endDate,
                                           @RequestParam(required = false) Integer status) {
        Map<String,Object> data = payRunService.pagePendingRuns(page, size, name, status, runCode, startDate, endDate);
        return Result.success(data);
    }

    @GetMapping("/{id}")
    public Result<Map<String,Object>> get(@PathVariable String id) {
        try {
            Long runId = tryParseLong(id);
            if (runId == null) return Result.error("无效的发放单ID");
            PayRun run = payRunService.getById(runId);
            if (run == null) return Result.error("发放单不存在");
            List<PayRecord> records = payRecordService.listByRunId(runId);
            List<Map<String,Object>> projects = payRecordService.listDistinctProjectsByRunId(runId);
            Map<String,Object> data = new HashMap<>();
            data.put("run", run);
            data.put("records", records);
            data.put("projects", projects);
            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    /**
     * 重新生成发放记录（手动触发）
     */
    @PostMapping("/{id}/generate")
    public Result<String> generate(@PathVariable String id) {
        Long runId = tryParseLong(id);
        if (runId == null) return Result.error("无效的发放单ID");
        try {
            payRunService.generateRecordsForRun(runId);
            return Result.success("已生成发放记录");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("生成失败：" + e.getMessage());
        }
    }

    /**
     * 保存草稿（仅保存 reward/deduction）
     * body: { records: [{ id, reward, deduction }, ...] }
     */
    @PostMapping("/{id}/save")
    public Result<String> saveDraft(@PathVariable String id, @RequestBody Map<String,Object> payload) {
        Long runId = tryParseLong(id);
        if (runId == null) return Result.error("无效的发放单ID");
        List<Map<String,Object>> recs = (List<Map<String,Object>>) payload.get("records");
        try {
            payRecordService.saveRewardsAndDeductions(runId, recs);
            return Result.success("保存成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("保存失败：" + e.getMessage());
        }
    }

    /**
     * 提交发放单（专员提交 -> 发起复核）
     * 前端在登记页面通常调用此接口。
     * body: { records: [{ id, reward, deduction }, ...] }  （可选）
     *
     * 行为：
     * 1) 保存前端提交的 reward/deduction（如果有）
     * 2) 将发放单状态设置为待复核（status=1），并将 pay_record 状态设为 1（已登记/待复核）
     */
    @PostMapping("/{id}/submit")
    public Result<String> submit(@PathVariable String id, @RequestBody(required = false) Map<String,Object> payload) {
        Long runId = tryParseLong(id);
        if (runId == null) return Result.error("无效的发放单ID");
        try {
            // 首先保存奖励/应扣（如果前端传了）
            if (payload != null && payload.get("records") != null) {
                List<Map<String,Object>> recs = (List<Map<String,Object>>) payload.get("records");
                payRecordService.saveRewardsAndDeductions(runId, recs);
            }
            // 然后更新发放单与记录状态
            payRunService.submitForReview(runId);
            return Result.success("提交成功，已进入复核流程");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("提交失败：" + e.getMessage());
        }
    }

    /**
     * 复核接口：经理调用 approve/reject
     * body: { action: 'approve'|'reject', comment?: string, records?: [{id,reward,deduction}, ...] }
     */
    @PostMapping("/{id}/review")
    public Result<String> review(@PathVariable String id, @RequestBody Map<String,Object> payload) {
        Long runId = tryParseLong(id);
        if (runId == null) return Result.error("无效的发放单ID");
        try {
            String action = payload.get("action") != null ? payload.get("action").toString() : "";
            String comment = payload.get("comment") != null ? payload.get("comment").toString() : "";
            List<Map<String,Object>> recs = (List<Map<String,Object>>) payload.get("records");
            payRunService.reviewRun(runId, action, comment, recs);
            return Result.success("复核操作成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("复核失败：" + e.getMessage());
        }
    }

    // utility
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