package com.example.littlepolice.service;

import com.example.littlepolice.model.SubtitleEntry;
import com.example.littlepolice.util.TokenCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.regex.Matcher;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;


@Service
@Slf4j
public class SubtitleService {
    @Data
    public static class BatchCorrection {
        private String text;
        private List<Integer> indices;

        public BatchCorrection(String text, List<Integer> indices) {
            this.text = text;
            this.indices = indices;
        }
    }

    // 将文本分割为句子，并赋予索引
    public List<SubtitleEntry> parseAbstractContent(String content) {
        String[] sentences = content.split("(?<=[。！？])");
        List<SubtitleEntry> entries = new ArrayList<>();

        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i].trim();
            if (!sentence.isEmpty()) {
                SubtitleEntry entry = new SubtitleEntry();
                entry.setIndex(i + 1);
                entry.setText(sentence);
                entries.add(entry);
            }
        }
        return entries;
    }

    // 提取需要修正的文本
    public List<BatchCorrection> extractTextForCorrection(List<SubtitleEntry> entries) {
        List<BatchCorrection> batches = new ArrayList<>();
        for (SubtitleEntry entry : entries) {
            batches.add(new BatchCorrection(
                    entry.getText(),
                    List.of(entry.getIndex())
            ));
        }
        return batches;
    }

    // 更新修正后的文本
    public void updateCorrectedText(List<SubtitleEntry> entries, BatchCorrection batch, String correctedText) {
        int index = batch.getIndices().get(0);
        entries.stream()
                .filter(e -> e.getIndex() == index)
                .findFirst()
                .ifPresent(e -> e.setCorrectedText(correctedText));
    }

    // 生成最终的文本内容
    public String generateAbstractContent(List<SubtitleEntry> entries) {
        return entries.stream()
                .sorted(Comparator.comparingInt(SubtitleEntry::getIndex))
                .map(e -> e.getCorrectedText() != null ? e.getCorrectedText() : e.getText())
                .collect(Collectors.joining(""));
    }

} 