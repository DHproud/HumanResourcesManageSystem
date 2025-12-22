package HumanManage.modules.pay.service;

import HumanManage.modules.pay.entity.PayRecord;

import java.util.List;
import java.util.Map;

public interface PayRecordService {
    List<PayRecord> listByRunId(Long runId);

    List<Map<String, Object>> listDistinctProjectsByRunId(Long runId);

    void saveRewardsAndDeductions(Long runId, List<Map<String,Object>> recs);
}