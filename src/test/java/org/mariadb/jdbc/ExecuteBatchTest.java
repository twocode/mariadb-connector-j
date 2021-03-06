package org.mariadb.jdbc;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.*;

public class ExecuteBatchTest extends BaseTest {

    /**
     * Create test tables.
     *
     * @throws SQLException if connection error occur
     */
    @BeforeClass()
    public static void initClass() throws SQLException {
        createTable("ExecuteBatchTest", "id int not null primary key auto_increment, test varchar(100) , test2 int");
        createTable("ExecuteBatchUseBatchMultiSend", "test varchar(100)");
    }

    static String oneHundredLengthString = "";
    static boolean profileSql = true;

    static {
        char[] chars = new char[100];
        for (int i = 27; i < 127; i++) chars[i - 27] = (char) i;
        oneHundredLengthString = new String(chars);
    }

    @Test
    public void serverBulk8mTest() throws SQLException {
        Assume.assumeTrue(checkMaxAllowedPacketMore8m("serverBulk8mTest"));
        Assume.assumeTrue(runLongTest);
        sharedConnection.createStatement().execute("TRUNCATE TABLE ExecuteBatchTest");

        try (Connection connection = setConnection("&useComMulti=false&useBatchMultiSend=true&profileSql=" + profileSql)) {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO ExecuteBatchTest(test, test2) values (?, ?)");
            //packet size : 7 200 068 kb
            addBatchData(preparedStatement, 60000, connection);
        }
    }

    @Test
    public void serverBulk20mTest() throws SQLException {
        Assume.assumeTrue(checkMaxAllowedPacketMore20m("serverBulk20mTest"));
        Assume.assumeTrue(runLongTest);
        sharedConnection.createStatement().execute("TRUNCATE TABLE ExecuteBatchTest");

        try (Connection connection = setConnection("&useComMulti=false&useBatchMultiSend=true&profileSql=" + profileSql)) {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO ExecuteBatchTest(test, test2) values (?, ?)");
            //packet size : 7 200 068 kb
            addBatchData(preparedStatement, 160000, connection);
        }
    }


    @Test
    public void serverStd8mTest() throws SQLException {
        Assume.assumeTrue(checkMaxAllowedPacketMore8m("serverStd8mTest"));
        Assume.assumeTrue(runLongTest);
        sharedConnection.createStatement().execute("TRUNCATE TABLE ExecuteBatchTest");

        try (Connection connection = setConnection("&useComMulti=false&useBatchMultiSend=false&profileSql=" + profileSql)) {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO ExecuteBatchTest(test, test2) values (?, ?)");
            addBatchData(preparedStatement, 60000, connection);
        }
    }

    @Test
    public void clientBulkTest() throws SQLException {
        Assume.assumeTrue(checkMaxAllowedPacketMore8m("serverStd8mTest"));
        Assume.assumeTrue(runLongTest);
        sharedConnection.createStatement().execute("TRUNCATE TABLE ExecuteBatchTest");

        try (Connection connection = setConnection("&useComMulti=false&useBatchMultiSend=true&useServerPrepStmts=false&profileSql=" + profileSql)) {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO ExecuteBatchTest(test, test2) values (?, ?)");
            addBatchData(preparedStatement, 60000, connection);
        }
    }

    @Test
    public void clientRewriteValuesNotPossible8mTest() throws SQLException {
        Assume.assumeTrue(checkMaxAllowedPacketMore8m("clientRewriteValuesNotPossibleTest"));
        Assume.assumeTrue(runLongTest);
        sharedConnection.createStatement().execute("TRUNCATE TABLE ExecuteBatchTest");
        try (Connection connection = setConnection("&rewriteBatchedStatements=true&profileSql=" + profileSql)) {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO ExecuteBatchTest(test, test2) values (?, ?) ON DUPLICATE KEY UPDATE id=?");
            addBatchData(preparedStatement, 60000, connection, true);
        }
    }


    @Test
    public void clientRewriteValuesNotPossible20mTest() throws SQLException {
        Assume.assumeTrue(checkMaxAllowedPacketMore8m("clientRewriteValuesNotPossibleTest"));
        Assume.assumeTrue(runLongTest);
        sharedConnection.createStatement().execute("TRUNCATE TABLE ExecuteBatchTest");
        try (Connection connection = setConnection("&rewriteBatchedStatements=true&profileSql=" + profileSql)) {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    "INSERT INTO ExecuteBatchTest(test, test2) values (?, ?) ON DUPLICATE KEY UPDATE id=?");
            addBatchData(preparedStatement, 160000, connection, true);
        }
    }

    @Test
    public void clientRewriteValuesPossibleTest() throws SQLException {
        // 8mb
        // 20mb
        // 40mb
    }

    @Test
    public void clientRewriteMultiTest() throws SQLException {
        // 8mb
        // 20mb
        // 40mb
    }

    @Test
    public void clientStdMultiTest() throws SQLException {
        // 8mb
        // 20mb
        // 40mb
    }

    private void addBatchData(PreparedStatement preparedStatement, int batchNumber, Connection connection) throws SQLException {
        addBatchData(preparedStatement, batchNumber, connection, false);
    }

    private void addBatchData(PreparedStatement preparedStatement, int batchNumber, Connection connection, boolean additionnalParameter)
            throws SQLException {
        for (int i = 0 ; i < batchNumber ; i++) {
            preparedStatement.setString(1, oneHundredLengthString);
            preparedStatement.setInt(2, i);
            if (additionnalParameter) preparedStatement.setInt(3, i);
            preparedStatement.addBatch();
        }
        int[] resultInsert = preparedStatement.executeBatch();

        //test result Size
        Assert.assertEquals(batchNumber, resultInsert.length);
        for (int i = 0 ; i < batchNumber ; i++) Assert.assertEquals(1, resultInsert[i]);

        //check that connection is OK and results are well inserted
        ResultSet resultSet = connection.createStatement().executeQuery("SELECT * FROM ExecuteBatchTest");
        for (int i = 0 ; i < batchNumber ; i++) {
            Assert.assertTrue(resultSet.next());
            Assert.assertEquals(i + 1, resultSet.getInt(1));
            Assert.assertEquals(oneHundredLengthString, resultSet.getString(2));
            Assert.assertEquals(i, resultSet.getInt(3));
        }
        Assert.assertFalse(resultSet.next());
    }

    @Test
    public void useBatchMultiSend() throws Exception {
        try (Connection connection = setConnection("&useBatchMultiSend=true")) {
            String sql = "insert into ExecuteBatchUseBatchMultiSend (test) values (?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                for (int i = 0; i < 10; i++) {
                    pstmt.setInt(1, i);
                    pstmt.addBatch();
                }
                int[] updateCounts = pstmt.executeBatch();
                Assert.assertEquals(10, updateCounts.length);
                for (int i = 0; i < updateCounts.length; i++) Assert.assertEquals(1, updateCounts[i]);
            }
        }
    }
}
