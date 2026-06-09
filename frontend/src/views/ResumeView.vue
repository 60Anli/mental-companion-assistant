<template>
  <main class="resume-page">
    <!-- 回到首页 -->
    <router-link to="/" class="back-link">
      <el-icon><Back /></el-icon> 回到首页
    </router-link>

    <!-- 左侧栏 -->
    <aside class="resume-sidebar">
      <div class="avatar-circle">伟</div>
      <h1 class="name">万伟</h1>
      <p class="title">后端开发工程师 / AI 应用开发</p>

      <div class="sidebar-section">
        <h3>联系方式</h3>
        <div class="contact-item">
          <el-icon><Phone /></el-icon>
          <span>178-167-85705</span>
        </div>
        <div class="contact-item">
          <el-icon><Message /></el-icon>
          <span>17816785705@163.com</span>
        </div>
        <div class="contact-item">
          <el-icon><ChatDotSquare /></el-icon>
          <span>a3221765795</span>
        </div>
        <div class="contact-item">
          <el-icon><Calendar /></el-icon>
          <span>2001.06</span>
        </div>
      </div>

      <div class="sidebar-section">
        <h3>教育背景</h3>
        <div class="edu-item">
          <div class="edu-degree">硕士 · 人工智能</div>
          <div class="edu-school">河北工业大学</div>
          <div class="edu-year">2024.09 - 2027.06</div>
        </div>
        <div class="edu-item" style="margin-top: 12px;">
          <div class="edu-degree">本科 · 电气工程及其自动化</div>
          <div class="edu-school">湖州学院</div>
          <div class="edu-year">2019.09 - 2023.06</div>
        </div>
        <div class="edu-award">🏅 校三等奖学金</div>
      </div>

      <div class="sidebar-section">
        <h3>专业技能</h3>
        <div class="skill-cat">Agent 开发</div>
        <div class="skill-group">
          <span class="skill-tag">LangGraph</span>
          <span class="skill-tag">LangChain</span>
          <span class="skill-tag">ReAct</span>
          <span class="skill-tag">Plan-Execute</span>
        </div>
        <div class="skill-cat">RAG 开发</div>
        <div class="skill-group">
          <span class="skill-tag">混合检索</span>
          <span class="skill-tag">BM25</span>
          <span class="skill-tag">RRF 融合</span>
          <span class="skill-tag">Cross-Encoder</span>
          <span class="skill-tag">Qdrant</span>
          <span class="skill-tag">Chroma</span>
        </div>
        <div class="skill-cat">后端 & 数据库</div>
        <div class="skill-group">
          <span class="skill-tag">FastAPI</span>
          <span class="skill-tag">Python</span>
          <span class="skill-tag">MySQL</span>
          <span class="skill-tag">Redis</span>
        </div>
        <div class="skill-cat">工具 & 其他</div>
        <div class="skill-group">
          <span class="skill-tag">Docker</span>
          <span class="skill-tag">Git</span>
          <span class="skill-tag">Vue 3</span>
          <span class="skill-tag">CET-6</span>
          <span class="skill-tag">AI 编程工具</span>
        </div>
      </div>
    </aside>

    <!-- 右侧主内容 -->
    <section class="resume-main">
      <div class="resume-block">
        <h2>关于我</h2>
        <p>人工智能专业硕士在读，专注于 RAG 系统、Agent 工作流编排与多模态 AI 应用开发。具备从文档解析、向量检索到对话系统上线的完整项目经验，熟悉 FastAPI + Qdrant + LangGraph 技术栈。平时喜欢先弄清楚原理再动手实践，乐于尝试 Claude Code、Codex 等新工具提升开发效率。</p>
      </div>

      <div class="resume-block">
        <h2>项目经历</h2>

        <!-- 项目 1：私域知识库 -->
        <div class="timeline-item">
          <div class="timeline-dot"></div>
          <div class="timeline-content">
            <div class="timeline-header">
              <strong>私域知识库问答系统</strong>
              <span>2025</span>
            </div>
            <p style="color: var(--warm-500); font-size: 14px; margin-bottom: 12px;">
              面向个人/企业非结构化文档管理场景，针对知识分散、检索困难的问题，构建 RAG 知识库系统，实现文档解析、语义检索与智能问答闭环。
            </p>
            <p style="margin-bottom: 4px;"><b>技术栈：</b>Python · FastAPI · LangGraph · BM25 · RAG · Qdrant</p>
            <ul>
              <li>基于 AST 设计层级化 Markdown 切块模块，保留标题路径上下文链，对代码块/表格做原子保护，结合 LLM 代码摘要与 VLM 图像描述将非文本转为可检索语义，16 篇长文本（总计 35 万 token）实测平均 chunk 405 token，&lt; 50 token 碎片 chunk &lt; 3%</li>
              <li>基于 Qdrant 构建混合检索体系（Dense + BM25），设计"向量召回 → 关键词召回 → RRF 融合 → Cross-Encoder 重排序"三阶段 Pipeline，相比纯向量检索 Precision@5 提升 11.2%</li>
              <li>基于 LangGraph 构建 RAG 工作流，引入意图识别与 Query Rewrite 对用户查询预处理，结合检索评估与自适应重试机制，构建多阶段优化检索流程</li>
            </ul>
          </div>
        </div>

        <!-- 项目 2：MindCare -->
        <div class="timeline-item">
          <div class="timeline-dot"></div>
          <div class="timeline-content">
            <div class="timeline-header">
              <strong>MindCare — 多模态心理陪伴与风险预警助手</strong>
              <span>2025</span>
            </div>
            <p style="color: var(--warm-500); font-size: 14px; margin-bottom: 12px;">
              面向校园心理陪伴与风险预警场景，构建支持文字、图片、视频输入的多模态 AI 心理陪伴平台，实现心理知识问答、高风险识别与邮件预警。已部署上线：<a href="http://39.106.116.229" target="_blank">39.106.116.229</a>
            </p>
            <p style="margin-bottom: 4px;"><b>技术栈：</b>Python · FastAPI · LangGraph · RAG · Qdrant · QLoRA · MySQL · MCP · Vue 3</p>
            <ul>
              <li>基于 Vue 3 + FastAPI 设计多模态会话链路，支持用户在对话中输入文字、上传图片和视频，完成前端预览、文件校验、后端接收、会话关联与消息记录存储</li>
              <li>基于 PIL、FFmpeg 构建媒体预处理流程，对上传图片进行压缩、格式统一和尺寸控制，对视频抽取关键帧，并将处理后的视觉信息与用户文本封装为多模态输入，接入视觉模型理解</li>
              <li>基于 LangGraph 构建对话处理流程，将知识库检索、用户记忆加载、意图识别、风险分级、回复生成、记录写入和邮件预警等环节统一编排，提升问答链路的可维护性与可追踪性</li>
              <li>基于 Qdrant 构建心理知识库 Hybrid RAG 检索链路，完成文档上传、文本清洗、分块切片、Embedding 生成与向量入库，结合向量检索、BM25、RRF 融合和 Cross-Encoder 重排序提升回答相关性</li>
              <li>设计心理风险识别与预警闭环，结合规则检测和大模型分类判断风险等级，对高风险会话自动写入风险记录，支持邮件通知、Excel 导出和管理员后台查看</li>
            </ul>
          </div>
        </div>
      </div>

      <div class="resume-block">
        <h2>自我评价</h2>
        <p>做事踏实认真，学习新技术时喜欢先弄清楚原理，再结合项目进行实践。对后端开发、AI 应用和开源工具比较感兴趣，喜欢尝试 Trae、Codex 等 AI 编程工具来提升开发和调试效率，也习惯整理学习笔记和复盘项目问题。生活中性格随和，能够较好地配合团队沟通和任务推进。</p>
      </div>
    </section>
  </main>
</template>

<script setup>
import { Back, Calendar, ChatDotSquare, Message, Phone } from '@element-plus/icons-vue'
</script>

<style scoped>
.resume-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  background: #f5f5f5;
}

/* ---- 左侧栏 ---- */
.resume-sidebar {
  padding: 48px 32px;
  background: #ffffff;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.avatar-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--green-500), var(--green-700));
  color: #fff;
  font-size: 32px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.name {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: var(--warm-800);
}

.title {
  margin: -20px 0 0;
  font-size: 14px;
  color: var(--green-600);
  font-weight: 500;
}

.sidebar-section h3 {
  font-size: 13px;
  font-weight: 600;
  color: var(--warm-500);
  text-transform: uppercase;
  letter-spacing: 1px;
  margin: 0 0 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--warm-200);
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: var(--warm-600);
  margin-bottom: 10px;
}

.contact-item .el-icon {
  color: var(--green-500);
  font-size: 16px;
}

.skill-cat {
  font-size: 12px;
  font-weight: 600;
  color: var(--warm-500);
  margin: 12px 0 6px;
  letter-spacing: 0.5px;
}
.skill-cat:first-child {
  margin-top: 0;
}

.skill-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 2px;
}

.skill-tag {
  padding: 4px 12px;
  background: var(--green-50);
  color: var(--green-700);
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  transition: background 0.15s;
}
.skill-tag:hover {
  background: var(--green-100);
}

/* 教育背景 */
.edu-item {
  margin-bottom: 4px;
}
.edu-degree {
  font-size: 14px;
  font-weight: 600;
  color: var(--warm-700);
}
.edu-school {
  font-size: 13px;
  color: var(--warm-500);
}
.edu-year {
  font-size: 12px;
  color: var(--warm-400);
}
.edu-award {
  margin-top: 10px;
  font-size: 13px;
  color: var(--green-600);
  font-weight: 500;
}

.lang-item {
  font-size: 14px;
  color: var(--warm-600);
  margin-bottom: 6px;
}

/* ---- 右侧主内容 ---- */
.resume-main {
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  gap: 32px;
  max-width: 900px;
}

.resume-block h2 {
  margin: 0 0 20px;
  font-size: 22px;
  font-weight: 700;
  color: var(--warm-800);
  padding-bottom: 10px;
  border-bottom: 3px solid var(--green-500);
  display: inline-block;
}

.resume-block p {
  margin: 0 0 10px;
  line-height: 1.8;
  color: var(--warm-600);
  font-size: 15px;
}

.resume-block ul {
  margin: 8px 0 0;
  padding-left: 20px;
  color: var(--warm-600);
  font-size: 14px;
  line-height: 2;
}

/* 时间线 */
.timeline-item {
  display: flex;
  gap: 16px;
  margin-bottom: 24px;
  position: relative;
}

.timeline-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--green-500);
  margin-top: 6px;
  flex-shrink: 0;
  box-shadow: 0 0 0 4px var(--green-100);
}

.timeline-content {
  flex: 1;
}

.timeline-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.timeline-header strong {
  font-size: 16px;
  color: var(--warm-800);
}

.timeline-header span {
  font-size: 13px;
  color: var(--warm-400);
  font-weight: 500;
}

/* ---- 返回按钮 ---- */
.back-link {
  position: fixed;
  top: 20px;
  right: 24px;
  font-size: 14px;
  color: var(--warm-500);
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 6px;
  z-index: 10;
  transition: color 0.15s;
}

.back-link:hover {
  color: var(--green-600);
}

/* 项目中的链接 */
.resume-main a {
  color: var(--green-600);
  text-decoration: none;
  font-weight: 500;
}
.resume-main a:hover {
  text-decoration: underline;
}

/* ---- 响应式 ---- */
@media (max-width: 768px) {
  .resume-page {
    grid-template-columns: 1fr;
  }

  .resume-sidebar {
    padding: 32px 24px;
    border-right: 0;
    border-bottom: 1px solid #e8e8e8;
  }

  .resume-main {
    padding: 32px 24px;
  }
}
</style>
