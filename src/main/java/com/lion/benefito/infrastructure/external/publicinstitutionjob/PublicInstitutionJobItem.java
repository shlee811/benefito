package com.lion.benefito.infrastructure.external.publicinstitutionjob;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 실제로 매핑에 사용하는 필드만 담는다(files/steps/disqlfcRsn 등 상세용 필드는 목록 API 응답에도
 * 포함돼 있지만 이번 범위에선 쓰지 않아 제외했다).
 */
public record PublicInstitutionJobItem(
        @JsonProperty("recrutPblntSn") Long recrutPblntSn,
        @JsonProperty("recrutPbancTtl") String recrutPbancTtl,
        @JsonProperty("instNm") String instNm,
        @JsonProperty("pbancEndYmd") String pbancEndYmd,
        @JsonProperty("srcUrl") String srcUrl,
        @JsonProperty("workRgnNmLst") String workRgnNmLst,
        @JsonProperty("hireTypeNmLst") String hireTypeNmLst,
        @JsonProperty("acbgCondNmLst") String acbgCondNmLst,
        @JsonProperty("aplyQlfcCn") String aplyQlfcCn,
        @JsonProperty("scrnprcdrMthdExpln") String scrnprcdrMthdExpln,
        @JsonProperty("prefCn") String prefCn,
        @JsonProperty("prefCondCn") String prefCondCn,
        @JsonProperty("ncsCdNmLst") String ncsCdNmLst,
        @JsonProperty("recrutSeNm") String recrutSeNm
) {
}
