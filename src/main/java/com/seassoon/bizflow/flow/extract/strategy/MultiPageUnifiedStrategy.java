package com.seassoon.bizflow.flow.extract.strategy;

/**
 * 多页提取策略。
 * <p>
 * 仅支持多页单份，不支持相同typeId下的多页且多份。如果表格存在多个相同的字段，优先匹配近邻字段，近邻字段由配置文件给出，用@符号表示。<br/>
 * 本模块是针对“多页、多表格、多文本”的文本信息提取，以310115-322-01 护士变更注册 事项为原型开发
 *
 * @author lw900925 (liuwei@seassoon.com)
 */
public class MultiPageUnifiedStrategy extends BasicUnifiedStrategy {
}
