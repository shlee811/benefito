package com.lion.benefito.infrastructure.external.seed;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.lion.benefito.infrastructure.external.ExternalPostingErrorCode;
import com.lion.benefito.infrastructure.external.ExternalPostingException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * RAG(pgvector 유사도 검색) 테스트용 시드 CSV(classpath:data/rag_seed_*.csv)를 읽는다.
 * SeedCsvReader(공모전/대외활동, 7컬럼, 지역·마감일 없음)와는 스키마가 달라(RagSeedCsvRow 참고)
 * 별도 리더로 분리했다 - 기존 SEED 소스/CSV는 이 작업에서 건드리지 않는다.
 */
@Component
public class RagSeedCsvReader {

    private final ResourceLoader resourceLoader;

    public RagSeedCsvReader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public List<RagSeedCsvRow> readAll(String classpathLocation) {
        Resource resource = resourceLoader.getResource(classpathLocation);

        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            CsvMapper csvMapper = new CsvMapper();
            CsvSchema schema = CsvSchema.emptySchema().withHeader();
            MappingIterator<Map<String, String>> it = csvMapper.readerFor(Map.class).with(schema).readValues(reader);
            return it.readAll().stream().map(this::toRow).toList();
        } catch (IOException e) {
            throw new ExternalPostingException(ExternalPostingErrorCode.API_CALL_FAILED);
        }
    }

    private RagSeedCsvRow toRow(Map<String, String> row) {
        return new RagSeedCsvRow(
                row.get("title"),
                row.get("organization"),
                row.get("region"),
                row.get("target"),
                row.get("major"),
                row.get("grade"),
                row.get("benefit"),
                row.get("applicationMethod"),
                row.get("content"),
                row.get("regStart"),
                row.get("deadline"),
                row.get("salary"),
                row.get("employmentType"),
                row.get("enterpriseType"),
                row.get("career"),
                row.get("education")
        );
    }
}
