# 🎓 Benefito

> RAG 기반 맞춤형 공고 추천 및 AI 챗봇 서비스

학업과 아르바이트를 병행하는 대학생들이 자신에게 맞는
대외활동, 자격증, 채용 등의 공고를 쉽고 빠르게 찾을 수 있도록 돕는 서비스입니다.

## ✨ 주요 기능

### 🤖 AI 대화형 공고 검색
사용자의 학년, 전공, 지역, 관심 분야를 기반으로 공고를 필터링하고,
RAG 기반 AI 챗봇을 통해 자연어로 원하는 공고를 검색할 수 있습니다.

### 📝 공고 자동 요약
공고의 지원 자격, 지원 내용, 마감일 등 핵심 정보를 AI가 요약하여
긴 공고를 직접 읽지 않아도 주요 내용을 빠르게 확인할 수 있습니다.

### 🔖 관심 공고 관리
관심 있는 공고를 저장하고 마이페이지에서 한 번에 확인할 수 있습니다.

---

## 🛠 Tech Stack

### Backend
- Java 17
- Spring Boot
- Spring Data JPA
- QueryDSL
- PostgreSQL
- pgvector
- Swagger

### AI
- Spring AI
- Claude API
- Vector Search
- RAG

### Infrastructure
- Docker
- GitHub Actions

---

## 🧠 RAG 검색 구조

사용자 질문
→ 조건 기반 공고 필터링
→ Embedding
→ pgvector 유사도 검색
→ 관련 공고 추출
→ LLM 답변 생성

---

## 👨‍💻 Backend

- REST API 설계 및 구현
- 공고 검색 및 필터링
- pgvector 기반 벡터 검색
- RAG 기반 AI 챗봇
- 공고 데이터 수집 및 관리 
