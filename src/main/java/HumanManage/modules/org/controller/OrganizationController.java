package HumanManage.modules.org.controller;

import HumanManage.modules.org.service.OrganizationService;
import HumanManage.common.result.Result;
import HumanManage.modules.org.entity.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/org")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService; // 注入 Service

    @GetMapping("/list/{parentId}")
    public Result<List<Organization>> listByParent(@PathVariable Long parentId) {
        List<Organization> list = organizationService.listByParent(parentId);
        return Result.success(list);
    }
    @PostMapping("/add")
    public Result<String> add(@RequestBody Organization org) {
        organizationService.save(org);
        return Result.success("机构添加成功");
    }
}