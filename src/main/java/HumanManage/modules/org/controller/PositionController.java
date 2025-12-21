package HumanManage.modules.org.controller;

import HumanManage.common.result.Result;
import HumanManage.modules.org.entity.Position;
import HumanManage.modules.org.service.PositionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/position")
public class PositionController {

    @Autowired
    private PositionService positionService;

    @PostMapping("/add")
    public Result<String> add(@RequestBody Map<String, Object> params) {
        Position p = new Position();

        // 1. 设置职位名称 (前端传 positionName -> 实体类 name)
        if (params.get("positionName") != null) {
            p.setName((String) params.get("positionName"));
        } else {
            return Result.error("职位名称不能为空");
        }

        // 2. 设置机构全路径 (前端传 orgNameChain -> 实体类 orgNamePath)
        // 【关键点】：这里取值必须对应前端的 Key
        if (params.get("orgNameChain") != null) {
            p.setOrgNamePath((String) params.get("orgNameChain"));
        }

        // 3. 设置机构ID (前端传 orgId -> 实体类 orgId)
        Object orgIdObj = params.get("orgId");
        if (orgIdObj != null) {
            p.setOrgId(Long.valueOf(orgIdObj.toString()));
        } else {
            return Result.error("必须选择三级机构");
        }

        try {
            // 调用 Service 执行查重和保存
            positionService.addPosition(p);
            return Result.success("职位添加成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 获取所有职位 (表格显示用)
    @GetMapping("/listAll")
    public Result<List<Position>> listAll() {
        // 调用 Service 实现的方法
        return Result.success(positionService.listByOrgId(null));
    }

    // 根据机构筛选 (下拉框联动用)
    @GetMapping("/list") // 支持 /list?orgId=xxx
    public Result<List<Position>> list(@RequestParam(required = false) Long orgId) {
        return Result.success(positionService.listByOrgId(orgId));
    }

    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id) {
        positionService.removeById(id);
        return Result.success("删除成功");
    }
}