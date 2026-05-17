package com.example.llmwiki.api;

import com.example.llmwiki.domain.EvalReport;
import com.example.llmwiki.eval.EvalRunner;
import com.example.llmwiki.repository.EvalReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 评测接口：上传 CSV 启动评测 / 列出报告 / 详情。
 *
 * @author llm-wiki
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/eval")
@RequiredArgsConstructor
public class EvalController {

    private final EvalRunner evalRunner;
    private final EvalReportRepository reportRepo;

    @PostMapping("/run")
    public EvalReport run(@RequestParam("file") MultipartFile file,
                          @RequestParam(value = "name", defaultValue = "report") String name,
                          @RequestParam(value = "useJudge", defaultValue = "false") boolean useJudge)
            throws IOException {
        return evalRunner.run(name, file.getBytes(), useJudge);
    }

    @GetMapping("/reports")
    public List<EvalReport> list() {
        return reportRepo.findAll();
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<EvalReport> detail(@PathVariable Long id) {
        return reportRepo.findById(id).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
