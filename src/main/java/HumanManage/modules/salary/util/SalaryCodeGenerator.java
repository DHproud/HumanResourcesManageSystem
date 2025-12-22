package HumanManage.modules.salary.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 简单的薪酬标准编号生成器：
 * 形式示例：STD20251222123045123
 */
public class SalaryCodeGenerator {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    public static String generate() {
        String ts = LocalDateTime.now().format(FMT);
        int r = ThreadLocalRandom.current().nextInt(100, 1000);
        return "STD" + ts + r;
    }
}