package HumanManage.modules.archive.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import HumanManage.modules.archive.entity.Archive;
import HumanManage.modules.archive.entity.ArchiveQueryVo;

/**
 * 独立的查询与变更服务，不改动原 ArchiveService，实现扩展
 */
public interface ArchiveManageService {

    /**
     * 档案分页查询（与关系，空条件不限制）
     */
    IPage<Archive> queryArchivePage(ArchiveQueryVo vo);

    /**
     * 档案变更（更新）
     */
    void updateArchive(Archive archive);
}