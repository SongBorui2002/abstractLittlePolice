package com.example.littlepolice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubtitleEntry {
    private int index;           // 序列号
    private String text;         // 字幕文本
    private String correctedText;
    private boolean needsCorrection;  // 是否需要修正（包含"的得地"）

} 