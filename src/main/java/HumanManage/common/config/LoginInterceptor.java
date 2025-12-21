package HumanManage.common.config;

import HumanManage.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 1. 放行 OPTIONS 请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. 获取 Token
        String token = request.getHeader(jwtUtils.getHeader()); // 默认为 "Authorization"

        if (!StringUtils.hasText(token)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\": 401, \"msg\": \"未登录，请先登录\"}");
            return false;
        }

        // 3. 解析 Token
        Claims claims = jwtUtils.getClaimsByToken(token);
        if (claims == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\": 401, \"msg\": \"登录凭证已过期\"}");
            return false;
        }

        // 4. 获取用户信息并存入 Request
        String role = (String) claims.get("role");
        String username = (String) claims.get("username");
        // 注意：claims获取数字类型可能会变成Integer，这里安全转换一下
        Long userId = Long.valueOf(claims.get("userId").toString());

        request.setAttribute("userId", userId);
        request.setAttribute("username", username);
        request.setAttribute("role", role);

        // ==========================================
        // 5. 【RBAC 核心权限控制】(使用英文角色判断)
        // ==========================================
        String uri = request.getRequestURI();

        // 【规则一】：系统管理 (机构/职位) -> 仅 ADMIN 可用
        if ((uri.startsWith("/api/org") || uri.startsWith("/api/position"))
                && !"ADMIN".equals(role)) {

            // 特例：允许非管理员读取下拉列表 (用于档案登记页面的职位联动)
            if (!uri.contains("/list")) {
                response.setStatus(403);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\": 403, \"msg\": \"权限不足：仅管理员可操作\"}");
                return false;
            }
        }

        // 【规则二】：档案复核 -> 仅 MANAGER 可用
        if (uri.startsWith("/api/archive/review") && !"MANAGER".equals(role)) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\": 403, \"msg\": \"权限不足：仅人事经理可复核\"}");
            return false;
        }

        // 【规则三】：档案登记 -> SPECIALIST 或 ADMIN 可用
        if (uri.startsWith("/api/archive/add") &&
                (!"SPECIALIST".equals(role) && !"ADMIN".equals(role))) {
            response.setStatus(403);
            return false;
        }

        return true;
    }
}