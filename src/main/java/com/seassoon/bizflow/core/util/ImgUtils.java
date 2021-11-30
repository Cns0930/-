package com.seassoon.bizflow.core.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.img.ImgUtil;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;

/**
 * @author lw900925 (liuwei@seassoon.com)
 */
public class ImgUtils extends ImgUtil {

    /**
     * 读取图片的宽高
     *
     * @param path 图片路径
     * @return {@link Shape}
     */
    public static Shape getShape(String path) {
        BufferedImage image = read(path);
        return Shape.of(image.getWidth(), image.getHeight());

    }

    /**
     * 计算一张图片的相对坐标
     *
     * @param path  图片路径
     * @param ratio 计算系数
     * @return 计算后的坐标
     */
    public static List<List<Integer>> calcLocation(String path, List<List<Double>> ratio) {
        // 读取图片的宽高
        Shape shape = getShape(path);
        if (CollectionUtil.isNotEmpty(ratio)) {
            Integer a = Double.valueOf(ratio.get(0).get(0) * shape.getHeight()).intValue();
            Integer b = Double.valueOf(ratio.get(0).get(1) * shape.getWidth()).intValue();
            Integer c = Double.valueOf(ratio.get(1).get(0) * shape.getHeight()).intValue();
            Integer d = Double.valueOf(ratio.get(1).get(1) * shape.getWidth()).intValue();

            List<Integer> lt = Arrays.asList(a, b);
            List<Integer> rb = Arrays.asList(c, d);
            return Arrays.asList(lt, rb);
        } else {
            return Arrays.asList(Arrays.asList(1, 1), Arrays.asList(shape.getHeight() - 1, shape.getWidth() - 1));
        }
    }

    public static class Shape {
        private Integer width;
        private Integer height;

        public Shape() {
        }

        public Shape(Integer width, Integer height) {
            this.width = width;
            this.height = height;
        }

        public Integer getWidth() {
            return width;
        }

        public void setWidth(Integer width) {
            this.width = width;
        }

        public Integer getHeight() {
            return height;
        }

        public void setHeight(Integer height) {
            this.height = height;
        }

        // static methods
        public static Shape of(Integer width, Integer height) {
            return new Shape(width, height);
        }
    }
}
