# cryptoTracker

This is crpto Tracker project that tracks various crypto transaction for a particular crypto wallet and provides a singular csv to analyse all your transactions.


##How To run

- git clone the  project 
```git clone https://github.com/yourusername/etherscan-wallet-scraper.git```
- Install dependencies, run : ```  mvn clean install```
- update `apiKey` and `walletAddress` properties in the `config.properites` file.
- run command `mvn exec:java` or if you are in IDE simply run the project.
- A `wallet_transactions.csv` file will be created in the main repo.