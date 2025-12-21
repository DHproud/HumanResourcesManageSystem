package HumanManage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ManageApplication {
    public static void main(String[] args) {
        SpringApplication.run(ManageApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉ HRMS 人力资源系统启动成功  ლ(´ڡ`ლ)ﾞ");
        System.out.println("后端接口地址: http://localhost:8080");
    }
}
