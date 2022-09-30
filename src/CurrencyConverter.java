import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.*;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CurrencyConverter {

    private class Currency {

        private final String currencyCode;
        private final double exchangeRate;

        public Currency(String currencyCode, double exchangeRate) {
            this.currencyCode = currencyCode;
            this.exchangeRate = exchangeRate;
        }

        public double getExchangeRate() {
            return exchangeRate;
        }
        public String getCurrencyCode() {
            return currencyCode;
        }
    }

    private static Connection connection;
    final String CONNECTION_URL = "jdbc:sqlite:CurrencyExchange.db";

    // Connect to database methods
    public Connection openConnection() {
        try {
            connection = DriverManager.getConnection(CONNECTION_URL);
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            System.out.println("There was an error connecting to the database");
        }
        return connection;
    }

    public Connection getConnection() throws Exception {
        if(connection == null) {
            return openConnection();
        } else {
            return connection;
        }
    }

    public boolean closeConnection(boolean commitChanges) {
        try {
            if (commitChanges) {
                connection.commit();
            } else {
                connection.rollback();
            }
            connection.close();
            connection = null;
        } catch (SQLException e) {
            System.out.println("There was an error closing the database");
            return false;
        }
        return true;
    }

    public Double convertCurrency(String currencyFrom, String currencyTo, double amount) {
        Currency currency1 = selectCurrency(currencyFrom);
        Currency currency2 = selectCurrency(currencyTo);
        if (currency1 != null && currency2 != null) {
            double currency1InDollars = convertToDollars(amount, currency1.exchangeRate);
            return convertDollarsToCurrency(currency1InDollars, currency2.exchangeRate);
        } else  {
            return null;
        }
    }

    public double convertToDollars(double amount, double exchangeRate) {
        return amount * exchangeRate;
    }

    public double convertDollarsToCurrency(double dollarAmount, double exchangeRate) {
        return dollarAmount / exchangeRate;
    }

    // Input validation methods
    private boolean validateCurrencyCode(String code) {
        return code.length() == 3;
    }

    private boolean validateExchangeRate(double rate) {
        return rate > 0.0;
    }

    private boolean validateExistingCurrency(String code) {
        Currency currency = selectCurrency(code);
        return currency != null;
    }

    // Database Query Methods

    // Insert a new currency into the database with a code and exchange rate.
    // The CurrencyID is an auto-incrementing primary key calculated in the database
    private boolean addCurrency(String currencyCode, double exchangeRate) {
        boolean validCode = validateCurrencyCode(currencyCode);
        boolean validRate = validateExchangeRate(exchangeRate);
        boolean existingCurrency = validateExistingCurrency(currencyCode);
        if (validCode && validRate && !existingCurrency) {
            insertCurrency(currencyCode, exchangeRate);
            return true;
        }
        return false;
    }

    private void insertCurrency(String currencyCode, double exchangeRate) {
        String insertStatement = "INSERT INTO CurrencyExchange (CurrencyCode, ExchangeRate)" +
                "VALUES (?,?)";
        try {
            PreparedStatement sqlStatement = connection.prepareStatement(insertStatement);
            sqlStatement.setString(1, currencyCode.toUpperCase());
            sqlStatement.setDouble(2, exchangeRate);

            sqlStatement.execute();
        } catch (SQLException e) {
            System.out.println("Error adding currency to the database");
        }
    };

    // remove a currency from the database
    private boolean removeCurrency(String currencyCode) {
        boolean validCode = validateCurrencyCode(currencyCode);
        boolean existingCode = validateExistingCurrency(currencyCode);
        if (validCode && existingCode) {
            deleteCurrency(currencyCode);
            return true;
        }
        return false;
    }

    private void deleteCurrency(String currencyCode) {
        String deleteStatement = "DELETE FROM CurrencyExchange WHERE CurrencyCode = ?";
        try {
            PreparedStatement sqlStatement = connection.prepareStatement(deleteStatement);
            sqlStatement.setString(1, currencyCode.toUpperCase());

            sqlStatement.execute();
        } catch (SQLException e) {
            System.out.println("Error deleting currency from the database");
        }
    }

    // change the currency code for an existing currency
    private boolean editCurrencyCode(String currencyCode, String newCode) {
        boolean validCode = validateCurrencyCode(currencyCode);
        boolean validNewCode = validateCurrencyCode(newCode);
        boolean existingCode = validateExistingCurrency(currencyCode);
        boolean existingNewCode = validateExistingCurrency(newCode);

        if (validCode && validNewCode && existingCode && !existingNewCode) {
            updateCurrencyCode(currencyCode, newCode);
            return true;
        }
        return false;
    }

    private void updateCurrencyCode(String currencyCode, String newCode) {
        String updateStatement = "UPDATE CurrencyExchange SET CurrencyCode = ? WHERE CurrencyCode = ?";
        try {
            PreparedStatement sqlStatement = connection.prepareStatement(updateStatement);
            sqlStatement.setString(1, newCode.toUpperCase());
            sqlStatement.setString(2, currencyCode.toUpperCase());

            sqlStatement.execute();
        } catch (SQLException e) {
            System.out.println("Error updating currency in the database");
        }
    }

    // change the exchange rate for a currency in the database
    private boolean editCurrencyRate(String currencyCode, double newRate) {
        boolean validCode = validateCurrencyCode(currencyCode);
        boolean existingCode = validateExistingCurrency(currencyCode);
        boolean validRate = validateExchangeRate(newRate);
        if (validCode && validRate && existingCode) {
            updateCurrencyRate(currencyCode, newRate);
            return true;
        }
        return false;
    }

    private void updateCurrencyRate(String currencyCode, double newRate) {
        String updateStatement = "UPDATE CurrencyExchange SET ExchangeRate = ? WHERE CurrencyCode = ?";
        try {
            PreparedStatement sqlStatement = connection.prepareStatement(updateStatement);
            sqlStatement.setDouble(1, newRate);
            sqlStatement.setString(2, currencyCode.toUpperCase());

            sqlStatement.execute();
        } catch (SQLException e) {
            System.out.println("Error updating currency in the database");
        }
    }

    // Retrieve a currency from the database that matches the given currency code
    private Currency selectCurrency(String currencyCode) {
        Currency currency;
        ResultSet result = null;
        String selectStatement = "SELECT * FROM CurrencyExchange WHERE CurrencyCode = ?";
        try {
            PreparedStatement sqlStatement = connection.prepareStatement(selectStatement);
            sqlStatement.setString(1, currencyCode.toUpperCase());

            result = sqlStatement.executeQuery();
            if (result.next()) {
                currency = new Currency(result.getString("CurrencyCode"),
                        result.getDouble("ExchangeRate"));
                return currency;
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving currency from database");
        } finally {
            if (result != null) {
                try {
                    result.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    // clears the database
    private void clearTable() {
        try {
            Statement statement = connection.createStatement();
            String sql = "DELETE FROM CurrencyExchange";
            statement.execute(sql);
        } catch (Exception e) {
            System.out.println("Error while clearing the database");
        }
    }

    // Unit Test Example Data
    Currency currency1 = new Currency("USD", 1.00);
    Currency currency2 = new Currency("EUR", 1.02);
    Currency currency3 = new Currency("GBP", 0.90);
    Currency currency4 = new Currency("JPY", 142.79);
    Currency currency5 = new Currency("MXN", 20.08);

    @BeforeEach
    public void initializeDatabase() throws Exception {
        connection = getConnection();
        addCurrency(currency1.currencyCode, currency1.exchangeRate);
        addCurrency(currency2.currencyCode, currency2.exchangeRate);
        addCurrency(currency3.currencyCode, currency3.exchangeRate);
        addCurrency(currency4.currencyCode, currency4.exchangeRate);
        addCurrency(currency5.currencyCode, currency5.exchangeRate);
    }

    @AfterEach
    public void closeDatabase() {
        clearTable();
        assertTrue(closeConnection(true));
    }

    // Unit Tests
    @Test
    public void selectCurrencyTest() throws Exception {
        assertNotNull(selectCurrency(currency1.currencyCode));
        assertNull(selectCurrency("ABC"));
    }

    @Test
    public void changeCurrencyRateTest() throws Exception {
        assertNotNull(selectCurrency(currency3.currencyCode));
        assertNotNull(selectCurrency(currency4.currencyCode));
        double convertedCurrency = convertCurrency(currency3.currencyCode, currency4.currencyCode, 50.00);
        editCurrencyRate(currency3.currencyCode, 3.50);
        double newConvertedCurrency = convertCurrency(currency3.currencyCode, currency4.currencyCode, 50.00);
        assertNotEquals(convertedCurrency, newConvertedCurrency);

    }

    @Test
    public void renameCurrencyTest() throws Exception {
        assertNotNull(selectCurrency(currency3.currencyCode));
        assertNull(selectCurrency("123"));
        editCurrencyCode(currency3.currencyCode, "123");
        assertNull(selectCurrency(currency3.currencyCode));
        assertNotNull(selectCurrency("123"));
    }

    @Test
    public void deleteCurrencyTest() throws Exception {
        assertNotNull(selectCurrency(currency5.currencyCode));
        assertTrue(removeCurrency(currency5.currencyCode));
        assertNull(selectCurrency(currency5.currencyCode));
    }

    @Test
    public void insertCurrencyTest() throws Exception {
        assertNull(selectCurrency("abc"));
        assertTrue(addCurrency("abc", 2.77));
        assertNotNull(selectCurrency("abc"));
        assertNotNull(selectCurrency("ABC"));
        assertNotNull(selectCurrency("aBc"));

    }

    @Test
    public void exchangeCurrencyTest() throws Exception {
        Currency cur1 = new Currency("ABC", 1.45);
        Currency cur2 = new Currency("DEF", .88);
        assertTrue(addCurrency(cur1.currencyCode, cur1.exchangeRate));
        assertTrue(addCurrency(cur2.currencyCode, cur2.exchangeRate));
        double amount = 10.00;
        double newAmount1 = amount * cur1.exchangeRate / cur2.exchangeRate;
        double newAmount2 = convertCurrency(cur1.currencyCode, cur2.currencyCode, amount);
        assert(newAmount1 == newAmount2);
    }

    @Test
    public void invalidInputTest() {
        // invalid insert data
        assertFalse(addCurrency("ABCD", 3.50));
        assertFalse(addCurrency("AB", 3.50));
        assertFalse(addCurrency("", 3.50));
        assertFalse(addCurrency("ABCD", -1.0));
        assertFalse(addCurrency("ABCD", 0));
        assertFalse(addCurrency("ABCD", -0.001));
        assertFalse(addCurrency(currency1.currencyCode, 2.0));

        // invalid code update
        assertFalse(editCurrencyCode(currency1.getCurrencyCode(), "AB"));
        assertFalse(editCurrencyCode("ABC", "ABD"));
        assertFalse(editCurrencyCode("AB", "ABC"));

        // invalid rate update
        assertFalse(editCurrencyRate(currency1.currencyCode, 0.0));
        assertFalse(editCurrencyRate(currency1.currencyCode, -1.0));
        assertFalse(editCurrencyRate("AB", 2.0));
        assertFalse(editCurrencyRate("ABC", 2.0));

        //invalid currency deletion
        assertFalse(removeCurrency("AB"));
        assertFalse(removeCurrency("ABCD"));
        assertFalse(removeCurrency("ABC"));
    }
}
