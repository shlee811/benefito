package com.lion.benefito.infrastructure.external.seed;

import com.lion.benefito.infrastructure.external.ExternalPostingData;
import com.lion.benefito.infrastructure.external.ExternalPostingSource;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * INTERN 카테고리는 어떤 자동 수집 소스(KStartup/공공기관 채용/Q-Net/장학금)도 채우지 않아, 이 세션
 * 시점 기준 실데이터가 10건뿐이고(그나마도 example.com 가짜 URL이 붙은 수동 SQL 시드라 RAG 검색
 * 쿼리(PostingRepository.findSimilarPostings*)에서 전부 걸러진다) RAG 추천/유사도 테스트를 할 만한
 * 데이터가 사실상 없다. 이를 보완하기 위한 임시 인턴십 시드 데이터를 제공한다.
 *
 * sourceType을 SEED_INTERN으로 고정해 다른 소스와 절대 섞이지 않게 한다 - 나중에 실제 인턴 공고를
 * 주는 외부 소스가 생기면 `DELETE FROM postings WHERE source_type = 'SEED_INTERN'`로 안전하게
 * 제거할 수 있다. originalUrl은 실존하지 않는 가상의 회사이므로 실제 URL을 지어내는 대신 null로 둔다
 * (PostingRepository의 RAG 쿼리는 homepage_url이 NULL이면 example.com과 달리 정상 통과시킨다).
 */
@Component
public class SeedInternSource implements ExternalPostingSource {

    private static final String SOURCE_TYPE = "SEED_INTERN";
    private static final String EMPLOYMENT_TYPE = "인턴";

    private static final List<InternSeed> SEEDS = List.of(
            new InternSeed("웹 프론트엔드 개발 인턴(React)", "라이언테크",
                    "React/TypeScript 기반 사용자 서비스 화면 개발에 참여하는 3개월 인턴십입니다.",
                    "HTML/CSS/JavaScript 기초, React 학습 경험자 우대", "서울",
                    LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 24)),
            new InternSeed("백엔드 서버 개발 인턴(Spring Boot)", "베네피토",
                    "Spring Boot 기반 REST API 설계·구현에 참여하며 코드 리뷰를 통해 실무 협업을 경험합니다.",
                    "Java 또는 Kotlin 기초, RDB 사용 경험자 우대", "경기",
                    LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 31)),
            new InternSeed("데이터 분석 인턴(신입)", "전남데이터랩",
                    "서비스 로그 데이터를 분석해 주간 리포트를 작성하고 대시보드를 개선하는 업무를 수행합니다.",
                    "SQL 활용 가능자, 통계/데이터 관련 전공자 우대", "전남",
                    LocalDate.of(2026, 8, 17), LocalDate.of(2026, 9, 7)),
            new InternSeed("AI 모델 개발 인턴", "비전테크",
                    "PyTorch 기반 이미지 인식 모델 학습·평가 파이프라인 구축을 보조합니다.",
                    "Python 및 딥러닝 프레임워크 사용 경험자 우대", "서울",
                    LocalDate.of(2026, 8, 24), LocalDate.of(2026, 9, 14)),
            new InternSeed("QA 테스트 자동화 인턴", "세종테스트랩",
                    "웹/앱 서비스의 테스트 케이스 작성과 자동화 스크립트 개발에 참여합니다.",
                    "테스트 프로세스에 대한 이해, Selenium 등 자동화 도구 관심자", "세종",
                    LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 21)),
            new InternSeed("UX/UI 디자인 인턴", "충남크리에이티브",
                    "신규 서비스의 와이어프레임 제작부터 프로토타입 디자인까지 전 과정에 참여합니다.",
                    "Figma 사용 가능자, 포트폴리오 제출 필수", "충남",
                    LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 28)),
            new InternSeed("마케팅 콘텐츠 기획 인턴", "주식회사 벤처프렌즈",
                    "SNS 콘텐츠 기획·제작과 캠페인 성과 분석 업무를 함께 수행합니다.",
                    "SNS 콘텐츠 제작 경험자 우대, 전공 무관", "광주",
                    LocalDate.of(2026, 9, 14), LocalDate.of(2026, 10, 5)),
            new InternSeed("브랜드 마케팅 인턴", "주식회사 라이언마케팅",
                    "브랜드 캠페인 기획부터 실행, 성과 리포트 작성까지 마케팅 전 과정을 경험합니다.",
                    "마케팅/광고홍보 전공자 우대", "인천",
                    LocalDate.of(2026, 9, 21), LocalDate.of(2026, 10, 12)),
            new InternSeed("인사(HR) 운영 인턴", "충북물류",
                    "채용 프로세스 운영 지원과 사내 교육 프로그램 기획을 보조합니다.",
                    "엑셀 활용 가능자, 꼼꼼한 문서 작업 가능자", "충북",
                    LocalDate.of(2026, 9, 28), LocalDate.of(2026, 10, 19)),
            new InternSeed("재무회계 인턴", "경북컴퍼니",
                    "월별 정산 업무 지원과 회계 전표 처리를 보조하며 실무 회계 프로세스를 경험합니다.",
                    "회계/경영 전공자 우대, 엑셀 함수 활용 가능자", "경북",
                    LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 26)),
            new InternSeed("해외영업 지원 인턴", "실리콘밸리반도체",
                    "해외 바이어 대응 문서 작성과 수출입 서류 관리를 보조합니다.",
                    "영어 비즈니스 커뮤니케이션 가능자 우대", "경기",
                    LocalDate.of(2026, 10, 12), LocalDate.of(2026, 11, 2)),
            new InternSeed("소셜미디어 운영 인턴", "체인브릿지",
                    "공식 SNS 채널 운영과 인플루언서 협업 콘텐츠 제작을 담당합니다.",
                    "SNS 트렌드에 대한 이해, 전공 무관", "서울",
                    LocalDate.of(2026, 10, 19), LocalDate.of(2026, 11, 9)),
            new InternSeed("게임 기획 인턴", "경남소프트",
                    "모바일 게임의 콘텐츠 기획서 작성과 밸런스 데이터 정리를 보조합니다.",
                    "게임 기획에 대한 관심, 기획 포트폴리오 우대", "경남",
                    LocalDate.of(2026, 10, 26), LocalDate.of(2026, 11, 16)),
            new InternSeed("물류/SCM 운영 인턴", "전북로지스틱스",
                    "입출고 데이터 관리와 재고 현황 리포트 작성 업무를 지원합니다.",
                    "엑셀 활용 가능자, 물류에 대한 관심", "전북",
                    LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 23)),
            new InternSeed("법무 컴플라이언스 인턴", "그린파워솔루션",
                    "계약서 검토 보조와 사내 컴플라이언스 자료 정리를 담당합니다.",
                    "법학 전공자 우대, 꼼꼼한 문서 검토 가능자", "대전",
                    LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 30)),
            new InternSeed("ESG 경영 지원 인턴", "환경부",
                    "탄소중립 이행 현황 자료 조사와 ESG 보고서 작성을 보조합니다.",
                    "환경/경영 전공자 우대", "세종",
                    LocalDate.of(2026, 11, 16), LocalDate.of(2026, 12, 7)),
            new InternSeed("헬스케어 서비스 기획 인턴", "라이프지노믹스",
                    "건강관리 앱의 신규 기능 기획과 사용자 인터뷰 정리를 보조합니다.",
                    "헬스케어/바이오 분야 관심자 우대", "인천",
                    LocalDate.of(2026, 11, 23), LocalDate.of(2026, 12, 14)),
            new InternSeed("핀테크 프로덕트 기획 인턴", "체인브릿지",
                    "송금·결제 서비스의 사용자 플로우 개선안을 조사하고 기획서를 작성합니다.",
                    "핀테크 서비스에 대한 관심, 기획 직무 관심자", "서울",
                    LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 21)),
            new InternSeed("임베디드 SW 개발 인턴", "충남크리에이티브",
                    "IoT 기기용 펌웨어 테스트와 간단한 기능 개발을 보조합니다.",
                    "C 언어 기초, 임베디드 시스템에 대한 관심", "충남",
                    LocalDate.of(2026, 12, 7), LocalDate.of(2026, 12, 28)),
            new InternSeed("영상 콘텐츠 제작 인턴", "테크벤처스",
                    "제품 소개 영상 촬영·편집과 유튜브 채널 운영을 보조합니다.",
                    "Premiere Pro 등 영상 편집 툴 사용 가능자", "대구",
                    LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 26)),
            new InternSeed("리서치 애널리스트 인턴", "한국여성과학기술인지원센터",
                    "산업 동향 리서치와 통계 자료 정리, 보고서 초안 작성을 보조합니다.",
                    "리서치/분석 업무에 대한 관심, 전공 무관", "대구",
                    LocalDate.of(2026, 8, 12), LocalDate.of(2026, 9, 2)),
            new InternSeed("고객경험(CX) 운영 인턴", "베네피토",
                    "고객 문의 응대 프로세스 개선과 VOC(고객의견) 정리·분석을 담당합니다.",
                    "커뮤니케이션 역량 우수자, 전공 무관", "경기",
                    LocalDate.of(2026, 8, 19), LocalDate.of(2026, 9, 9)),
            new InternSeed("이커머스 MD 지원 인턴", "제주트래블랩",
                    "상품 소싱 리서치와 상세페이지 기획, 판매 데이터 분석을 보조합니다.",
                    "이커머스/유통에 대한 관심, 엑셀 활용 가능자", "제주",
                    LocalDate.of(2026, 8, 26), LocalDate.of(2026, 9, 16)),
            new InternSeed("물류데이터 분석 인턴", "실리콘밸리반도체",
                    "물류센터 운영 데이터를 분석해 배송 효율화 방안을 제안하는 업무를 수행합니다.",
                    "SQL 또는 Python 데이터 분석 경험자 우대", "경기",
                    LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 23))
    );

    @Override
    public List<ExternalPostingData> fetchPostings() {
        return SEEDS.stream().map(this::toExternalPostingData).toList();
    }

    private ExternalPostingData toExternalPostingData(InternSeed seed) {
        String externalId = seed.organization + "-" + seed.title;
        String content = seed.description + "\n근무형태: " + EMPLOYMENT_TYPE
                + "\n모집기간: " + seed.regStart + " ~ " + seed.regEnd;

        return new ExternalPostingData(
                SOURCE_TYPE,
                externalId,
                seed.title,
                content,
                seed.organization,
                null,
                null,
                seed.region,
                seed.qualification,
                seed.regStart,
                seed.regEnd);
    }

    private record InternSeed(
            String title,
            String organization,
            String description,
            String qualification,
            String region,
            LocalDate regStart,
            LocalDate regEnd
    ) {
    }
}
