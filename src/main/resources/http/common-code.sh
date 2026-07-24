#!/bin/bash

BASE_URL="http://localhost:8080"

echo "카테고리 공통 코드 조회 (장학금/정부지원사업/공모전/대외활동/자격증/채용/인턴)"
curl -X GET "$BASE_URL/api/v1/common-codes?type=CATEGORY"

echo -e "\n\n지역 공통 코드 조회 (서울/경기/인천 등 17개 시도)"
curl -X GET "$BASE_URL/api/v1/common-codes?type=REGION"

echo -e "\n\n등급 공통 코드 조회"
curl -X GET "$BASE_URL/api/v1/common-codes?type=GRADE"

echo -e "\n\n잘못된 type 요청 (400 Bad Request 예상)"
curl -X GET "$BASE_URL/api/v1/common-codes?type=INVALID"
