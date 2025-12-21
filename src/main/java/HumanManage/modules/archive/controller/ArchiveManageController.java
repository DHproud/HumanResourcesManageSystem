package HumanManage.modules.archive.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import HumanManage.common.result.Result;
import HumanManage.modules.archive.entity.Archive;
import HumanManage.modules.archive.entity.ArchiveQueryVo;
import HumanManage.modules.archive.service.ArchiveManageService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;



/**
 * 独立的查询/变更控制器，不改原 ArchiveController
 * 路由前缀与原有保持一致，避免前端大改
 */
@RestController
@RequestMapping("/api/archive")
public class ArchiveManageController {

    @Resource
    private ArchiveManageService archiveManageService;

    /**
     * 档案查询：与关系、空条件不限制
     */
    @PostMapping("/search")
    public Result<IPage<Archive>> search(@RequestBody ArchiveQueryVo vo) {
        IPage<Archive> page = archiveManageService.queryArchivePage(vo);
        return Result.success(page);
    }

    /**
     * 档案变更
     */
    @PutMapping("/update")
    public Result<String> update(@RequestBody Archive archive) {
        archiveManageService.updateArchive(archive);
        return Result.success("档案变更成功");
    }
}
