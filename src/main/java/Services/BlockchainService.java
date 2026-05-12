package Services;

import DataBase.MyConnection;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Central blockchain gateway service.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Health-check the RPC node, deployed contract, and signer key (sub-task 7.2).</li>
 *   <li>Detect dev-mode via {@code APP_BLOCKCHAIN_DEV_MODE} (sub-task 7.1).</li>
 *   <li>Resolve wallet blockchain addresses from the DB (sub-task 7.4).</li>
 *   <li>Dispatch mint / transfer / retire operations (sub-task 7.5).</li>
 *   <li>Route to simulated or live execution (sub-task 7.6).</li>
 *   <li>Build simulated results in dev mode (sub-task 7.7).</li>
 *   <li>Invoke the Node.js contract script for live operations (sub-task 7.8).</li>
 * </ul>
 *
 * <p>All environment variables are read via {@link System#getenv(String)}.
 * Database access uses {@link MyConnection#getConnection()} (plain JDBC).
 *
 * Requirements: 1.1–1.6, 2.1–2.7, 3.1–3.4, 4.1–4.5
 */
public class BlockchainService {

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final EventListenerService eventListenerService;
    private final TransactionService   transactionService;

    // -------------------------------------------------------------------------
    // Constants
    // -------------------------------------------------------------------------

    /** Regex pattern for a valid Ethereum address: 0x followed by 40 hex chars. */
    private static final Pattern ETH_ADDRESS_PATTERN =
            Pattern.compile("^0x[a-fA-F0-9]{40}$");

    /** HTTP connect/read timeout for RPC health checks (milliseconds). */
    private static final int RPC_TIMEOUT_MS = 5_000;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    /**
     * Construct a {@code BlockchainService} with the given collaborators.
     *
     * @param eventListenerService the event-listener service used for dev-mode simulation
     * @param transactionService   the transaction-lifecycle service
     */
    public BlockchainService(EventListenerService eventListenerService,
                             TransactionService transactionService) {
        this.eventListenerService = eventListenerService;
        this.transactionService   = transactionService;
    }

    // =========================================================================
    // Sub-task 7.1 — isDevModeEnabled()
    // =========================================================================

    /**
     * Return {@code true} when the {@code APP_BLOCKCHAIN_DEV_MODE} environment
     * variable is set to one of {@code 1}, {@code true}, {@code yes}, or
     * {@code on} (case-insensitive).
     *
     * @return {@code true} if dev mode is active, {@code false} otherwise
     * Requirements: 2.1
     */
    public boolean isDevModeEnabled() {
        String value = System.getenv("APP_BLOCKCHAIN_DEV_MODE");
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        String v = value.trim().toLowerCase(Locale.ROOT);
        return v.equals("1") || v.equals("true") || v.equals("yes") || v.equals("on");
    }

    // =========================================================================
    // Sub-task 7.2 — getHealthStatus()
    // =========================================================================

    /**
     * Return a structured health-status map with three sub-checks:
     * {@code rpc}, {@code contract}, and {@code signer}.
     *
     * <p>Each sub-map contains at minimum a {@code ready} (Boolean) key, and
     * optionally {@code url}, {@code address}, or {@code issue} (String) keys.
     *
     * @return health-status map
     * Requirements: 1.1, 1.2, 1.3, 1.4, 1.6
     */
    public Map<String, Map<String, Object>> getHealthStatus() {
        Map<String, Map<String, Object>> status = new LinkedHashMap<>();

        String rpcUrl             = System.getenv("CHAIN_RPC_URL");
        String contractAddress    = System.getenv("CHAIN_CARBON_TOKEN_ADDRESS");
        String privateKey         = System.getenv("CHAIN_PRIVATE_KEY");

        // ----- RPC check -----
        Map<String, Object> rpcCheck = new LinkedHashMap<>();
        rpcCheck.put("url", rpcUrl != null ? rpcUrl : "");
        try {
            String rpcBody = "{\"jsonrpc\":\"2.0\",\"method\":\"eth_blockNumber\",\"params\":[],\"id\":1}";
            String rpcResponse = httpPost(rpcUrl, rpcBody);
            rpcCheck.put("ready", rpcResponse != null && !rpcResponse.trim().isEmpty());
        } catch (Exception e) {
            rpcCheck.put("ready", false);
            rpcCheck.put("issue", e.getMessage());
        }
        status.put("rpc", rpcCheck);

        // ----- Contract check -----
        Map<String, Object> contractCheck = new LinkedHashMap<>();
        contractCheck.put("address", contractAddress != null ? contractAddress : "");
        try {
            String contractBody =
                    "{\"jsonrpc\":\"2.0\",\"method\":\"eth_getCode\",\"params\":[\"" +
                    (contractAddress != null ? contractAddress : "") +
                    "\",\"latest\"],\"id\":1}";
            String contractResponse = httpPost(rpcUrl, contractBody);
            String result = extractJsonStringField(contractResponse, "result");
            if (result == null || result.isEmpty() || result.equals("0x")) {
                contractCheck.put("ready", false);
                contractCheck.put("issue", "contract not deployed at that address");
            } else {
                contractCheck.put("ready", true);
            }
        } catch (Exception e) {
            contractCheck.put("ready", false);
            contractCheck.put("issue", e.getMessage());
        }
        status.put("contract", contractCheck);

        // ----- Signer check -----
        Map<String, Object> signerCheck = new LinkedHashMap<>();
        boolean signerReady = privateKey != null && !privateKey.trim().isEmpty();
        signerCheck.put("ready", signerReady);
        if (!signerReady) {
            signerCheck.put("issue", "CHAIN_PRIVATE_KEY is not set or empty");
        }
        status.put("signer", signerCheck);

        return status;
    }

    // =========================================================================
    // Sub-task 7.3 — preflightCheck()
    // =========================================================================

    /**
     * Call {@link #getHealthStatus()} and throw a {@link RuntimeException} if
     * any sub-check reports {@code ready = false}.
     *
     * @throws RuntimeException with a descriptive message listing failed checks
     * Requirements: 1.5
     */
    public void preflightCheck() {
        Map<String, Map<String, Object>> health = getHealthStatus();
        List<String> failures = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : health.entrySet()) {
            String checkName = entry.getKey();
            Map<String, Object> checkResult = entry.getValue();
            Object ready = checkResult.get("ready");
            if (!Boolean.TRUE.equals(ready)) {
                String issue = (String) checkResult.get("issue");
                String msg = checkName + " check failed";
                if (issue != null && !issue.isEmpty()) {
                    msg += ": " + issue;
                }
                failures.add(msg);
            }
        }

        if (!failures.isEmpty()) {
            throw new RuntimeException(
                "Blockchain preflight check failed — " + String.join("; ", failures));
        }
    }

    // =========================================================================
    // Sub-task 7.4 — requireWalletBlockchainAddress(int walletId)
    // =========================================================================

    /**
     * Look up the {@code blockchain_address} for the given wallet from the DB.
     *
     * <ul>
     *   <li>If the address is present and matches {@code 0x[a-fA-F0-9]{40}}, return it.</li>
     *   <li>If missing/invalid and dev mode is enabled: return a deterministic address
     *       derived from {@code sha256("dev:wallet:" + walletId)}.</li>
     *   <li>If missing/invalid and dev mode is disabled: throw {@link RuntimeException}.</li>
     * </ul>
     *
     * @param walletId wallet primary key
     * @return valid Ethereum address string
     * @throws RuntimeException if the wallet has no address and dev mode is disabled
     * Requirements: 2.7, 4.4
     */
    public String requireWalletBlockchainAddress(int walletId) {
        String address = fetchBlockchainAddress(walletId);

        if (address != null && ETH_ADDRESS_PATTERN.matcher(address).matches()) {
            return address;
        }

        // Address is missing or invalid
        if (isDevModeEnabled()) {
            return generateDevAddress(walletId);
        }

        throw new RuntimeException("Wallet " + walletId + " has no blockchain address");
    }

    // =========================================================================
    // Sub-task 7.5 — mintBatch, transferBatch, retireBatch
    // =========================================================================

    /**
     * Mint carbon credits for a batch.
     *
     * @param walletId        recipient wallet identifier
     * @param batchId         carbon credit batch identifier
     * @param amountBaseUnits on-chain amount in base units
     * @param metadata        additional payload entries
     * @return result map from {@link #submitTransaction(Map)}
     * Requirements: 4.1
     */
    public Map<String, Object> mintBatch(int walletId, int batchId,
                                         String amountBaseUnits,
                                         Map<String, Object> metadata) {
        String walletAddress = requireWalletBlockchainAddress(walletId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("method", "mint");
        payload.put("walletAddress", walletAddress);
        payload.put("wallet_id", walletId);
        payload.put("batchId", batchId);
        payload.put("amountBaseUnits", amountBaseUnits);
        if (metadata != null) {
            payload.putAll(metadata);
        }

        return submitTransaction(payload);
    }

    /**
     * Transfer carbon credits between wallets.
     *
     * @param fromWalletId    seller wallet identifier
     * @param toWalletId      buyer wallet identifier
     * @param batchId         carbon credit batch identifier
     * @param amountBaseUnits on-chain amount in base units
     * @param metadata        additional payload entries
     * @return result map from {@link #submitTransaction(Map)}
     * Requirements: 4.2
     */
    public Map<String, Object> transferBatch(int fromWalletId, int toWalletId,
                                              int batchId, String amountBaseUnits,
                                              Map<String, Object> metadata) {
        String fromAddress = requireWalletBlockchainAddress(fromWalletId);
        String toAddress   = requireWalletBlockchainAddress(toWalletId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("method", "transfer");
        payload.put("fromAddress", fromAddress);
        payload.put("toAddress", toAddress);
        payload.put("from_wallet_id", fromWalletId);
        payload.put("to_wallet_id", toWalletId);
        payload.put("batchId", batchId);
        payload.put("amountBaseUnits", amountBaseUnits);
        if (metadata != null) {
            payload.putAll(metadata);
        }

        return submitTransaction(payload);
    }

    /**
     * Retire (burn) carbon credits.
     *
     * @param walletId        retiring wallet identifier
     * @param batchId         carbon credit batch identifier
     * @param amountBaseUnits on-chain amount in base units
     * @param reason          human-readable retirement reason
     * @param metadata        additional payload entries
     * @return result map from {@link #submitTransaction(Map)}
     * Requirements: 4.3
     */
    public Map<String, Object> retireBatch(int walletId, int batchId,
                                            String amountBaseUnits, String reason,
                                            Map<String, Object> metadata) {
        String walletAddress = requireWalletBlockchainAddress(walletId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("method", "burn");
        payload.put("walletAddress", walletAddress);
        payload.put("wallet_id", walletId);
        payload.put("batchId", batchId);
        payload.put("amountBaseUnits", amountBaseUnits);
        payload.put("reason", reason);
        if (metadata != null) {
            payload.putAll(metadata);
        }

        return submitTransaction(payload);
    }

    // =========================================================================
    // Sub-task 7.6 — submitTransaction routing
    // =========================================================================

    /**
     * Route a transaction payload to either the simulated path (dev mode) or
     * the live Node.js script path.
     *
     * @param payload operation payload map
     * @return result map
     * Requirements: 4.5
     */
    public Map<String, Object> submitTransaction(Map<String, Object> payload) {
        if (isDevModeEnabled()) {
            return buildSimulatedResult(payload);
        }
        preflightCheck();
        return runNodeScript(payload);
    }

    // =========================================================================
    // Sub-task 7.7 — buildSimulatedResult(Map payload)
    // =========================================================================

    /**
     * Build a simulated blockchain result without invoking the Node.js script.
     *
     * <ol>
     *   <li>Generate {@code txHash = "dev_" + randomHex(8)}.</li>
     *   <li>If {@code transaction_id} is present and &gt; 0, call
     *       {@code transactionService.markSubmitted(transactionId, txHash, 0, 0)}.</li>
     *   <li>Convert {@code amountBaseUnits} to credits via
     *       {@link CreditUnitConverter#fromBaseUnits(String)}.</li>
     *   <li>Dispatch to the appropriate {@code eventListenerService.simulate*()} method.</li>
     *   <li>Return a map with {@code txHash}, {@code tx_hash}, {@code blockNumber=0},
     *       {@code simulated=true}.</li>
     * </ol>
     *
     * @param payload operation payload map
     * @return simulated result map
     * Requirements: 2.2, 2.3, 2.4, 2.5, 2.6
     */
    public Map<String, Object> buildSimulatedResult(Map<String, Object> payload) {
        // 1. Generate dev tx hash
        String txHash = "dev_" + randomHex(8);

        // 2. Mark transaction as submitted if transaction_id is present
        Object txIdObj = payload.get("transaction_id");
        if (txIdObj instanceof Number) {
            int transactionId = ((Number) txIdObj).intValue();
            if (transactionId > 0) {
                transactionService.markSubmitted(transactionId, txHash, 0, 0);
            }
        }

        // 3. Convert amountBaseUnits to credits
        String amountBaseUnits = objectToString(payload.get("amountBaseUnits"));
        double amount = CreditUnitConverter.fromBaseUnits(amountBaseUnits);

        // 4. Dispatch based on method
        String method = objectToString(payload.get("method"));
        int batchId = toInt(payload.get("batchId"));

        if ("mint".equals(method)) {
            int walletId = toInt(payload.get("wallet_id"));
            eventListenerService.simulateMint(txHash, batchId, walletId, amount);

        } else if ("transfer".equals(method)) {
            int fromWalletId = toInt(payload.get("from_wallet_id"));
            int toWalletId   = toInt(payload.get("to_wallet_id"));
            eventListenerService.simulateTransfer(txHash, batchId, fromWalletId, toWalletId, amount);

        } else if ("burn".equals(method)) {
            int walletId = toInt(payload.get("wallet_id"));
            eventListenerService.simulateRetire(txHash, batchId, walletId, amount);
        }

        // 5. Return result map
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("txHash", txHash);
        result.put("tx_hash", txHash);
        result.put("blockNumber", 0);
        result.put("simulated", true);
        return result;
    }

    // =========================================================================
    // Sub-task 7.8 — runNodeScript(Map payload)
    // =========================================================================

    /**
     * Invoke the Node.js contract script with the given payload.
     *
     * <ol>
     *   <li>Serialize {@code payload} to JSON and write to a temp file.</li>
     *   <li>Execute {@code node symfonysrc/blockchain/scripts/carbon-batch-token.js <tempfile>}
     *       via {@link ProcessBuilder}.</li>
     *   <li>On exit code 0: parse stdout as JSON and return as {@code Map<String,Object>}.</li>
     *   <li>On non-zero exit or parse failure: throw {@link RuntimeException} with raw output
     *       and exit code.</li>
     *   <li>Delete the temp file in a {@code finally} block.</li>
     * </ol>
     *
     * @param payload operation payload map
     * @return parsed result map from the Node.js script
     * @throws RuntimeException on non-zero exit code or unparseable output
     * Requirements: 3.1, 3.2, 3.3, 3.4
     */
    public Map<String, Object> runNodeScript(Map<String, Object> payload) {
        File tempFile = null;
        try {
            // 1. Write payload JSON to temp file
            tempFile = File.createTempFile("blockchain_payload_", ".json");
            String json = TransactionService.mapToJson(payload);
            try (FileWriter fw = new FileWriter(tempFile, StandardCharsets.UTF_8)) {
                fw.write(json);
            }

            // 2. Execute Node.js script
            ProcessBuilder pb = new ProcessBuilder(
                "node",
                "symfonysrc/blockchain/scripts/carbon-batch-token.js",
                tempFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // 3. Capture stdout
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }

            int exitCode = process.waitFor();
            String rawOutput = output.toString().trim();

            if (exitCode != 0) {
                throw new RuntimeException(
                    "Node.js script exited with code " + exitCode + ". Output: " + rawOutput);
            }

            // 4. Parse stdout as JSON
            Map<String, Object> result = parseJsonToMap(rawOutput);
            if (result == null) {
                throw new RuntimeException(
                    "Node.js script output could not be parsed as JSON. Output: " + rawOutput);
            }
            return result;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to run Node.js blockchain script: " + e.getMessage(), e);
        } finally {
            // 5. Delete temp file
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Fetch the raw {@code blockchain_address} value from the {@code wallet} table.
     *
     * @param walletId wallet primary key
     * @return address string, or {@code null} if not found
     */
    private String fetchBlockchainAddress(int walletId) {
        String sql = "SELECT blockchain_address FROM wallet WHERE id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = MyConnection.getConnection();
            if (conn == null) return null;
            ps = conn.prepareStatement(sql);
            ps.setInt(1, walletId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString(1);
            }
            return null;
        } catch (SQLException e) {
            System.err.println("[BlockchainService] fetchBlockchainAddress failed: " + e.getMessage());
            return null;
        } finally {
            closeQuietly(rs);
            closeQuietly(ps);
            closeQuietly(conn);
        }
    }

    /**
     * Generate a deterministic dev-mode Ethereum address for the given wallet.
     *
     * <p>Algorithm: {@code "0x" + sha256("dev:wallet:" + walletId).substring(0, 40)}
     *
     * @param walletId wallet primary key
     * @return deterministic {@code 0x}-prefixed address
     * Requirements: 2.7
     */
    private String generateDevAddress(int walletId) {
        try {
            String input = "dev:wallet:" + walletId;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            // Take first 40 hex characters (20 bytes)
            return "0x" + hex.substring(0, 40);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Generate a random hex string of the given byte length.
     *
     * @param byteCount number of random bytes (each produces 2 hex chars)
     * @return lowercase hex string of length {@code byteCount * 2}
     */
    private String randomHex(int byteCount) {
        byte[] bytes = new byte[byteCount];
        new Random().nextBytes(bytes);
        StringBuilder sb = new StringBuilder(byteCount * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Send an HTTP POST request with a JSON body and return the response body as a string.
     *
     * @param urlString target URL
     * @param jsonBody  request body
     * @return response body string
     * @throws Exception on connection failure or timeout
     */
    private String httpPost(String urlString, String jsonBody) throws Exception {
        if (urlString == null || urlString.trim().isEmpty()) {
            throw new IllegalArgumentException("RPC URL is not configured (CHAIN_RPC_URL is empty)");
        }
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(RPC_TIMEOUT_MS);
        conn.setReadTimeout(RPC_TIMEOUT_MS);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode >= 200 && responseCode < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        if (is == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Extract a string field value from a minimal JSON response.
     *
     * <p>Handles the pattern {@code "result":"<value>"} or {@code "result": "<value>"}.
     * Returns {@code null} if the field is not found.
     *
     * @param json      JSON string to search
     * @param fieldName field name to extract
     * @return field value string, or {@code null}
     */
    private String extractJsonStringField(String json, String fieldName) {
        if (json == null || json.isEmpty()) return null;
        // Match: "fieldName":"value" or "fieldName": "value"
        String pattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    /**
     * Parse a minimal JSON object string into a {@code Map<String,Object>}.
     *
     * <p>Supports string, number, boolean, and null values at the top level.
     * Returns {@code null} if the input is not a valid JSON object.
     *
     * @param json JSON string
     * @return parsed map, or {@code null} on failure
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) return null;
        String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null;

        Map<String, Object> result = new LinkedHashMap<>();
        // Strip outer braces
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        if (inner.isEmpty()) return result;

        // Simple tokenizer: split on commas that are not inside strings
        List<String> pairs = splitJsonPairs(inner);
        for (String pair : pairs) {
            int colonIdx = pair.indexOf(':');
            if (colonIdx < 0) continue;
            String rawKey   = pair.substring(0, colonIdx).trim();
            String rawValue = pair.substring(colonIdx + 1).trim();

            // Unquote key
            String key = unquoteJsonString(rawKey);
            if (key == null) continue;

            // Parse value
            Object value = parseJsonValue(rawValue);
            result.put(key, value);
        }
        return result;
    }

    /** Split a JSON object body into key:value pair strings, respecting quoted strings. */
    private List<String> splitJsonPairs(String s) {
        List<String> pairs = new ArrayList<>();
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escape) { escape = false; continue; }
            if (c == '\\' && inString) { escape = true; continue; }
            if (c == '"') { inString = !inString; continue; }
            if (!inString) {
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') depth--;
                else if (c == ',' && depth == 0) {
                    pairs.add(s.substring(start, i).trim());
                    start = i + 1;
                }
            }
        }
        String last = s.substring(start).trim();
        if (!last.isEmpty()) pairs.add(last);
        return pairs;
    }

    /** Parse a JSON value token into a Java object. */
    private Object parseJsonValue(String raw) {
        if (raw == null || raw.equals("null")) return null;
        if (raw.equals("true"))  return Boolean.TRUE;
        if (raw.equals("false")) return Boolean.FALSE;
        if (raw.startsWith("\"") && raw.endsWith("\"")) {
            return unquoteJsonString(raw);
        }
        // Try number
        try {
            if (raw.contains(".")) return Double.parseDouble(raw);
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {}
        // Nested object — return as string for simplicity
        return raw;
    }

    /** Remove surrounding quotes from a JSON string token and unescape basic sequences. */
    private String unquoteJsonString(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (t.startsWith("\"") && t.endsWith("\"") && t.length() >= 2) {
            return t.substring(1, t.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t");
        }
        return null;
    }

    /** Convert an Object to its string representation, returning empty string for null. */
    private String objectToString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    /** Convert an Object to int, returning 0 for null or non-numeric values. */
    private int toInt(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(obj.toString()); } catch (NumberFormatException e) { return 0; }
    }

    // -------------------------------------------------------------------------
    // JDBC close helpers
    // -------------------------------------------------------------------------

    private static void closeQuietly(java.sql.Connection c) {
        if (c != null) { try { c.close(); } catch (SQLException ignored) {} }
    }

    private static void closeQuietly(java.sql.Statement s) {
        if (s != null) { try { s.close(); } catch (SQLException ignored) {} }
    }

    private static void closeQuietly(java.sql.ResultSet rs) {
        if (rs != null) { try { rs.close(); } catch (SQLException ignored) {} }
    }
}
