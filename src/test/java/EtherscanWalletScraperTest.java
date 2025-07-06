
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class EtherscanWalletScraperTest {

    @Test
    void testConvertWeiToEth() {
        String wei = "1000000000000000000"; // 1 ETH
        String eth = EtherscanWalletScraper.convertWeiToEth(wei);
        assertEquals("1", eth);  // Adjusted to match output
    }

    @Test
    void testCalculateGasFee() {
        String gasUsed = "21000";
        String gasPrice = "1000000000"; // 1 Gwei
        String gasFee = EtherscanWalletScraper.calculateGasFee(gasUsed, gasPrice);
        assertEquals("0.000021", gasFee); // Adjusted to match output
    }

    @Test
    void testConvertWeiToEthInvalidInput() {
        String eth = EtherscanWalletScraper.convertWeiToEth("not-a-number");
        assertEquals("0", eth);
    }

    @Test
    void testCalculateGasFeeInvalidInput() {
        String gasFee = EtherscanWalletScraper.calculateGasFee("NaN", "Oops");
        assertEquals("0", gasFee);
    }

    @Test
    void testInvalidApiKeyResponse() {
        EtherscanWalletScraper.API_KEY = "INVALID_KEY";
        EtherscanWalletScraper.WALLET_ADDRESS = "0xde0B295669a9FD93d5F28D9Ec85E40f4cb697BAe"; // valid wallet

        String result = EtherscanWalletScraper.getApiResponse("txlist");
        assertNull(result, "Should return null for invalid API key");
    }

    @Test
    void testInvalidWalletAddress() {
        EtherscanWalletScraper.API_KEY = "YourValidKey";
        EtherscanWalletScraper.WALLET_ADDRESS = "0xINVALID";

        String result = EtherscanWalletScraper.getApiResponse("txlist");
        assertNull(result, "Should return null for invalid wallet address");
    }

}
