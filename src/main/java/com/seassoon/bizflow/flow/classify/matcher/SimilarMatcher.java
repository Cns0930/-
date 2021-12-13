package com.seassoon.bizflow.flow.classify.matcher;

import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.model.config.SortConfig;
import com.seassoon.bizflow.core.util.TextUtils;
import com.seassoon.bizflow.flow.classify.DefaultDocClassify;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 相似性匹配器，对应{@link SortConfig.TypeIdField}#method = similar
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class SimilarMatcher extends AbstractMatcher implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(SimilarMatcher.class);

    private double threshold;

    @Autowired
    private BizFlowProperties properties;

    @Override
    protected boolean matchPattern(List<String> patterns, List<String> texts) {
        return texts.stream().anyMatch(str -> {
            String content = ReUtil.replaceAll(str, Pattern.compile(IGNORE_PATTERN), StrUtil.EMPTY);
            if (StringUtils.isEmpty(content)) {
                logger.info("相似度比较有空的字符串{}", texts);
                return false;
            } else {
                return patterns.stream().anyMatch(pattern -> TextUtils.similarity(pattern, content, threshold) > threshold);
            }
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.threshold = properties.getAlgorithm().getMatchThreshold();
    }
}
