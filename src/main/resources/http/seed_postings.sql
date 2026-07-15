-- postings 테스트 데이터 30건
-- category_code_id / region_code_id는 common_codes(code_type + code)를 조회하여 FK로 매핑한다.
-- 실행 전 common-code-seed.sql이 먼저 적용되어 있어야 한다 (CATEGORY/REGION 코드 존재 필요).

WITH data (title, company, content, category_code, region_code, end_date,
           enterprise_type, employment_type, salary, qualification, homepage_url,
           created_at, updated_at) AS (
    VALUES
    ('2026년 국가장학금 2차 신청 안내', '한국장학재단', '소득분위 기준에 따라 국가장학금을 지원합니다. 재학 중인 대학생을 대상으로 합니다.', 'SCHOLARSHIP', 'SEOUL', DATE '2026-07-10', '공공기관', NULL, '전액 지원', '4년제 대학 재학생, 소득 8분위 이하', 'https://example.com/postings/1', now() - interval '30 days', now() - interval '30 days'),
    ('청년 우수인재 장학생 모집', '서울장학재단', '서울 소재 대학 재학 중인 저소득층 우수 인재를 선발하여 장학금을 지급합니다.', 'SCHOLARSHIP', 'SEOUL', DATE '2026-07-10', '지방공기업', NULL, '연 500만원', '서울 거주 대학생, 성적 상위 20%', 'https://example.com/postings/2', now() - interval '28 days', now() - interval '28 days'),
    ('경기도 미래인재 장학금', '경기도장학재단', '경기도 내 대학에 재학 중인 학생을 위한 성적우수 장학금입니다.', 'SCHOLARSHIP', 'GYEONGGI', DATE '2026-07-10', '지방공기업', NULL, '연 300만원', '경기도 거주 대학생', 'https://example.com/postings/3', now() - interval '27 days', now() - interval '27 days'),
    ('청년창업 지원사업 참여기업 모집', '중소벤처기업부', '예비창업자 및 초기 창업기업을 대상으로 사업화 자금을 지원합니다.', 'GOVERNMENT_SUPPORT', 'SEOUL', DATE '2026-07-15', '공공기관', NULL, '최대 1억원 지원', '만 39세 이하 예비창업자', 'https://example.com/postings/4', now() - interval '26 days', now() - interval '26 days'),
    ('청년내일채움공제 참여 안내', '고용노동부', '중소기업 재직 청년의 장기근속과 자산형성을 지원하는 사업입니다.', 'GOVERNMENT_SUPPORT', 'BUSAN', DATE '2026-07-15', '공공기관', '정규직', '월 적립금 지원', '중소기업 정규직 재직 청년', 'https://example.com/postings/5', now() - interval '25 days', now() - interval '25 days'),
    ('지역특화 청년창업 지원사업', '부산창조경제혁신센터', '부산 지역 특화산업 분야 청년 창업팀을 발굴하여 지원합니다.', 'GOVERNMENT_SUPPORT', 'BUSAN', DATE '2026-07-15', '공공기관', NULL, '최대 5000만원 지원', '부산 소재 예비창업자', 'https://example.com/postings/6', now() - interval '24 days', now() - interval '24 days'),
    ('제3회 대학생 아이디어 공모전', '한국콘텐츠진흥원', '디지털 콘텐츠 분야의 창의적인 아이디어를 공모합니다.', 'CONTEST', 'SEOUL', DATE '2026-07-15', '공공기관', NULL, '총상금 1000만원', '전국 대학(원)생 누구나', 'https://example.com/postings/7', now() - interval '23 days', now() - interval '23 days'),
    ('전국 대학생 마케팅 공모전', '주식회사 라이언마케팅', '신제품 마케팅 전략을 주제로 한 대학생 대상 공모전입니다.', 'CONTEST', 'INCHEON', DATE '2026-07-15', '중소기업', NULL, '총상금 500만원', '대학교 재학생 및 휴학생', 'https://example.com/postings/8', now() - interval '22 days', now() - interval '22 days'),
    ('AI 서비스 아이디어톤', '테크벤처스', '인공지능을 활용한 서비스 아이디어를 발굴하는 해커톤형 공모전입니다.', 'CONTEST', 'DAEGU', DATE '2026-08-01', '스타트업', NULL, '총상금 800만원', '개발/기획 전공자 우대', 'https://example.com/postings/9', now() - interval '21 days', now() - interval '21 days'),
    ('글로벌 대학생 서포터즈 8기', '한국관광공사', '국내 관광 콘텐츠를 SNS에 홍보하는 대학생 서포터즈를 모집합니다.', 'EXTRACURRICULAR', 'SEOUL', DATE '2026-08-01', '공공기관', NULL, '활동비 지급', '어학 능력 우수 대학생', 'https://example.com/postings/10', now() - interval '20 days', now() - interval '20 days'),
    ('기업 서포터즈 봄 시즌 모집', '주식회사 벤처프렌즈', '기업 브랜드 홍보 콘텐츠를 제작하는 서포터즈 활동입니다.', 'EXTRACURRICULAR', 'GWANGJU', DATE '2026-08-01', '중소기업', NULL, '활동비 지급', '콘텐츠 제작에 관심있는 대학생', 'https://example.com/postings/11', now() - interval '19 days', now() - interval '19 days'),
    ('사회공헌 봉사단 모집', '대전사회복지공동모금회', '지역사회 취약계층을 지원하는 봉사활동 프로그램입니다.', 'EXTRACURRICULAR', 'DAEJEON', DATE '2026-08-05', '비영리단체', NULL, '무급 (봉사시간 인정)', '만 20세 이상 성인 누구나', 'https://example.com/postings/12', now() - interval '18 days', now() - interval '18 days'),
    ('정보처리기사 대비반 모집', '한국산업인력공단', '국가기술자격 정보처리기사 취득을 위한 온라인 교육과정입니다.', 'CERTIFICATE', 'ULSAN', DATE '2026-08-05', '공공기관', NULL, '해당없음', '컴퓨터공학 관련 전공자', 'https://example.com/postings/13', now() - interval '17 days', now() - interval '17 days'),
    ('전산회계 자격증 무료강좌', '한국세무사회', '전산세무회계 자격증 취득을 위한 무료 온라인 강좌를 제공합니다.', 'CERTIFICATE', 'SEJONG', DATE '2026-08-10', '비영리단체', NULL, '해당없음', '회계/세무 관심자 누구나', 'https://example.com/postings/14', now() - interval '16 days', now() - interval '16 days'),
    ('빅데이터분석기사 실무특강', '한국데이터산업진흥원', '빅데이터분석기사 실기 대비를 위한 실무 중심 특강입니다.', 'CERTIFICATE', 'GANGWON', DATE '2026-08-10', '공공기관', NULL, '해당없음', '통계/데이터 관련 전공자 우대', 'https://example.com/postings/15', now() - interval '15 days', now() - interval '15 days'),
    ('2026 상반기 백엔드 개발자 채용', '라이언테크', 'Spring Boot 기반 백엔드 서비스를 개발할 신입/경력 개발자를 채용합니다.', 'JOB', 'SEOUL', DATE '2026-08-15', '스타트업', '정규직', '회사 내규에 따름', 'Java, Spring 기반 개발 경험자 우대', 'https://example.com/postings/16', now() - interval '14 days', now() - interval '14 days'),
    ('프론트엔드 개발자 경력 채용', '베네피토', 'React 기반 사용자 서비스를 개발할 경력 프론트엔드 개발자를 모집합니다.', 'JOB', 'GYEONGGI', DATE '2026-08-15', '중소기업', '정규직', '연 4000만원 ~ 5500만원', '3년 이상 프론트엔드 개발 경력', 'https://example.com/postings/17', now() - interval '13 days', now() - interval '13 days'),
    ('영업관리 직원 채용', '충북물류', '지역 거래처 관리 및 영업 지원 업무를 담당할 직원을 채용합니다.', 'JOB', 'CHUNGBUK', DATE '2026-08-20', '중소기업', '계약직', '월 250만원', '운전면허 소지자 우대', 'https://example.com/postings/18', now() - interval '12 days', now() - interval '12 days'),
    ('디자인 스튜디오 신입 디자이너 채용', '충남크리에이티브', 'UI/UX 디자인 및 브랜드 디자인 업무를 담당할 신입 디자이너를 채용합니다.', 'JOB', 'CHUNGNAM', DATE '2026-08-20', '중소기업', '정규직', '연 3200만원', '포트폴리오 제출 필수', 'https://example.com/postings/19', now() - interval '11 days', now() - interval '11 days'),
    ('물류센터 현장관리직 채용', '전북로지스틱스', '물류센터 입출고 및 현장 인력 관리 업무를 수행할 관리자를 채용합니다.', 'JOB', 'JEONBUK', DATE '2026-08-25', '중소기업', '정규직', '연 3500만원', '물류/유통 업계 경력 우대', 'https://example.com/postings/20', now() - interval '10 days', now() - interval '10 days'),
    ('데이터 분석 신입 인턴 채용', '전남데이터랩', '서비스 데이터를 분석하고 리포트를 작성하는 인턴을 채용합니다.', 'INTERN', 'JEONNAM', DATE '2026-08-25', '스타트업', '인턴', '월 200만원', 'SQL 활용 가능자 우대', 'https://example.com/postings/21', now() - interval '9 days', now() - interval '9 days'),
    ('마케팅팀 여름방학 인턴십', '경북컴퍼니', '마케팅팀에서 콘텐츠 기획 및 SNS 운영을 경험할 인턴을 모집합니다.', 'INTERN', 'GYEONGBUK', DATE '2026-09-01', '중소기업', '인턴', '월 180만원', '마케팅 전공 또는 관심자', 'https://example.com/postings/22', now() - interval '8 days', now() - interval '8 days'),
    ('백엔드 개발 방학 인턴십', '경남소프트', '실무 프로젝트에 참여하며 백엔드 개발을 경험할 인턴을 모집합니다.', 'INTERN', 'GYEONGNAM', DATE '2026-09-01', '스타트업', '인턴', '월 220만원', 'Java 또는 Python 사용 가능자', 'https://example.com/postings/23', now() - interval '7 days', now() - interval '7 days'),
    ('제주 관광 스타트업 인턴십', '제주트래블랩', '제주 지역 관광 서비스를 기획하는 인턴을 모집합니다.', 'INTERN', 'JEJU', DATE '2026-09-05', '스타트업', '인턴', '월 190만원', '여행/관광 산업 관심자', 'https://example.com/postings/24', now() - interval '6 days', now() - interval '6 days'),
    ('공공데이터 활용 공모전', '행정안전부', '공공데이터를 활용한 서비스 아이디어 및 프로토타입을 공모합니다.', 'CONTEST', 'SEOUL', DATE '2026-09-10', '공공기관', NULL, '총상금 2000만원', '전 국민 누구나', 'https://example.com/postings/25', now() - interval '5 days', now() - interval '5 days'),
    ('지역 인재 성장 장학금', '인천인재육성재단', '인천 지역 대학 재학생을 대상으로 하는 성장 장학금입니다.', 'SCHOLARSHIP', 'INCHEON', DATE '2026-09-10', '지방공기업', NULL, '연 400만원', '인천 소재 대학 재학생', 'https://example.com/postings/26', now() - interval '4 days', now() - interval '4 days'),
    ('소상공인 디지털 전환 지원사업', '중소벤처기업부', '소상공인의 온라인 판로 개척을 위한 디지털 전환 비용을 지원합니다.', 'GOVERNMENT_SUPPORT', 'DAEGU', DATE '2026-09-15', '공공기관', NULL, '최대 500만원 지원', '사업자등록증 보유 소상공인', 'https://example.com/postings/27', now() - interval '3 days', now() - interval '3 days'),
    ('해외 봉사단 서포터즈 모집', '한국국제협력단', '개발도상국 지역사회를 지원하는 해외 봉사 프로그램입니다.', 'EXTRACURRICULAR', 'ULSAN', DATE '2026-09-20', '공공기관', NULL, '활동비 및 항공료 지원', '만 20세 이상 대학생 및 취업준비생', 'https://example.com/postings/28', now() - interval '2 days', now() - interval '2 days'),
    ('네트워크관리사 자격증 특강', '한국정보통신진흥협회', '네트워크관리사 2급 자격증 취득을 위한 실무 특강입니다.', 'CERTIFICATE', 'GANGWON', DATE '2026-09-25', '비영리단체', NULL, '해당없음', '전공 무관 누구나', 'https://example.com/postings/29', now() - interval '1 days', now() - interval '1 days'),
    ('QA 엔지니어 경력 채용', '세종테스트랩', '서비스 품질 보증을 위한 테스트 설계 및 자동화를 담당할 QA 엔지니어를 채용합니다.', 'JOB', 'SEJONG', DATE '2026-09-30', '중소기업', '정규직', '연 4200만원', 'QA 또는 테스트 자동화 경력 2년 이상', 'https://example.com/postings/30', now(), now())
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
