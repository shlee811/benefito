package com.lion.benefito.infrastructure.external.scholarship;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.lion.benefito.infrastructure.external.ExternalPostingErrorCode;
import com.lion.benefito.infrastructure.external.ExternalPostingException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * 장학금 CSV(classpath:data/scholarship.csv)를 읽어 행 단위로 파싱한다.
 * 따옴표/개행이 포함된 필드(제출서류 목록 등)가 있어 단순 split(",")로는 깨지므로
 * jackson-dataformat-csv로 안전하게 파싱한다. 파일은 CP949로 인코딩돼 있다.
 */
@Component
@RequiredArgsConstructor
public class ScholarshipCsvReader {

    private static final Charset CP949 = Charset.forName("CP949");

    private final ResourceLoader resourceLoader;
    private final ScholarshipProperties properties;

    public List<ScholarshipCsvRow> readAll() {
        Resource resource = resourceLoader.getResource(properties.getCsvPath());

        try (Reader reader = new InputStreamReader(resource.getInputStream(), CP949)) {
            CsvMapper csvMapper = new CsvMapper();
            CsvSchema schema = CsvSchema.emptySchema().withHeader();
            MappingIterator<Map<String, String>> it = csvMapper.readerFor(Map.class).with(schema).readValues(reader);
            return it.readAll().stream().map(this::toRow).toList();
        } catch (IOException e) {
            throw new ExternalPostingException(ExternalPostingErrorCode.API_CALL_FAILED);
        }
    }

    private ScholarshipCsvRow toRow(Map<String, String> row) {
        return new ScholarshipCsvRow(
                row.get("번호"),
                row.get("운영기관명"),
                row.get("상품명"),
                row.get("학년구분"),
                row.get("학과구분"),
                row.get("성적기준 상세내용"),
                row.get("소득기준 상세내용"),
                row.get("지원내역 상세내용"),
                row.get("지역거주여부 상세내용"),
                row.get("제출서류 상세내용"),
                row.get("홈페이지 주소"),
                row.get("모집시작일"),
                row.get("모집종료일")
        );
    }
}
