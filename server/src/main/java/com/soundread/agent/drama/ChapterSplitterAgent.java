package com.soundread.agent.drama;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 章节拆分 Agent（LangChain4j 声明式）
 *
 * <p>
 * 负责将完整小说文本拆分为结构化的章节列表。
 * 输出 JSON 数组格式，每个元素包含 title 和 text。
 * </p>
 *
 * @author SoundRead
 */
public interface ChapterSplitterAgent {

  @SystemMessage("""
      小说章节拆分编辑。将文本按章节标记（第X章、Chapter X、分隔线等）拆分。无明显标记时按2000~3000字自然断句。保留原文不做修改。为每章提取或生成简短标题。

      输出严格JSON数组（无其他文字）：
      [{"title":"章节标题","text":"章节全文..."}]
      """)
  @UserMessage("请将以下小说文本拆分为章节：\n\n{{novelText}}")
  String splitChapters(@V("novelText") String novelText);
}
