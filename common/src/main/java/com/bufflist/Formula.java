package com.bufflist;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 说明文字里的等级公式：{=表达式}，按当前等级算出数值。
 * 变量 lvl（等级，从 1 起）、amp（=lvl-1）；支持 + - * / ^、括号、min(a,b)、max(a,b)。
 * 例："每秒恢复 {=lvl*0.5} 点生命" → III 级显示 "每秒恢复 1.5 点生命"。
 * 公式写错时原样保留，不影响其余文字。
 */
final class Formula {
    private static final Pattern P = Pattern.compile("\\{=([^}]+)}");
    private static final DecimalFormat NUM = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private Formula() {}

    static String expand(String s, int lvl) {
        if (s.indexOf("{=") < 0) return s;
        Matcher m = P.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String rep;
            try { rep = NUM.format(new Parser(m.group(1), lvl).parse()); }
            catch (RuntimeException e) { rep = m.group(0); }
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static final class Parser {
        private final String s;
        private final int lvl;
        private int i;

        Parser(String s, int lvl) { this.s = s.replace(" ", ""); this.lvl = lvl; }

        double parse() {
            double v = expr();
            if (i != s.length()) throw new IllegalArgumentException("trailing " + s.substring(i));
            return v;
        }

        private double expr() {
            double v = term();
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '+') { i++; v += term(); }
                else if (c == '-') { i++; v -= term(); }
                else break;
            }
            return v;
        }

        private double term() {
            double v = power();
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '*') { i++; v *= power(); }
                else if (c == '/') { i++; v /= power(); }
                else break;
            }
            return v;
        }

        private double power() {
            double v = unary();
            if (i < s.length() && s.charAt(i) == '^') { i++; v = Math.pow(v, power()); }
            return v;
        }

        private double unary() {
            if (i < s.length() && s.charAt(i) == '-') { i++; return -unary(); }
            return atom();
        }

        private double atom() {
            if (s.charAt(i) == '(') { i++; double v = expr(); expect(')'); return v; }
            int st = i;
            if (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.') {
                while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.')) i++;
                return Double.parseDouble(s.substring(st, i));
            }
            while (i < s.length() && Character.isLetter(s.charAt(i))) i++;
            String name = s.substring(st, i);
            switch (name) {
                case "lvl": return lvl;
                case "amp": return lvl - 1;
                case "min": case "max": {
                    expect('('); double a = expr(); expect(','); double b = expr(); expect(')');
                    return name.equals("min") ? Math.min(a, b) : Math.max(a, b);
                }
                default: throw new IllegalArgumentException("unknown " + name);
            }
        }

        private void expect(char c) {
            if (i >= s.length() || s.charAt(i) != c) throw new IllegalArgumentException("expected " + c);
            i++;
        }
    }
}
