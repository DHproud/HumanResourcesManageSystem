package HumanManage.modules.archive.service;

import com.baomidou.mybatisplus.extension.service.IService;
import HumanManage.modules.archive.entity.Archive;
import java.util.List;

public interface ArchiveService extends IService<Archive> {

    /**
     * 档案登记（自动生成编号，状态设为待复核）
     * @param archive 前端提交的档案表单
     */
    void registerArchive(Archive archive);

    /**
     * 获取所有待复核的档案列表
     * @return 待复核档案列表
     */
    List<Archive> getPendingReviews();

    /**
     * 档案复核通过
     * @param archive 复核提交的数据（包含修改后的信息）
     */
    void reviewPass(Archive archive);
}