package com.seassoon.bizflow.flow.extract.detect;

import cn.hutool.cache.impl.LRUCache;
import com.seassoon.bizflow.core.component.HTTPCaller;
import com.seassoon.bizflow.core.model.element.ElementResponse;
import com.seassoon.bizflow.core.model.element.Elements;
import com.seassoon.bizflow.core.model.extra.Field;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.util.JSONUtils;
import com.seassoon.bizflow.support.BizFlowContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link Detector}接口的实现，提供四合一（盖章，签字，打勾，日期）元素检测
 * <p>实现公共的行为，并提供一个抽象方法，具体检测逻辑可以扩展这个抽象方法
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public abstract class DocElementDetector implements Detector {

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

    private ApplicationContext appContext;
    private String url;

    @SuppressWarnings("unchecked")
    @Override
    public Field detect(Map<String, Object> params) {

        // 获取文档元素检测结果
        String formTypeId = (String) params.get("formTypeId");
        String imageId = (String) params.get("imageId");
        List<Image> images = (List<Image>) params.get("images");

        Elements elements = getElements(formTypeId, imageId, images);
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
        Map<String, String> params = images.stream().collect(Collectors.toMap(image -> image.getImageId() + ".jpg", image -> image.getCorrected().getUrl()));
        String strQueryParam = JSONUtils.writeValueAsString(params);
        String strUrl = this.url + "?" + strQueryParam;
        HTTPCaller httpCaller = this.appContext.getBean(HTTPCaller.class);
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
     * @param elements 未合并的元素
     * @return 合并后坐标
     */
    @SuppressWarnings("unchecked")
    protected List<List<Integer>> merge(List<Map<String, Object>> elements) {
        int xMin = 10000, yMin = 10000, xMax = 0, yMax = 0;
        if (elements.size() == 1) {
            return (List<List<Integer>>) elements.get(0).get("position");
        }

        for (Map<String, Object> element : elements) {
            List<List<Integer>> position = (List<List<Integer>>) element.get("position");
            xMin = Math.min(position.get(0).get(0), xMin);
            yMin = Math.min(position.get(0).get(1), yMin);
            xMax = Math.max(position.get(1).get(0), xMax);
            yMax = Math.max(position.get(1).get(1), yMax);
        }
        return Arrays.asList(Arrays.asList(xMin, yMin), Arrays.asList(xMax, yMax));
    }

    public void setAppContext(ApplicationContext appContext) {
        this.appContext = appContext;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
