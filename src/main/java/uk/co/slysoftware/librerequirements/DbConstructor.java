package uk.co.slysoftware.librerequirements;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DbConstructor {

    private static Logger log = LoggerFactory.getLogger(DbConstructor.class);

    private String dbUrl;
    private File db;

    public DbConstructor(File db) {
        this.db = db;
        dbUrl = "jdbc:sqlite:" + db.getPath();
    }

    public Connection load(boolean deleteBeforeLoad) throws SQLException, LiquibaseException  {

        if (deleteBeforeLoad) {
            if (db.exists()) {
                log.info(db.getPath() + " database already exists, deleting");
            }
            db.delete();
        }
        ClassLoaderResourceAccessor accessor = new ClassLoaderResourceAccessor();

        Connection con = DriverManager.getConnection(dbUrl);
        log.info("Connected to " + dbUrl);

        JdbcConnection connection = new JdbcConnection(con);
        Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connection);

        Liquibase liquibase = new Liquibase(
                "uk/co/slysoftware/librerequirements/changelog/changelog-master.yml",
                accessor, connection);
        Contexts context = new Contexts();

        liquibase.update(context, new LabelExpression());

        database.close();

        return DriverManager.getConnection(dbUrl);
    }
}
