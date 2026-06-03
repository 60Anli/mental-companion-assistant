<template>
  <main class="resume-page">
    <!-- 回到首页 -->
    <router-link to="/" class="back-link">
      <el-icon><Back /></el-icon> 回到首页
    </router-link>

    <!-- 左侧栏 -->
    <aside class="resume-sidebar">
      <div class="avatar-circle">安</div>
      <h1 class="name">安力</h1>
      <p class="title">后端开发工程师</p>

      <div class="sidebar-section">
        <h3>联系方式</h3>
        <div class="contact-item">
          <el-icon><Message /></el-icon>
          <span>3221765795@qq.com</span>
        </div>
        <div class="contact-item">
          <el-icon><Phone /></el-icon>
          <span>+86 你的电话</span>
        </div>
        <div class="contact-item">
          <el-icon><Location /></el-icon>
          <span>你的城市</span>
        </div>
        <div class="contact-item">
          <el-icon><Link /></el-icon>
          <span>github.com/你的ID</span>
        </div>
      </div>

      <div class="sidebar-section">
        <h3>专业技能</h3>
        <div class="skill-group">
          <span class="skill-tag">Python</span>
          <span class="skill-tag">Java</span>
          <span class="skill-tag">Spring Boot</span>
          <span class="skill-tag">FastAPI</span>
          <span class="skill-tag">MySQL</span>
          <span class="skill-tag">Redis</span>
          <span class="skill-tag">Docker</span>
          <span class="skill-tag">Linux</span>
          <span class="skill-tag">Git</span>
          <span class="skill-tag">Vue</span>
        </div>
      </div>

      <div class="sidebar-section">
        <h3>语言</h3>
        <div class="lang-item">
          <span>中文 — 母语</span>
        </div>
        <div class="lang-item">
          <span>英语 — CET-4/6</span>
        </div>
      </div>
    </aside>

    <!-- 右侧主内容 -->
    <section class="resume-main">
      <div class="resume-block">
        <h2>关于我</h2>
        <p>热爱技术的后端开发工程师，专注于 Python / Java 后端开发和 AI 应用落地。具备从零搭建项目的经验，熟悉主流后端技术栈。善于学习新技术，追求代码质量和系统稳定性。</p>
      </div>

      <div class="resume-block">
        <h2>教育经历</h2>
        <div class="timeline-item">
          <div class="timeline-dot"></div>
          <div class="timeline-content">
            <div class="timeline-header">
              <strong>你的学校名称</strong>
              <span>2021 - 2025</span>
            </div>
            <p>专业名称 · 本科/硕士 · GPA: 3.x/4.0</p>
            <p style="color: var(--warm-500); font-size: 14px;">主修课程：数据结构、操作系统、计算机网络、数据库原理等</p>
          </div>
        </div>
      </div>

      <div class="resume-block">
        <h2>项目经历</h2>
        <div class="timeline-item">
          <div class="timeline-dot"></div>
          <div class="timeline-content">
            <div class="timeline-header">
              <strong>MindCare — 心理陪伴助手</strong>
              <span>2025</span>
            </div>
            <p>基于 <b>FastAPI + LangGraph</b> 的 AI 心理陪伴平台，采用 ReAct Agent 编排对话工作流</p>
            <ul>
              <li>实现 <b>混合 RAG 检索</b>：Qdrant 向量召回 + BM25 关键词召回 + RRF 融合 + Cross-Encoder 重排序</li>
              <li>基于 <b>LangGraph StateGraph</b> 实现 reason→act→observe 多轮工具调用 Agent</li>
              <li>集成阿里云百炼 LLM API，支持意图识别、风险分级、高危邮件预警</li>
              <li>前端 Vue 3 + Element Plus，后端 FastAPI，MySQL + Redis + Qdrant 数据层</li>
              <li>全量部署至阿里云 ECS（Docker + Nginx + systemd）</li>
            </ul>
          </div>
        </div>

        <div class="timeline-item">
          <div class="timeline-dot"></div>
          <div class="timeline-content">
            <div class="timeline-header">
              <strong>你的另一个项目</strong>
              <span>2024</span>
            </div>
            <p>简要描述项目背景、你的角色和核心贡献</p>
            <ul>
              <li>技术栈：xxx</li>
              <li>解决了 xxx 问题</li>
            </ul>
          </div>
        </div>
      </div>

      <div class="resume-block">
        <h2>实习经历</h2>
        <div class="timeline-item">
          <div class="timeline-dot"></div>
          <div class="timeline-content">
            <div class="timeline-header">
              <strong>你的实习公司</strong>
              <span>2024</span>
            </div>
            <p>后端开发实习生</p>
            <ul>
              <li>参与了 xxx 项目的开发与维护</li>
              <li>优化了 xxx 模块性能</li>
            </ul>
          </div>
        </div>
      </div>

      <div class="resume-block">
        <h2>证书 &amp; 其他</h2>
        <ul>
          <li>阿里云 ACA/ACP 认证（如有）</li>
          <li>计算机等级考试</li>
          <li>全国大学生 xxx 竞赛</li>
        </ul>
      </div>
    </section>
  </main>
</template>

<script setup>
import { Message, Phone, Location, Link, Back } from '@element-plus/icons-vue'
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

.skill-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skill-tag {
  padding: 4px 12px;
  background: var(--green-50);
  color: var(--green-700);
  border-radius: 20px;
  font-size: 13px;
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
