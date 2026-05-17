package com.example.llmwiki.parser.impl;

import com.example.llmwiki.domain.RawDocument;
import com.example.llmwiki.parser.ParseRequest;
import com.example.llmwiki.parser.SourceParser;
import com.example.llmwiki.util.TextUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.Locale;

/**
 * Excel 解析器（xls / xlsx），按行展开为 Markdown 表格。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@Order(30)
@Component
public class ExcelParser implements SourceParser {

    @Override
    public String kind() {
        return "FILE/EXCEL";
    }

    @Override
    public boolean supports(ParseRequest req) {
        if (!"FILE".equalsIgnoreCase(req.getKind())) {
            return false;
        }
        String name = (req.getDisplayName() == null ? req.getRef() : req.getDisplayName()).toLowerCase(Locale.ROOT);
        return name.endsWith(".xls") || name.endsWith(".xlsx");
    }

    @Override
    public RawDocument parse(ParseRequest req) throws Exception {
        StringBuilder sb = new StringBuilder();
        DataFormatter formatter = new DataFormatter();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(req.getFileBytes()))) {
            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                Sheet sheet = wb.getSheetAt(s);
                sb.append("\n## Sheet: ").append(sheet.getSheetName()).append('\n');
                int rowLimit = Math.min(sheet.getLastRowNum() + 1, 2000);
                for (int r = 0; r < rowLimit; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) {
                        continue;
                    }
                    sb.append('|');
                    for (int c = 0; c < row.getLastCellNum(); c++) {
                        Cell cell = row.getCell(c);
                        String v = cell == null ? "" : formatter.formatCellValue(cell);
                        sb.append(' ').append(v.replace('|', '\\')).append(" |");
                    }
                    sb.append('\n');
                }
            }
        }
        String text = sb.toString();
        return RawDocument.builder()
                .sourceKind("FILE")
                .sourceRef(req.getRef())
                .displayName(req.getDisplayName())
                .text(text)
                .contentHash(TextUtils.sha256(text))
                .build();
    }
}
