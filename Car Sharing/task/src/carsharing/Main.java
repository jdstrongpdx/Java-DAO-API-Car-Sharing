package carsharing;
import org.h2.jdbcx.JdbcDataSource;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;


/**
 *  ABOUT THIS PROGRAM AND DESIGN
 *  This is a Data Access Object implementation for a very simple car rental program that allows interactions between
 *  companies, customers, and cars.  DbClient allows for a single source interface for inputs and outputs using a
 *  standard DataSource object - allowing changes to the underlying storage (in this case H2).  The Developer class
 *  abstracts the collection and return of data between the user and the database.  The DeveloperDao interface
 *  defines methods for basic CRUD operations on Developer objects - which are implemented by CompanyDao, CarDao,
 *  and CustomerDao respectively for each of their own tables.  A fourth RentalDao allows for rental car transactions
 *  between companies, cars, and customers.
 *
 *  PERSONAL NOTES
 *  This is the 'Graduate Project' for the Hyperskill Java Backend Developer Certificate.  I had implemented much of
 *  this program using a basic methodology when I ran into a template for DAO and JDBC.  I decided it would be a much
 *  better learning experience to implement the DAO model and started over from scratch - developing the solution in
 *  two days.  It was a fun project, but both the simplicity and lack of functionality of the end program
 *  leave something to be desired, but does not warrant further development.  After avoiding COVID for years, my son
 *  brought the latest strain home after his first week back at school.  This was my 'quarantine' project - partly to
 *  distract from the pain and 102 fever.  Good times.
 */

public class Main {
    // Scanner for user input
    private final static Scanner scanner = new Scanner(System.in);

    // Database client instance
    private static DbClient dbClient = null;

    // DAO instances for managing different entities
    private static DeveloperDao companyDao = null;
    private static DeveloperDao carDao = null;
    private static DeveloperDao customerDao = null;
    private static DeveloperDao rentalDao = null;

    /**
     *  Class for running SQL statements or getting db data and returning it as class objects
     */
    public class DbClient {
        private final DataSource dataSource;

        /**
         * Constructor for DBClient.
         * @param dataSource - The DataSource class object used for connections.
         */
        public DbClient(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        /**
         * Execute a direct SQL query - used for table creation - DO NOT USE WITH USER INPUT
         * @param str - a string of SQL data
         */
        public void run(String str) {
            try (Connection con = dataSource.getConnection(); // Statement creation
                 Statement statement = con.createStatement()
            ) {
                statement.executeUpdate(str); // Statement execution
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        /**
         * Execute a query looking for a single row.  Results with multiple rows will throw exception
         * @param query - string of SQL text using a formatted string for injection prevention
         * @param choice - the number of columns in the table to set into class objects
         * @return - Developer object containing the found data
         */
        public Developer select(String query, int choice) {
            List<Developer> developers = selectForList(query, choice);
            if (developers.size() == 1) {
                return developers.get(0);
            } else if (developers.isEmpty()) {
                return null;
            } else {
                throw new IllegalStateException("Query returned more than one object");
            }
        }

        /**
         * Execute a query looking for a one or more rows
         * @param query - string of SQL text using a formatted string for injection prevention
         * @param choice - the number of columns in the table to set into class objects
         * @return - List of Developer objects containing the found data
         */
        public List<Developer> selectForList(String query, int choice) {
            List<Developer> developers = new ArrayList<>();

            try (Connection con = dataSource.getConnection();
                 Statement statement = con.createStatement();
                 ResultSet resultSetItem = statement.executeQuery(query)
            ) {
                while (resultSetItem.next()) {
                    String name = "";
                    Developer developer = new Developer("");
                    int id = resultSetItem.getInt(1);
                    name = resultSetItem.getString(2);
                    if (choice == 2) {
                        developer = new Developer(id, name);
                    } else if (choice == 3) {
                        int parent_id = resultSetItem.getInt(3);
                        developer = new Developer(id, name, parent_id);
                    }
                    developers.add(developer);
                }

                return developers;
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return developers;
        }
    }

    //-----------------------------------------------------------------------------------------------------------------

    /**
     * Developer class that de-couples the data interactions between the user and the SQL database
     */
    public class Developer {
        private int id;
        private String name;
        private int parent;

        /**
         * Constructor used by user for creating a developer object with only a name
         * @param name - String naming the item
         */
        public Developer(String name) {
            this.name = name;
            this.id = -1;
            this.parent = -1;
        }

        /**
         * Constructor used by DbClient for setting SQL data into class objects with only names & ids
         * @param id - auto_incremented id integer generated by the database
         * @param name - String naming the item
         */
        public Developer(int id, String name) {
            this.name = name;
            this.id = id;
            this.parent = -1;
        }

        /**
         * Constructor used by user for creating a developer object with a name and a parent
         * @param name - String naming the item
         * @param parent - parent id linking item to another table (field names vary)
         */
        public Developer(String name, int parent) {
            this.name = name;
            this.id = -1;
            this.parent = parent;
        }

        /**
         * Constructor used by DBClient for setting SQL data into class objects with three fields
         * @param id - auto_incremented id integer generated by the database
         * @param name - String naming the item
         * @param parent_id - - parent id linking item to another table (field names vary)
         */
        public Developer(int id, String name, int parent_id) {
            this.name = name;
            this.id = id;
            this.parent = parent_id;
        }

        /**
         * Getter for name field
         * @return - String of name
         */
        String getName() {
            return this.name;
        }

        /**
         * Getter for id field
         * @return - int of id
         */
        int getId() {
            return this.id;
        }

        /**
         * Getter for parent id field
         * @return - int of id
         */
        int getParent() {return this.parent;}

        // setters - UN-USED - DISABLED
        // void setName(String name) {this.name = name;}
        // void setId(int id) {this.id = id;}
        // void setParent(int parent) {this.parent = parent;}
    }

    //-----------------------------------------------------------------------------------------------------------------

    /**
     * Interface between the Developer class and the Data Access Object that allows CRUD operations
     */
    public interface DeveloperDao {
        List<Developer> findAll();
        Developer findById(int id);
        List<Developer> findByParentId(int id);
        void add(Developer developer);
        void update(Developer developer);
        void deleteById(int id);
    }

    //-----------------------------------------------------------------------------------------------------------------

    /**
     *  Class that creates and facilitates transactions with the company table.
     */
    public class CompanyDao implements DeveloperDao {

        private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS company " +
                "(id INTEGER PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255) UNIQUE NOT NULL);";
        private static final String SELECT_ALL = "SELECT * FROM company ORDER BY id";
        private static final String SELECT = "SELECT * FROM company WHERE id = %d";
        private static final String INSERT_DATA = "INSERT INTO company (name) VALUES ('%s')";
        private static final String UPDATE_DATA = "UPDATE company SET name = '%s' WHERE id = %d";
        private static final String DELETE_DATA = "DELETE FROM company WHERE id = %d";

        /**
         * Class constructor that creates the company table in the database
         */
        public CompanyDao() {
            dbClient.run(CREATE_TABLE);
        }

        /**
         * Add a new company to the database using Developer data and a pre-defined SQL statement
         * @param developer - Developer object containing the name of the company to add
         */
        @Override
        public void add(Developer developer) {
            dbClient.run(String.format(INSERT_DATA, developer.getName()));
            System.out.println("The company was created!");
        }

        /**
         * Find all companies in the database and return them as Developer objects
         * @return List of Developer objects with all companies
         */
        @Override
        public List<Developer> findAll() {
            List<Developer> found = dbClient.selectForList(SELECT_ALL, 2);
            if (found.isEmpty()) {
                System.out.println("The company list is empty!");
            } else {
                int i = 1;
                System.out.println("\nChoose the company: ");
                for (Developer developer : found) {
                    int id = developer.getId();
                    String name = developer.getName();
                    System.out.println(i + ". " + name);
                    i++;
                }
                System.out.println("0. Back");
            }
            return found;
        }

        /**
         * Find a specific company by id number
         * @param id - integer value of the company id to find
         * @return - Developer object if found, null if not found, thows IllegalStateException if more than one found
         */
        @Override
        public Developer findById(int id) {
            return dbClient.select(String.format(SELECT, id), 2);
        }

        /**
         * UNUSED IN THIS CLASS - THERE ARE NO PARENT FIELDS TO ACCESS
         * @param id - N/A
         * @return - N/A
         */
        @Override
        public List<Developer> findByParentId(int id) {
            return findAll();
        }

        /**
         * Update the name of the company given a specific company id
         * @param developer - Developer object containing the new name and current id of the company to update
         */
        @Override
        public void update(Developer developer) {
            dbClient.run(String.format(UPDATE_DATA, developer.getName(), developer.getId()));}

        /**
         * Delete a company by its id number
         * @param id - integer id number of the company id
         */
        @Override
        public void deleteById(int id) {
            dbClient.run(String.format(DELETE_DATA, id));
        }
    }

    //-----------------------------------------------------------------------------------------------------------------

    /**
     *  Class that creates and facilitates transactions with the car table.
     */
    public class CarDao implements DeveloperDao {

        private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS car " +
                "(id INTEGER PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255) UNIQUE NOT NULL, " +
                "company_id INTEGER NOT NULL, FOREIGN KEY (company_id) REFERENCES company(id));";
        private static final String SELECT_ALL = "SELECT * FROM car ORDER BY id";
        private static final String SELECT = "SELECT * FROM car WHERE id = %d";
        private static final String PARENT_SELECT = "SELECT * FROM car WHERE company_id = %d";
        private static final String INSERT_DATA = "INSERT INTO car (name, company_id) VALUES ('%s', %d)";
        private static final String UPDATE_DATA = "UPDATE car SET name = '%s', company_id = %d WHERE id = %d";
        private static final String DELETE_DATA = "DELETE FROM car WHERE id = %d";

        /**
         * Class constructor that creates the car table in the database
         */
        public CarDao() {
            dbClient.run(CREATE_TABLE);
        }

        /**
         * Add a new company to the database using Developer data and a pre-defined SQL statement
         * @param developer - Developer object containing the name of the company to add
         */
        @Override
        public void add(Developer developer) {
            dbClient.run(String.format(INSERT_DATA, developer.getName(), developer.getParent()));
            System.out.println("The car was created!");
        }

        /**
         * Find all cars in the database and return them as Developer objects
         * @return List of Developer objects with all cars
         */
        @Override
        public List<Developer> findAll() {
            List<Developer> found = dbClient.selectForList(SELECT_ALL, 3);
            if (found.isEmpty()) {
                System.out.println("The car list is empty!");
            } else {
                System.out.println("\nCar list: ");
                for (Developer developer : found) {
                    int id = developer.getId();
                    String name = developer.getName();
                    System.out.println(id + ". " + name);
                }
                System.out.println("0. Back");
            }
            return found;
        }

        /**
         * Find a specific car by id number
         * @param id - integer value of the car id to find
         * @return - Developer object if found, null if not found, thows IllegalStateException if more than one found
         */
        @Override
        public Developer findById(int id) {
            return dbClient.select(String.format(SELECT, id), 3);
        }

        /**
         * Finds all cars matching the argument parentId (company_id) number
         * @param parentId - the integer value of the company id to find all cars for
         * @return - List of Developer objects containing all cars found or null
         */
        @Override
        public List<Developer> findByParentId(int parentId) {
            List<Developer> found = dbClient.selectForList(String.format(PARENT_SELECT, parentId), 3);
            if (found.isEmpty()) {
                System.out.println("The car list is empty!");
            } else {
                int i = 1;
                System.out.println("\nCar list: ");
                for (Developer developer : found) {
                    int id = developer.getId();
                    String name = developer.getName();
                    System.out.println(i + ". " + name);
                    i++;
                }
            }
            return found;
        }

        /**
         * Update the name of the car given a specific company id
         * @param developer - Developer object containing the new name and current id of the car to update
         */
        @Override
        public void update(Developer developer) {
            dbClient.run(String.format(UPDATE_DATA, developer.getName(), developer.getParent(), developer.getId()));}

        /**
         * Delete a car by its id number
         * @param id - integer id number of the car id
         */
        @Override
        public void deleteById(int id) {
            dbClient.run(String.format(DELETE_DATA, id));
        }
    }

    //-----------------------------------------------------------------------------------------------------------------

    /**
     *  Class that creates and facilitates transactions with the customer table.
     */
    public class CustomerDao implements DeveloperDao {

        private static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS customer " +
                "(id INTEGER PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255) UNIQUE NOT NULL, " +
                "rented_car_id INTEGER DEFAULT NULL, FOREIGN KEY (rented_car_id) REFERENCES car(id));";
        private static final String SELECT_ALL = "SELECT * FROM customer ORDER BY id";
        private static final String SELECT = "SELECT * FROM customer WHERE id = %d";
        private static final String PARENT_SELECT = "SELECT * FROM customer WHERE rented_car_id = %d";
        private static final String INSERT_DATA = "INSERT INTO customer (name) VALUES ('%s')";
        private static final String UPDATE_DATA = "UPDATE customer SET name = '%s', rented_car_id = %d WHERE id = %d";
        private static final String DELETE_DATA = "DELETE FROM customer WHERE id = %d";

        /**
         * Class constructor that creates the customer table in the database
         */
        public CustomerDao() {
            dbClient.run(CREATE_TABLE);
        }

        /**
         * Adds a customer to the database
         * @param developer - Developer object containing the name for the customer
         */
        @Override
        public void add(Developer developer) {
            dbClient.run(String.format(INSERT_DATA, developer.getName()));
            System.out.println("The customer was created!");
        }

        /**
         * Selects all customers from the database and returns them as a list of Developer objects
         * @return List of Developer objects containing all customers
         */
        @Override
        public List<Developer> findAll() {
            List<Developer> found = dbClient.selectForList(SELECT_ALL, 3);
            if (found.isEmpty()) {
                System.out.println("The customer list is empty!");
            } else {
                int i = 1;
                System.out.println("\nCustomer list: ");
                for (Developer developer : found) {
                    String name = developer.getName();
                    System.out.println(i + ". " + name);
                    i++;
                }
                System.out.println("0. Back");
            }
            return found;
        }

        /**
         * Find a single customer by id and return the data in a Developer object
         * @param id - integer id of the customer to find
         * @return - Developer object of the customer found or empty list if none found
         */
        @Override
        public Developer findById(int id) {
            return dbClient.select(String.format(SELECT, id), 3);
        }

        /**
         * Find any customers with a specific parentId (referring to rented_car_id in this class) and return
         * the results as a List of Developer objects
         * @param parentId -
         * @return
         */
        @Override
        public List<Developer> findByParentId(int parentId) {
            List<Developer> found = dbClient.selectForList(String.format(PARENT_SELECT, parentId), 3);
            if (found.isEmpty()) {
                System.out.println("The car list is empty!");
            } else {
                int j = 1;
                System.out.println("\nCar list: ");
                for (Developer developer : found) {
                    String name = developer.getName();
                    System.out.println(j + ". " + name);
                    j++;
                }
            }
            return found;
        }

        /**
         * Update the rented_car_id of given a specific customer
         * @param developer - Developer object containing the new name and current id of the car to update
         */
        @Override
        public void update(Developer developer) {
            if (developer.getParent() == 0) {
            dbClient.run("UPDATE customer SET rented_car_id = NULL WHERE id = " + developer.getId());
        } else {
            dbClient.run(String.format(UPDATE_DATA, developer.getName(), developer.getParent(), developer.getId()));
            }
            }

        /**
         * Delete a customer by its id number
         * @param id - integer id number of the customer id
         */
        @Override
        public void deleteById(int id) {
            dbClient.run(String.format(DELETE_DATA, id));
        }
    }

    //-----------------------------------------------------------------------------------------------------------------
    /**
     *  Class that creates and transacts with the rented_car_id field on the car table.
     */
    public class RentalDao implements DeveloperDao {

        private static final String SELECT_ALL = "SELECT * FROM car ORDER BY id";
        private static final String SELECT = "SELECT * FROM car WHERE id = %d";
        private static final String PARENT_SELECT = "SELECT * FROM car a LEFT JOIN customer b ON a.id = " +
                "b.rented_car_id WHERE b.id IS NULL AND a.company_id = %d ORDER BY id";
        private static final String INSERT_DATA = "INSERT INTO car (name, company_id) VALUES ('%s', %d)";
        private static final String UPDATE_DATA = "UPDATE car SET name = '%s', company_id = %d WHERE id = %d";
        private static final String DELETE_DATA = "DELETE FROM car WHERE id = %d";

        // NO class constructor to create table - uses car table

        /**
         * UNUSED - USE CarDao CLASS TO CREATE CAR RECORDS
         * @param developer - Developer object containing the name for the customer
         */
        @Override
        public void add(Developer developer) {
            dbClient.run(String.format(INSERT_DATA, developer.getName(), developer.getParent()));
            System.out.println("The car was created!");
        }

        /**
         * UNUSED - USE CarDao CLASS TO FIND CAR RECORDS
         * @return List of Developer objects containing all customers
         */
        @Override
        public List<Developer> findAll() {
            List<Developer> found = dbClient.selectForList(SELECT_ALL, 3);
            if (found.isEmpty()) {
                System.out.println("The car list is empty!");
            } else {
                System.out.println("\nCar list: ");
                for (Developer developer : found) {
                    int id = developer.getId();
                    String name = developer.getName();
                    System.out.println(id + ". " + name);
                }
                System.out.println("0. Back");
            }
            return found;
        }

        /**
         * UNUSED - USE CarDao CLASS TO FIND CAR RECORDS
         * @return List of Developer objects containing all customers
         */
        @Override
        public Developer findById(int id) {
            return dbClient.select(String.format(SELECT, id), 3);
        }

        /**
         * Find any cars by a given company matching
         * @param parentId -
         */
        @Override
        public List<Developer> findByParentId(int parentId) {
            List<Developer> found = dbClient.selectForList(String.format(PARENT_SELECT, parentId), 3);
            if (found.isEmpty()) {
                System.out.println("The car list is empty!");
            } else {
                int i = 1;
                System.out.println("\nCar list: ");
                for (Developer developer : found) {
                    String name = developer.getName();
                    System.out.println(i + ". " + name);
                    i++;
                }
                System.out.println("0. Back");
            }
            return found;
        }

        /**
         * Update the rented_car_id of given a specific customer
         * @param developer - Developer object containing the new name and current id of the car to update
         */
        @Override
        public void update(Developer developer) {
            dbClient.run(String.format(UPDATE_DATA, developer.getName(), developer.getParent(), developer.getId()));}

        /**
         * UNUSED - USE CustomerDao CLASS TO DELETE CUSTOMER RECORDS
         */
        @Override
        public void deleteById(int id) {
            dbClient.run(String.format(DELETE_DATA, id));
        }
    }

    //-----------------------------------------------------------------------------------------------------------------

    /**
     * Create the database connection and store dataSource connection in static variable for use elsewhere
     */
    class getDbClient {

        public getDbClient(String[] args) {
            // get command line argument if present to append to filename
            String filename = "databaseName";
            for (int i = 0; i < args.length; i++) {
                if (Objects.equals(args[i], "-databaseFileName")) {
                    i++;
                    filename = args[i];
                    break;
                }
            }
            String CONNECTION_URL = "jdbc:h2:./src/carsharing/db/";

            /* Database credentials - NO CREDENTIALS
            // private static final String USER = "sa";
            // private static final String PASS = "";  */

            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setUrl(CONNECTION_URL + filename);

            dbClient = new DbClient(dataSource);
        }
    }

    //-----------------------------------------------------------------------------------------------------------------

    /**  Main function for running program (singleton instantiation)
     *
     */
    public static void main(String[] args) {
        Main prog = new Main();
        prog.run(args);
    }

    /**
     * Create DbClient, DAO objects and run main menu
     * @param args - sys args for getting db filename (if present in args)
     */
    void run (String[] args) {
        new getDbClient(args);
        companyDao = new CompanyDao();
        carDao = new CarDao();
        customerDao = new CustomerDao();
        rentalDao = new RentalDao();
        menuMain();
    }

    /**
     * Tiered menu system that exits only on user input = 0
     */
    void menuMain () {
        int option = -1;
        while (option != 0) {
            System.out.println("\n1. Log in as a manager");
            System.out.println("2. Log in as a customer");
            System.out.println("3. Create a customer");
            System.out.println("0. Exit");
            option = scanner.nextInt();
            switch (option) {
                case 1 -> companyMenu();
                case 2 -> customerMenu();
                case 3 -> {
                    System.out.println("\nEnter the customer name: ");
                    String strip = scanner.nextLine();
                    String name = scanner.nextLine();
                    customerDao.add(new Developer(name));
                }
            }
        }
    }

    /**
     * Company menu options - list/create companies
     */
    void companyMenu() {
        int action = -1;
        while (action != 0) {
            System.out.println("\n1. Company list");
            System.out.println("2. Create a company");
            System.out.println("0. Back");
            action = scanner.nextInt();
            switch (action) {
                case 1 -> {carMenu();
                }
                case 2 -> {
                    System.out.println("\nEnter the company name: ");
                    String strip = scanner.nextLine();
                    String name = scanner.nextLine();
                    companyDao.add(new Developer(name));
                }
            }
        }
    }

    /**
     * Car menu options - list/create cars linked to a company
     */
    void carMenu() {
        List<Developer> companies = companyDao.findAll();
        if (companies.isEmpty()) {return;}
        int option1 = scanner.nextInt();
        if (option1 == 0) {return;}
        Developer company = companies.get(option1 - 1);
        int option2 = -1;
        System.out.println("\n'" + company.getName() + "' company");
        while (option2 != 0) {
            System.out.println("1. Car list");
            System.out.println("2. Create a car");
            System.out.println("0. Back");
            option2 = scanner.nextInt();
            switch (option2) {
                case 0 -> {return;}
                case 1 -> {
                    carDao.findByParentId(company.getId());
                    System.out.println(); }
                case 2 -> {
                    System.out.println("\nEnter the car name: ");
                    String strip = scanner.nextLine();
                    String name = scanner.nextLine();
                    carDao.add(new Developer(name, company.getId()));
                    System.out.println();}
            }
        }
    }

    /**
     * Customer menu options - rent/return/list cars from specific companies
     */
    void customerMenu() {
        List<Developer> customerList = customerDao.findAll();
        if (customerList.isEmpty()) {return;}
        int input = scanner.nextInt();
        if (input == 0) {return;}
        Developer customer = customerList.get(input - 1);
        int customerId = customer.getId();
        int option2 = -1;
        while (option2 != 0) {
            System.out.println("\n1. Rent a car");
            System.out.println("2. Return a rented car");
            System.out.println("3. My rented car");
            System.out.println("0. Back");
            option2 = scanner.nextInt();
            switch (option2) {
                case 1 -> rentAcar(customerId);
                case 2 -> {
                    customer = customerDao.findById(customerId);
                    if (customer.getParent() == 0) {
                        System.out.println("You didn't rent a car!");
                        break;
                    } else {
                        customerDao.update(new Developer(customer.getId(), customer.getName(), 0));}
                        System.out.println("You've returned a rented car!");
                    }
                case 3 -> {
                    customer = customerDao.findById(customerId);
                    if (customer.getParent() == 0) {
                        System.out.println("You didn't rent a car!");
                    } else {
                        Developer car = carDao.findById(customer.getParent());
                        Developer company = companyDao.findById(car.getParent());
                        System.out.println("\nYour rented car:");
                        System.out.println(car.getName());
                        System.out.println("Company:");
                        System.out.println(company.getName());
                    }
                }
            }
        }
    }

    /**
     * Rent-a-car menu - to rent a car from a specific company listing only un-rented cars
     * @param customerId
     */
    void rentAcar(int customerId) {
        Developer customer = customerDao.findById(customerId);
        if (customer.getParent() == 0) {
            List<Developer> companies = companyDao.findAll();
            int option = scanner.nextInt();
            if (option == 0) {return;}
            Developer selectedCompany = companies.get(option - 1);

            List<Developer> cars = rentalDao.findByParentId(selectedCompany.getId());
            if (cars.isEmpty()) {return;}
            option = scanner.nextInt();
            if (option == 0) {return;}
            Developer selectedCar = cars.get(option - 1);
            customerDao.update(new Developer(customer.getId(), customer.getName(), selectedCar.getId()));
            System.out.println("You rented '" + selectedCar.getName() + "'");
        } else {
            System.out.println("You've already rented a car!");
        }
    }

}
