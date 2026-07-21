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
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * 공모전/대외활동 시연용 시드 CSV(classpath:data/seed_contest_activity.csv)를 읽는다.
 * 직접 작성한 UTF-8 파일이라 장학금 CSV(CP949)와 달리 인코딩 변환이 필요 없다.
 */
@Component
@RequiredArgsConstructor
public class SeedCsvReader {

    private final ResourceLoader resourceLoader;
    private final SeedProperties properties;

    public List<SeedCsvRow> readAll() {
        Resource resource = resourceLoader.getResource(properties.getCsvPath());

        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            CsvMapper csvMapper = new CsvMapper();
            CsvSchema schema = CsvSchema.emptySchema().withHeader();
            MappingIterator<Map<String, String>> it = csvMapper.readerFor(Map.class).with(schema).readValues(reader);
            return it.readAll().stream().map(this::toRow).toList();
        } catch (IOException e) {
            throw new ExternalPostingException(ExternalPostingErrorCode.API_CALL_FAILED);
        }
    }

    private SeedCsvRow toRow(Map<String, String> row) {
        return new SeedCsvRow(
                row.get("title"),
                row.get("organization"),
                row.get("category"),
                row.get("target"),
                row.get("qualification"),
                row.get("content"),
                row.get("benefit")
        );
    }
}
