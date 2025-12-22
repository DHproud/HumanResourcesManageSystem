package HumanManage.modules.salary.controller;

import HumanManage.common.result.Result;
import HumanManage.modules.salary.entity.SalaryProject;
import HumanManage.modules.salary.service.SalaryProjectService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 薪酬项目管理（接口风格与 PositionController 保持一致）
 *
 * 提供：
 *  - POST  /api/salary/project/add      : 新增（前端保证只有管理员显示入口）
 *  - GET   /api/salary/project/listAll  : 列出所有（表格显示）
 *  - GET   /api/salary/project/list     : 按关键字筛选（下拉或搜索）
 *  - GET   /api/salary/project/page     : 分页查询（用于表格分页）
 *  - PUT   /api/salary/project/{id}     : 更新（新增缺失的接口）
 *  - DELETE /api/salary/project/delete/{id} : 删除（物理删除，与 PositionController 行为一致）
 *
 * 后端不再做复杂权限判断（由前端页面/路由控制显示），仅保持行为与项目中其它 Controller 风格一致。
 */
@RestController
@RequestMapping("/api/salary/project")
public class SalaryProjectController {

    @Autowired
    private SalaryProjectService salaryProjectService;

    /**
     * 新增薪酬项目
     * POST /api/salary/project/add
     * body: { projectCode, projectName, description, status, createdBy }
     */
    @PostMapping("/add")
    public Result<String> add(@RequestBody Map<String, Object> params) {
        SalaryProject sp = new SalaryProject();

        Object codeObj = params.get("projectCode");
        if (codeObj != null && codeObj.toString().trim().length() > 0) {
            sp.setProjectCode(codeObj.toString().trim());
        } else {
            return Result.error("项目编号不能为空");
        }

        Object nameObj = params.get("projectName");
        if (nameObj != null && nameObj.toString().trim().length() > 0) {
            sp.setProjectName(nameObj.toString().trim());
        } else {
            return Result.error("项目名称不能为空");
        }

        if (params.get("description") != null) sp.setDescription(params.get("description").toString());

        if (params.get("status") != null) {
            try {
                sp.setStatus(Integer.valueOf(params.get("status").toString()));
            } catch (Exception ignored) {
            }
        } else {
            sp.setStatus(1);
        }

        if (params.get("createdBy") != null) sp.setCreatedBy(params.get("createdBy").toString());

        sp.setCreateTime(LocalDateTime.now());
        sp.setUpdateTime(LocalDateTime.now());
        sp.setIsDeleted(0);

        // 唯一性检查
        SalaryProject exists = salaryProjectService.getByProjectCode(sp.getProjectCode());
        if (exists != null) {
            return Result.error("项目编号已存在");
        }

        try {
            salaryProjectService.save(sp);
            return Result.success("新增成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("新增失败: " + e.getMessage());
        }
    }

    /**
     * 更新薪酬项目（通过路径 id）
     * PUT /api/salary/project/{id}
     * 说明：此处接受路径 id 为 String（兼容前端以字符串形式传递的大整数 id），内部再转换为 Long
     */
    @PutMapping("/{id}")
    public Result<String> update(@PathVariable("id") String idStr, @RequestBody Map<String, Object> params) {
        if (idStr == null || idStr.trim().isEmpty()) {
            return Result.error("记录不存在");
        }

        Long id;
        try {
            id = Long.valueOf(idStr.trim());
        } catch (Exception e) {
            return Result.error("无效的记录 id");
        }

        SalaryProject sp = salaryProjectService.getById(id);
        if (sp == null) {
            return Result.error("记录不存在");
        }

        if (sp.getIsDeleted() != null && sp.getIsDeleted() == 1) {
            return Result.error("记录已被删除");
        }

        // 更新字段（只更新前端提供的字段）
        Object codeObj = params.get("projectCode");
        if (codeObj != null) {
            String code = codeObj.toString().trim();
            if (code.isEmpty()) {
                return Result.error("项目编号不能为空");
            }
            // 唯一性检查（若修改了编号，且与其它记录冲突）
            SalaryProject other = salaryProjectService.getByProjectCode(code);
            if (other != null && !other.getId().equals(id)) {
                return Result.error("项目编号已存在");
            }
            sp.setProjectCode(code);
        }

        Object nameObj = params.get("projectName");
        if (nameObj != null) {
            String name = nameObj.toString().trim();
            if (name.isEmpty()) return Result.error("项目名称不能为空");
            sp.setProjectName(name);
        }

        if (params.get("description") != null) sp.setDescription(params.get("description").toString());

        if (params.get("status") != null) {
            try {
                sp.setStatus(Integer.valueOf(params.get("status").toString()));
            } catch (Exception ignored) {}
        }

        sp.setUpdateTime(LocalDateTime.now());

        try {
            salaryProjectService.updateById(sp);
            return Result.success("更新成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("更新失败: " + e.getMessage());
        }
    }

    /**
     * 列出所有薪酬项目（表格展示用，无分页）
     * GET /api/salary/project/listAll
     */
    @GetMapping("/listAll")
    public Result<List<SalaryProject>> listAll() {
        QueryWrapper<SalaryProject> w = new QueryWrapper<>();
        w.eq("is_deleted", 0);
        w.orderByAsc("project_code");
        List<SalaryProject> list = salaryProjectService.list(w);
        return Result.success(list);
    }

    /**
     * 按关键字筛选（用于下拉或搜索）
     * GET /api/salary/project/list?name=关键字
     */
    @GetMapping("/list")
    public Result<List<SalaryProject>> list(@RequestParam(required = false) String name) {
        QueryWrapper<SalaryProject> w = new QueryWrapper<>();
        w.eq("is_deleted", 0);
        if (name != null && !name.trim().isEmpty()) {
            String kw = name.trim();
            w.and(wrapper -> wrapper.like("project_name", kw).or().like("project_code", kw));
        }
        w.orderByAsc("project_code");
        List<SalaryProject> list = salaryProjectService.list(w);
        return Result.success(list);
    }

    /**
     * 分页查询（用于表格分页）
     * GET /api/salary/project/page?page=1&size=10&name=关键字
     * 返回数据包裹在 Result.data 中，data 为 Map 包含 records & total & current & size
     */
    @GetMapping("/page")
    public Result<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String name
    ) {
        Page<SalaryProject> p = new Page<>(page, size);
        QueryWrapper<SalaryProject> w = new QueryWrapper<>();
        w.eq("is_deleted", 0);
        if (name != null && !name.trim().isEmpty()) {
            String kw = name.trim();
            w.and(wrapper -> wrapper.like("project_name", kw).or().like("project_code", kw));
        }
        w.orderByDesc("create_time");

        IPage<SalaryProject> res = salaryProjectService.page(p, w);

        Map<String, Object> data = new HashMap<>();
        data.put("records", res.getRecords());
        data.put("total", res.getTotal());
        data.put("current", res.getCurrent());
        data.put("size", res.getSize());

        return Result.success(data);
    }

    /**
     * 删除（与 PositionController 保持一致：直接 removeById）
     * DELETE /api/salary/project/delete/{id}
     */
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable Long id) {
        try {
            salaryProjectService.removeById(id);
            return Result.success("删除成功");
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage());
        }
    }
}