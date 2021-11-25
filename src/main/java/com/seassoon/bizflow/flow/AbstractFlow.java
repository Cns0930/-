package com.seassoon.bizflow.flow;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.config.RuleConfig;
import com.seassoon.bizflow.core.model.config.SortConfig;
import com.seassoon.bizflow.core.model.extra.DocumentKV;
import com.seassoon.bizflow.core.model.extra.ExtraKVInfo;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.Input;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;
import com.seassoon.bizflow.core.model.Output;
import com.seassoon.bizflow.core.model.rule.Approval;

import java.util.List;
import java.util.Map;

/**
 * 对流程处理器{@link Flow}进一步抽象，提供默认处理方式，包括：
 * 1.OCR识别
 * 2.文档分类
 * 3.结构化数据提取
 * 4.规则处理
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public abstract class AbstractFlow implements Flow {

    @Override
    public Output process(Input input) {
        Output output = new Output();
        BeanUtil.copyProperties(input, output);

        // STEP-01.OCR结果识别
        List<OcrOutput> ocrOutputs = ocr(input.getImageList());

        // STEP-02.文档分类
        List<Image> images = sort(input.getImageList(), ocrOutputs, input.getConfig().getSortConfig(), input.getExtraInfo().getExtraKvInfo());
        output.setDocumentClassify(Output.DocumentClassify.of(images));

        // STEP-03.结构化数据提取
        List<DocumentKV> docKVs = extract(images, ocrOutputs, input.getConfig().getCheckpointConfig(),
                input.getConfig().getMappingConfig(), input.getExtraInfo().getExtraKvInfo());
        output.setDocumentKvInfo(Output.House.of(docKVs));

        // STEP-04.规则解析
        List<Approval> approvals = rule(docKVs, input.getConfig().getRuleConfig());
        output.setRuleOutputData(Output.House.of(approvals));
        return output;
    }

    /**
     * OCR识别
     *
     * @param images 图片列表
     * @return {@link OcrOutput}
     */
    protected abstract List<OcrOutput> ocr(List<Image> images);

    /**
     * 文档分类
     *
     * @param images     待分类图片
     * @param ocrOutputs OCR识别结果
     * @param sortConfig 分类配置
     * @param extraKVs   结构化数据
     * @return 已分类的文档列表
     */
    protected abstract List<Image> sort(List<Image> images, List<OcrOutput> ocrOutputs, SortConfig sortConfig, List<ExtraKVInfo> extraKVs);

    /**
     * 提取文档结构化数据
     *
     * @param images      已分类的图片
     * @param ocrOutputs  图片OCR结果
     * @param checkpoints 事项配置-提取点
     * @param mapping     事项配置-提取策略映射
     * @param extraKVs    帮办结构化数据
     * @return 结构化数据列表
     */
    protected abstract List<DocumentKV> extract(List<Image> images, List<OcrOutput> ocrOutputs, List<CheckpointConfig> checkpoints,
                                                Map<String, String> mapping, List<ExtraKVInfo> extraKVs);

    /**
     * 规则检查
     *
     * @param docKVs 文档结构化数据
     * @param rules  事项配置-规则
     * @return 规则审批结果
     */
    protected abstract List<Approval> rule(List<DocumentKV> docKVs, List<RuleConfig> rules);
}
