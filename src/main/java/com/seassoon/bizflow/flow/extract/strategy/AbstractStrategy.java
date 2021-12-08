package com.seassoon.bizflow.flow.extract.strategy;

import cn.hutool.core.map.MapUtil;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.Content;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;
import com.seassoon.bizflow.flow.extract.resolve.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
public abstract class AbstractStrategy implements UnifiedStrategy, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(SinglePageStrategy.class);

    private List<Resolver> resolvers = new ArrayList<>();

    @Autowired
    private ApplicationContext appContext;

    /**
     * 初始化所有提取工具{@link Resolver}
     *
     * @throws Exception 异常
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, Resolver> namedResolvers = appContext.getBeansOfType(Resolver.class);
        if (MapUtil.isEmpty(namedResolvers)) {
            throw new NullPointerException("没有可用的Resolver");
        }
        resolvers = new ArrayList<>(namedResolvers.values());
    }

    @Override
    public abstract List<Content> parse(List<Image> images, List<OcrOutput> ocrOutputs, CheckpointConfig checkpoint);

    /**
     * 根据{@link com.seassoon.bizflow.core.model.config.CheckpointConfig.ExtractPoint}信息匹配到正确的{@link Resolver}
     *
     * @param extractPoint {@link com.seassoon.bizflow.core.model.config.CheckpointConfig.ExtractPoint}
     * @return {@link Resolver}集合
     */
    protected Resolver getResolver(CheckpointConfig.ExtractPoint extractPoint) {
        return resolvers.stream().filter(resolver -> resolver.support(extractPoint)).findAny().orElse(null);
    }
}
