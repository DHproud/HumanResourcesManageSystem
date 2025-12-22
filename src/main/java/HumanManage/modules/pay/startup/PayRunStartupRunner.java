package HumanManage.modules.pay.startup;

import HumanManage.modules.pay.service.PayRunService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


/**
 * 应用启动后初始化发放单（基于已有档案）
 *
 * 行为：
 *  - 读取配置 payrun.generateOnStartup（默认 true），若为 true 则调用 payRunService.generateForAllActiveThirdOrgs()
 *  - 捕获异常并记录日志
 *
 * 如需禁用：在 application.properties 中设置 payrun.generateOnStartup=false
 */
@Component
public class PayRunStartupRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(PayRunStartupRunner.class);

    @Resource
    private PayRunService payRunService;

    @Value("${payrun.generateOnStartup:true}")
    private boolean generateOnStartup;

    @Override
    public void run(ApplicationArguments args) {
        if (!generateOnStartup) {
            logger.info("PayRunStartupRunner: payrun.generateOnStartup=false -> 跳过启动时生成发放单");
            return;
        }

        logger.info("PayRunStartupRunner: 启动时开始扫描档案并生成/刷新发放单（可能耗时，视档案数量而定）...");
        try {
            payRunService.generateForAllActiveThirdOrgs();
            logger.info("PayRunStartupRunner: 完成发放单生成/刷新任务。");
        } catch (Exception e) {
            // 捕获异常，记录但不抛出，避免阻塞应用启动
            logger.error("PayRunStartupRunner: 生成发放单发生异常：", e);
        }
    }
}