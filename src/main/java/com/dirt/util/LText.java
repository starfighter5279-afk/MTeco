package com.dirt.util;

import com.dirt.DirtEconomy;
import org.bukkit.entity.Player;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.InflaterInputStream;

public class LText {

    private static final Pattern GOOGLE_DRIVE_PATTERN = Pattern.compile(
            "https?://drive\\.google\\.com/file/d/([a-zA-Z0-9_-]+)");
    private static final Pattern DROPBOX_PATTERN = Pattern.compile(
            "https?://(www\\.)?dropbox\\.com/");
    private static final Pattern ICLOUD_PATTERN = Pattern.compile(
            "https?://(www\\.)?icloud\\.com/iclouddrive/");
    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://\\S+", Pattern.CASE_INSENSITIVE);

    private static final int MAX_FILE_SIZE = 1_048_576;

    public static boolean isLink(String input) {
        return URL_PATTERN.matcher(input.trim()).matches();
    }

    public static boolean isCloudLink(String input) {
        String trimmed = input.trim();
        return GOOGLE_DRIVE_PATTERN.matcher(trimmed).find()
                || DROPBOX_PATTERN.matcher(trimmed).find()
                || ICLOUD_PATTERN.matcher(trimmed).find();
    }

    public static void handleInput(DirtEconomy plugin, Player player, String input,
                                   Consumer<String> onText, Runnable onFail) {
        if (isLink(input)) {
            player.sendMessage("\u00a7eDetected link. Extracting text...");
            plugin.getPlatformScheduler().runAsync(() -> {
                try {
                    String text = extractText(input);
                    if (text == null || text.isBlank()) {
                        sync(plugin, player, () -> {
                            player.sendMessage("\u00a7cNo text could be extracted from the link.");
                            if (onFail != null) onFail.run();
                        });
                        return;
                    }
                    sync(plugin, player, () -> {
                        player.sendMessage("\u00a7aText extracted successfully! (" + text.length() + " characters)");
                        onText.accept(text);
                    });
                } catch (Exception ex) {
                    sync(plugin, player, () -> {
                        player.sendMessage("\u00a7cFailed to extract text: " + ex.getMessage());
                        if (onFail != null) onFail.run();
                    });
                }
            });
        } else {
            onText.accept(input.trim());
        }
    }

    public static void handleInput(DirtEconomy plugin, Player player, String input, Consumer<String> onText) {
        handleInput(plugin, player, input, onText, null);
    }

    private static void sync(DirtEconomy plugin, Player player, Runnable r) {
        plugin.getPlatformScheduler().runAtEntity(player, r);
    }

    public static String extractText(String link) throws Exception {
        String trimmed = link.trim();
        if (isCloudLink(trimmed)) {
            String directUrl = convertToDirectUrl(trimmed);
            if (directUrl == null) {
                throw new Exception("Unsupported cloud link format.");
            }
            return downloadAndExtract(directUrl);
        }
        return downloadAndExtract(trimmed);
    }

    public static String extractTextFromLink(String link) throws Exception {
        return extractText(link);
    }

    private static String convertToDirectUrl(String link) {
        Matcher gdrive = GOOGLE_DRIVE_PATTERN.matcher(link);
        if (gdrive.find()) {
            return "https://drive.google.com/uc?export=download&id=" + gdrive.group(1);
        }

        if (DROPBOX_PATTERN.matcher(link).find()) {
            if (link.contains("dl=0")) {
                return link.replace("dl=0", "dl=1");
            } else if (link.contains("?")) {
                return link + "&dl=1";
            } else {
                return link + "?dl=1";
            }
        }

        if (ICLOUD_PATTERN.matcher(link).find()) {
            return link;
        }

        return null;
    }

    private static String downloadAndExtract(String urlStr) throws Exception {
        URL url = new URI(urlStr).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);
        conn.setInstanceFollowRedirects(true);

        String contentType = conn.getContentType();
        byte[] data;
        try (InputStream is = conn.getInputStream();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int read;
            while ((read = is.read(buf)) != -1) {
                bos.write(buf, 0, read);
                if (bos.size() > MAX_FILE_SIZE) {
                    throw new Exception("File too large (max 1MB).");
                }
            }
            data = bos.toByteArray();
        } finally {
            conn.disconnect();
        }

        if (data.length == 0) {
            throw new Exception("Downloaded file is empty.");
        }

        if (isPdf(data, contentType)) {
            return extractTextFromPdfBytes(data);
        }

        String charset = detectCharset(contentType);
        String text = new String(data, charset);

        if (isHtml(contentType, text)) {
            text = stripHtml(text);
        }

        return text.trim();
    }

    private static boolean isPdf(byte[] data, String contentType) {
        if (contentType != null && contentType.toLowerCase().contains("application/pdf")) return true;
        return data.length >= 4 && data[0] == '%' && data[1] == 'P' && data[2] == 'D' && data[3] == 'F';
    }

    private static boolean isHtml(String contentType, String text) {
        if (contentType != null && contentType.toLowerCase().contains("text/html")) return true;
        String sample = text.substring(0, Math.min(500, text.length())).toLowerCase();
        return sample.contains("<html") || sample.contains("<!doctype html");
    }

    private static String detectCharset(String contentType) {
        if (contentType != null) {
            String lower = contentType.toLowerCase();
            int idx = lower.indexOf("charset=");
            if (idx >= 0) {
                String charset = contentType.substring(idx + 8).trim();
                int semi = charset.indexOf(';');
                if (semi >= 0) charset = charset.substring(0, semi);
                charset = charset.replace("\"", "").replace("'", "").trim();
                if (!charset.isEmpty()) return charset;
            }
        }
        return "UTF-8";
    }

    private static String stripHtml(String html) {
        String cleaned = html.replaceAll("(?is)<script[^>]*>.*?</script>", " ");
        cleaned = cleaned.replaceAll("(?is)<style[^>]*>.*?</style>", " ");
        cleaned = cleaned.replaceAll("(?i)<br\\s*/?>", "\n");
        cleaned = cleaned.replaceAll("(?i)<?(p|div|h[1-6]|li|tr)\\b[^>]*>", "\n");
        cleaned = cleaned.replaceAll("<[^>]+>", "");
        cleaned = cleaned.replaceAll("&", "&");
        cleaned = cleaned.replaceAll("<", "<");
        cleaned = cleaned.replaceAll(">", ">");
        cleaned = cleaned.replaceAll("&quot;", "\"");
        cleaned = cleaned.replaceAll("'", "'");
        cleaned = cleaned.replaceAll("'", "'");
        cleaned = cleaned.replaceAll("&nbsp;", " ");
        cleaned = cleaned.replaceAll("[ \t]+", " ");
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");
        return cleaned.trim();
    }

    private static String extractTextFromPdfBytes(byte[] pdf) throws Exception {
        String content = new String(pdf, "ISO-8859-1");
        List<String> extractedTexts = new ArrayList<>();

        List<byte[]> streams = extractStreams(pdf, content);
        for (byte[] stream : streams) {
            String text = extractTextFromStream(new String(stream, "ISO-8859-1"));
            if (text != null && !text.isBlank()) {
                extractedTexts.add(text);
            }
        }

        if (extractedTexts.isEmpty()) {
            String fallback = extractRawTextStrings(content);
            if (fallback != null && !fallback.isBlank()) {
                return fallback.trim();
            }
            throw new Exception("Could not extract any readable text from the PDF.");
        }

        return String.join("\n", extractedTexts).trim();
    }

    private static List<byte[]> extractStreams(byte[] pdf, String content) {
        List<byte[]> streams = new ArrayList<>();
        int idx = 0;
        while (true) {
            int start = content.indexOf("stream\r\n", idx);
            if (start == -1) {
                start = content.indexOf("stream\n", idx);
            }
            if (start == -1) break;

            start = content.indexOf('\n', start) + 1;

            int end = content.indexOf("endstream", start);
            if (end == -1) break;

            byte[] raw = new byte[end - start];
            System.arraycopy(pdf, start, raw, 0, raw.length);

            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(raw);
                InflaterInputStream inflater = new InflaterInputStream(bis);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int read;
                while ((read = inflater.read(buf)) != -1) {
                    bos.write(buf, 0, read);
                }
                streams.add(bos.toByteArray());
            } catch (Exception e) {
                streams.add(raw);
            }

            idx = end + 9;
        }
        return streams;
    }

    private static String extractTextFromStream(String stream) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        boolean inText = false;

        while (i < stream.length()) {
            if (i + 1 < stream.length() && stream.charAt(i) == 'B' && stream.charAt(i + 1) == 'T') {
                if (i == 0 || !Character.isLetterOrDigit(stream.charAt(i - 1))) {
                    inText = true;
                    i += 2;
                    continue;
                }
            }
            if (i + 1 < stream.length() && stream.charAt(i) == 'E' && stream.charAt(i + 1) == 'T') {
                if (i == 0 || !Character.isLetterOrDigit(stream.charAt(i - 1))) {
                    inText = false;
                    if (!result.isEmpty() && result.charAt(result.length() - 1) != '\n') {
                        result.append('\n');
                    }
                    i += 2;
                    continue;
                }
            }

            if (!inText) {
                i++;
                continue;
            }

            if (stream.charAt(i) == '(') {
                int depth = 1;
                int start = i + 1;
                i++;
                while (i < stream.length() && depth > 0) {
                    if (stream.charAt(i) == '\\') {
                        i += 2;
                        continue;
                    }
                    if (stream.charAt(i) == '(') depth++;
                    if (stream.charAt(i) == ')') depth--;
                    i++;
                }
                String raw = stream.substring(start, i - 1);
                result.append(unescapePdfString(raw));
            } else if (stream.charAt(i) == '<') {
                int end = stream.indexOf('>', i + 1);
                if (end != -1) {
                    String hex = stream.substring(i + 1, end).replaceAll("\\s", "");
                    result.append(hexToString(hex));
                    i = end + 1;
                } else {
                    i++;
                }
            } else if (i + 1 < stream.length() && stream.charAt(i) == 'T') {
                char next = stream.charAt(i + 1);
                if (next == '*' || next == 'd' || next == 'D') {
                    result.append('\n');
                }
                i++;
            } else if (stream.charAt(i) == '\'') {
                result.append('\n');
                i++;
            } else {
                i++;
            }
        }

        return result.toString();
    }

    private static String unescapePdfString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case 'n' -> { sb.append('\n'); i++; }
                    case 'r' -> { sb.append('\r'); i++; }
                    case 't' -> { sb.append('\t'); i++; }
                    case 'b' -> { sb.append('\b'); i++; }
                    case 'f' -> { sb.append('\f'); i++; }
                    case '(' -> { sb.append('('); i++; }
                    case ')' -> { sb.append(')'); i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    default -> {
                        if (next >= '0' && next <= '7') {
                            StringBuilder octal = new StringBuilder();
                            octal.append(next);
                            i++;
                            for (int j = 0; j < 2 && i + 1 < s.length(); j++) {
                                char c = s.charAt(i + 1);
                                if (c >= '0' && c <= '7') {
                                    octal.append(c);
                                    i++;
                                } else break;
                            }
                            sb.append((char) Integer.parseInt(octal.toString(), 8));
                        } else {
                            sb.append(next);
                            i++;
                        }
                    }
                }
            } else {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    private static String hexToString(String hex) {
        if (hex.length() % 2 != 0) hex += "0";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            int val = Integer.parseInt(hex.substring(i, i + 2), 16);
            if (val > 0) sb.append((char) val);
        }
        return sb.toString();
    }

    private static String extractRawTextStrings(String content) {
        StringBuilder result = new StringBuilder();
        Pattern pattern = Pattern.compile("\\(([^)]{2,})\\)\\s*Tj");
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            String text = unescapePdfString(matcher.group(1));
            if (text.chars().allMatch(c -> c >= 32 && c < 127 || c == '\n' || c == '\r' || c == '\t')) {
                result.append(text).append(" ");
            }
        }
        return result.toString();
    }
}