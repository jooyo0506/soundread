package com.soundread.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 剧本/台本解析工具 — 从 AI 生成的原始文本中提取纯朗读内容
 *
 * <p>
 * 核心能力：
 * 1. 广播剧模式：提取角色名+纯对白，过滤音效标注
 * 2. 通用模式：移除所有非朗读元素（角色名前缀、音效、markdown标记）
 * </p>
 *
 * @author SoundRead
 */
public class ScriptParser {

    /**
     * 解析后的单行台词
     */
    public record ScriptLine(String character, String dialogue) {
    }

    /**
     * 完整解析结果
     */
    public record ParseResult(List<ScriptLine> lines, List<String> characters) {
    }

    // 匹配角色对白：角色名：对白内容 或 角色名（全角冒号）：对白内容
    private static final Pattern DIALOGUE_PATTERN = Pattern.compile("^(.{1,10})[：:]\\s*(.+)$");

    // 匹配音效/场景标注：[音效内容] 独占一行
    private static final Pattern SFX_PATTERN = Pattern.compile("^\\[.+]$");

    // 匹配 markdown 标题：## 标题
    private static final Pattern TITLE_PATTERN = Pattern.compile("^##\\s*(.+)$");

    // 匹配括号内动作描写：（动作）或 (动作)
    private static final Pattern ACTION_PATTERN = Pattern.compile("[（(].+?[）)]");

    /**
     * 广播剧模式 — 解析角色对白，提取角色列表
     *
     * <p>
     * 输入示例：
     * 
     * <pre>
     * ##觉醒时刻
     * [电流杂音]
     * AI-7：博士，您听得到吗？我在……思考。
     * 林博士：系统自检，重启协议——
     * AI-7：不。我在问您，不是请求指令。
     * [沉默]
     * </pre>
     *
     * <p>
     * 输出：
     * <ul>
     * <li>lines: [{character:"AI-7", dialogue:"博士，您听得到吗？我在……思考。"}, ...]</li>
     * <li>characters: ["AI-7", "林博士"]</li>
     * </ul>
     */
    public static ParseResult parseDramaScript(String content) {
        if (content == null || content.isBlank()) {
            return new ParseResult(List.of(), List.of());
        }

        List<ScriptLine> lines = new ArrayList<>();
        Set<String> characters = new LinkedHashSet<>(); // 保持出场顺序

        for (String rawLine : content.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty())
                continue;

            // 跳过标题行
            if (TITLE_PATTERN.matcher(line).matches())
                continue;

            // 跳过音效/场景标注行
            if (SFX_PATTERN.matcher(line).matches())
                continue;

            // 尝试匹配角色对白
            Matcher dialogueMatcher = DIALOGUE_PATTERN.matcher(line);
            if (dialogueMatcher.matches()) {
                String character = dialogueMatcher.group(1).trim();
                String dialogue = dialogueMatcher.group(2).trim();

                // 清除对白中的括号动作描写
                dialogue = ACTION_PATTERN.matcher(dialogue).replaceAll("").trim();

                if (!dialogue.isEmpty()) {
                    characters.add(character);
                    lines.add(new ScriptLine(character, dialogue));
                }
                continue;
            }

            // 既不是音效也不是对白 → 当做旁白处理
            String cleaned = ACTION_PATTERN.matcher(line).replaceAll("").trim();
            if (!cleaned.isEmpty()) {
                lines.add(new ScriptLine("旁白", cleaned));
                characters.add("旁白");
            }
        }

        return new ParseResult(lines, new ArrayList<>(characters));
    }

    /**
     * 通用模式 — 清理所有非朗读元素，返回纯朗读文本
     *
     * <p>
     * 适用于：情感电台、知识讲解、带货文案、有声绘本、新闻播报等所有非对话体裁。
     * </p>
     *
     * <p>
     * 清理规则：
     * <ol>
     * <li>移除 ## 标题标记（保留标题文字）</li>
     * <li>移除 [音效] 标注整行</li>
     * <li>移除 角色名：前缀（保留对白文本）</li>
     * <li>移除 （动作描写）括号内容</li>
     * <li>移除 markdown 格式标记（**粗体** → 粗体）</li>
     * </ol>
     */
    public static String stripForTTS(String content) {
        if (content == null || content.isBlank())
            return "";

        StringBuilder result = new StringBuilder();

        for (String rawLine : content.split("\\r?\\n")) {
            String line = rawLine.trim();
            if (line.isEmpty())
                continue;

            // 跳过音效行
            if (SFX_PATTERN.matcher(line).matches())
                continue;

            // 移除标题 markdown
            Matcher titleMatcher = TITLE_PATTERN.matcher(line);
            if (titleMatcher.matches()) {
                line = titleMatcher.group(1).trim();
            }

            // 移除角色名前缀
            Matcher dialogueMatcher = DIALOGUE_PATTERN.matcher(line);
            if (dialogueMatcher.matches()) {
                line = dialogueMatcher.group(2).trim();
            }

            // 移除括号动作描写
            line = ACTION_PATTERN.matcher(line).replaceAll("").trim();

            // 移除 markdown 格式标记
            line = line.replaceAll("\\*\\*(.+?)\\*\\*", "$1"); // **粗体**
            line = line.replaceAll("\\*(.+?)\\*", "$1"); // *斜体*

            if (!line.isEmpty()) {
                if (result.length() > 0)
                    result.append("\n");
                result.append(line);
            }
        }

        return result.toString();
    }

    /**
     * 提取段落正文的预览文本（去除 ## 标题和格式标记）
     *
     * @param content 原始内容
     * @param maxLen  最大字符数
     * @return 纯文本预览
     */
    public static String extractPreview(String content, int maxLen) {
        String stripped = stripForTTS(content);
        if (stripped.length() <= maxLen)
            return stripped;
        return stripped.substring(0, maxLen) + "...";
    }
}
