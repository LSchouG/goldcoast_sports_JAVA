/********************************************************************************
 * FileName: DB_Read.java
 * Purpose: 1. Make a connection to the database using the app.config file.
 *          2. passing a sql statement the database and storing the data in Arrays or Objects
 * Author:  Lars Schou Gregersen
 * Date:    25-4-2025
 * Version: 1.0
 * NOTES: 
 *******************************************************************************/
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DB_Read 
{

    // Database connection details
    private String dbURL = "jdbc:mysql://localhost:8889/db_e_sport"; // Change to your actual DB name
    private String usrID = "root";
    private String usrPWD = "root"; // Change if your MySQL has a password

    private Connection conn;
    private Statement stmt;
    public static ResultSet rs;
    public static int recordCountObject;
    private int recordCount;
    private String errorMessage;
    private Object[][] objDataSet;
    private String[] stringCSVData;
    private int maxCompID;

    // Constructor - Executes SQL query and processes results
    public DB_Read(String sql, String qryType) 
    {

        //initialise private data fields
        dbURL = ""; // Change to your actual DB name
        usrID = "";
        usrPWD = ""; // Change if your MySQL has a password
        recordCount = 0;
        errorMessage = "";
        objDataSet = null;
        stringCSVData = null;
        maxCompID = 0;
        //Read external config file (app.config) which contains 3 lines of text
        try 
        {
            // buffered reader (used to read external file)
            BufferedReader br = new BufferedReader(new FileReader("app.config"));
            // get first line from ecternal file
            String line = br.readLine();
            // set line counter (first line read in)
            int lineCounter = 1;
            // loop while the line value in not null
            while (line != null) 
            {
                //System.out.println("Line " + lineCounter + ": " + line); 
                switch (lineCounter) 
                {
                    //read db URL String
                    case 1:
                        dbURL = line.substring(6, line.length());
                        break;
                    //read db URL String
                    case 2:
                        usrID = line.substring(6, line.length());
                        break;
                    //read db URL String
                    case 3:
                        usrPWD = line.substring(7, line.length());
                        break;
                    default:
                        break;
                } // end of switch

                // read futher lines
                line = br.readLine();
                // increment line counter 
                lineCounter++;
            } //end of loop
            // close file io opbejct
            br.close();
        } 
        catch (IOException ioe) 
        {
            errorMessage = ioe.getMessage() + "\n";
        } 
        catch (Exception e) 
        {
            errorMessage = e.getMessage() + "\n";
        }
        try 
        {
            // Establish connection
            conn = DriverManager.getConnection(dbURL, usrID, usrPWD);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs = stmt.executeQuery(sql);

            if (rs != null) 
            {
                rs.beforeFirst();
                rs.last();
                recordCount = rs.getRow();
            }
            // display attributes of each record in a while loop
            if (recordCount > 0) 
            {
                // declare counter and set to zero (used for the array indexes) 
                int counter = 0;
                // insatiate arrays
                objDataSet = new Object[recordCount][];
                stringCSVData = new String[recordCount];
                maxCompID = 0;

                // start at beginnig of record set
                rs.beforeFirst();
                // lop through the resultset
                while (rs.next()) 
                {
                    // get all competitions data 
                    if (qryType.equals("competition")) 
                    {
                        // create object array for each row (contains all 5 requrired data item for one competition)
                        Object[] obj = new Object[5];
                        obj[0] = rs.getString("gameName");
                        obj[1] = rs.getString("team1");
                        obj[2] = rs.getInt("team1Points");
                        obj[3] = rs.getString("team2");
                        obj[4] = rs.getInt("team2Points");

                        objDataSet[counter] = obj;
                        counter++;
                    } 
                    else if (qryType.equals("team")) 
                    {
                        // get all nemas and put them into a string [] array
                        stringCSVData[counter] = rs.getString("name") + ","
                                               + rs.getString("contact") + ","
                                               + rs.getString("phone") + ","
                                               + rs.getString("email");
                        counter++;
                    } 
                    else if (qryType.equals("event")) 
                    {
                        // get all events and put them into a string [] array
                        stringCSVData[counter] = rs.getString("name") + ","
                                               + rs.getString("date") + ","
                                               + rs.getString("location");
                        counter++;

                    } 
                    else if (qryType.equals("game")) 
                    {
                        // get all events and put them into a string [] array
                        stringCSVData[counter] = rs.getString("name") + ","
                                               + rs.getString("type");
                        counter++;

                    } 
                    else if (qryType.equals("leaderBoard")) 
                    {
                        // Each row: [Team Name, Total Points]
                        Object[] obj = new Object[2];
                        obj[0] = rs.getString("name"); // alias from SQL
                        obj[1] = rs.getInt("points"); // alias from SQL
                        objDataSet[counter] = obj;
                        counter++;
                    }
                    else if (qryType.equals("maxcompID")) 
                    {
                        maxCompID = rs.getInt("maxID");
                    }
                } // end while loop
                // close the connection to the database
                conn.close();
            } // end if (recordCount > 0)
        } //end of try block
        catch (SQLException sqlE) 
        {
            errorMessage += sqlE.getMessage();
        } catch (Exception e) 
        {
            errorMessage += e.getMessage();
        }

    } // end of constructor        

    // Getters for accessing retrieved data
    public int getRecordCount() 
    {
        return recordCount;
    }

    public int recordCountObject() 
    {
        return recordCountObject;
    }

    public String getErrorMessage() 
    {
        return errorMessage;
    }

    public int getMaxCompID() 
    {
        return maxCompID;
    }

    public String[] getStringData() 
    {
        return stringCSVData;
    }

    public Object[][] getObjDataSet() 
    {
        return objDataSet;
    }
    /*************************************************************************
    Method:     formatDateToString()
    Purpose:    Converts a date string from "yyyy-MM-dd" format number format 
                to a number and letter format for the database         
    Inputs:     String containing a date in "yyyy-MM-dd" format
    Outputs:    Returns a String number and letter in the format of "dd-MMM-yyyy"
    *************************************************************************/
    public static String formatDateToString(String inputDateString)
    {
        // vreate data string version
        String formattedDataStr = "";
        String day = inputDateString.substring(8,10);
        String year = inputDateString.substring(0,4);
        String month = "Jan";
        String MonthNbr = inputDateString.substring(5,7);
        switch (MonthNbr)
        {
            case "2":
                month = "Feb";
                break;
            case "3":
                month = "Mar";
                break;
            case "4":
                month = "Apr";
                break;
            case "5":
                month = "May";
                break;
            case "6":
                month = "Jun";
                break;
            case "7":
                month = "Jul";
                break;
            case "8":
                month = "Agu";
                break;
            case "9":
                month = "Sep";
                break;
            case "10":
                month = "Oct";
                break;
            case "11":
                month = "Nov";
                break;
            case "12":
                month = "Dec";
                break;
        }
        formattedDataStr = day + "-" + month + "-" + year;
        
        return formattedDataStr;
    }
    /*************************************************************************
    Method:     toString()
    Purpose:    Returns a string of the database connection 
                details.
    Inputs:     None
    Outputs:    A String with the database URL, user ID, and user password.
    *************************************************************************/
    @Override
    public String toString()
    {
        return " database URL = " + dbURL + " User ID = " + usrID + " User Password = " + usrPWD;
    }
}