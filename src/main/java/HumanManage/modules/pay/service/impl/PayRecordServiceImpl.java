package HumanManage.modules.pay.service.impl;

import HumanManage.modules.pay.entity.PayRecord;
import HumanManage.modules.pay.entity.PayRecordItem;
import HumanManage.modules.pay.mapper.PayRecordItemMapper;
import HumanManage.modules.pay.mapper.PayRecordMapper;
import HumanManage.modules.pay.service.PayRecordService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PayRecordServiceImpl implements PayRecordService {

    @Resource
    private PayRecordMapper payRecordMapper;

    @Resource
    private PayRecordItemMapper payRecordItemMapper;

    @Override
    public List<PayRecord> listByRunId(Long runId) {
        QueryWrapper<PayRecord> qw = new QueryWrapper<>();
        qw.eq("run_id", runId).eq("is_deleted", 0);
        List<PayRecord> recs = payRecordMapper.selectList(qw);
        if (recs == null) return Collections.emptyList();
        // load items for each record
        List<Long> ids = recs.stream().map(PayRecord::getId).collect(Collectors.toList());
        if (!ids.isEmpty()) {
            QueryWrapper<PayRecordItem> iw = new QueryWrapper<>();
            iw.in("record_id", ids);
            List<PayRecordItem> items = payRecordItemMapper.selectList(iw);
            Map<Long, List<PayRecordItem>> map = new HashMap<>();
            for (PayRecordItem it : items) {
                map.computeIfAbsent(it.getRecordId(), k -> new ArrayList<>()).add(it);
            }
            recs.forEach(r -> r.setItems(map.getOrDefault(r.getId(), Collections.emptyList())));
        }
        return recs;
    }

    @Override
    public List<Map<String, Object>> listDistinctProjectsByRunId(Long runId) {
        // Query distinct project_code/project_name from items joined with records under runId
        String sql = "SELECT DISTINCT pri.project_code AS projectCode, pri.project_name AS projectName, pri.project_id AS projectId " +
                " FROM hr_pay_record_item pri " +
                " JOIN hr_pay_record pr ON pri.record_id = pr.id " +
                " WHERE pr.run_id = #{runId} ";
        // For simplicity, use manual mapper? Here we implement via QueryWrapper: get all items for run and collect distinct
        QueryWrapper<PayRecord> prw = new QueryWrapper<>();
        prw.eq("run_id", runId);
        List<PayRecord> recs = payRecordMapper.selectList(prw);
        if (recs == null || recs.isEmpty()) return Collections.emptyList();
        List<Long> ids = recs.stream().map(PayRecord::getId).collect(Collectors.toList());
        QueryWrapper<PayRecordItem> iw = new QueryWrapper<>();
        iw.in("record_id", ids);
        List<PayRecordItem> items = payRecordItemMapper.selectList(iw);
        Map<String, Map<String, Object>> distinct = new LinkedHashMap<>();
        for (PayRecordItem it : items) {
            String key = it.getProjectCode() != null ? it.getProjectCode() : String.valueOf(it.getProjectId());
            distinct.putIfAbsent(key, new HashMap<String, Object>() {{
                put("projectCode", it.getProjectCode());
                put("projectName", it.getProjectName());
                put("projectId", it.getProjectId());
            }});
        }
        return new ArrayList<>(distinct.values());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRewardsAndDeductions(Long runId, List<Map<String, Object>> recs) {
        if (recs == null || recs.isEmpty()) return;
        for (Map<String, Object> m : recs) {
            Object idObj = m.get("id");
            Long id = null;
            if (idObj != null) {
                try {
                    id = Long.valueOf(idObj.toString());
                } catch (Exception ignored) {}
            }
            if (id == null) continue;
            PayRecord r = payRecordMapper.selectById(id);
            if (r == null) continue;
            Object rewardObj = m.get("reward");
            Object deductionObj = m.get("deduction");
            BigDecimal reward = r.getReward() != null ? r.getReward() : BigDecimal.ZERO;
            BigDecimal deduction = r.getDeduction() != null ? r.getDeduction() : BigDecimal.ZERO;
            try {
                if (rewardObj != null) reward = new BigDecimal(rewardObj.toString());
            } catch (Exception ignored) {}
            try {
                if (deductionObj != null) deduction = new BigDecimal(deductionObj.toString());
            } catch (Exception ignored) {}
            BigDecimal basic = r.getBasicSalary() != null ? r.getBasicSalary() : BigDecimal.ZERO;
            BigDecimal total = basic.add(reward).subtract(deduction);
            r.setReward(reward);
            r.setDeduction(deduction);
            r.setTotalPayable(total);
            r.setUpdateTime(LocalDateTime.now());
            payRecordMapper.updateById(r);
        }
    }
}