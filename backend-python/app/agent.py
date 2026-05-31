from __future__ import annotations

from typing import Any, Literal, TypedDict

from langgraph.graph import END, StateGraph
from sqlalchemy.orm import Session

from app.llm import LlmClient
from app.models import ChatMessage, ChatSession, RiskRecord, WorkflowRecord
from app.prompts import INTENT_SYSTEM, intent_user, reply_system, reply_user
from app.retrieval import HybridRetrievalPipeline
from app.schemas import ClassificationResult, RagReference
from app.services import EmailService, ExcelService, MemoryService, RiskRuleService
from app.utils import safe_json

RISK_ORDER = {"LOW": 0, "MEDIUM": 1, "HIGH": 2}


class ReActAgentState(TypedDict, total=False):
    db: Session
    user_id: int
    session_id: int | None
    message: str

    session: ChatSession
    references: list[RagReference]
    rag_context: str
    short_memory: str
    long_memory: str
    classification: ClassificationResult
    rule_risk_level: str
    rule_risk_type: str
    final_intent: str
    final_risk: str
    risk_type: str
    reply: str

    workflow_record: WorkflowRecord
    risk_record: RiskRecord
    email_sent: bool | None
    excel_written: bool
    ai_message_saved: bool
    response: dict[str, Any]

    next_action: str
    thought: str
    observation: str
    actions: list[str]
    react_trace: list[dict[str, str]]


class LangGraphReActAgent:
    """ReAct-style LangGraph agent for the mental companion workflow.

    The graph follows a Thought -> Action -> Observation loop. The reason node
    chooses the next tool-like action from the current state; the act node runs
    that action; the observe node records the result and loops back.
    """

    def __init__(self, retriever: HybridRetrievalPipeline, llm_client: LlmClient) -> None:
        self.retriever = retriever
        self.llm_client = llm_client
        self.risk_rules = RiskRuleService()
        self.memory = MemoryService()
        self.excel = ExcelService()
        self.email = EmailService()
        self.graph = self._build_graph()

    def process_message(self, db: Session, user_id: int, session_id: int | None, message: str) -> dict[str, Any]:
        state: ReActAgentState = {
            "db": db,
            "user_id": user_id,
            "session_id": session_id,
            "message": message,
            "actions": [],
            "react_trace": [],
            "excel_written": False,
            "email_sent": None,
            "ai_message_saved": False,
        }
        result = self.graph.invoke(state)
        return result["response"]

    def _build_graph(self):
        graph = StateGraph(ReActAgentState)
        graph.add_node("reason", self._reason)
        graph.add_node("act", self._act)
        graph.add_node("observe", self._observe)
        graph.set_entry_point("reason")
        graph.add_conditional_edges("reason", self._route_after_reason, {"act": "act", "end": END})
        graph.add_edge("act", "observe")
        graph.add_edge("observe", "reason")
        return graph.compile()

    def _reason(self, state: ReActAgentState) -> dict[str, Any]:
        next_action, thought = self._choose_next_action(state)
        trace = self._trace(state)
        trace.append({"thought": thought, "action": next_action})
        return {"next_action": next_action, "thought": thought, "react_trace": trace}

    def _route_after_reason(self, state: ReActAgentState) -> Literal["act", "end"]:
        return "end" if state.get("next_action") == "finish" else "act"

    def _act(self, state: ReActAgentState) -> dict[str, Any]:
        action = state["next_action"]
        handler = getattr(self, f"_tool_{action}", None)
        if handler is None:
            raise ValueError(f"Unknown ReAct action: {action}")
        update = handler(state)
        actions = self._actions(state)
        actions.append(self._action_label(action))
        update["actions"] = actions
        return update

    def _observe(self, state: ReActAgentState) -> dict[str, Any]:
        observation = state.get("observation", "")
        trace = self._trace(state)
        if trace:
            trace[-1]["observation"] = observation
        return {"react_trace": trace}

    def _choose_next_action(self, state: ReActAgentState) -> tuple[str, str]:
        if "response" in state:
            return "finish", "最终响应已经构造完成，结束图执行。"
        if "session" not in state:
            return "save_user_message", "先保存用户消息，建立本轮会话上下文。"
        if "references" not in state:
            return "knowledge_search", "所有输入必须先检索知识库，获得 RAG 参考片段。"
        if "short_memory" not in state or "long_memory" not in state:
            return "load_memory", "加载 Redis 短期记忆和 MySQL 长期记忆，增强多轮上下文。"
        if "classification" not in state:
            return "classify_risk", "结合 RAG 上下文进行意图识别，并合并规则风险判断。"
        if "reply" not in state:
            return "generate_reply", "根据意图和风险等级选择安全的回复策略。"
        if not state.get("ai_message_saved"):
            return "save_ai_and_memory", "保存 AI 回复并更新短期/长期记忆。"
        if state["final_intent"] == "CHAT":
            return "finish_response", "闲聊只返回自然回复，不写工作流记录。"
        if "workflow_record" not in state:
            return "save_workflow_record", "非闲聊需要写入 workflow_record。"
        if state["final_intent"] == "HIGH_RISK" and "risk_record" not in state:
            return "save_risk_record", "高风险会话需要额外写入 risk_record。"
        if state["final_intent"] == "HIGH_RISK" and state.get("email_sent") is None:
            return "send_email_alert", "高风险会话需要发送邮件预警。"
        if not state.get("excel_written"):
            return "write_excel", "非闲聊记录需要追加写入 Excel。"
        return "finish_response", "所有必要动作已完成，返回最终响应。"

    def _tool_save_user_message(self, state: ReActAgentState) -> dict[str, Any]:
        db = state["db"]
        session = self._ensure_session(db, state["user_id"], state.get("session_id"), state["message"])
        self._save_message(db, session.id, "USER", state["message"], None, None)
        return {"session": session, "observation": f"已保存用户消息，会话ID={session.id}。"}

    def _tool_knowledge_search(self, state: ReActAgentState) -> dict[str, Any]:
        references = self.retriever.search(state["db"], state["message"])
        rag_context = self._rag_context(references)
        return {
            "references": references,
            "rag_context": rag_context,
            "observation": f"完成 Qdrant 向量召回、BM25 关键词召回、RRF 融合和 Cross-Encoder 重排，命中 {len(references)} 个片段。",
        }

    def _tool_load_memory(self, state: ReActAgentState) -> dict[str, Any]:
        session = state["session"]
        short_memory = self.memory.recent_context(state["user_id"], session.id)
        long_memory = self.memory.long_context(state["db"], state["user_id"])
        return {
            "short_memory": short_memory,
            "long_memory": long_memory,
            "observation": "已加载最近 10 轮短期记忆和长期记忆摘要。",
        }

    def _tool_classify_risk(self, state: ReActAgentState) -> dict[str, Any]:
        classification = self._classify(state["message"], state["rag_context"])
        rule_level, rule_type, keyword = self.risk_rules.detect(state["message"])
        final_risk = self._max_risk(classification.riskLevel, rule_level)
        final_intent = classification.intent
        risk_type = rule_type if rule_level != "LOW" else classification.riskType or "none"
        if final_intent == "HIGH_RISK" or final_risk == "HIGH":
            final_intent = "HIGH_RISK"
            final_risk = "HIGH"
        observation = f"LLM意图={classification.intent}，LLM风险={classification.riskLevel}，规则风险={rule_level}。"
        if keyword:
            observation += f" 规则命中关键词：{keyword}。"
        return {
            "classification": classification,
            "rule_risk_level": rule_level,
            "rule_risk_type": rule_type,
            "final_intent": final_intent,
            "final_risk": final_risk,
            "risk_type": risk_type,
            "observation": observation,
        }

    def _tool_generate_reply(self, state: ReActAgentState) -> dict[str, Any]:
        reply = self._reply(
            state["final_intent"],
            state["message"],
            state["rag_context"],
            state["short_memory"],
            state["long_memory"],
        )
        return {"reply": reply, "observation": f"已生成 {state['final_intent']} 类型回复。"}

    def _tool_save_ai_and_memory(self, state: ReActAgentState) -> dict[str, Any]:
        db = state["db"]
        session = state["session"]
        self._save_message(db, session.id, "ASSISTANT", state["reply"], state["final_intent"], state["final_risk"])
        self.memory.append_turn(state["user_id"], session.id, state["message"], state["reply"])
        self.memory.save_long_memory(
            db,
            state["user_id"],
            session.id,
            state["message"],
            state["final_intent"],
            state["final_risk"],
        )
        return {"ai_message_saved": True, "observation": "已保存 AI 回复并更新记忆。"}

    def _tool_save_workflow_record(self, state: ReActAgentState) -> dict[str, Any]:
        db = state["db"]
        session = state["session"]
        record = WorkflowRecord(
            user_id=state["user_id"],
            session_id=session.id,
            user_message=state["message"],
            intent=state["final_intent"],
            risk_type=state["risk_type"],
            risk_level=state["final_risk"],
            rag_hit=bool(state.get("references")),
            rag_references=safe_json([reference.model_dump() for reference in state.get("references", [])]),
            ai_reply=state["reply"],
            excel_exported=False,
            email_sent=False,
        )
        db.add(record)
        db.flush()
        return {"workflow_record": record, "observation": f"已写入 workflow_record，记录ID={record.id}。"}

    def _tool_save_risk_record(self, state: ReActAgentState) -> dict[str, Any]:
        db = state["db"]
        session = state["session"]
        risk_record = RiskRecord(
            user_id=state["user_id"],
            session_id=session.id,
            user_message=state["message"],
            risk_type=state["risk_type"],
            risk_level=state["final_risk"],
            ai_reply=state["reply"],
            handled=False,
        )
        db.add(risk_record)
        db.flush()
        return {"risk_record": risk_record, "observation": f"已写入 risk_record，记录ID={risk_record.id}。"}

    def _tool_write_excel(self, state: ReActAgentState) -> dict[str, Any]:
        path = self.excel.append_workflow_record(state["db"], state["workflow_record"])
        return {"excel_written": True, "observation": f"已追加写入 Excel：{path}。"}

    def _tool_send_email_alert(self, state: ReActAgentState) -> dict[str, Any]:
        sent = self.email.send_risk_alert(state["db"], state["risk_record"])
        state["workflow_record"].email_sent = sent
        return {"email_sent": sent, "observation": f"邮件预警发送结果：{sent}。"}

    def _tool_finish_response(self, state: ReActAgentState) -> dict[str, Any]:
        state["db"].commit()
        references = state.get("references", [])
        response = {
            "sessionId": state["session"].id,
            "reply": state["reply"],
            "intent": state["final_intent"],
            "riskLevel": state["final_risk"],
            "riskType": state["risk_type"],
            "ragHit": bool(references),
            "references": [reference.model_dump() for reference in references],
            "actions": self._actions(state),
            "reactTrace": self._trace(state),
        }
        return {"response": response, "observation": "ReAct Agent 完成全部步骤。"}

    def _ensure_session(self, db: Session, user_id: int, session_id: int | None, message: str) -> ChatSession:
        if session_id:
            existing = db.get(ChatSession, session_id)
            if existing and existing.user_id == user_id:
                return existing
        session = ChatSession(user_id=user_id, title=message[:18] or "新会话")
        db.add(session)
        db.flush()
        return session

    def _save_message(
        self,
        db: Session,
        session_id: int,
        role: str,
        content: str,
        intent: str | None,
        risk: str | None,
    ) -> None:
        db.add(ChatMessage(session_id=session_id, role=role, content=content, intent=intent, risk_level=risk))

    def _classify(self, message: str, rag_context: str) -> ClassificationResult:
        try:
            data = self.llm_client.chat_json(INTENT_SYSTEM, intent_user(message, rag_context))
            return ClassificationResult(**data)
        except Exception:
            rule_level, rule_type, _ = self.risk_rules.detect(message)
            if rule_level == "HIGH":
                return ClassificationResult(intent="HIGH_RISK", riskLevel="HIGH", riskType=rule_type, reason="规则命中高风险关键词")
            if any(word in message for word in ["什么", "如何", "方法", "概念", "为什么", "哪些"]):
                return ClassificationResult(intent="KNOWLEDGE", riskLevel=rule_level, riskType=rule_type, reason="启发式识别为知识问答")
            if any(word in message for word in ["压力", "焦虑", "难受", "失眠", "关系", "情绪", "累"]):
                return ClassificationResult(intent="CONSULT", riskLevel=rule_level, riskType=rule_type, reason="启发式识别为咨询")
            return ClassificationResult(intent="CHAT", riskLevel=rule_level, riskType=rule_type, reason="启发式识别为闲聊")

    def _reply(self, intent: str, message: str, rag_context: str, short_memory: str, long_memory: str) -> str:
        try:
            return self.llm_client.chat(reply_system(intent), reply_user(message, rag_context, short_memory, long_memory))
        except Exception:
            return self._fallback_reply(intent)

    def _fallback_reply(self, intent: str) -> str:
        if intent == "HIGH_RISK":
            return "我听见你现在很痛苦。请立刻联系身边可信任的人陪着你，并尽快联系当地紧急服务或专业心理危机热线。先让自己远离可能造成伤害的物品，不要一个人扛着。"
        if intent == "CONSULT":
            return "听起来你最近承受了不少压力。可以先把今晚必须做的事减到最少，做几轮缓慢呼吸，把担心的事情写下来留到明天处理。如果这种状态持续，也建议联系专业支持。"
        if intent == "KNOWLEDGE":
            return "当前知识库中没有找到明确依据。一般来说，规律作息、放松训练、适度运动和记录情绪触发点，常被用于缓解压力与焦虑体验。"
        return "我在呢。无聊的时候可以先挑一件很小的事做做，比如听首歌、倒杯水，或者和我随便聊两句。"

    def _rag_context(self, references: list[RagReference]) -> str:
        if not references:
            return "当前知识库没有找到明确依据。"
        return "\n\n".join(f"[{idx}] 文档：{ref.documentName}\n{ref.content}" for idx, ref in enumerate(references, start=1))

    def _max_risk(self, left: str, right: str) -> str:
        return left if RISK_ORDER.get(left, 0) >= RISK_ORDER.get(right, 0) else right

    def _action_label(self, action: str) -> str:
        mapping = {
            "save_user_message": "REACT_SAVE_USER_MESSAGE",
            "knowledge_search": "REACT_RAG_SEARCH",
            "load_memory": "REACT_LOAD_MEMORY",
            "classify_risk": "REACT_CLASSIFY_RISK",
            "generate_reply": "REACT_GENERATE_REPLY",
            "save_ai_and_memory": "REACT_SAVE_AI_AND_MEMORY",
            "save_workflow_record": "REACT_SAVE_WORKFLOW_RECORD",
            "save_risk_record": "REACT_SAVE_RISK_RECORD",
            "write_excel": "REACT_WRITE_EXCEL",
            "send_email_alert": "REACT_SEND_EMAIL_ALERT",
            "finish_response": "REACT_FINISH",
        }
        return mapping.get(action, f"REACT_{action.upper()}")

    def _actions(self, state: ReActAgentState) -> list[str]:
        return list(state.get("actions", []))

    def _trace(self, state: ReActAgentState) -> list[dict[str, str]]:
        return list(state.get("react_trace", []))
