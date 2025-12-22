package HumanManage.modules.pay.service;

import HumanManage.modules.pay.entity.PayRun;

import java.util.List;
import java.util.Map;

public interface PayRunService {
    /**
     * 分页查询发放单（按条件）
     * @param page 页
     * @param size 页大小
     * @param name 关键字（机构名或 run_code）
     * @param status 状态（可空）
     * @param runCode 发放单号（可空）
     * @param startDate 开始日期 yyyy-MM-dd（可空）
     * @param endDate 结束日期 yyyy-MM-dd（可空）
     */
    Map<String,Object> pagePendingRuns(long page, long size, String name, Integer status, String runCode, String startDate, String endDate);

    PayRun getById(Long id);

    void generateRecordsForRun(Long runId);

    void submitForReview(Long runId);

    Long createOrUpdateRunForOrg(Long firstLevelOrgId, String firstLevelOrgName,
                                 Long secondLevelOrgId, String secondLevelOrgName,
                                 Long thirdLevelOrgId, String thirdLevelOrgName);

    void generateForAllActiveThirdOrgs();

    void reviewRun(Long runId, String action, String comment, List<Map<String,Object>> records);
}