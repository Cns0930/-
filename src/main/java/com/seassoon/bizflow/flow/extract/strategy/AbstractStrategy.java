package com.seassoon.bizflow.flow.extract.strategy;

import cn.hutool.core.map.MapUtil;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.extra.Content;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;
import com.seassoon.bizflow.core.util.JSONUtils;
import com.seassoon.bizflow.flow.extract.resolve.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;

import java.util.*;
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
    protected List<Resolver> forResolvers(CheckpointConfig.ExtractPoint extractPoint) {
        return resolvers.stream().filter(resolver -> resolver.support(extractPoint)).collect(Collectors.toList());
    }

    /**
     * 对提取点分组
     * @param extractPointList
     * @param multiPage
     * @return
     */
    protected Map<String, List<CheckpointConfig.ExtractPoint>> divideIntoGroups(List<CheckpointConfig.ExtractPoint> extractPointList, Boolean multiPage) {
        // 已测
        Map<String, List<CheckpointConfig.ExtractPoint>> map = new HashMap<>();
        extractPointList.forEach(extractPoint -> {
            List<String> groupNames = groupNames(extractPoint, multiPage);
            for (String group : groupNames) {
                List<CheckpointConfig.ExtractPoint> list = map.getOrDefault(group, new ArrayList<>());
                list.add(extractPoint);
                map.put(group, map.getOrDefault(group, list));
            }
        });
        return map;
    }

    private List<String> groupNames(CheckpointConfig.ExtractPoint extractPoint, Boolean multiPage) {
        // valueEnvironment
        boolean text = "text".equals(extractPoint.getValueEnvironment());
        boolean texts = "texts".equals(extractPoint.getValueEnvironment());
        boolean table = "table".equals(extractPoint.getValueEnvironment());
        // textStringPatternRange
        boolean line = "line".equals(extractPoint.getTextStringPatternRange());
        boolean context = "context".equals(extractPoint.getTextStringPatternRange());
        // keyValueRelativePosition
        boolean position6 = Arrays.asList("up", "down", "left", "right", "middle", "@middle").contains(extractPoint.getKeyValueRelativePosition());
        boolean right = "right".equals(extractPoint.getKeyValueRelativePosition());
        boolean rightAll = "right_all".equals(extractPoint.getKeyValueRelativePosition());
        boolean around = "around".equals(extractPoint.getKeyValueRelativePosition());
        boolean down = "down".equals(extractPoint.getKeyValueRelativePosition());
        boolean down2 = Arrays.asList("down", "down_first").contains(extractPoint.getKeyValueRelativePosition());
        boolean position4 = Arrays.asList("up", "down", "left", "right").contains(extractPoint.getKeyValueRelativePosition());
        // valueType
        boolean string = "string".equals(extractPoint.getValueType());
        boolean img = "img".equals(extractPoint.getValueType());
        // alias
        boolean nearby = !CollectionUtils.isEmpty(extractPoint.getAlias()) && extractPoint.getAlias().get(0).contains("@");
        boolean cell = !CollectionUtils.isEmpty(extractPoint.getAlias()) && extractPoint.getAlias().get(0).startsWith("@");
        boolean cross = !CollectionUtils.isEmpty(extractPoint.getAlias()) && extractPoint.getAlias().get(0).startsWith("&");
        boolean nearbyText = !CollectionUtils.isEmpty(extractPoint.getAlias()) && extractPoint.getAlias().get(0).contains("_text@");
        boolean valueNot = !CollectionUtils.isEmpty(extractPoint.getAlias()) && !extractPoint.getAlias().get(0).contains("_value@");
        boolean value = !CollectionUtils.isEmpty(extractPoint.getAlias()) && extractPoint.getAlias().get(0).contains("_value@");
        // 其他判定
        boolean f = "19".equals(extractPoint.getSignSealId());

        List<String> groups = new ArrayList<>();
        // multipage_text_string_lm_pattern  1
        if (text && f && string && multiPage) {
            groups.add("multipage_text_string_lm_pattern");
        }
        // multipage_text_line_string        2
        if (text && line && string && multiPage && !nearby && !f) {
            groups.add("multipage_text_line_string");
        }
        // multipage_text_lines_string       3
        if (texts && line && string && multiPage && !nearby && !f) {
            groups.add("multipage_text_lines_string");
        }
        // multipage_text_line_string_nb      4
        if (text && line && string && multiPage && nearby) {
            groups.add("multipage_text_line_string_nb");
        }
        // multipage_text_context_string    5
        if (text && context && string && multiPage && !nearby) {
            groups.add("multipage_text_context_string");
        }
        // multipage_text_img               6
        if (text && position6 && img && multiPage && !nearby) {
            groups.add("multipage_text_img");
        }
        // multipage_text_imgs              7
        if (texts && position6 && img && multiPage && !nearby) {
            groups.add("multipage_text_imgs");
        }
        // multipage_text_img_nb            8
        if (text && position6 && img && multiPage && nearby) {
            groups.add("multipage_text_img_nb");
        }
        // multipage_right_table_string     9
        if (table && right && string && multiPage && !nearby) {
            groups.add("multipage_right_table_string");
        }
        // multipage_right_table_all_string 10
        if (table && rightAll && string && multiPage && !nearby) {
            groups.add("multipage_right_table_all_string");
        }
        // around_text_img                  11
        if (text && around && img && multiPage) {
            groups.add("around_text_img");
        }
        // multipage_down_table_string      12
        if (table && down && string && multiPage && !nearby) {
            groups.add("multipage_down_table_string");
        }
        // multipage_down_table_string_cell  13
        if (table && down2 && string && multiPage && nearby && cell) {
            groups.add("multipage_down_table_string_cell");
        }
        // multipage_down_table_string_cell_nb  14
        if (table && down2 && string && multiPage && nearby && !cell && !nearbyText) {
            groups.add("multipage_down_table_string_cell_nb");
        }
        // multipage_down_table_string_cross_cell  15
        if (table & down2 && string & multiPage && cross) {
            groups.add("multipage_down_table_string_cross_cell");
        }
        // multipage_down_table_string_cell_nb_text  16
        if (table && down2 && string && multiPage && nearbyText) {
            groups.add("multipage_down_table_string_cell_nb_text");
        }
        // multipage_up_down_left_right_table_img   17
        if (table && position4 && img && multiPage) {
            groups.add("multipage_up_down_left_right_table_img");
        }
        // multipage_right_nb_table_string，这个是以《食品经营许可变更申请表》为原型开发 18
        if (table && right && string && valueNot && nearby) {
            groups.add("multipage_right_nb_table_string");
        }
        // multipage_right_nb_table_string_value   19
        if (table && right && string && value) {
            groups.add("multipage_right_nb_table_string_value");
        }
        return groups;
    }

}
