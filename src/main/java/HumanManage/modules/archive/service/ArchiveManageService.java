package HumanManage.modules.archive.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import HumanManage.modules.archive.entity.Archive;

import java.util.List;

public interface ArchiveManageService {

    /**
     * 使用 Archive 参数进行分页查询（page,size,status,startTime,endTime...）
     */
    IPage<Archive> queryArchivePage(Archive param);

    void updateArchive(Archive archive);

    void markDeleted(Long id);

    void recoverArchive(Long id);

    List<Archive> queryByStatus(Integer status);
}