import com.opencsv.CSVWriter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class EtherscanWalletScraper {

    public static String API_KEY;
    public static String WALLET_ADDRESS;

    private static final String BASE_URL = "https://api.etherscan.io/api?module=account&action=";

    public static void main(String[] args) {
        loadConfig();

        if (API_KEY == null || WALLET_ADDRESS == null) {
            System.err.println("Missing API key or wallet address in config.properties.");
            return;
        }

        List<String[]> allTxs = new ArrayList<>();
        allTxs.add(getCsvHeader());

        String[][] txTypes = {
                {"txlist", "ETH Transfer"},
                {"txlistinternal", "Internal Tx"},
                {"tokentx", "ERC-20 Transfer"},
                {"tokennfttx", "ERC-721 Transfer"},
                {"token1155tx", "ERC-1155 Transfer"}
        };

        for (String[] type : txTypes) {
            try {
                String response = getApiResponse(type[0]);
                if (response == null) {
                    System.out.println("No data for: " + type[1]);
                    continue;
                }
                allTxs.addAll(parseTransactions(response, type[1]));
            } catch (Exception e) {
                System.err.println("Error processing " + type[1] + ": " + e.getMessage());
            }
        }

        writeCsv(allTxs);
        System.out.println("✅ Done! Transactions saved to wallet_transactions.csv");
    }

    public static void loadConfig() {
        Properties prop = new Properties();
        try (FileInputStream input = new FileInputStream("config.properties")) {
            prop.load(input);
            API_KEY = prop.getProperty("apiKey");
            WALLET_ADDRESS = prop.getProperty("walletAddress");
        } catch (IOException e) {
            System.err.println("Failed to load config.properties: " + e.getMessage());
        }
    }

    public static String getApiResponse(String action) {
        try {
            String url = BASE_URL + action + "&address=" + WALLET_ADDRESS + "&apikey=" + API_KEY;
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");

            if (conn.getResponseCode() != 200) {
                System.err.println("HTTP error: " + conn.getResponseCode());
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder json = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }

                JSONObject obj = new JSONObject(json.toString());
                if (obj.has("status") && obj.getString("status").equals("0")) {
                    System.err.println("API Error: " + obj.optString("result"));
                    return null;
                }

                return json.toString();
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
            return null;
        }
    }


    public static List<String[]> parseTransactions(String response, String type) throws Exception {
        if (response == null || response.isEmpty()) throw new IllegalArgumentException("Empty API response");

        List<String[]> rows = new ArrayList<>();
        JSONObject json = new JSONObject(response);
        if (!json.has("result")) return rows;

        JSONArray txs = json.getJSONArray("result");

        for (int i = 0; i < txs.length(); i++) {
            JSONObject tx = txs.getJSONObject(i);

            String hash = tx.optString("hash");
            String time = formatUnixTime(tx.optString("timeStamp"));
            String from = tx.optString("from");
            String to = tx.optString("to");
            String contract = tx.optString("contractAddress");
            String symbol = tx.optString("tokenSymbol", type.contains("ERC-") ? "NFT/Token" : "ETH");
            String tokenId = tx.optString("tokenID", "");
            String value = convertWeiToEth(tx.optString("value"));
            String gasFee = calculateGasFee(tx.optString("gasUsed"), tx.optString("gasPrice"));

            rows.add(new String[] {
                    hash, time, from, to, type, contract, symbol, tokenId, value, gasFee
            });
        }
        return rows;
    }

    public static void writeCsv(List<String[]> data) {
        try (CSVWriter writer = new CSVWriter(new FileWriter("wallet_transactions.csv"))) {
            writer.writeAll(data);
        } catch (IOException e) {
            System.err.println("Failed to write CSV: " + e.getMessage());
        }
    }

    public static String[] getCsvHeader() {
        return new String[] {
                "Transaction Hash", "Date & Time", "From Address", "To Address", "Transaction Type",
                "Asset Contract Address", "Asset Symbol / Name", "Token ID", "Value / Amount", "Gas Fee (ETH)"
        };
    }

    public static String convertWeiToEth(String wei) {
        try {
            return new BigDecimal(wei).divide(new BigDecimal("1000000000000000000")).toPlainString();
        } catch (Exception e) {
            return "0";
        }
    }

    public static String calculateGasFee(String gasUsed, String gasPrice) {
        try {
            BigDecimal gas = new BigDecimal(gasUsed);
            BigDecimal price = new BigDecimal(gasPrice);
            return gas.multiply(price).divide(new BigDecimal("1000000000000000000")).toPlainString();
        } catch (Exception e) {
            return "0";
        }
    }

    public static String formatUnixTime(String timestamp) {
        try {
            long time = Long.parseLong(timestamp) * 1000;
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(time));
        } catch (Exception e) {
            return "";
        }
    }
}
