-- common_codes 테스트 데이터 (Benefito 서비스 도메인 기준)
-- 기존 예시 데이터(CAFE, FOOD, SEOUL, BUSAN 등)를 제거하고 아래 데이터로 대체한다.

DELETE FROM common_codes WHERE code_type IN ('CATEGORY', 'REGION');

-- CATEGORY
INSERT INTO common_codes (code_type, code, name, display_order, is_active, created_at, updated_at) VALUES
('CATEGORY', 'SCHOLARSHIP', '장학금', 1, true, now(), now()),
('CATEGORY', 'GOVERNMENT_SUPPORT', '정부지원사업', 2, true, now(), now()),
('CATEGORY', 'CONTEST', '공모전', 3, true, now(), now()),
('CATEGORY', 'EXTRACURRICULAR', '대외활동', 4, true, now(), now()),
('CATEGORY', 'CERTIFICATE', '자격증', 5, true, now(), now()),
('CATEGORY', 'JOB', '채용', 6, true, now(), now()),
('CATEGORY', 'INTERN', '인턴', 7, true, now(), now()),
('CATEGORY', 'BUSINESS_ADMINISTRATION', '경영.사무', 8, true, now(), now()),
('CATEGORY', 'MARKETING_PR', '마케팅.광고.홍보', 9, true, now(), now()),
('CATEGORY', 'TRADE_LOGISTICS', '무역.유통', 10, true, now(), now()),
('CATEGORY', 'IT_INTERNET', 'IT.인터넷', 11, true, now(), now()),
('CATEGORY', 'MANUFACTURING', '생산.제조', 12, true, now(), now()),
('CATEGORY', 'SALES_CUSTOMER', '영업.고객상담', 13, true, now(), now()),
('CATEGORY', 'CONSTRUCTION', '건설', 14, true, now(), now()),
('CATEGORY', 'FINANCE', '금융', 15, true, now(), now()),
('CATEGORY', 'RND_DESIGN', '연구개발.설계', 16, true, now(), now()),
('CATEGORY', 'DESIGN', '디자인', 17, true, now(), now()),
('CATEGORY', 'MEDIA', '미디어', 18, true, now(), now()),
('CATEGORY', 'PROFESSIONAL_SPECIAL', '전문.특수직', 19, true, now(), now());

-- REGION
INSERT INTO common_codes (code_type, code, name, display_order, is_active, created_at, updated_at) VALUES
('REGION', 'SEOUL', '서울', 1, true, now(), now()),
('REGION', 'GYEONGGI', '경기', 2, true, now(), now()),
('REGION', 'INCHEON', '인천', 3, true, now(), now()),
('REGION', 'BUSAN', '부산', 4, true, now(), now()),
('REGION', 'DAEGU', '대구', 5, true, now(), now()),
('REGION', 'GWANGJU', '광주', 6, true, now(), now()),
('REGION', 'DAEJEON', '대전', 7, true, now(), now()),
('REGION', 'ULSAN', '울산', 8, true, now(), now()),
('REGION', 'SEJONG', '세종', 9, true, now(), now()),
('REGION', 'GANGWON', '강원', 10, true, now(), now()),
('REGION', 'CHUNGBUK', '충북', 11, true, now(), now()),
('REGION', 'CHUNGNAM', '충남', 12, true, now(), now()),
('REGION', 'JEONBUK', '전북', 13, true, now(), now()),
('REGION', 'JEONNAM', '전남', 14, true, now(), now()),
('REGION', 'GYEONGBUK', '경북', 15, true, now(), now()),
('REGION', 'GYEONGNAM', '경남', 16, true, now(), now()),
('REGION', 'JEJU', '제주', 17, true, now(), now()),
('REGION', 'ALL', '전국', 18, true, now(), now());
