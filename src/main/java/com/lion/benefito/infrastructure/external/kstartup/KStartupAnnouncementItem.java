package com.lion.benefito.infrastructure.external.kstartup;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * K-Startup 공고 항목 중 실제로 사용하는 필드만 담는다(pbanc_rcpt_bgng_dt, rcrt_prgs_yn 등은 매핑에
 * 쓰이지 않아 제외했다). pbanc_sn은 문서상 예시와 달리 실제 응답에서는 JSON 정수로 내려온다.
 */
public record KStartupAnnouncementItem(
        @JsonProperty("pbanc_sn") Long pbancSn,
        @JsonProperty("biz_pbanc_nm") String bizPbancNm,
        @JsonProperty("pbanc_ctnt") String pbancCtnt,
        @JsonProperty("pbanc_ntrp_nm") String pbancNtrpNm,
        @JsonProperty("sprv_inst") String sprvInst,
        @JsonProperty("supt_biz_clsfc") String suptBizClsfc,
        @JsonProperty("supt_regin") String suptRegin,
        @JsonProperty("aply_trgt_ctnt") String aplyTrgtCtnt,
        @JsonProperty("aply_trgt") String aplyTrgt,
        @JsonProperty("pbanc_rcpt_end_dt") String pbancRcptEndDt,
        @JsonProperty("detl_pg_url") String detlPgUrl
) {
}
