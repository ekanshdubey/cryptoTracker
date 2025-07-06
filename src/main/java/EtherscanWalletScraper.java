import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.*;
import java.util.*;
import com.opencsv.CSVWriter;
import org.json.*;

public class EtherscanWalletScraper {

    private static final String API_KEY = "<Your API KEY>";
    private static final String WALLET_ADDRESS = "0xfb50526f49894b78541b776f5aaefe43e3bd8590"; // Replace

    public static void main(String[] args) throws IOException {
        List<String[]> allData = new ArrayList<>();

        fetchAndAppend("txlist", "Normal Transaction", allData);
        fetchAndAppend("txlistinternal", "Internal Transaction", allData);
        fetchAndAppend("tokentx", "ERC20 Token Transfer", allData);
        fetchAndAppend("tokennfttx", "ERC721 NFT Transfer", allData);
        fetchAndAppend("token1155tx", "ERC1155 Token Transfer", allData);

        writeCsv("wallet_transactions.csv", allData);
        System.out.println("Done! CSV saved.");
    }

    private static void fetchAndAppend(String action, String category, List<String[]> data) throws IOException {
        String urlStr = String.format(
                "https://api.etherscan.io/api?module=account&action=%s&address=%s&startblock=0&endblock=99999999&page=1&offset=10000&sort=asc&apikey=%s",
                action, WALLET_ADDRESS, API_KEY);

        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }

        in.close();
        con.disconnect();

        JSONObject response = new JSONObject(content.toString());
        if (!response.getString("status").equals("1")) {
            System.err.println("Error fetching " + category + ": " + response.getString("message"));
            return;
        }

        JSONArray txArray = response.getJSONArray("result");
        for (int i = 0; i < txArray.length(); i++) {
            JSONObject tx = txArray.getJSONObject(i);
            data.add(parseTransaction(tx, category));
        }
    }

    private static String[] parseTransaction(JSONObject tx, String category) {
        String hash = tx.optString("hash");
        long timestamp = tx.optLong("timeStamp", 0);
        String dateTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new java.util.Date(timestamp * 1000L));

        String from = tx.optString("from");
        String to = tx.optString("to");

        String contractAddress = tx.optString("contractAddress", "");
        String tokenSymbol = tx.optString("tokenSymbol", "");
        String tokenName = tx.optString("tokenName", "");
        String tokenID = tx.optString("tokenID", ""); // for NFTs

        String value = tx.optString("value", "0");
        // Convert from Wei to ETH or token units
        String readableValue = convertWeiToEth(value);

        // Gas fee in ETH = gasUsed * gasPrice / 1e18
        String gasUsed = tx.optString("gasUsed", "0");
        String gasPrice = tx.optString("gasPrice", "0");
        String gasFeeEth = calculateGasFee(gasUsed, gasPrice);

        return new String[]{
                hash,
                dateTime,
                from,
                to,
                category,
                contractAddress,
                tokenSymbol.isEmpty() ? tokenName : tokenSymbol,
                tokenID,
                readableValue,
                gasFeeEth
        };
    }

    private static String convertWeiToEth(String weiStr) {
        try {
            BigDecimal wei = new BigDecimal(weiStr);
            return wei.divide(new BigDecimal("1000000000000000000"), 18, RoundingMode.HALF_UP).toPlainString();
        } catch (Exception e) {
            return "0";
        }
    }

    private static String calculateGasFee(String gasUsedStr, String gasPriceStr) {
        try {
            BigDecimal gasUsed = new BigDecimal(gasUsedStr);
            BigDecimal gasPrice = new BigDecimal(gasPriceStr);
            BigDecimal weiFee = gasUsed.multiply(gasPrice);
            return convertWeiToEth(weiFee.toPlainString());
        } catch (Exception e) {
            return "0";
        }
    }



    private static void writeCsv(String fileName, List<String[]> data) throws IOException {
        FileWriter fileWriter = new FileWriter(fileName);
        CSVWriter writer = new CSVWriter(fileWriter);

        // Header
        writer.writeNext(new String[]{
                "Transaction Hash", "Date & Time", "From Address", "To Address",
                "Transaction Type", "Asset Contract Address", "Asset Symbol / Name",
                "Token ID", "Value / Amount", "Gas Fee (ETH)"
        });

        writer.writeAll(data);
        writer.close();
    }
}
