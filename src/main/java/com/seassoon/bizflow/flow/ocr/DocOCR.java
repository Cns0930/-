package com.seassoon.bizflow.flow.ocr;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.file.FileNameUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.component.FileDownloader;
import com.seassoon.bizflow.core.component.ocr.OcrProcessor;
import com.seassoon.bizflow.core.model.ocr.Character;
import com.seassoon.bizflow.core.model.ocr.*;
import com.seassoon.bizflow.core.util.Collections3;
import com.seassoon.bizflow.core.util.JSONUtils;
import com.seassoon.bizflow.support.BizFlowContextHolder;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * {@link OCR}的默认实现
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
@Component
public class DocOCR implements OCR {

    private static final Logger logger = LoggerFactory.getLogger(DocOCR.class);

    @Autowired
    private BizFlowProperties properties;
    @Autowired
    private FileDownloader fileDownloader;
    @Autowired
    private OcrProcessor ocrProcessor;

    @Override
    public List<OcrOutput> ocr(List<Image> images) {
        // 从上下文获取RecordID
        String recordId = BizFlowContextHolder.getInput().getRecordId();

        // 1.图片下载到本地
        String target = Paths.get(properties.getLocalStorage(), recordId, "/files/src").toString();
        List<Pair<String, String>> urls = images.stream()
                .map(image -> {
                    String strExtension = FileNameUtil.extName(image.getImageUrl());
                    String strFilename = image.getImageId() + "." + strExtension;
                    return Pair.of(strFilename, image.getImageUrl());
                }).collect(Collectors.toList());

        List<String> files = fileDownloader.download(urls, target).stream().map(strPath -> {
            // OCR仅支持jpg格式图片，需要将非jpg格式转换为jpg
            Path path = Paths.get(strPath);
            String filename = path.getFileName().toString();
            if (!filename.endsWith("jpg") && !filename.endsWith("jpeg")) {
                Path dest = Paths.get(path.getParent().toString(), FileNameUtil.mainName(filename) + ".jpg");
                ImgUtil.convert(path.toFile(), dest.toFile());
                path = dest;
            }
            return path.toString();
        }).collect(Collectors.toList());

        // 2.图片倾斜矫正/方向矫正
        // TODO 需要Python来做，后续提供接口调用，
        // 接口返回矫正后的图片的URL和角度，合并。
        // 将图片链接填入Image的correctedImageUrl
        // 将图片保存到本地，保存路径为 /files/corrected
        // 后续给OCR识别时候用correctedImageUrl

        // 3.合并倾斜角度和图像方向角度

        // 4.上传矫正后的图片

        // 5.OCR识别处理
        List<OcrResult> ocrResults = post(files.stream().map(Paths::get).collect(Collectors.toList()));

        // 6.处理OCR结果
        return ocrResults.stream()
                .map(this::sortBlock) // 对Block排序
                .map(ocrResult -> mergeLine(ocrResult, -1)) // 行合并
                .collect(Collectors.toList());
    }

    /**
     * 批量OCR识别处理
     *
     * @param files 图片列表
     * @return OCR结果Map，key为图片ID，value为OCR结果（json格式）
     */
    private List<OcrResult> post(List<Path> files) {
        logger.info("正在执行OCR，共{}张图片", files.size());

        // 计数器
        AtomicInteger indexAI = new AtomicInteger(0);
        String recordId = BizFlowContextHolder.getInput().getRecordId();

        return files.stream().parallel().map(file -> {
            // 初始化线程上下文MDC
            BizFlowContextHolder.putMDC(recordId);

            String strFilename = FileNameUtil.mainName(file.getFileName().toString());
            String strExtension = FileNameUtil.extName(file.getFileName().toString());
            String strOcrResponse = ocrProcessor.process(file);

            // OCR结果转JSON
            JsonNode ocrResultNode = JSONUtils.readTree(strOcrResponse);
            if (ocrResultNode == null) {
                throw new NullPointerException("JSON反序列化结果为NULL。");
            }

            // OCR返回结果
            OcrResult ocrResult = new OcrResult();
            ocrResult.setErrorMsg(ocrResultNode.get("error_msg").asText());
            ocrResult.setImageName(strFilename);
            ocrResult.setImageExtension("." + strExtension);
            ocrResult.setBlocks(JSONUtils.readValue(ocrResultNode.get("blocks").toString(), new TypeReference<List<Block>>() {
            }));
            ocrResult.setImagePath(file.toString());

            logger.info("[{}/{}]个图片已处理：{}", indexAI.incrementAndGet(), files.size(), strFilename);
            return ocrResult;
        }).collect(Collectors.toList());
    }

    /**
     * 判断两个{@link Position}是否为同一行
     *
     * @param a {@link List<Position>}
     * @param b {@link List<Position>}
     * @return {@link Boolean}
     */
    private boolean needMerge(List<Position> a, List<Position> b) {
        // 获取重合像素的阈值
        Integer dotCoincide = properties.getAlgorithm().getDotCoincide();

        // 下面这个公式太复杂了，我看不懂
        boolean b1 = Math.abs(a.get(0).getY() - b.get(0).getY()) < dotCoincide;
        boolean b2 = Math.abs(a.get(2).getY() - b.get(2).getY()) < dotCoincide;
        boolean b3 = Math.abs(((a.get(0).getY() + a.get(2).getY()) / 2) - ((b.get(0).getY() + b.get(2).getY()) / 2)) < dotCoincide;
        boolean b4 = Math.abs(((b.get(0).getY() + b.get(2).getY()) / 2) - a.get(0).getY()) < dotCoincide / 2;
        boolean b5 = Math.abs(((b.get(0).getY() + b.get(2).getY()) / 2) - a.get(2).getY()) < dotCoincide / 2;

        return (b1 && b2) || b3 || b4 || b5;
    }

    /**
     * 对block排序
     *
     * @param ocrResult {@link OcrResult}
     * @return 排序后的 {@link OcrResult}
     */
    public OcrResult sortBlock(OcrResult ocrResult) {
        int threshold = properties.getAlgorithm().getDotCoincide();
        List<Block> blocks = ocrResult.getBlocks();
        for (int i = 0; i < blocks.size(); i++) {
            Block a = blocks.get(i);
            for (int j = 0; j < blocks.size(); j++) {
                Block b = blocks.get(j);
                if (j > i) {
                    boolean needMerge = needMerge(a.getPosition(), b.getPosition());
                    boolean gtDotCoincide = (a.getPosition().get(0).getX() - b.getPosition().get(0).getX()) > threshold / 2;

                    if (needMerge && gtDotCoincide) {
                        blocks.set(i, b);
                        blocks.set(j, a);
                    }
                }
            }
        }

        // 更新排序后的Block
        ocrResult.setBlocks(blocks);
        return ocrResult;
    }

    /**
     * 对OCR结果合并，同一行文字间距超过OCR阈值，会作为两行返回，该方法对这种返回情况进行修正。
     * <p>
     * <br/>
     * {@code distance}取值：
     * <ul>
     *     <li>0 - 表示不合并，采用原始OCR返回结果</li>
     *     <li>-1 - 表示合并，不管行与行水平距离多远，均需合并</li>
     *     <li>任意integer值 - 表示合并水平距离小于int的文本行</li>
     * </ul>
     *
     * @param ocrResult {@link OcrResult}
     * @param distance  合并行间距
     * @return {@link OcrOutput}
     */
    public OcrOutput mergeLine(OcrResult ocrResult, int distance) {
        OcrOutput ocrOutput = new OcrOutput();
        ocrOutput.setImageName(ocrResult.getImageName());
        ocrOutput.setOcrResultWithoutLineMerge(ocrResult);

        List<Block> blocks = ocrResult.getBlocks();

        // lineMerged表示该行已经合并到上一行
        Boolean[] lineMerged = blocks.stream().map(block -> false).toArray(Boolean[]::new);

        // 将两行合并为一行
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            if (lineMerged[i]) {
                continue;
            }
            String textA = block.getText();
            List<Position> positionA = block.getPosition();
            Double scoreA = block.getScore();
            List<Character> charactersA = block.getCharacters();

            for (int j = 0; j < blocks.size(); j++) {
                block = blocks.get(j);
                if (j <= i) {
                    continue;
                }

                String textB = block.getText();
                List<Position> positionB = block.getPosition();
                Double scoreB = block.getScore();
                List<Character> charactersB = block.getCharacters();

                if (positionA.get(0).getX() - positionB.get(0).getX() >= 0) {
                    continue;
                }

                if (distance != -1 && (positionB.get(0).getX() - positionA.get(2).getX() > distance)) {
                    continue;
                }

                if (needMerge(positionA, positionB)) {
                    lineMerged[j] = true;

                    String text = textA + textB;
                    Double score = Math.min(scoreA, scoreB);
                    List<Character> characters = Collections3.merge(charactersA, charactersB);

                    List<Position> position = Lists.newArrayList(
                            Position.of(positionA.get(0).getX(), Math.min(positionA.get(0).getY(), positionB.get(0).getY())),
                            Position.of(positionB.get(1).getX(), Math.min(positionA.get(1).getY(), positionB.get(1).getY())),
                            Position.of(positionB.get(2).getX(), Math.min(positionA.get(2).getY(), positionB.get(2).getY())),
                            Position.of(positionA.get(3).getX(), Math.min(positionA.get(3).getY(), positionB.get(3).getY()))
                    );

                    blocks.get(i).setText(text);
                    blocks.get(i).setScore(score);
                    blocks.get(i).setCharacters(characters);
                    blocks.get(i).setPosition(position);

                    textA = text;
                    positionA = position;
                    scoreA = score;
                    charactersA = characters;
                }
            }
        }

        // 删除已合并的行
        int i = 0;
        for (Iterator<Block> iterator = blocks.iterator(); iterator.hasNext(); i++) {
            Block block = iterator.next();
            if (lineMerged[i]) {
                iterator.remove();
            }
        }

        ocrResult.setBlocks(blocks);
        ocrOutput.setOcrResult(ocrResult);
        return ocrOutput;
    }
}
