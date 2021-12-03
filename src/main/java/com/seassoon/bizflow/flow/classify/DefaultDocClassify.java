package com.seassoon.bizflow.flow.classify;

import cn.hutool.core.collection.CollectionUtil;
import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.model.extra.ExtraKVInfo;
import com.seassoon.bizflow.core.model.extra.FieldKV;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.model.config.SortConfig;
import com.seassoon.bizflow.core.model.extra.Document;
import com.seassoon.bizflow.core.model.ocr.Block;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;
import com.seassoon.bizflow.core.model.ocr.OcrResult;
import com.seassoon.bizflow.core.model.ocr.Position;
import com.seassoon.bizflow.core.util.Collections3;
import com.seassoon.bizflow.core.util.TextUtils;
import com.seassoon.bizflow.flow.classify.matcher.*;
import com.seassoon.bizflow.flow.classify.special.DrivingLicenceDetect;
import com.seassoon.bizflow.flow.classify.special.IdCardDetect;
import com.seassoon.bizflow.flow.classify.special.SpecialDetect;
import com.seassoon.bizflow.flow.classify.special.SpecialServer;
import com.seassoon.bizflow.flow.extract.detect.Detector;
import com.seassoon.bizflow.flow.extract.detect.HandwritingDetector;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

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
import java.util.concurrent.atomic.AtomicReference;
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

    /**
     * 文档分类匹配器
     */
    private final Map<String, Matcher> MATCHERS = new HashMap<>();
    private final Map<String, SpecialDetect> SPECIAL_DETECT_MAP = new HashMap<>();
    @Autowired
    private BizFlowProperties properties;
    @Autowired
    private ApplicationContext appContext;

    @PostConstruct
    private void postConstruct() {
        // 初始化分类匹配器
        MATCHERS.put("regular", new RegularMatcher());
        MATCHERS.put("Regular", new FullTextRegularMatcher());
        MATCHERS.put("similar", new SimilarMatcher(properties.getAlgorithm().getMatchThreshold()));
        MATCHERS.put("extra", new ExtraMatcher());
        //特殊材料分类实例
        SPECIAL_DETECT_MAP.put("ID_CARD", appContext.getBean(IdCardDetect.class));
        SPECIAL_DETECT_MAP.put("DRIVING_LICENCE", appContext.getBean(DrivingLicenceDetect.class));
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
        Map<String, Map<String, List<List<SortConfig.TypeIdField>>>> typeIdField = simplifyTypeIdField(origTypeIdField, xDoc);

        // 整理并分组图片OCR结果（按照图片ID分组）
        Map<String, List<String>> docTexts = ocrOutputs.stream().collect(Collectors.toMap(OcrOutput::getImageName, ocrOutput ->
                ocrOutput.getOcrResult().getBlocks().stream().map(Block::getText).collect(Collectors.toList())));
        //用与后面判断分类是否正确 先复制一份
        List<Image> copyImages = images.stream().map(image -> image.clone()).collect(Collectors.toList());
        // 文档标题
        Map<String, Object> docTitles = Collections3.parseMapForFilter(ocrOutputs.stream().collect(Collectors.toMap(OcrOutput::getImageName, ocrOutput ->
                getTitle(ocrOutput.getOcrResultWithoutLineMerge()))));
        // 初始化计数器
        AtomicInteger skipAI = new AtomicInteger(0),
                classifiedAI = new AtomicInteger(0);

        // 图片分类
        images.forEach(image -> {
//            // 如果材料来源是超级帮办并且有分类，跳过分类
            if (image.getDocumentSource() == 1 && !image.getDocumentLabel().equals("0")
                    && image.getDocumentPage() != null && image.getTotalPages() != null) {
                // 分类配置
                List<List<SortConfig.TypeIdField>> typeIdFields = origTypeIdField.get(image.getDocumentLabel());
                if (CollectionUtil.isNotEmpty(typeIdFields)) {
                    if (typeIdFields.stream().anyMatch(field -> field.stream().anyMatch(f -> f.getTotalPages() == -1))) {
                        // 单页材料
                        image.setDocumentPage(-1);
                        image.setTotalPages(-1);
                    } else if (typeIdFields.stream().anyMatch(field -> field.stream().anyMatch(f -> f.getPage() == 999))) {
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
                // 根据ocr结果-规则进行分类；
                matchClassif(image, typeIdField.get("R"), texts, docTitles, copyImages);
                // 根据调用结构化数据中的字段进行额外补充细分;
                if ("0".equals(image.getDocumentLabel()) && !CollectionUtils.isEmpty(typeIdField.get("X"))) {
                    matchClassif(image, typeIdField.get("X"), texts, docTitles, copyImages);
                }
                // step2. 获取身份证复印件，驾驶证分类 特殊分类
                if ("0".equals(image.getDocumentLabel())) {
                    origTypeIdField.keySet().stream().filter(field -> field.equals("SC-E02") || field.equals("JD-XS01")).
                            findAny().ifPresent(key -> {
                        boolean isIdCard = key.equals("SC-E02") && SPECIAL_DETECT_MAP.get("ID_CARD").preProcessing(image);
                        boolean isDrivingLicence = key.equals("JD-XS01") && SPECIAL_DETECT_MAP.get("DRIVING_LICENCE").preProcessing(image);
                        if ((isIdCard && !isDrivingLicence) || (!isIdCard && isDrivingLicence)) {
                            image.saveResult(image, key, 1, 1);
                        }
                    });
                }
            }
        });
        return images;
    }

    /**
     * 对分类配置进行简化，将pattern和except字段替换为具体值。
     *
     * @param typeIdField 分类配置
     * @return 分组后的分类配置，Key：R表示正常分类配置，X表示包含排除字段和辅助字段的配置
     */
    private Map<String, Map<String, List<List<SortConfig.TypeIdField>>>> simplifyTypeIdField(Map<String, List<List<SortConfig.TypeIdField>>> typeIdField, Document xDoc) {
        // 重新整理分类配置的结构，使其更容易操作（太乱了，套了好几层）
//        Map<String, List<SortConfig.TypeIdField>> simplifyMap = typeIdField.entrySet().stream()
//                .collect(Collectors.toMap(
//                        Map.Entry::getKey,
//                        entry -> entry.getValue().stream().flatMap(Collection::stream).collect(Collectors.toList())));

//         对整理后的分类配置进行分组，分为key为R的正常配置和key为X的含有排除/辅助字段的配置
//        Map<String, Map<String, List<SortConfig.TypeIdField>>> grouped = simplifyMap.entrySet().stream()
//                .collect(Collectors.groupingBy(entry -> entry.getValue().stream().anyMatch(field -> {
//                    // 分组逻辑：pattern或except任何字段中含有x_的值，就表示包含辅助字段
//                    Boolean isExcept = field.getExcept().stream().anyMatch(except -> except.startsWith("x_"));
//                    Boolean isPattern = field.getPattern().stream().anyMatch(pattern -> pattern.startsWith("x_"));
//                    return isExcept || isPattern;
//                }) ? "X" : "R")).entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry ->
//                        // 分组后转化value的值为Map，其中key为documentLabel，value为分类配置
//                        entry.getValue().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
        Map<String, Map<String, List<List<SortConfig.TypeIdField>>>> grouped = new HashMap<>();
        Map<String, List<List<SortConfig.TypeIdField>>> RtypeIdField = new LinkedHashMap<>();
        Map<String, List<List<SortConfig.TypeIdField>>> XtypeIdField = new LinkedHashMap<>();
        typeIdField.keySet().forEach(key -> {
            List<List<SortConfig.TypeIdField>> typeId = typeIdField.get(key);
            if (typeId.stream().anyMatch(f -> f.stream().anyMatch(field ->
                    field.getExcept().stream().anyMatch(except -> except.startsWith("x_")) ||
                            field.getPattern().stream().anyMatch(pattern -> pattern.startsWith("x_"))))) {
                XtypeIdField.put(key, typeId);
            } else {
                RtypeIdField.put(key, typeId);
            }
        });
        //分成两组
        grouped.put("X", XtypeIdField);
        grouped.put("R", RtypeIdField);

        // 如果没有排除字段或辅助字段，直接返回（结构化数据不存在也直接返回）
        if (grouped.get("X") == null || grouped.get("X").size() == 0 || Objects.isNull(xDoc)) {
            return grouped;
        }

        // 结构化数据，来源于cjbb，且document_label包含-999
//        ExtraKVInfo extraKVInfo = xDoc.getFieldVal().get(0);
//        Document xDoc = null;
//        if ("cjbb".equals(extraKVInfo.getSource())) {
//            xDoc = extraKVInfo.getDocumentList().stream()
//                    .filter(document -> "-999".equals(document.getDocumentLabel()))
//                    .findAny().orElse(null);
//        }

        // 结构化数据中没有-999的字段，移除辅助字段和排除字段并返回
        if (xDoc == null) {
//             simplifyMap.forEach((key, value) -> value.forEach(field -> {
//                // 过滤掉辅助字段和排除字段中x_的字段名
//                field.setPattern(field.getPattern().stream().filter(str -> !str.startsWith("x_")).collect(Collectors.toList()));
//                field.setExcept(field.getExcept().stream().filter(str -> !str.startsWith("x_")).collect(Collectors.toList()));
//            }));
            grouped.get("X").forEach((key, value) -> value.forEach(field -> {
                field.forEach(f -> {
                    // 过滤掉辅助字段和排除字段中x_的字段名
                    f.setPattern(f.getPattern().stream().filter(str -> !str.startsWith("x_")).collect(Collectors.toList()));
                    f.setExcept(f.getExcept().stream().filter(str -> !str.startsWith("x_")).collect(Collectors.toList()));
                });
            }));
            return grouped;
        }

        // 根据结构化数据替换分类配置：辅助字段"x_委托人姓名"替换为"张三"，排除字段"x_委托人姓名"替换为"x_张三"
        List<FieldKV> xDocFieldKV = xDoc.getFieldVal();
//        simplifyMap.forEach((key, value) -> value.forEach(field -> {
//            // 辅助字段中包含x_开头，将method设置为extra
//            if (field.getPattern().stream().anyMatch(str -> str.startsWith("x_")) || field.getExcept().stream().anyMatch(str -> str.startsWith("x_"))) {
//                field.setMethod("extra");
//                // 替换为结构化数据里的值
//                xDocFieldKV.forEach(kv -> {
//                    field.setPattern(replaceFieldValue(field.getPattern(), kv, str -> str));
//                    field.setExcept(replaceFieldValue(field.getExcept(), kv, str -> "x_" + str));
//                });
//            }
//        }));
        grouped.get("X").forEach((key, value) -> value.forEach(f -> {
            f.forEach(field -> {
                // 辅助字段中包含x_开头，将method设置为extra
                if (field.getPattern().stream().anyMatch(str -> str.startsWith("x_")) || field.getExcept().stream().anyMatch(str -> str.startsWith("x_"))) {
                    field.setMethod("extra");
                }
                // 替换为结构化数据里的值
                xDocFieldKV.forEach(fieldVal -> {
                    field.setPattern(replaceFieldValue(field.getPattern(), fieldVal, str -> str));
                    field.setExcept(replaceFieldValue(field.getExcept(), fieldVal, str -> "x_" + str));
                });
            });
        }));

        //删掉没有匹配到结构化数据的配置信息
        List<Object> kvinfo = xDocFieldKV.stream().map(FieldKV::getVal).collect(Collectors.toList());
        grouped.get("X").forEach((key, value) -> value.forEach(info -> {
            info.forEach(field -> {
                if (field.getPattern().stream().anyMatch(str -> str.startsWith("x_"))) {
                    field.setPattern(field.getPattern().stream().filter(str -> !str.startsWith("x_")).collect(Collectors.toList()));
                }
                if (field.getExcept().stream().anyMatch(str -> str.startsWith("x_"))) {
                    field.setExcept(field.getExcept().stream().filter(str ->
                            !str.startsWith("x_") ||
                                    !CollectionUtils.isEmpty(kvinfo.stream().filter(f -> f.toString().contains(str.substring(str.lastIndexOf("_") + 1, str.length()))).collect(Collectors.toList()))).collect(Collectors.toList()));
                }
            });
        }));
        return grouped;
    }

    /**
     * 替换分类配置中Pattern和Except的辅助字段和排除字段
     *
     * @param fields  pattern或者except值
     * @param fieldKV 结构化数据-999中的部分
     * @param mapper  替换逻辑，由使用者提供实现
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

    /**
     * 匹配分类信息
     *
     * @param image       图片分类信息
     * @param typeIdField 配置信息(存在普通分类和补充分类)
     */
    private void matchClassif(Image image, Map<String, List<List<SortConfig.TypeIdField>>> typeIdField, List<String> texts, Map<String, Object> docTitles, List<Image> copyImages) {
        for (String typeId : typeIdField.keySet()) {
            List<List<SortConfig.TypeIdField>> typeIdFieldInfoList = typeIdField.get(typeId);
            //匹配
            match(image, typeIdFieldInfoList, texts, typeId, docTitles, copyImages);
        }
    }

    /**
     * @param image               分类图片
     * @param typeIdFieldInfoList 配置列表
     * @param texts               ocr
     * @param typeId              材料
     * @param docTitles           图片标题
     * @param copyImages          copy的图片信息
     */
    public void match(Image image, List<List<SortConfig.TypeIdField>> typeIdFieldInfoList, List<String> texts, String typeId, Map<String, Object> docTitles, List<Image> copyImages) {
        for (List<SortConfig.TypeIdField> field : typeIdFieldInfoList) {
            AtomicReference<Boolean> isMatch = new AtomicReference<>(false);
            AtomicReference<Integer> documentPage = new AtomicReference<>(0);
            AtomicReference<Integer> totalPages = new AtomicReference<>(0);
            for (SortConfig.TypeIdField f : field) {
                Matcher matcher = MATCHERS.get(f.getMethod());
                //一组数据必须全部满足
                if (!matcher.match(f, texts)) {
                    return;
                }
                isMatch.set(matcher.match(f, texts));
                documentPage.set(f.getPage());
                totalPages.set(f.getTotalPages());
            }
            if (isMatch.get()) {
                if (!"0".equals(image.getDocumentLabel())) {
                    Double matchThreshold = properties.getAlgorithm().getMatchThreshold();
                    Optional.ofNullable(docTitles.get(image.getImageId())).ifPresent(title -> {
                        // 如果又多个分类匹配是否更像标题 不知道这里为什么拿最后一个
                        boolean isMatchTitle = field.get(field.size() - 1).getPattern().stream()
                                .anyMatch(pattern ->
                                        TextUtils.similarity(pattern, title.toString(), matchThreshold) > matchThreshold);
                        if (isMatchTitle) {
                            image.saveResult(image, typeId, documentPage.get(), totalPages.get());
                        }
                    });
                } else {
                    image.saveResult(image, typeId, documentPage.get(), totalPages.get());
                }
                //判断分类是否正确 如果是帮办数据判断是否与传入的类别一致  # 若自备，传入的有类别，且传入类别和程序分类结果不一致
                copyImages.stream().filter(f -> f.getImageId().equals(image.getImageId())).findAny().ifPresent(img -> {
                    if (img.getDocumentSource() == 0 && !"0".equals(img.getDocumentLabel()) && !typeId.equals(img.getDocumentLabel())) {
                        logger.info("图片{}分类结果存在不一致：{}||{}  ", image.getImageId(), image.getDocumentLabel(), typeId);
                        image.saveResult(image, "-1", 0, 0);
                    } else if (img.getDocumentSource() == 0 && "0".equals(img.getDocumentLabel()) && typeId.equals(img.getDocumentLabel())) {
                        image.saveResult(image, typeId, documentPage.get(), totalPages.get());
                    }
                });
                break;
            }
        }
    }
}
