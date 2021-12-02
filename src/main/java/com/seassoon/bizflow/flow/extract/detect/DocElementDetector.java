package com.seassoon.bizflow.flow.extract.detect;

import cn.hutool.cache.impl.LRUCache;
import cn.hutool.core.collection.CollectionUtil;
import com.seassoon.bizflow.config.BizFlowProperties;
import com.seassoon.bizflow.core.component.HTTPCaller;
import com.seassoon.bizflow.core.model.element.ElementResponse;
import com.seassoon.bizflow.core.model.element.Elements;
import com.seassoon.bizflow.core.model.element.Item;
import com.seassoon.bizflow.core.model.extra.Field;
import com.seassoon.bizflow.core.model.ocr.*;
import com.seassoon.bizflow.core.model.ocr.Character;
import com.seassoon.bizflow.core.util.JSONUtils;
import com.seassoon.bizflow.flow.ocr.DocOCR;
import com.seassoon.bizflow.support.BizFlowContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link Detector}接口的实现，提供四合一（盖章，签字，打勾，日期）元素检测
 * <p>实现公共的行为，并提供一个抽象方法，具体检测逻辑可以扩展这个抽象方法
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public abstract class DocElementDetector implements Detector {

    @Autowired
    private HTTPCaller httpCaller;
    @Autowired
    private BizFlowProperties properties;
    @Resource
    DocOCR docOcr;

    private static final Logger logger = LoggerFactory.getLogger(DocElementDetector.class);

    /**
     * 文档元素检测的结果缓存，这里用LRU算法，结构如下：
     * <p>
     * <pre>
     * {
     *     key: "1458638206939873282:SC-F09", # 第一层Key为recordID:材料编号
     *     value: {
     *          # 这里value为具体材料分类的元素检测结果
     *          1447849947778166786.jpg": {
     *              "checkbox": [],
     *              "hw": [],
     *              "stamp": [],
     *              "idcard": [],
     *              "date": [],
     *          }
     *     }
     * }
     * </pre>
     */
    private final LRUCache<String, Map<String, Elements>> ELEMENTS_CACHE = new LRUCache<>(20);

    @SuppressWarnings("unchecked")
    @Override
    public Field detect(Map<String, Object> params) {
        // 获取参数
        Image image = (Image) params.get("image");
        String formTypeId = (String) params.get("formTypeId");
        List<Image> images = (List<Image>) params.get("images");

        // 获取文档元素检测结果
        Elements elements = getElements(formTypeId, image.getImageId(), images);
        params.put("elements", elements);
        return detectField(params);
    }

    public abstract Field detectField(Map<String, Object> params);

    /**
     * 根据材料编号和图片ID获取元素检测结果
     * @param label 材料编号
     * @param imageId 图片ID
     * @param images 当前分类的所有图片
     * @return 图片元素检测结果
     */
    public Elements getElements(String label, String imageId, List<Image> images) {
        String recordId = BizFlowContextHolder.getInput().getRecordId();
        String key = recordId + ":" + label;

        // 缓存中是否已有结果
        Map<String, Elements> elements = ELEMENTS_CACHE.get(key);
        if (elements == null) {
            elements = callAPI(images);
            ELEMENTS_CACHE.put(key, elements);
        }

        if (elements == null) {
            logger.error("获取文档元素出错，结果为null");
            return null;
        }

        // 获取指定文档ID的结果
        String imageKey = imageId + ".jpg";
        return elements.get(imageKey);
    }

    public Map<String, Elements> callAPI(List<Image> images) {
        String strURL = properties.getIntegration().get(BizFlowProperties.Service.DOC_ELEMENT);
        Map<String, String> params = images.stream().collect(Collectors.toMap(image -> image.getImageId() + ".jpg", image -> image.getCorrected().getUrl()));
        String strQueryParam = JSONUtils.writeValueAsString(params);
        String strUrl = strURL + "?" + strQueryParam;
        ElementResponse response = httpCaller.post(strUrl, new HashMap<>(), ElementResponse.class);
        if (!response.getStatus().equals("ok")) {
            logger.error("调用元素检测接口返回失败：{}", response);
        }

        Map<String, Elements> elements = response.getElements();
        if (elements == null) {
            logger.error("调用元素检测接口返回为空，未找到elements属性");
        }
        return elements;
    }

    /**
     * 判定目标A在B区域中的比例<p>
     * box : [y_start, x_start, y_end, x_end]
     *
     * @param a 目标区域
     * @param b 参考区域
     * @return 比例值
     */
    protected Double getIOT(List<List<Integer>> a, List<List<Integer>> b) {
        Integer xA = Math.max(a.get(0).get(1), b.get(0).get(1));
        Integer yA = Math.max(a.get(0).get(0), b.get(0).get(0));
        Integer xB = Math.min(a.get(1).get(1), b.get(1).get(1));
        Integer yB = Math.min(a.get(1).get(0), b.get(1).get(0));

        int interArea = Math.max(0, xB - xA + 1) * Math.max(0, yB - yA + 1);
        int aArea = (a.get(1).get(1) - a.get(0).get(1) + 1) * (a.get(1).get(0) - a.get(0).get(0) + 1);
        return (double) interArea / aArea;
    }

    /**
     * 合并检测元素中的block属性
     *
     * @param items 未合并的元素
     * @return 合并后坐标
     */
    protected List<List<Integer>> merge(List<Item> items) {
        int xMin = 10000, yMin = 10000, xMax = 0, yMax = 0;
        if (items.size() == 1) {
            return items.get(0).getPosition();
        }

        for (Item item : items) {
            List<List<Integer>> position = item.getPosition();
            xMin = Math.min(position.get(0).get(0), xMin);
            yMin = Math.min(position.get(0).get(1), yMin);
            xMax = Math.max(position.get(1).get(0), xMax);
            yMax = Math.max(position.get(1).get(1), yMax);
        }
        return Arrays.asList(Arrays.asList(xMin, yMin), Arrays.asList(xMax, yMax));
    }

    /**
     * 元素检测结果中 获取符合的坐标
     *
     * @param items
     * @param detectArea
     * @param threshold
     * @return
     */
    protected List<List<Integer>> detectLocation(List<Item> items, List<List<Integer>> detectArea, Double threshold) {
        items = items.stream().filter(item -> {
            List<List<Integer>> position = item.getPosition();
            Double overlapArea = getIOT(position, detectArea);
            return overlapArea > threshold;
        }).collect(Collectors.toList());
        if (CollectionUtil.isNotEmpty(items)) {
            List<List<Integer>> position = merge(items);
            return Arrays.asList(
                    Arrays.asList(position.get(0).get(1), position.get(0).get(0)),
                    Arrays.asList(position.get(1).get(1), position.get(1).get(0)));
        } else {
            return null;
        }
    }

    /**
     * 根据裁剪信息 获取裁剪后ocr
     *
     * @param origin 原图ocr
     * @param path   文件路径 名称包含裁剪信息
     * @return
     */
    protected OcrResult getBlockOcr(OcrResult origin, String path) {
        // 把文件名里的坐标截出来（取坐标部分已测）
        String shotName = path.substring(path.lastIndexOf(File.separator) + 1, path.lastIndexOf("."));
        String[] locations = shotName.replace("_", "-").split("-");
        Integer yStart = Integer.valueOf(locations[0]), xStart = Integer.valueOf(locations[1]),
                yEnd = Integer.valueOf(locations[2]), xEnd = Integer.valueOf(locations[3]);
        List<List<Integer>> position = Arrays.asList(
                Arrays.asList(yStart, xStart), Arrays.asList(yEnd, xEnd));
        List<Block> blockList = origin.getBlocks().stream().filter(line -> {
            List<Position> positionList = line.getPosition();
            List<List<Integer>> cornerPoint = Arrays.asList(
                    Arrays.asList(positionList.get(0).getY(), positionList.get(0).getX()),
                    Arrays.asList(positionList.get(2).getY(), positionList.get(2).getX()));
            return getArea(cornerPoint, position);
        }).map(line -> {
            Block block = new Block();
            block.setScore(1.0);
            List<Character> characters = new ArrayList<>();
            StringBuffer text = new StringBuffer();
            line.getCharacters().forEach(ch -> {
                int chStartX = ch.getPosition().get(0).getX();
                int chEndx = ch.getPosition().get(2).getX();
                if (Math.min(chEndx, xEnd) - Math.max(chStartX, xStart) > 0.4 * (chEndx - chStartX)) {
                    characters.add(ch);
                    // block的score取列表中的最小值
                    block.setScore(Math.min(block.getScore(), ch.getScore()));
                    text.append(ch.getText());
                }
            });
            block.setCharacters(characters);
            block.setText(text.toString());
            if (!CollectionUtils.isEmpty(characters)) {
                block.setPosition(Arrays.asList(
                        characters.get(0).getPosition().get(0),
                        characters.get(characters.size() - 1).getPosition().get(1),
                        characters.get(characters.size() - 1).getPosition().get(2),
                        characters.get(0).getPosition().get(3)));
            }
            return block;
        }).collect(Collectors.toList());

        OcrResult ocrResult = new OcrResult();
        ocrResult.setBlocks(blockList);
        // linemerge
        OcrOutput ocrOutput = docOcr.mergeLine(ocrResult, -1);
        // sort
        ocrOutput.getOcrResult().getBlocks().sort(Comparator.comparing(block -> block.getPosition().get(3).getY()));
        // fixme 过滤掉无效字符 需要测一下看效果是不是一样的
        ocrOutput.getOcrResult().getBlocks().forEach( block -> {
            block.setText(block.getText().replaceAll("[【】,。\\[\\]：;；\\-_、，() （）\\s]", ""));
            block.setCharacters(block.getCharacters().stream().filter( ch ->
                !ch.getText().matches("^[【】,。\\[\\]：;；\\-_、，() （）\\s]+$")
            ).collect(Collectors.toList()));
        });
        return ocrOutput.getOcrResult();
    }


    /**
     * 对两个区域做了某个判定 虽然不清楚原理是啥
     *
     * @param cornerPoint
     * @param position
     * @return
     */
    private boolean getArea(List<List<Integer>> cornerPoint, List<List<Integer>> position) {
        // 交叉高度
        int deltaH = Math.min(position.get(1).get(0), cornerPoint.get(1).get(0)) - Math.max(position.get(0).get(0), cornerPoint.get(0).get(0));
        // 交叉宽度
        int deltaW = Math.min(position.get(1).get(1), cornerPoint.get(1).get(1)) - Math.max(position.get(0).get(1), cornerPoint.get(0).get(1));
        return deltaH > 0.5 * (cornerPoint.get(1).get(0) - cornerPoint.get(0).get(0)) && deltaW > 0;
    }

}
