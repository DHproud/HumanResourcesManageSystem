package HumanManage.modules.sys.controller;

import HumanManage.common.result.Result;
import HumanManage.common.utils.JwtUtils;
import HumanManage.modules.sys.entity.User;
import HumanManage.modules.sys.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sys")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody User loginUser) {
        User user = userService.login(loginUser.getUsername(), loginUser.getPassword());

        if (user != null) {
            // 1. 准备 Token 载荷 (放入英文角色)
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("username", user.getUsername());
            claims.put("role", user.getRole()); // 这里 user.getRole() 应该是 "ADMIN", "MANAGER" 等

            // 2. 生成 Token (调用支持 Map 参数的 generateToken)
            String token = jwtUtils.generateToken(claims);

            // 3. 构造返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("username", user.getUsername());
            data.put("realName", user.getRealName());
            data.put("role", user.getRole());

            // 4. 返回成功
            return Result.success("登录成功", data);
        } else {
            return Result.error("用户名或密码错误");
        }
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody User user) {
        try {
            // 默认角色兜底
            if (user.getRole() == null || user.getRole().isEmpty()) {
                user.setRole("SPECIALIST");
            }
            // 禁止通过接口注册管理员
            if ("ADMIN".equals(user.getRole())) {
                return Result.error("无法注册管理员账号");
            }

            userService.register(user);
            return Result.success("注册成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // 获取当前用户信息
    @GetMapping("/info")
    public Result<User> info(@RequestAttribute("userId") Long userId) {
        User user = userService.getById(userId);
        if (user != null) user.setPassword(null);
        return Result.success(user);
    }
}