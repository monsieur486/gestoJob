package com.mr486.gestojob.service;

import com.mr486.gestojob.tools.HtmlToPlainText;
import org.springframework.stereotype.Service;

@Service
public class HtmlConverterService {
    public String htmlToPlainText(String html) {
        return HtmlToPlainText.toPlainTextKeepLines(html);
    }
}
