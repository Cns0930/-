package com.seassoon.bizflow.flow.classify;

import cn.hutool.core.collection.CollectionUtil;
import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.model.extra.ExtraKVInfo;
import com.seassoon.bizflow.core.model.extra.FieldKV;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.config.SortConfig;
import com.seassoon.bizflow.core.model.extra.Document;
import com.seassoon.bizflow.core.model.extra.ExtraInfo;
import com.seassoon.bizflow.core.model.ocr.Block;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;
import com.seassoon.bizflow.core.model.ocr.OcrResult;
import com.seassoon.bizflow.core.model.ocr.Position;
import com.seassoon.bizflow.core.util.TextUtils;
import com.seassoon.bizflow.flow.classify.matcher.*;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link DocClassify} 的默认实现
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class DefaultDocClassify implements DocClassify {

    private static final Logger logger = LoggerFactory.getLogger(DefaultDocClassify.class);

    /** 文档分类匹配器 */
    private final Map<String, Matcher> MATCHERS = new HashMap<>();

    @Autowired
    private BizFlowProperties properties;

    @PostConstruct
    private void postConstruct() {
        // 初始化分类匹配器
        MATCHERS.put("regular", new RegularMatcher());
        MATCHERS.put("Regular", new FullTextRegularMatcher());
        MATCHERS.put("similar", new SimilarMatcher(properties.getAlgorithm().getMatchThreshold()));
        MATCHERS.put("extra", new ExtraMatcher());
    }

    @Override
    public List<Image> classify(List<OcrOutput> ocrOutputs, List<Image> images, SortConfig sortConfig, List<ExtraKVInfo> extraKVs) {
        // 分类映射表，重新分组整理，拆分为正常分类和补充分类
        Map<String, List<List<SortConfig.TypeIdField>>> origTypeIdField = sortConfig.getTypeIdField();

        // 结构化数据，来源于cjbb，且document_label包含-999
        Document xDoc = Optional.ofNullable(extraKVs.get(0))
                .filter(extraKVInfo -> "cjbb".equals(extraKVInfo.getSource()))
                .flatMap(extraKVInfo -> extraKVInfo.getDocumentList().stream()
                        .filter(document -> "-999".equals(document.getDocumentLabel()))
                        .findAny())
                .orElse(null);
        Map<String, List<SortConfig.TypeIdField>> typeIdField = simplifyTypeIdField(origTypeIdField, xDoc);

        // 整理并分组图片OCR结果（按照图片ID分组）
        Map<String, List<String>> docTexts = ocrOutputs.stream().collect(Collectors.toMap(OcrOutput::getImageName, ocrOutput ->
                ocrOutput.getOcrResult().getBlocks().stream().map(Block::getText).collect(Collectors.toList())));

        // 文档标题
        Map<String, String> docTitles = ocrOutputs.stream().collect(Collectors.toMap(OcrOutput::getImageName, ocrOutput ->
                getTitle(ocrOutput.getOcrResultWithoutLineMerge())));

        // 初始化计数器
        AtomicInteger skipAI = new AtomicInteger(0),
                classifiedAI = new AtomicInteger(0);

        // 图片分类
        images.forEach(image -> {

            // 如果材料来源是超级帮办并且有分类，跳过分类
            if (image.getDocumentSource() == 1 && !image.getDocumentLabel().equals("0")
                    && image.getDocumentPage() != null && image.getTotalPages() != null) {
                // 分类配置
                List<SortConfig.TypeIdField> typeIdFields = typeIdField.get(image.getDocumentLabel());
                if (CollectionUtil.isNotEmpty(typeIdFields)) {
                    if (typeIdFields.stream().anyMatch(field -> field.getTotalPages() == -1)) {
                        // 单页材料
                        image.setDocumentPage(-1);
                        image.setTotalPages(-1);
                    } else if (typeIdFields.stream().anyMatch(field -> field.getPage() == 999)) {
                        // 尾页包含多页
                        Integer documentPage = image.getDocumentPage().equals(image.getTotalPages()) ? 999 : -1;
                        image.setDocumentPage(documentPage);
                        image.setTotalPages(-1);
                    } else {
                        // 多页材料
                        image.setDocumentPage(image.getDocumentPage());
                        image.setTotalPages(image.getTotalPages());
                    }
                }

                // 更新计数器
                skipAI.incrementAndGet();
            } else {
                // TODO apiInfo中已有分类结果，跳过分类

                // 获取图片OCR结果，进行分类
                List<String> texts = docTexts.get(image.getImageId());
                typeIdField.forEach((key, value) -> value.forEach(field -> {
                    // 获取对应的Matcher
                    Matcher matcher = MATCHERS.get(field.getMethod());
                    if (matcher == null) {
                        logger.error("分类{}配置错误，未找到对应的匹配器：{}", key, field);
                    } else {
                        if (matcher.match(field, texts)) {
                            // 根据标题相似度匹配
                            Double matchThreshold = properties.getAlgorithm().getMatchThreshold();
                            Optional.ofNullable(docTitles.get(image.getImageId())).ifPresent(title -> {
                                boolean isMatchTitle = field.getPattern().stream()
                                        .anyMatch(pattern -> TextUtils.similarity(pattern, title, matchThreshold) > matchThreshold);

                            });
                        }
                    }
                }));
            }
        });

        return null;
    }

    /**
     * 对分类配置进行简化，将pattern和except字段替换为具体值。
     *
     * @param typeIdField 分类配置
     * @return 分组后的分类配置，Key：R表示正常分类配置，X表示包含排除字段和辅助字段的配置
     */
    private Map<String, List<SortConfig.TypeIdField>> simplifyTypeIdField(Map<String, List<List<SortConfig.TypeIdField>>> typeIdField, Document xDoc) {
        // 重新整理分类配置的结构，使其更容易操作（太乱了，套了好几层）
        Map<String, List<SortConfig.TypeIdField>> simplifyMap = typeIdField.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().flatMap(Collection::stream).collect(Collectors.toList())));

        // 对整理后的分类配置进行分组，分为key为R的正常配置和key为X的含有排除/辅助字段的配置
//        Map<String, Map<String, List<SortConfig.TypeIdField>>> grouped = simplifyMap.entrySet().stream()
//                .collect(Collectors.groupingBy(entry -> entry.getValue().stream().anyMatch(field -> {
//                    // 分组逻辑：pattern或except任何字段中含有x_的值，就表示包含辅助字段
//                    Boolean isExcept = field.getExcept().stream().anyMatch(except -> except.startsWith("x_"));
//                    Boolean isPattern = field.getPattern().stream().anyMatch(pattern -> pattern.startsWith("x_"));
//                    return isExcept || isPattern;
//                }) ? "X" : "R")).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry ->
//                        // 分组后转化value的值为Map，其中key为documentLabel，value为分类配置
//                        entry.getValue().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));

        // 如果没有排除字段或辅助字段，直接返回（结构化数据不存在也直接返回）
//        if (grouped.get("X") == null || grouped.get("X").size() == 0 || CollectionUtil.isEmpty(extraInfo.getExtraKvInfo())) {
//            return grouped;
//        }

        // 结构化数据，来源于cjbb，且document_label包含-999
//        ExtraKVInfo extraKVInfo = extraInfo.getExtraKvInfo().get(0);
//        Document xDoc = null;
//        if ("cjbb".equals(extraKVInfo.getSource())) {
//            xDoc = extraKVInfo.getDocumentList().stream()
//                    .filter(document -> "-999".equals(document.getDocumentLabel()))
//                    .findAny().orElse(null);
//        }

        // 结构化数据中没有-999的字段，移除辅助字段和排除字段并返回
        if (xDoc == null) {
            simplifyMap.forEach((key, value) -> value.forEach(field -> {
                // 过滤掉辅助字段和排除字段中x_的字段名
                field.setPattern(field.getPattern().stream().filter(str -> !str.startsWith("x_")).collect(Collectors.toList()));
                field.setExcept(field.getExcept().stream().filter(str -> !str.startsWith("x_")).collect(Collectors.toList()));
            }));
//            grouped.get("X").forEach((key, value) -> value.forEach(field -> {
//                // 过滤掉辅助字段和排除字段中x_的字段名
//                field.setPattern(field.getPattern().stream().filter(str -> !str.startsWith("x_")).collect(Collectors.toList()));
//                field.setExcept(field.getExcept().stream().filter(str -> !str.startsWith("x_")).collect(Collectors.toList()));
//            }));
            return simplifyMap;
        }

        // 根据结构化数据替换分类配置：辅助字段"x_委托人姓名"替换为"张三"，排除字段"x_委托人姓名"替换为"x_张三"
        List<FieldKV> xDocFieldKV = xDoc.getFieldVal();
        simplifyMap.forEach((key, value) -> value.forEach(field -> {
            // 辅助字段中包含x_开头，将method设置为extra
            if (field.getPattern().stream().anyMatch(str -> str.startsWith("x_"))) {
                field.setMethod("extra");

                // 替换为结构化数据里的值
                xDocFieldKV.forEach(kv -> {
                    field.setPattern(replaceFieldValue(field.getPattern(), kv, str -> str));
                    field.setExcept(replaceFieldValue(field.getExcept(), kv, str -> "x_" + str));
                });
            }
        }));
        return simplifyMap;
//        grouped.get("X").forEach((key, value) -> value.forEach(field -> {
//            // 辅助字段中包含x_开头，将method设置为extra
//            if (field.getPattern().stream().anyMatch(str -> str.startsWith("x_"))) {
//                field.setMethod("extra");
//            }
//
//            // 替换为结构化数据里的值
//            xDocFieldVal.forEach(fieldVal -> {
//                field.setPattern(replaceFieldValue(field.getPattern(), fieldVal, str -> str));
//                field.setExcept(replaceFieldValue(field.getExcept(), fieldVal, str -> "x_" + str));
//            });
//        }));

//        return grouped;
    }

    /**
     * 替换分类配置中Pattern和Except的辅助字段和排除字段
     *
     * @param fields   pattern或者except值
     * @param fieldKV 结构化数据-999中的部分
     * @param mapper   替换逻辑，由使用者提供实现
     * @return 替换后的pattern或except
     */
    private List<String> replaceFieldValue(List<String> fields, FieldKV fieldKV, Function<String, String> mapper) {
        return fields.stream().map(str -> {
            if (str.startsWith("x_")) {
                // 将字段名中的x_替换为空白字符，再匹配结构化数据
                String strField = RegExUtils.replaceAll(str, "x_", "");
                if (fieldKV.getKey().equals(strField) && CollectionUtil.isNotEmpty(fieldKV.getVal())) {
                    str = mapper.apply(fieldKV.getVal().get(0));
                }
            }
            return str;
        }).collect(Collectors.toList());
    }

    /**
     * 根据OCR结果获取文档标题
     * <p>
     * 1.明显大于其它行文本高度的平均值，并且只用一行
     * 2.位于整个文档的中间
     * <p>
     * 标题只能有一行，多行的识别不出
     *
     * @param ocrResult {@link OcrResult}
     * @return 文档标题，若无标题，返回null
     */
    private String getTitle(OcrResult ocrResult) {
        // 检查文本方向
        boolean isHorizontal = checkTextHorizontal(ocrResult);

        // 获取Block每行的行高（只判断前5行）
        List<Pair<String, Double>> lineHeights = ocrResult.getBlocks().stream().limit(5)
                .map(block -> Pair.of(block.getText(), calcLineHeight(block.getPosition(), isHorizontal)))
                .collect(Collectors.toList());
        if (lineHeights.isEmpty()) {
            return StringUtils.EMPTY;
        }

        // 字体最大的文本
        Pair<String, Double> maxLineHeight = lineHeights.stream()
                .max(Comparator.comparingDouble(Pair::getRight)).orElseThrow(() -> new NullPointerException("获取最大文本出错"));

        // 前5行文本与字体最大的文本对比
        lineHeights = lineHeights.stream().filter(pair -> maxLineHeight.getRight() - pair.getRight() <= 5).collect(Collectors.toList());

        // 最高的相差2，认为等高，有两行等高认为不是标题，直接返回
        boolean isEqualHeight = lineHeights.stream()
                .anyMatch(pair -> !maxLineHeight.getLeft().equals(pair.getLeft()) && (maxLineHeight.getValue() - pair.getValue()) <= 2);
        if (isEqualHeight) {
            return StringUtils.EMPTY;
        }

        // 图片分辨率
        Pair<Integer, Integer> resolution = readImageResolution(Paths.get(ocrResult.getImagePath()));

        // 文档边界
        Pair<Integer, Integer> textBoard = calcTextBoard(ocrResult.getBlocks(), isHorizontal);

        // 确定文档标题
        return ocrResult.getBlocks().stream()
                .filter(block -> block.getText().equals(maxLineHeight.getLeft()))
                .filter(block -> {
                    // 在文档中居中，认为是标题
                    int diffVal = 0, edgeDiffVal = 0;
                    if (isHorizontal) {
                        diffVal = Math.abs((resolution.getLeft() - block.getPosition().get(1).getX()) - block.getPosition().get(0).getX());
                        edgeDiffVal = Math.abs((textBoard.getRight() - block.getPosition().get(1).getX()) - (block.getPosition().get(0).getX() - textBoard.getLeft()));
                    } else {
                        diffVal = Math.abs((resolution.getRight() - block.getPosition().get(2).getY()) - block.getPosition().get(1).getY());
                        edgeDiffVal = Math.abs((textBoard.getRight() - block.getPosition().get(2).getY()) - (block.getPosition().get(1).getY() - textBoard.getLeft()));
                    }

                    // 左右空白出相减不能大于3个个字体，认为文本位于中间的位置，可能是标题
                    return diffVal < maxLineHeight.getRight() * 3 || edgeDiffVal < maxLineHeight.getRight() * 3;
                }).map(Block::getText).findAny().orElse(StringUtils.EMPTY);
    }

    /**
     * 根据文本坐标计算行高
     *
     * @param p            坐标
     * @param isHorizontal 文本方向是否水平
     * @return 行高
     */
    private double calcLineHeight(List<Position> p, boolean isHorizontal) {
        double averageHeight = 0.00D;
        if (isHorizontal) {
            averageHeight = Math.abs((p.get(0).getY() + p.get(1).getY()) / 2 - (p.get(2).getY() + p.get(3).getY()) / 2);
        } else {
            averageHeight = Math.abs((p.get(0).getX() + p.get(1).getX()) / 2 - (p.get(2).getX() + p.get(3).getX()) / 2);
        }
        return BigDecimal.valueOf(averageHeight).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 获取文档文本的左右边界
     *
     * @param blocks       文档文本{@link Block}
     * @param isHorizontal 文本方向是否水平
     * @return {@link Pair}
     */
    private Pair<Integer, Integer> calcTextBoard(List<Block> blocks, boolean isHorizontal) {
        Integer leftEdge = blocks.stream()
                .map(block -> isHorizontal ? block.getPosition().get(0).getX() : block.getPosition().get(1).getY())
                .min(Integer::compare).orElse(0);
        Integer rightEdge = blocks.stream()
                .map(block -> isHorizontal ? block.getPosition().get(1).getX() : block.getPosition().get(2).getY())
                .max(Integer::compare).orElse(0);
        return Pair.of(leftEdge, rightEdge);
    }

    /**
     * 读取图片分辨率（宽 * 高）
     *
     * @param path 图片路径
     * @return {@link Pair}对象，left为宽，right为高
     */
    private Pair<Integer, Integer> readImageResolution(Path path) {
        // 读取图片高度和宽度
        try (InputStream inputStream = Files.newInputStream(path)) {
            BufferedImage image = ImageIO.read(inputStream);
            return Pair.of(image.getWidth(), image.getHeight());
        } catch (IOException e) {
            logger.error("读取图片文件失败 - " + e.getMessage(), e);
            return Pair.of(0, 0);
        }
    }

    /**
     * 检查文本方向是否为水平
     *
     * @param ocrResult {@link OcrResult}
     * @return true of false
     */
    private boolean checkTextHorizontal(OcrResult ocrResult) {
        return ocrResult.getBlocks().stream().anyMatch(block -> {
            // text文本至少2个字符
            boolean textCheck = StringUtils.isNotBlank(block.getText()) && block.getText().length() > 1;

            List<Position> p = block.getPosition();
            boolean positionCheck = p.get(1).getX() - p.get(0).getX() > p.get(2).getY() - p.get(1).getY();
            return textCheck && positionCheck;
        });
    }
}
