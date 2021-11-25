package com.seassoon.bizflow.flow.extract.tools;

import cn.hutool.core.io.FileUtil;
import com.seassoon.bizflow.core.component.FileDownloader;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 表格切分工具。
 * <p>
 * 工具本身由python程序提供，通过http调用获取切分后的表格
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class TableCutter {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private FileDownloader fileDownloader;

    /**
     * 切分并保存。<br/>
     * 检测图片是否包含表格，如果包含就将表格的单元格（cell）切分，并保存到目标文件夹。该工具由python提供。
     *
     * @param strPath 图片文件路径
     * @param target  切分后文件保存的目录
     * @return 切分后的cell存放路径
     */
    public List<String> cutAndSave(String strPath, String target) {
        // TODO 调用http接口切分图片
        List<String> cellUrls = new ArrayList<>();

        // 下载列表
        List<Pair<String, String>> downloadUrls = cellUrls.stream().map(url -> Pair.of(FileUtil.getName(url), url)).collect(Collectors.toList());
        return fileDownloader.download(downloadUrls, target);
    }
}
