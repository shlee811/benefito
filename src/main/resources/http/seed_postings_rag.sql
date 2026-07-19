-- RAG / pgvector 검색 테스트용 공고 시드 데이터 15건
-- 기존 seed_postings.sql과 별도로 실행 가능 (기존 데이터는 건드리지 않음).
-- 각 공고는 임베딩 검색 시 서로 명확히 구분되도록 산업/키워드를 최대한 다르게 구성했다.

WITH data (title, company, content, category_code, region_code, end_date,
           enterprise_type, employment_type, salary, qualification, homepage_url,
           created_at, updated_at) AS (
    VALUES
    ('AI 컴퓨터비전 백엔드 엔지니어 채용', '비전테크', '자율주행 및 영상인식 딥러닝 모델을 서빙하는 백엔드 시스템을 개발합니다. PyTorch 기반 모델을 GPU 클러스터 환경에서 운영하며 실시간 추론 파이프라인을 구축합니다.', 'JOB', 'SEOUL', DATE '2026-08-20', '스타트업', '정규직', '연 5000만원 ~ 7000만원', 'Python, PyTorch, GPU 서빙 경험자 우대', 'https://example.com/rag/1', now() - interval '5 days', now() - interval '5 days'),
    ('지속가능 에너지 데이터 분석가 채용', '그린파워솔루션', '태양광 및 풍력 발전량 예측 모델을 개발하고 시계열 데이터를 분석합니다. 신재생에너지 발전 효율화를 위한 데이터 기반 의사결정을 지원합니다.', 'JOB', 'DAEJEON', DATE '2026-08-25', '중소기업', '정규직', '연 4500만원', 'R 또는 Python 시계열 분석 경험자', 'https://example.com/rag/2', now() - interval '4 days', now() - interval '4 days'),
    ('반도체 공정 품질관리(QC) 엔지니어 채용', '실리콘밸리반도체', '웨이퍼 검사 및 수율 개선 업무를 담당하며 반도체 8대 공정 전반의 품질 이슈를 분석합니다.', 'JOB', 'GYEONGGI', DATE '2026-09-05', '대기업', '정규직', '연 5500만원', '반도체 공정 또는 품질관리 경력자 우대', 'https://example.com/rag/3', now() - interval '3 days', now() - interval '3 days'),
    ('핀테크 블록체인 개발 인턴십', '체인브릿지', '스마트컨트랙트(Solidity) 개발 및 탈중앙화 금융(DeFi) 서비스 프로토타입 제작에 참여하는 인턴을 모집합니다.', 'INTERN', 'SEOUL', DATE '2026-08-15', '스타트업', '인턴', '월 250만원', 'Solidity 또는 Web3 라이브러리 사용 경험', 'https://example.com/rag/4', now() - interval '6 days', now() - interval '6 days'),
    ('바이오헬스 데이터 사이언스 인턴', '라이프지노믹스', '유전체(genome) 데이터 분석 및 신약 개발 R&D를 보조하는 바이오인포매틱스 인턴을 모집합니다.', 'INTERN', 'INCHEON', DATE '2026-08-30', '중소기업', '인턴', '월 220만원', '생명과학 또는 통계학 전공자 우대', 'https://example.com/rag/5', now() - interval '2 days', now() - interval '2 days'),
    ('청년 농업창업 정착 지원사업', '농림축산식품부', '스마트팜 기반 청년 농업 창업자를 대상으로 정착자금과 시설 구축비를 지원하는 사업입니다. 귀농귀촌 청년을 우선 지원합니다.', 'GOVERNMENT_SUPPORT', 'JEONNAM', DATE '2026-09-10', '공공기관', NULL, '최대 3억원 융자 지원', '만 40세 미만 예비 농업 창업자', 'https://example.com/rag/6', now() - interval '7 days', now() - interval '7 days'),
    ('문화콘텐츠 해외 수출 지원사업', '한국콘텐츠진흥원', 'K-웹툰, K-드라마 등 문화콘텐츠의 해외 진출을 위한 번역, 마케팅, 현지화 비용을 지원합니다.', 'GOVERNMENT_SUPPORT', 'SEOUL', DATE '2026-09-15', '공공기관', NULL, '최대 8000만원 지원', '콘텐츠 제작 실적 보유 기업/개인', 'https://example.com/rag/7', now() - interval '1 days', now() - interval '1 days'),
    ('이공계 여성인재 육성 장학금', '한국여성과학기술인지원센터', 'STEM(과학·기술·공학·수학) 전공 여학생을 대상으로 학업 장려금과 멘토링 프로그램을 제공하는 장학금입니다.', 'SCHOLARSHIP', 'DAEGU', DATE '2026-08-18', '비영리단체', NULL, '연 350만원', '이공계열 재학 중인 여학생', 'https://example.com/rag/8', now() - interval '8 days', now() - interval '8 days'),
    ('다문화가정 자녀 교육 장학금', '무지개장학재단', '다문화가정 초·중·고등학생을 대상으로 학업 지원금과 정서 멘토링을 제공하는 장학 프로그램입니다.', 'SCHOLARSHIP', 'BUSAN', DATE '2026-08-22', '비영리단체', NULL, '연 200만원', '다문화가정 자녀 (초·중·고)', 'https://example.com/rag/9', now() - interval '9 days', now() - interval '9 days'),
    ('정보보안기사 자격증 대비 특강', '한국인터넷진흥원', '사이버보안, 모의해킹, 침해사고 대응 실무를 다루는 정보보안기사 자격증 대비 온라인 특강입니다.', 'CERTIFICATE', 'ULSAN', DATE '2026-09-01', '공공기관', NULL, '해당없음', '정보보호 또는 컴퓨터공학 관련 전공자', 'https://example.com/rag/10', now() - interval '10 days', now() - interval '10 days'),
    ('TESOL 영어교육 자격과정', '글로벌에듀센터', '해외 영어강사 취업을 준비하는 이들을 위한 TESOL(테솔) 국제 영어교육 자격과정입니다.', 'CERTIFICATE', 'GANGWON', DATE '2026-09-20', '중소기업', NULL, '과정 수강료 별도', '영어 회화 가능자, 해외 취업 희망자', 'https://example.com/rag/11', now() - interval '11 days', now() - interval '11 days'),
    ('메타버스 게임 개발 해커톤', '넥슨 유니버스랩', 'Unity/Unreal 엔진을 활용한 VR·AR 메타버스 게임 콘텐츠 기획 및 개발을 겨루는 해커톤입니다.', 'CONTEST', 'GYEONGGI', DATE '2026-09-25', '대기업', NULL, '총상금 3000만원', 'Unity 또는 Unreal Engine 사용 가능자', 'https://example.com/rag/12', now() - interval '12 days', now() - interval '12 days'),
    ('탄소중립 아이디어 공모전', '환경부', '기후위기 대응을 위한 정책 제안 및 친환경 기술 아이디어를 공모합니다. 탄소중립 실현 방안을 주제로 합니다.', 'CONTEST', 'SEJONG', DATE '2026-09-28', '공공기관', NULL, '총상금 1500만원', '환경/에너지 분야 관심자 누구나', 'https://example.com/rag/13', now() - interval '13 days', now() - interval '13 days'),
    ('국제개발협력 해외인턴십 프로그램', '코이카(KOICA)', '개발도상국에 파견되어 ODA(공적개발원조) 사업에 직접 참여하는 국제개발협력 해외인턴십입니다.', 'EXTRACURRICULAR', 'SEOUL', DATE '2026-10-05', '공공기관', NULL, '체재비 및 항공료 지원', '국제개발협력 분야 관심 대학생', 'https://example.com/rag/14', now() - interval '14 days', now() - interval '14 days'),
    ('실버세대 디지털 교육 봉사단', '서울시니어스타워', '어르신들을 대상으로 스마트폰 활용법과 무인 키오스크 이용법을 교육하는 디지털 격차 해소 봉사활동입니다.', 'EXTRACURRICULAR', 'SEOUL', DATE '2026-10-10', '비영리단체', NULL, '무급 (봉사시간 인정)', '디지털 기기 활용에 능숙한 성인', 'https://example.com/rag/15', now() - interval '15 days', now() - interval '15 days')
)
INSERT INTO postings (
    category_code_id, region_code_id, title, company, content, end_date,
    enterprise_type, employment_type, salary, qualification, homepage_url,
    view_count, created_at, updated_at
)
SELECT
    cat.id,
    reg.id,
    d.title,
    d.company,
    d.content,
    d.end_date,
    d.enterprise_type,
    d.employment_type,
    d.salary,
    d.qualification,
    d.homepage_url,
    floor(random() * 1000)::bigint AS view_count,
    d.created_at,
    d.updated_at
FROM data d
JOIN common_codes cat ON cat.code_type = 'CATEGORY' AND cat.code = d.category_code
JOIN common_codes reg ON reg.code_type = 'REGION' AND reg.code = d.region_code;
