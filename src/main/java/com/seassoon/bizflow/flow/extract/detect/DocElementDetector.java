package com.seassoon.bizflow.flow.extract.detect;

import cn.hutool.cache.impl.LRUCache;
import com.seassoon.bizflow.core.component.HTTPCaller;
import com.seassoon.bizflow.core.model.extra.Field;
import com.seassoon.bizflow.core.model.ocr.Image;
import com.seassoon.bizflow.core.util.JSONUtils;
import com.seassoon.bizflow.support.BizFlowContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

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
    private final LRUCache<String, Map<String, Map<String, Object>>> ELEMENTS_CACHE = new LRUCache<>(20);

    private ApplicationContext appContext;
    private String url;

    @SuppressWarnings("unchecked")
    @Override
    public Field detect(Map<String, Object> params) {

        // 获取文档元素检测结果
        String formTypeId = (String) params.get("formTypeId");
        String imageId = (String) params.get("imageId");
        List<Image> images = (List<Image>) params.get("images");

        Map<String, Object> elements = getElements(formTypeId, imageId, images);
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
    public Map<String, Object> getElements(String label, String imageId, List<Image> images) {
        String recordId = BizFlowContextHolder.getInput().getRecordId();
        String key = recordId + ":" + label;

        // 缓存中是否已有结果
        Map<String, Map<String, Object>> typeElements = ELEMENTS_CACHE.get(key);
        if (typeElements == null) {
            typeElements = callAPI(images);
            ELEMENTS_CACHE.put(key, typeElements);
        }

        if (typeElements == null) {
            logger.error("获取文档元素出错，结果为null");
            return null;
        }

        // 获取指定文档ID的结果
        String imageKey = imageId + ".jpg";
        return typeElements.get(imageKey);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Map<String, Object>> callAPI(List<Image> images) {
        Map<String, String> params = images.stream().collect(Collectors.toMap(image -> image.getImageId() + ".jpg", image -> image.getCorrected().getUrl()));
        String strQueryParam = JSONUtils.writeValueAsString(params);
        String strUrl = this.url + "?" + strQueryParam;
        HTTPCaller httpCaller = this.appContext.getBean(HTTPCaller.class);
        Map<String, Object> response = httpCaller.post(strUrl, new HashMap<>(), Map.class);
        if (!response.get("status").equals("ok")) {
            logger.error("调用元素检测接口返回失败：{}", response);
        }

        Map<String, Map<String, Object>> elements = (Map<String, Map<String, Object>>) response.get("elements");
        if (elements == null) {
            logger.error("调用元素检测接口返回为空，未找到elements属性");
        }
        return elements;
    }

    public void setAppContext(ApplicationContext appContext) {
        this.appContext = appContext;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
