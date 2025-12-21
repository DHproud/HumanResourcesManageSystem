package HumanManage.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import HumanManage.modules.sys.entity.User;

public interface UserService extends IService<User> {

    /**
     * 用户登录
     * @param username 用户名
     * @param password 密码
     * @return 登录成功的用户信息
     */
    User login(String username, String password);

    /**
     * 用户注册
     * @param user 用户信息
     */
    void register(User user);
}