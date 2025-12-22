package HumanManage.modules.archive.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import HumanManage.modules.archive.entity.ArchiveEditRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface ArchiveEditRequestService {

    /**
     * 提交修改申请（专员）
     */
    void submitRequest(ArchiveEditRequest req);

    /**
     * 分页查询申请列表（经理复核页面使用）
     */
    IPage<ArchiveEditRequest> pageRequests(Page<ArchiveEditRequest> page, Integer status);

    /**
     * 经理通过：会把 newData 写回 Archive 表（调用 ArchiveManageService.updateArchive）
     */
    void approveRequest(Long id, String reviewer);

    /**
     * 经理拒绝
     */
    void rejectRequest(Long id, String reviewer, String remark);
}