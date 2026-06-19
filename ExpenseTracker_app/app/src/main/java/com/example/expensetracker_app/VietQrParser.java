package com.example.expensetracker_app;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class VietQrParser {
    public static class QrData {
        public String bankBin = "";
        public String bankName = "";
        public String accountNumber = "";
        public String recipientName = "";
        public String amount = "";
        public String memo = "";
        public boolean isValid = false;
    }

    public static QrData parse(String qrCode) {
        QrData data = new QrData();
        if (qrCode == null || !qrCode.startsWith("000201")) {
            return data;
        }

        try {
            Map<String, String> tags = parseTlv(qrCode);
            data.isValid = true;

            if (tags.containsKey("59")) {
                data.recipientName = tags.get("59");
            }

            if (tags.containsKey("54")) {
                data.amount = tags.get("54");
            }

            if (tags.containsKey("38")) {
                Map<String, String> sub38 = parseTlv(tags.get("38"));
                String targetSub = "";
                if (sub38.containsKey("01")) {
                    targetSub = sub38.get("01");
                } else if (sub38.containsKey("00")) {
                    targetSub = sub38.get("00");
                }

                if (!targetSub.isEmpty()) {
                    Map<String, String> bankInfo = parseTlv(targetSub);
                    if (bankInfo.containsKey("00")) {
                        data.bankBin = bankInfo.get("00");
                        data.bankName = getBankNameFromBin(data.bankBin);
                    }
                    if (bankInfo.containsKey("01")) {
                        data.accountNumber = bankInfo.get("01");
                    }
                }
            }

            if (tags.containsKey("62")) {
                Map<String, String> sub62 = parseTlv(tags.get("62"));
                if (sub62.containsKey("08")) {
                    data.memo = sub62.get("08");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            data.isValid = false;
        }

        return data;
    }

    private static Map<String, String> parseTlv(String input) {
        Map<String, String> map = new HashMap<>();
        try {
            byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
            int index = 0;
            while (index + 4 <= bytes.length) {
                String tag = new String(bytes, index, 2, StandardCharsets.UTF_8);
                int length = Integer.parseInt(new String(bytes, index + 2, 2, StandardCharsets.UTF_8));
                index += 4;

                if (index + length <= bytes.length) {
                    String value = new String(bytes, index, length, StandardCharsets.UTF_8);
                    map.put(tag, value);
                    index += length;
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            map.clear();
        }
        return map;
    }

    public static String getBankNameFromBin(String bin) {
        if (bin == null) return "Unknown Bank";
        switch (bin) {
            case "970436": return "Vietcombank";
            case "970415": return "VietinBank";
            case "970418": return "BIDV";
            case "970405": return "Agribank";
            case "970407": return "Techcombank";
            case "970416": return "ACB";
            case "970422": return "MBBank";
            case "970423": return "TPBank";
            case "970403": return "Sacombank";
            case "970432": return "VPBank";
            case "970448": return "OCB";
            case "970428": return "Nam A Bank";
            case "970441": return "VIB";
            case "970437": return "HDBank";
            case "970454": return "VietCapitalBank";
            case "970429": return "SCB";
            case "970443": return "SHB";
            case "970438": return "BaoVietBank";
            case "970414": return "OceanBank";
            case "970439": return "PublicBank";
            case "970440": return "SeABank";
            case "970412": return "PVcomBank";
            case "970426": return "MSB";
            default: return "Ngân hàng (" + bin + ")";
        }
    }
}