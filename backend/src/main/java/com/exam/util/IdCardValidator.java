package com.exam.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 身份证号码校验工具类
 * <p>支持18位身份证号码的完整校验：长度、格式、出生日期合法性、校验位（GB 11643-1999）</p>
 */
public class IdCardValidator {

    /** 18位身份证正则：前17位数字，末位数字或X/x */
    private static final java.util.regex.Pattern IDCARD_PATTERN =
            java.util.regex.Pattern.compile("^\\d{17}[\\dXx]$");

    /** 校验位权重因子 */
    private static final int[] WEIGHT = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};

    /** 校验位对照表（模11取余后对应的校验码） */
    private static final char[] CHECK_CODE = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};

    /**
     * 完整校验身份证号码（含校验位算法）
     * @param idCard 身份证号码
     * @return true=合法 false=非法
     */
    public static boolean isValid(String idCard) {
        if (idCard == null || idCard.length() != 18) {
            return false;
        }
        if (!IDCARD_PATTERN.matcher(idCard).matches()) {
            return false;
        }
        // 校验出生日期
        if (!isValidBirthDate(idCard)) {
            return false;
        }
        // 校验校验位
        return verifyCheckCode(idCard);
    }

    /**
     * 校验出生日期是否合法
     */
    private static boolean isValidBirthDate(String idCard) {
        try {
            String dateStr = idCard.substring(6, 14); // yyyyMMdd
            LocalDate birth = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            // 不能是未来日期
            return !birth.isAfter(LocalDate.now());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 校验末位校验码
     */
    private static boolean verifyCheckCode(String idCard) {
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (idCard.charAt(i) - '0') * WEIGHT[i];
        }
        char expectedCheck = CHECK_CODE[sum % 11];
        return expectedCheck == Character.toUpperCase(idCard.charAt(17));
    }

    /**
     * 从身份证号提取性别（第17位奇数=男，偶数=女）
     * @return 1=男 2=女 null=无法判断
     */
    public static Integer extractGender(String idCard) {
        if (idCard == null || idCard.length() < 18) {
            return null;
        }
        char c = idCard.charAt(16);
        if (c < '0' || c > '9') {
            return null;
        }
        return ((c - '0') % 2 == 1) ? 1 : 2;
    }

    /**
     * 从身份证号提取出生日期
     * @return LocalDate 或 null
     */
    public static LocalDate extractBirthDate(String idCard) {
        if (idCard == null || idCard.length() < 14) {
            return null;
        }
        try {
            return LocalDate.parse(idCard.substring(6, 14), DateTimeFormatter.ofPattern("yyyyMMdd"));
        } catch (Exception e) {
            return null;
        }
    }
}
