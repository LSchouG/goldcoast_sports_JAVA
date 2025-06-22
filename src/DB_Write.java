
/** ******************************************************************************
 * FileName:
 * Purpose: 1. Make a connection to the database using the app.config file.
 *          2. passing the data to the database
 * Author: Lars S Gregersen
 * Date: 13-5-2025
 * Version: 1.0
 * NOTES:
 ****************************************************************************** */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;


 /* ***********************************************************************
 * Method: DB_Write() Constructor
 * Purpose: 1. Initital private data fields
 *          2. Connect to databse
 *          3. Run SQL write statments (UPDATE; INSERT)
 * Input:  String sqp (SQL statement)
 * Output: none.
 * ***********************************************************************/
public class DB_Write {

    // Database connection details
    private String dbURL;
    private String usrID;
    private String usrPWD;
    private Connection conn;
    private Statement stmt;
    private String errorMessage;

    // Constructor - Executes SQL write/update query
    public DB_Write(String sql) {
        dbURL = "";
        usrID = "";
        usrPWD = "";
        errorMessage = "";
        conn = null;
        stmt = null;
               //Read external config file (app.config) which contains 3 lines of text
        try {
            // file io buffered reader (used to read externak file)
            BufferedReader br = new BufferedReader(new FileReader("app.config"));
            // get first line from ecternal file
            String line = br.readLine();
            // set line counter (first line read in)
            int lineCounter = 1;
            // loop while the line value in not null
            while (line != null) {
                //System.out.println("Line " + lineCounter + ": " + line); 
                switch (lineCounter) {
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
        } catch (IOException ioe) {
            errorMessage = ioe.getMessage() + "\n";
        } catch (Exception e) {
            errorMessage = e.getMessage() + "\n";
        }
        
        // connect to database, create the sql statment and execute the write stament
        try 
        {
            // make connection to database
            conn = java.sql.DriverManager.getConnection(dbURL, usrID, usrPWD);
            //creat statment object for connection
            stmt = conn.createStatement();
            // EXECUTE THE SQL STATEMENT
             stmt.executeUpdate(sql);
            // close the connection
            conn.close();
        } // end if try
        catch (SQLException sqle) 
        {
            errorMessage += sqle.getMessage() + "\n";
        } catch (Exception e) 
        {
            errorMessage += e.getMessage() + "\n";
        }
    }
 /* ***********************************************************************
 * Method: getErrorMessage()
 * Purpose: Return ErrorMessage
 * Input:  void
 * Output: String (ErrorMessage)
 * ***********************************************************************/
    // Get any error messages
    public String getErrorMessage() 
    {
        return errorMessage;
    }

    /*************************************************************************
    Method:     toString()
    Purpose:    
    Inputs:     
    Outputs:    
     * @return 
    *************************************************************************/
    @Override
    public String toString()
    {
        return " database URL = " + dbURL + " User ID = " + usrID + " User Password = " + usrPWD;
    }
}
