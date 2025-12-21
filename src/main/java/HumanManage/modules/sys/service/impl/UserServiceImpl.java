package HumanManage.modules.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import HumanManage.modules.sys.entity.User;
import HumanManage.modules.sys.mapper.UserMapper;
import HumanManage.modules.sys.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User login(String username, String password) {
        // 简单明文密码匹配（实际项目建议加密）
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getPassword, password));
        return user;
    }

    @Override
    public void register(User user) {
        // 检查用户名是否存在
        long count = this.count(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername()));
        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }
        this.save(user);
    }
}
