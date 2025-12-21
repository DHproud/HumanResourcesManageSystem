package HumanManage.modules.archive.controller;

import HumanManage.common.result.Result;
import HumanManage.modules.archive.entity.Archive;
import HumanManage.modules.archive.service.ArchiveService;
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
}