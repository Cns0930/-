package com.seassoon.bizflow.flow.extract.detect;

import com.google.common.base.Joiner;
import com.seassoon.bizflow.core.model.config.CheckpointConfig;
import com.seassoon.bizflow.core.model.element.Elements;
import com.seassoon.bizflow.core.model.element.Item;
import com.seassoon.bizflow.core.model.extra.Field;
import com.seassoon.bizflow.core.model.ocr.Block;
import com.seassoon.bizflow.core.model.ocr.Character;
import com.seassoon.bizflow.core.model.ocr.OcrOutput;
import com.seassoon.bizflow.core.model.ocr.OcrResult;
import com.seassoon.bizflow.core.model.ocr.Position;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 是否勾选/勾选内容
 * @author chimney
 * @date 2021/12/1
 */
@Component
public class CheckboxDetector extends DocElementDetector{

    private final String[] rightTags = new String[]{"口", "（"};
    private final String[] leftTags = new String[]{"口", ":", "：", "）"};

    @Override
    public Field detectField(Map<String, Object> params) {
        CheckpointConfig.ExtractPoint.SignSealId signSealId =
                CheckpointConfig.ExtractPoint.SignSealId.getByValue((String) params.get("signSealId"));
        if (signSealId != null) {
            switch (signSealId) {
                case CHECKBOX:
                    return detectCheckbox(params);
                case CHECK_LEFT:
                    return detectCheckboxContent(params, "left");
                case CHECK_RIGHT:
                    return detectCheckboxContent(params, "right");
            }
        }
        return Field.of(null, null, 0);
    }

    /**
     * 是否勾选
     * @param params
     * @return
     */
    private Field detectCheckbox(Map<String, Object> params) {
        Elements elements = (Elements) params.get("elements");
        if (elements != null) {
            if (!CollectionUtils.isEmpty(elements.getCheckbox())) {
                // TODO 待我研究一下这个 location 是哪里来的
                return Field.of("已勾选", null, 1.0);
            }
        }
        return Field.of("未勾选", null, 1.0);
    }

    private Field detectCheckboxContent(Map<String, Object> params, String relativePosition) {
        // checkbox_value_match
        Elements elements = (Elements) params.get("elements");
        if (elements != null) {
            List<Item> checkboxTargets = elements.getCheckbox();
            if (!CollectionUtils.isEmpty(checkboxTargets)) {
                List<Block> result = checkboxTargets.stream().map(check -> {
                    Block block = new Block();
                    // 原文分了obj_score和text_score 但后面似乎没有用到
                    block.setScore(check.getScore());
                    block.setPosition(Arrays.asList(
                            Position.of(check.getPosition().get(0).get(0), check.getPosition().get(0).get(1)),
                            Position.of(check.getPosition().get(1).get(0), check.getPosition().get(0).get(1)),
                            Position.of(check.getPosition().get(1).get(0), check.getPosition().get(1).get(1)),
                            Position.of(check.getPosition().get(0).get(0), check.getPosition().get(1).get(0))
                    ));
                    return block;
                }).collect(Collectors.toList());
                // get_checkbox_value
                // 获取ocr识别信息  原为调接口 现换成裁剪方式
                OcrResult ocrResult = getBlockOcr(((OcrOutput) params.get("ocr")).getOcrResultWithoutLineMerge(), (String) params.get("path"));
                List<String> checkboxTextList = new ArrayList<>();
                if (CollectionUtils.isEmpty(result)) {
                    // 判断是否为打印勾选✔，or识别为V   口是框
                    // 确定是否为 框+勾选形式
                    // 先count一下有没有框
                    boolean hasBox = ocrResult.getBlocks().stream().anyMatch(block -> block.getText().charAt(0) == '口');
                    // 默认勾选框位于最最左边
                    Pattern contentPattern = Pattern.compile("V(.*?)口");
                    checkboxTextList = ocrResult.getBlocks().stream().filter(
                            block -> block.getText().contains("V") || (hasBox && block.getText().charAt(0) != '口'))
                            .map(
                                    block -> {
                                        // 正则部分已测
                                        Matcher matcher = contentPattern.matcher(block.getText());
                                        return matcher.find() ? matcher.group(1) : block.getText();
                                    }
                            ).collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(checkboxTextList)) {
                        // 返回勾选内容
                        Field.of(Joiner.on("&").join(checkboxTextList), null, 1.0);
                    }
                } else {
                    /*
                       通过checkbox信息定位勾选框所对应文本字符的位置索引,并增加标记 下面整个一段都是为了插标记
                     */
                    // find_index
                    boolean[] resultFlag = new boolean[result.size()];
                    // result下标 -> [行下标, ch下标]
                    int[][] resultIndex = new int[result.size()][2];
                    int count =  0;
                    for (int i = 0; i < ocrResult.getBlocks().size(); i++) {
                        // 对ocr结果遍历每行，每行内遍历检测到的勾选目标，根据位置判定是否属于该行
                        Block line = ocrResult.getBlocks().get(i);
                        int centerLine = (line.getPosition().get(0).getY() + line.getPosition().get(2).getY()) / 2;
                        List<int[]> matchList = new ArrayList<>();
                        for (int idx = 0; idx < result.size(); idx++) {
                            // 如果这个 idx 已经被处理过 就不用再看了
                            if(resultFlag[idx]){
                                continue;
                            }

                            Block checkBox = result.get(idx);
                            int centerCheckbox = (checkBox.getPosition().get(0).getY() + checkBox.getPosition().get(2).getY()) / 2;
                            if (Math.abs(centerLine - centerCheckbox) > 15) {
                                continue;
                            }
                            // 此时标记这个 idx 已经被找到了
                            resultFlag[idx] = true;
                            count++;
                            // 寻找勾选匹配的字符索引char_idx
                            int dist = 9999;
                            int charIdx = 0;
                            for (int j = 0; j < line.getCharacters().size(); j++) {
                                Character character = line.getCharacters().get(j);
                                int tmpDist = Math.abs(character.getPosition().get(0).getX() + character.getPosition().get(1).getX()
                                        - checkBox.getPosition().get(0).getX() - checkBox.getPosition().get(1).getX());
                                if(tmpDist < dist){
                                    dist = tmpDist;
                                    charIdx = j;
                                }
                            }
                            matchList.add(new int[]{idx, charIdx});
                            resultIndex[idx][0] = i;
                        }
                        // 对同一行中收集到的match_list按位置排序，把“口”作为标记插入每个勾选对应的位置，用于切分勾选内容，并更新插入标记字符后对应勾选的位置序号
                        matchList.sort(Comparator.comparingInt(i1 -> i1[1]));
                        StringBuilder stringBuffer = new StringBuilder(line.getText());
                        for(int step = 0; step < matchList.size() ; step++){
                            int charIdx = matchList.get(step)[1] + step;
                            resultIndex[matchList.get(step)[0]][1] = charIdx;
                            stringBuffer.insert(charIdx, '口');
                        }
                        line.setText(stringBuffer.toString());
                        // 若该行结束后，check_list 已全为True，即所有的勾选目标都已完成匹配，则跳出，返回result，若未完成全部的勾选匹配，则继续下一行
                        if(count >= result.size()){
                            break;
                        }
                    }

                    // find_text_by_char
                    // 用 resultIndex 和 orcResult 计算
                    for (int idx = 0; idx < resultIndex.length; idx++) {
                        // idx
                        String lineContent = ocrResult.getBlocks().get(resultIndex[idx][0]).getText();
                        if(!resultFlag[idx]){
                            // fixme 沿用原先逻辑 似乎直接跳过更合理一点 但先保持不变
                            checkboxTextList.add("");
                            continue;
                        }
                        checkboxTextList.add(findTextByChar(resultIndex[idx][1], lineContent, relativePosition));
                    }
                    Field.of(Joiner.on("&").join(checkboxTextList), null, 1.0);
                }

            }

        }
        return Field.of("未勾选", null, 1.0);
    }

    private String findTextByChar(int charIndex, String lineContent, String relativePosition){
        // 局部已测
        lineContent = lineContent.replace("(","（").replace(")", "）");
        if("right".equals(relativePosition)){
            if(charIndex > lineContent.length() - 1){
                return lineContent;
            }
            lineContent = lineContent.substring(charIndex+1);
            // 找到第一个tag字符并且截出来，如果没有tag就返回全部
            int minTagIndex = lineContent.length();
            for(String tag : rightTags){
                int index = lineContent.indexOf(tag);
                if(index > -1 && index < minTagIndex){
                    minTagIndex = index;
                }
            }
            if(minTagIndex < lineContent.length()){
                return lineContent.substring(0, minTagIndex);
            }else {
                // 在最右侧匹配不到tag时
                return lineContent;
            }
        }else if("left".equals(relativePosition)){
            if(charIndex < 1){
                return lineContent;
            }
            lineContent = lineContent.substring(0, charIndex);
            // 找到最后一个tag字符并截右边的
            int maxTagIndex = 0;
            for(String tag: leftTags){
                int index = lineContent.lastIndexOf(tag);
                if(index > -1 && index > maxTagIndex){
                    maxTagIndex = index;
                }
            }
            if(maxTagIndex > 0){
                return lineContent.substring(maxTagIndex + 1);
            }else
                return lineContent;
        }
        return lineContent;
    }

}
