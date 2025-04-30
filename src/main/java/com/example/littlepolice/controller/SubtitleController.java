package com.example.littlepolice.controller;

import com.example.littlepolice.model.ModelParameters;
import com.example.littlepolice.model.SubtitleEntry;
import com.example.littlepolice.service.SiliconFlowService;
import com.example.littlepolice.service.SubtitleService;
import com.example.littlepolice.service.SubtitleService.BatchCorrection;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController  // 改为 RestController
@RequestMapping("/api")  // 添加基础路径
@RequiredArgsConstructor
public class SubtitleController {
    private final SubtitleService subtitleService;
    private final SiliconFlowService siliconflowService;

    // 用于接收请求的类
    @Data
    public static class CorrectionRequest {
        private String content;
        private ModelParameters parameters;
    }

    // 用于返回原始和修正后内容的响应类
    @Data
    public static class SubtitleResponse {
        @JsonProperty("original")
        private String originalContent;
        @JsonProperty("corrected")
        private String correctedContent;

        public SubtitleResponse setOriginalContent(String content) {
            this.originalContent = content;
            return this;
        }

        public SubtitleResponse setCorrectedContent(String content) {
            this.correctedContent = content;
            return this;
        }
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/correct")
    public ResponseEntity<?> correctAbstract(@RequestBody CorrectionRequest request) {
        try {
            String content = request.getContent();
            ModelParameters parameters = request.getParameters();

            log.info("开始处理论文摘要内容，使用模型: {}", parameters.getModel());
            log.debug("内容前100个字符: {}", content.substring(0, Math.min(content.length(), 100)));

            // 按句子分割摘要
            log.info("分割摘要为句子...");
            // 解析摘要文本
            List<SubtitleEntry> entries = subtitleService.parseAbstractContent(content);

            // 提取需要修正的文本
            List<BatchCorrection> batches = subtitleService.extractTextForCorrection(entries);

            if (batches.isEmpty()) {
                return ResponseEntity.ok(new SubtitleResponse()
                        .setOriginalContent(content)
                        .setCorrectedContent(content));
            }

            // 收集所有句子的文本
            List<String> sentenceTexts = batches.stream()
                    .map(BatchCorrection::getText)
                    .collect(Collectors.toList());

            // 并行处理所有句子
            List<String> correctedTexts = siliconflowService.correctTextsParallel(sentenceTexts, parameters);

            // 更新所有句子的修正文本
            for (int i = 0; i < batches.size(); i++) {
                subtitleService.updateCorrectedText(entries, batches.get(i), correctedTexts.get(i));
            }

            // 生成新内容
            String newContent = subtitleService.generateAbstractContent(entries);

            return ResponseEntity.ok(new SubtitleResponse()
                    .setOriginalContent(content)
                    .setCorrectedContent(newContent));

        } catch (Exception e) {
            log.error("处理摘要文件时发生错误", e);
            return ResponseEntity.internalServerError()
                    .body("处理摘要文件时发生错误: " + e.getMessage());
        }
    }
} 