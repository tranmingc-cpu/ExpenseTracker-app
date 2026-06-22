package com.expensetracker.expensetracker_api.service;

import com.expensetracker.expensetracker_api.dto.response.OcrResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class OcrService {

    @Value("${ocr.space.api-key}")
    private String apiKey;

    @Value("${ocr.space.url}")
    private String apiUrl;

    // Hàm quét ảnh lấy chữ thô từ OCR.space
    public String parseImage(MultipartFile file) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("apikey", apiKey);
            body.add("language", "vie");
            body.add("OCREngine", "3");
            body.add("isOverlayRequired", "false");

            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, requestEntity, String.class);

            return extractTextFromJson(response.getBody());

        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi xử lý OCR hệ thống: " + e.getMessage();
        }
    }

    // Hàm phụ bóc tách JSON
    private String extractTextFromJson(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            if (jsonObject.has("IsErroredOnProcessing") && jsonObject.getBoolean("IsErroredOnProcessing")) {
                return "Server OCR báo lỗi: " + jsonObject.getJSONArray("ErrorMessage").getString(0);
            }
            JSONArray parsedResults = jsonObject.getJSONArray("ParsedResults");
            if (parsedResults.length() > 0) {
                return parsedResults.getJSONObject(0).getString("ParsedText");
            }
        } catch (Exception e) {
            return "Lỗi phân tích cú pháp dữ liệu JSON trả về.";
        }
        return "Không tìm thấy nội dung chữ hay số nào.";
    }
    public OcrResponse extractBillDetails(String fullText) {
        String foundAmount = "";
        String foundNote = "";
        String foundTime = "";
        String foundAccountName = "";
        String foundAccountNumber = "";

        if (fullText == null || fullText.trim().isEmpty()) {
            return new OcrResponse("", "Không đọc được chữ từ ảnh", "", "", "", "");
        }

        String[] lines = fullText.split("\n");
        List<String> allLines = new ArrayList<>();
        for (String line : lines) {
            allLines.add(line.trim());
        }

        // Cải tiến Regex để bắt Ngày/Tháng/Năm linh hoạt hơn (chấp nhận cả dấu gạch chéo /, gạch ngang -, dấu chấm .)
        String dateTimeRegex = ".*(\\d{2}[/\\-\\.]\\d{2}[/\\-\\.]\\d{4}|\\d{4}[/\\-\\.]\\d{2}[/\\-\\.]\\d{2}).*";

        for (int i = 0; i < allLines.size(); i++) {
            String lineText = allLines.get(i);
            String lowerText = lineText.toLowerCase();

            // 1. Logic lấy Số tiền
            // Đã xóa bỏ điều kiện lỗi "lowerText.contains(" ")"
            if (lowerText.contains("số tiền") || lowerText.contains("chuyển tiền") || lowerText.contains("giao dịch thành công") || lowerText.contains("amount")) {
                String digits = lineText.replace(".", "").replace(",", "").replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    foundAmount = digits;
                }

                if (foundAmount.isEmpty() && i + 1 < allLines.size()) {
                    String nextLine = allLines.get(i + 1);
                    foundAmount = nextLine.replace(".", "").replace(",", "").replace(" ", "").replaceAll("[^0-9]", "");
                }

                // Quét thử 2 dòng tiếp theo dưới dòng giá tiền vừa tìm thấy để bắt Ngày Giờ
                if (foundTime.isEmpty()) {
                    for (int j = 1; j <= 2; j++) {
                        if (i + j < allLines.size()) {
                            String checkLine = allLines.get(i + j);
                            if (checkLine.matches(dateTimeRegex)) {
                                foundTime = checkLine;
                                break;
                            }
                        }
                    }
                }

            } else if ((lowerText.contains("vnd") || lowerText.contains("vnđ") || lowerText.contains("đ")) && foundAmount.isEmpty()) {
                String digits = lineText.replace(".", "").replace(",", "").replace(" ", "").replaceAll("[^0-9]", "");
                if (digits.length() >= 3 && !digits.matches("^0+$")) {
                    foundAmount = digits;

                    // Tiếp tục quét 2 dòng dưới nếu dòng này chứa ký hiệu tiền tệ VND/Đ
                    if (foundTime.isEmpty()) {
                        for (int j = 1; j <= 2; j++) {
                            if (i + j < allLines.size()) {
                                String checkLine = allLines.get(i + j);
                                if (checkLine.matches(dateTimeRegex)) {
                                    foundTime = checkLine;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            // Logic lấy Ngày Giờ theo Từ khóa (Bổ sung để lấy trọn vẹn Giờ:Phút:Giây)
            if (lowerText.contains("ngày") || lowerText.contains("thời gian") || lowerText.contains("date") || lowerText.contains("time")) {
                if (foundTime.isEmpty()) {
                    if (lineText.contains(":")) {
                        foundTime = lineText.substring(lineText.indexOf(":") + 1).trim();
                    } else if (i + 1 < allLines.size()) {
                        foundTime = allLines.get(i + 1).trim();
                    }
                }
            }

            // Nếu chạy hết các logic trên mà vẫn không bắt được ngày giờ, dùng Regex hốt các dòng chứa ngày tháng đơn lẻ
            if (foundTime.isEmpty() && lineText.matches(dateTimeRegex)) {
                foundTime = lineText;
            }

            //  Logic lấy Nội dung / Ghi chú
            if (lowerText.contains("nội dung") || lowerText.contains("lời nhắn") || lowerText.contains("ghi chú") || lowerText.contains("ndck")) {
                if (lineText.contains(":")) {
                    foundNote = lineText.substring(lineText.indexOf(":") + 1).trim();
                } else if (i + 1 < allLines.size()) {
                    foundNote = allLines.get(i + 1).trim();
                }
            }

            //   lấy Tên tài khoản
            if (lowerText.contains("tên tài khoản") || lowerText.contains("người thụ hưởng") || lowerText.contains("tên người hưởng") || lowerText.contains("tên người nhận") || lowerText.contains("người nhận") || lowerText.contains("đến")) {
                if (lineText.contains(":")) {
                    foundAccountName = lineText.substring(lineText.indexOf(":") + 1).trim();
                } else {
                    for (int j = 1; j <= 3; j++) {
                        if (i + j < allLines.size()) {
                            String checkLine = allLines.get(i + j).trim();
                            // Dòng lấy tên phải không trống và không chứa toàn ký số (tránh lộn sang STK)
                            if (!checkLine.isEmpty() && !checkLine.matches("^[0-9\\s]+$")) {
                                foundAccountName = checkLine;
                                break;
                            }
                        }
                    }
                }
            }

            //  lấy Số tài khoản
            if (lowerText.contains("số tài khoản") || lowerText.contains("stk") || lowerText.contains("số tk") || lowerText.contains("tài khoản")) {
                String cleanStk = lineText.replaceAll("[^0-9]", "");
                if (cleanStk.length() >= 6) {
                    foundAccountNumber = cleanStk;
                } else {
                    for (int j = 1; j <= 3; j++) {
                        if (i + j < allLines.size()) {
                            String nextLineClean = allLines.get(i + j).replaceAll("[^0-9]", "");
                            if (nextLineClean.length() >= 6) {
                                foundAccountNumber = nextLineClean;
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (foundNote.isEmpty()) {
            foundNote = "Chi tiêu từ ảnh biên lai";
        }

        String foundBankName = extractBankName(fullText, allLines);

        return new OcrResponse(foundAmount, foundNote, foundTime, foundAccountName, foundAccountNumber, foundBankName);
    }

    private String extractBankName(String fullText, List<String> allLines) {
        for (String line : allLines) {
            String lowerLine = line.toLowerCase();
            if (lowerLine.contains("tại") || lowerLine.contains("ngân hàng") || lowerLine.contains("nganhang")) {
                String detected = matchBankFuzzy(line);
                if (!detected.isEmpty()) {
                    return detected;
                }
            }
        }
        return matchBankFuzzy(fullText);
    }

    private String matchBankFuzzy(String text) {
        String lower = text.toLowerCase();
        if (lower.matches("(?s).*(vietcom[a-z]+|vcb|ngoại\\s*thương).*")) {
            return "Vietcombank";
        }
        if (lower.matches("(?s).*(techcom[a-z]+|tcb|kỹ\\s*thương).*")) {
            return "Techcombank";
        }
        if (lower.matches("(?s).*(bidv|đầu\\s*tư\\s*và\\s*phát\\s*triển).*")) {
            return "BIDV";
        }
        if (lower.matches("(?s).*(agri[a-z]+|vba|nông\\s*nghiệp).*")) {
            return "Agribank";
        }
        if (lower.matches("(?s).*(mb\\s*bank|mbbank|quân\\s*đội|mbb).*")) {
            return "MB Bank";
        }
        if (lower.matches("(?s).*(tp\\s*bank|tpbank|tiên\\s*phong).*")) {
            return "TPBank";
        }
        if (lower.matches("(?s).*(vp\\s*bank|vpbank|thịnh\\s*vượng).*")) {
            return "VPBank";
        }
        if (lower.matches("(?s).*(acb|á\\s*châu).*")) {
            return "ACB";
        }
        if (lower.matches("(?s).*(sacom[a-z]+|sài\\s*gòn\\s*thương\\s*tín).*")) {
            return "Sacombank";
        }
        if (lower.matches("(?s).*(vib|quốc\\s*tế).*")) {
            return "VIB";
        }
        if (lower.matches("(?s).*shinhan.*")) {
            return "Shinhan Bank";
        }
        if (lower.matches("(?s).*(msb|hàng\\s*hải).*")) {
            return "MSB";
        }
        if (lower.matches("(?s).*(shb|sài\\s*gòn\\s*-\\s*hà\\s*nội).*")) {
            return "SHB";
        }
        if (lower.matches("(?s).*(hd\\s*bank|hdbank|hdb).*")) {
            return "HDBank";
        }
        if (lower.matches("(?s).*(ocb|phương\\s*đông).*")) {
            return "OCB";
        }
        if (lower.matches("(?s).*scb.*")) {
            return "SCB";
        }
        return "";
    }

}