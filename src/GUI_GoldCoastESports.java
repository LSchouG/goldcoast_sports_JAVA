
/** ******************************************************************************
 * FileName: GoldCoast ESports
 * Purpose: to manage the games of all the events and teams for the gold coast esprtos
 * Author: Lars S Gregersen
 * Date: 13-5-2025
 * Version: 1.0
 * NOTES:
 ****************************************************************************** */
import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.table.DefaultTableModel;

public class GUI_GoldCoastESports extends javax.swing.JFrame {

    /**
     * Creates new form GUI_GoldCoastESports
     */
    private DB_Read dbRead;
    private DB_Write dbWrite;
    private int recordsFound;
    private DefaultTableModel compResultsTableModel;
    private DefaultTableModel leaderBoardEventTableModel;

    private String sql; // for SQL strings
    private String chosenEvent; // tracks the chosen event
    private String chosenTeam;  // tracks the chosen team
    private String[] teamsCSVStrArray; // use to populate team combo-boxes
    private String[] gamesCSVStrArray;  // use to populate game combo-box
    private String[] eventsCSVStrArray;  // use to populate event combo-boxes
    private Object[][] objArrayForTableDisplay;// 2D array used for populating the JTables
    // workaround to avoid triggering combo-box events when setting
    // up all combo boxes during initialisation
    private boolean comboBoxStatus;

    public GUI_GoldCoastESports() 
    {

        initComponents();
        this.setSize(800, 750);

        /************* CUSTOM JTABLES USING DEFAULT TABLE MODEL ***************/
        // initialise and set up customised table model for competition results
        String[] columnNames_CompResults = new String[]{"Game", "Team 1", "Pt", "Team 2", "Pt"};
        compResultsTableModel = new DefaultTableModel();
        compResultsTableModel.setColumnIdentifiers(columnNames_CompResults);
        compResultsjTable1Tab1.setModel(compResultsTableModel);

        // initialise and set up customised table model for leader board 
        String[] columnNames_EventsLeaderBoard = new String[]{"Team", "Total points"};
        leaderBoardEventTableModel = new DefaultTableModel();
        leaderBoardEventTableModel.setColumnIdentifiers(columnNames_EventsLeaderBoard);
        leaderBoardjTable1Tab1.setModel(leaderBoardEventTableModel);

        /****************** INITIALISE PRIVATE DATA FIELDS ********************/
        recordsFound = 0;
        sql = "";
        chosenEvent = "All Events";
        chosenTeam = "All Teams";
        dbRead = null;
        dbWrite = null;
        teamsCSVStrArray = null;
        gamesCSVStrArray = null;
        eventsCSVStrArray = null;
        objArrayForTableDisplay = null;
        // NOTE: This prevents the combo box event handlers from being triggered when initialising them with string data      
        comboBoxStatus = false;

        DB_Read dbRead = new DB_Read(sql, "getObjDataSet");

        /*************** CUSTOMISE TABLE COLUMNS FOR ALL JTABLES **************/
        // customise column sizing for compResults_JTable
        resizeTableColumnsForCompResults();
        // customise column sizing for events leader board JTables
        resizeTableColumnsForEventsLeaderBoard();

        /************** DISPLAY COMPETITION RESULTS FOR ALL EVENTS ************/
        displayCompResults();

        /**************** EVENT LISTING IN JCOMBOBOX CONTROLS *****************/
        // set up event names in event_JComboBox control 
        // and newCompEvent_JComboBox
        displayEventListing();

        /**************** TEAM LISTING IN JCOMBOBOX CONTROLS ******************/
        // seat up team names for combo-boxes
        displayTeamListing();

        /***************** GAME LISTING IN JCOMBOBOX CONTROLS *****************/
        // set up game names for combo-box
        displayGameListing();

        /************************ DISPLAY LEADER BOARD ************************/
        // display leader board for all events showing accumulated points for all teams
        // note chosenEvent is equal to "All events"
        displayEventsLeaderBoard();

        /*********** SET UP LOCAL DATE IN newEventDate_JTextField *************/
        LocalDate dateObj = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String todaysDate = dateObj.format(formatter);
        newEventDate_JTextField.setText(todaysDate);
        newEventLocation_JTextField.setText("TAFE Coomera");

        /********************** SET comboBoxStatus*****************************/
        comboBoxStatus = true;
    }

   
    /**************************************************************************
    * Method: resizeTableColumnsForCompResults() 
    * Purpose: resize table columns for competition results JTable 
    * Inputs: void 
    * Outputs: void
    ***************************************************************************/
    private void resizeTableColumnsForCompResults() 
    {
        // "Game", "Team 1", "Points", "Team 2", "Points"
        float[] columnWidthPercentage = {0.3f, 0.3f, 0.05f, 0.3f, 0.05f};
        // Use TableColumnModel.getTotalColumnWidth() if table is included in a JScrollPane
        int tW = compResultsjTable1Tab1.getWidth();
        javax.swing.table.TableColumn column;
        javax.swing.table.TableColumnModel jTableColumnModel = compResultsjTable1Tab1.getColumnModel();
        int cantCols = jTableColumnModel.getColumnCount();
        for (int i = 0; i < cantCols; i++) 
        {
            column = jTableColumnModel.getColumn(i);
            int pWidth = Math.round(columnWidthPercentage[i] * tW);
            column.setPreferredWidth(pWidth);
        }
    }

    /***************************************************************************
    * Method: resizeTableColumnsForEventsLeaderBoard() 
    * Purpose: resize table columns for event leader board JTable 
    * Inputs: void 
    * Outputs: void
    ***************************************************************************/
    private void resizeTableColumnsForEventsLeaderBoard() 
    {
        // "Team", "Total Points"
        float[] columnWidthPercentage = {0.65f, 0.35f};
        // Use TableColumnModel.getTotalColumnWidth() if table is included in a JScrollPane
        int tW = leaderBoardjTable1Tab1.getWidth();
        javax.swing.table.TableColumn column;
        javax.swing.table.TableColumnModel jTableColumnModel = leaderBoardjTable1Tab1.getColumnModel();
        int cantCols = jTableColumnModel.getColumnCount();
        for (int i = 0; i < cantCols; i++) 
        {
            column = jTableColumnModel.getColumn(i);
            int pWidth = Math.round(columnWidthPercentage[i] * tW);
            column.setPreferredWidth(pWidth);
        }
    }

    /***************************************************************************
    * Method:  displayCompResults() 
    * Purpose: display competition results from the database and show them in the JTable.
    * Inputs:  Uses 'chosenEvent' and 'chosenTeam' to filter the results.
    * Outputs: Fills table with data
    * Notes:   Clears old data before adding new. Shows error if SQL fails.   
    ***************************************************************************/
    private void displayCompResults()
    {
        // start with the spl string to retrive all the competition results
        sql = "SELECT gameName, team1, team1Points, team2, team2Points "
                + "FROM competition";

        String compResultLabelText = "Competition Results";

        // check the string value of chosenEvent -- this can cgange when a paticular event is chosen
        // check if it is NOT "All events" ... if it is, then add Where clause
        if (!chosenEvent.equals("All Events")) 
        {
            sql += " WHERE eventName = '" + chosenEvent + "'";
            compResultLabelText += " for " + chosenEvent;

            // check chosenTeam value is NOT "ALL teams" ... if it is, then add AND clause
            if (!chosenTeam.equals("All Teams")) 
            {
                sql += " AND (team1 = '" + chosenTeam + "' OR team2 ='" + chosenTeam + "')";
                compResultLabelText += " (" + chosenTeam + ")";
            } 
            else 
            {
                compResultLabelText += " (all teams)";
            }
        } 
        else 
        {
            // at this point, the chosenEvent is "All events"
            compResultLabelText += " for all events";
            // check chosenTeam value is NOT "ALL teams" ... if it is, then add AND clause
            if (!chosenTeam.equals("All Teams")) 
            {
                sql += " WHERE (team1 = '" + chosenTeam + "' OR team2 ='" + chosenTeam + "')";
                compResultLabelText += " (" + chosenTeam + ")";
            } 
            else 
            {
                compResultLabelText += " (all teams)";
            }
        }
        jLabel3.setText(compResultLabelText);

        // test only: desplay full sql String for testing
        //System.out.println("SQL used for competition table display: " + sql);
        // create new instance for dbREAD
        dbRead = new DB_Read(sql, "competition");

        //test only: display sql error if any exists (if it does, get out of the method)
        if (dbRead.getErrorMessage().isEmpty() == false) 
        {
            System.out.println("ERROR: " + dbRead.getErrorMessage());
            return;
        }

        // check if there are rows that the result from the sql execution from competition.
        if (dbRead.getRecordCount() > 0) 
        {
            // removes all existing rows from compResultsjTable1Tab1
            if (compResultsTableModel.getRowCount() > 0) 
            {
                for (int i = compResultsTableModel.getRowCount() - 1; i > -1; i--) 
                {
                    compResultsTableModel.removeRow(i);
                }
            }
        }
        // populate the rows of the competition result table
        if (dbRead.getObjDataSet() != null) 
        {
            // add data to tableModel
            for (int row = 0; row < dbRead.getObjDataSet().length; row++) 
            {
                compResultsTableModel.addRow(dbRead.getObjDataSet()[row]);

            }
            compResultsTableModel.fireTableDataChanged();
        } 
        else 
        {
            // at this stage, there are no rows resulting from the sql execution
            // remove all existing rows from compResults_Jtable
            if (compResultsTableModel.getRowCount() > 0) 
            {
                for (int i = compResultsTableModel.getRowCount() - 1; i > - 1; i--) 
                {
                    compResultsTableModel.removeRow(i);
                }
            }
        }
        if (compResultsTableModel.getRowCount() > 0) 
        {
            recordsFound = dbRead.getRecordCount();
            // display number of records found
            dispRecordTextF.setText(recordsFound + " records found");
        }
    }

    /***************************************************************************
    * Method:  displayEventsLeaderBoard() 
    * Purpose: display leaderboard results from the database and show them in the JTable.
    * Inputs:  Uses 'chosenEvent' to filter results.
    * Outputs: Fills 'leaderBoardEventTableModel' with data and updates 'jLabel4'.
    ***************************************************************************/
    private void displayEventsLeaderBoard()
    {
        String newTextForjLabel4 = "Event Leaderboard for ";

        if (chosenEvent.equals("All Events")) 
        {
            sql = "SELECT name, SUM(totalPoints) AS points "
                + "FROM (SELECT team.name, SUM(team1Points) AS totalPoints "
                + "FROM competition INNER JOIN team ON team.name = competition.team1 "
                + "GROUP BY team.name "
                + "UNION "
                + "SELECT team.name, SUM(team2Points) AS totalPoints "
                + "FROM competition INNER JOIN team ON team.name = competition.team2 "
                + "GROUP BY team.name) AS derivedTable "
                + "GROUP BY name ORDER BY points DESC;";
            System.out.println(sql);
            newTextForjLabel4 += "all events";
        } 
        else 
        {
            sql = "SELECT name, SUM(totalPoints) AS points "
                + "FROM (SELECT team.name, SUM(team1Points) AS totalPoints "
                + "FROM competition INNER JOIN team ON team.name = competition.team1 "
                + "WHERE competition.eventName = '" + chosenEvent + "' "
                + "GROUP BY team.name "
                + "UNION "
                + "SELECT team.name, SUM(team2Points) AS totalPoints "
                + "FROM competition INNER JOIN team ON team.name = competition.team2 "
                + "WHERE competition.eventName = '" + chosenEvent + "' "
                + "GROUP BY team.name) AS derivedTable "
                + "GROUP BY name ORDER BY points DESC;";
            System.out.println(sql);
            newTextForjLabel4 += chosenEvent;
        }

        jLabel4.setText(newTextForjLabel4);

        DB_Read dbRead = new DB_Read(sql, "leaderBoard");
        Object[][] leaderboardData = dbRead.getObjDataSet();

        String[] columnNames = {"Team", "Total Points"};

        if (leaderboardData != null && leaderboardData.length > 0) 
        {
            leaderBoardEventTableModel = new DefaultTableModel(leaderboardData, columnNames);
        } 
        else 
        {
            leaderBoardEventTableModel = new DefaultTableModel(new Object[][]{}, columnNames);
        }
        leaderBoardjTable1Tab1.setModel(leaderBoardEventTableModel);
    }

    /***************************************************************************
    * Method: displayEventListing() 
    * Purpose: populate event data from the database and in the combo boxes.
    * Inputs: None
    * Outputs: Fills 'eventComboBox1Tab1' and 'eventjComboBox1Tab2' with event names. 
    ***************************************************************************/
    private void displayEventListing()
    {
        sql = "SELECT name, date, location FROM event ORDER BY name";
        dbRead = new DB_Read(sql, "event");

        // test only: display sql Errors if any
        if (!dbRead.getErrorMessage().isEmpty()) 
        {
            System.out.println("ERROR: " + dbRead.getErrorMessage());
            return;
        }

        if (dbRead.getRecordCount() > 0) 
        {
            // assign event CSV array
            eventsCSVStrArray = dbRead.getStringData();

            // Remove existing items from event combo boxes
            eventComboBox1Tab1.removeAllItems();
            eventjComboBox1Tab2.removeAllItems();

            // Add "All Events" as the first selectable option
            eventComboBox1Tab1.addItem("All Events");

            if (eventsCSVStrArray == null) 
            {
                System.out.println("eventsCSVStrArray is null - possible SQL error");
                return;
            }

            // Loop through array and add event names to the combo boxes
            for (int i = 0; i < eventsCSVStrArray.length; i++) 
            {
                String[] splitEventStr = eventsCSVStrArray[i].split(",");
                String eventName = splitEventStr[0];
                String eventDate = splitEventStr[1];
                String eventLocation = splitEventStr[2];

                String displayString = eventName + " (" + eventDate + " " + eventLocation + ")";
                eventComboBox1Tab1.addItem(displayString);
                eventjComboBox1Tab2.addItem(eventName);

            } // end for loop
        } // end if
    }

    /**************************************************************************
    * Method: displayTeamListing() 
    * Purpose: populate team data from the database and in the combo boxes.
    * Inputs: None.
    * Outputs: Fills team combo boxes with team names and sets team details in text fields.
    *************************************************************************/
    private void displayTeamListing()
    {
        sql = "SELECT name,contact, phone, email FROM team ORDER BY name";
        dbRead = new DB_Read(sql, "team");

        // test only: display sql Errors if any
        if (dbRead.getErrorMessage().isEmpty() == false) 
        {
            System.out.println("ERROR: " + dbRead.getErrorMessage());
            return;
        }
        if (dbRead.getRecordCount() > 0) 
        {
            // assign team CSVStrArray
            teamsCSVStrArray = dbRead.getStringData();

            // Remove any existing items from JcomboBox controles that contain team details
            teamJComboBox2Tab1.removeAllItems();
            team1jComboBox3Tab2.removeAllItems();
            team2jComboBox4Tab2.removeAllItems();
            teamNamejComboBox5Tab4.removeAllItems();

            // add "All Teams" to teamJComboBox2Tab1
            teamJComboBox2Tab1.addItem("All Teams");
            if (teamsCSVStrArray == null) 
            {
                System.out.println("teamsCSVStrArray is null - possible SQL error");
                return;
            }
            // add team details to the JComboBox's 
            // loop through the teamsCSVStrArray
            for (int i = 0; i < teamsCSVStrArray.length; i++) 
            {
                String[] splitTeamStr = teamsCSVStrArray[i].split(",");
                teamJComboBox2Tab1.addItem(splitTeamStr[0]);
                team1jComboBox3Tab2.addItem(splitTeamStr[0]);
                team2jComboBox4Tab2.addItem(splitTeamStr[0]);
                teamNamejComboBox5Tab4.addItem(splitTeamStr[0]);
                // add team details to exiting team tab panel 
                if (i == 0) 
                {
                    contactNamejTextField1Tab4.setText(splitTeamStr[1]);
                    phoneNumberjTextField2Tab4.setText(splitTeamStr[2]);
                    emailAddressjTextField3Tab4.setText(splitTeamStr[3]);
                }
            } // end loop
        } // end if (dbRead.getRecordCount() > 0)
    }

    /***************************************************************************
    * Method: displayGameListing() 
    * Purpose: Get game data from the database and show it in the combo box.
    * Inputs: None.
    * Outputs: Fills 'gamejComboBox2Tab2' with all game names.
    ***************************************************************************/
    private void displayGameListing()
    {
        sql = "SELECT name, type FROM game ORDER BY name";
        dbRead = new DB_Read(sql, "game");

        // test only: display sql Errors if any
        if (dbRead.getErrorMessage().isEmpty() == false) 
        {
            System.out.println("ERROR: " + dbRead.getErrorMessage());
            return;
        }
        System.out.println("RecordCount -> " + dbRead.getRecordCount());
        if (dbRead.getRecordCount() > 0) 
        {
            // assign game CSVStrArray
            gamesCSVStrArray = dbRead.getStringData();

            // Remove any existing items from JcomboBox controles that contain game details
            gamejComboBox2Tab2.removeAllItems();

            if (gamesCSVStrArray == null) 
            {
                System.out.println("gamesCSVStrArray is null - possible SQL error");
                return;
            }
            // add game details to the JComboBox's 
            // loop through the teamsCSVStrArray
            for (int i = 0; i < gamesCSVStrArray.length; i++) 
            {
                String[] splitGameStr = gamesCSVStrArray[i].split(",");
                gamejComboBox2Tab2.addItem(splitGameStr[0]);

            } // end loop
        } // end if (dbRead.getRecordCount() > 0)
    }
    
    /**************************************************************************
    * Method: exportCSVData(DefaultTableModel tableModel, String resultType) 
    * Purpose: Export table data to a CSV file.
    * Inputs:  tableModel and a result typw.
    * Outputs: Saves the data as a CSV file.  
    ***************************************************************************/
    private void exportCSVData(DefaultTableModel tableModel, String resultType) 
    {
        // 1. Check if the table is empty
        if (tableModel.getRowCount() == 0) 
        {
            JOptionPane.showMessageDialog(this, "Sorry, there is no " + resultType + " data to save.\nSelect another option which contains result", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 2. Set up file chooser
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save CSV File");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) 
        {
            try {
                // 3. Get selected file path
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();

                // Ensure file ends with .csv
                if (!filePath.toLowerCase().endsWith(".csv")) 
                {
                    filePath += ".csv";
                }

                // 4. Create file writer
                FileWriter writer = new FileWriter(filePath);

                // 5. Write column headers
                for (int i = 0; i < tableModel.getColumnCount(); i++) 
                {
                    writer.append(tableModel.getColumnName(i)).append(",");
                }
                writer.append("\n");

                // 6. Write table rows
                for (int i = 0; i < tableModel.getRowCount(); i++) 
                {
                    for (int j = 0; j < tableModel.getColumnCount(); j++) 
                    {
                        writer.append(tableModel.getValueAt(i, j).toString()).append(",");
                    }
                    writer.append("\n");
                }

                // 7. Close writer manually
                writer.close();

                // 8. Show success message
                String fileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
                JOptionPane.showMessageDialog(this, "CSV data successfully written to file: " + resultType + " - " + fileName);

            } 
            catch (IOException e) 
            {
                JOptionPane.showMessageDialog(this, "Error exporting CSV: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /*************************************************************************
    * Method: addNewCompetitionResults() 
    * Purpose: Add a new competition result to the database after validating input.
    * Inputs: Uses selected values from combo boxes and text fields.
    * Outputs: Inserts a new competition record, updates tables, and shows messages.
    * Notes: Checks for valid scores, duplicate entries, and confirms before saving.   
    ************************************************************************/
    private void addNewCompetitionResults() 
    {
        // 1. Get selected values from combo boxes
        String Event = (String) eventjComboBox1Tab2.getSelectedItem();
        String Game = (String) gamejComboBox2Tab2.getSelectedItem();
        String Team1 = (String) team1jComboBox3Tab2.getSelectedItem();
        String Team2 = (String) team2jComboBox4Tab2.getSelectedItem();

        // 2. Validate selections
        if (Event == null || Game == null || Team1 == null || Team2 == null) 
        {
            JOptionPane.showMessageDialog(this, "Please select all fields (Event, Game, Team 1, Team 2).", "Missing Information", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (Team1.equals(Team2)) 
        {
            JOptionPane.showMessageDialog(this, "Team 1 and Team 2 cannot be the same.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int TeamPoint1;
        int TeamPoint2;

        try 
        {
            TeamPoint1 = Integer.parseInt(jTextField2.getText());
        } 
        catch (NumberFormatException e) 
        {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for Team 1 points.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try 
        {
            TeamPoint2 = Integer.parseInt(jTextField3.getText());
        } 
        catch (NumberFormatException e) 
        {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for Team 2 points.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // 3. Validate total points logic (sum must be 2)
        boolean validScore = (TeamPoint1 >= 0 && TeamPoint1 <= 2)
                          && (TeamPoint2 >= 0 && TeamPoint2 <= 2)
                          && (TeamPoint1 + TeamPoint2 == 2);

        if (!validScore) 
        {
            JOptionPane.showMessageDialog(this, "Each team must score 0, 1, or 2 points, and total must be 2.", "Scoring Rules", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 4. Get the next competitionID
        String getMaxIDQuery = "SELECT MAX(competitionID) AS maxID FROM competition";
        int nextCompetitionID;

        try 
        {
            DB_Read dbRead = new DB_Read(getMaxIDQuery, "maxcompID");

            if (dbRead.getErrorMessage().isEmpty()) 
            {
                nextCompetitionID = dbRead.getMaxCompID() + 1;
            } 
            else 
            {
                JOptionPane.showMessageDialog(this, "Error getting competition ID:\n" + dbRead.getErrorMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } 
        catch (Exception ex) 
        {
            JOptionPane.showMessageDialog(this, "Unexpected error: " + ex.getMessage(), "System Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        // 4.5 Check for duplicate match (same event, same game, same teams)
        String checkDuplicateSQL = "SELECT * FROM competition WHERE eventName = '" + Event 
                                 + "' AND gameName = '" + Game + "' " + "AND ((team1 = '" 
                                 + Team1 + "' AND team2 = '" + Team2 + "') OR "
                                 + "(team1 = '" + Team2 + "' AND team2 = '" + Team1 + "'))";

        DB_Read duplicateCheck = new DB_Read(checkDuplicateSQL, "checkOnly");

        if (duplicateCheck.getRecordCount() > 0) 
        {
            JOptionPane.showMessageDialog(this, "This match-up has already been recorded for the same event and game.", "Duplicate Match", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // 4.6 Ask user to confirm
        int confirmation = JOptionPane.showConfirmDialog(this, "Are you sure you want to save this new competition result?", "Confirm Submission", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirmation == JOptionPane.NO_OPTION) 
        {
            return; // Cancel the insert
        }

        // 5. Build and run INSERT query
        String insertSQL = "INSERT INTO competition (competitionID, eventName, gameName, team1, team1Points, team2, team2Points) "
                + "VALUES (" + nextCompetitionID + ", '" + Event + "', '" + Game + "', '" + Team1 + "', "
                + TeamPoint1 + ", '" + Team2 + "', " + TeamPoint2 + ")";

        DB_Write dbWrite = new DB_Write(insertSQL);

        if (dbWrite.getErrorMessage().isEmpty()) 
        {
            JOptionPane.showMessageDialog(this, "Competition results added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

            // Clear inputs
            jTextField2.setText("");
            jTextField3.setText("");

            // Refresh displays
            comboBoxStatus = false;
            displayCompResults();
            displayEventsLeaderBoard();
            comboBoxStatus = true;

        } 
        else 
        {
            JOptionPane.showMessageDialog(this, "Failed to add competition results:\n" + dbWrite.getErrorMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /***************************************************************************
    * Method: addNewEvent() 
    * Purpose: Add a new event to the database after validating input fields.
    * Inputs: Gets event name, date, and location from text fields.
    * Outputs: Inserts a new event record, and refreshes the event list.
    ***************************************************************************/
    private void addNewEvent() 
    {
        // 1. Retrieve values from the text fields
        String eventName = jTextField11.getText().trim();
        String date = newEventDate_JTextField.getText().trim();
        String location = newEventLocation_JTextField.getText().trim();
        String errorMessage = "";
  
        // 2. Check that no fields are empty
        if (eventName.isEmpty()) 
        {
            errorMessage += "- Event name is required\n";
        }
        if (date.isEmpty()) 
        {
            errorMessage += "- Date (yyyy-MM-dd) is required\n";
        }
        if (location.isEmpty()) 
        {
            errorMessage += "- Location is required\n";
        }

        // If any errors were found, show them all at once
        if (!errorMessage.isEmpty()) 
        {
            JOptionPane.showMessageDialog(this, "ERROR(S) DETECTED\n" + errorMessage, "ERROR(S) DETECTED", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 3. Validate date format (yyyy-MM-dd)
        String datePattern = "yyyy-MM-dd";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(datePattern);

        try 
        {
            LocalDate.parse(date, formatter); // will throw if format is incorrect
        } 
        catch (DateTimeParseException e) 
        {
            errorMessage += "- Event date must be 10 chars - format: yyyy-MM-dd";
            JOptionPane.showMessageDialog(this, "ERROR(S) DETECTED\n" + errorMessage, "ERROR(S) DETECTED", JOptionPane.ERROR_MESSAGE);
            return;
        }
       // 3.1 Ask user to confirm
        int confirmation = JOptionPane.showConfirmDialog(this, "You are about to add a new ecent for: " + eventName + "\nDo you wish to continue?", "ADD NEW EVENT", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirmation == JOptionPane.NO_OPTION) 
        {
            return; // Cancel the insert
        }
        // 4. Construct SQL query for insert
        String sql = "INSERT INTO event (name, date, location) "
                + "VALUES ('" + eventName + "', '" + date + "', '" + location + "')";

        // 5. Execute the query using DB_Write
        DB_Write dbWrite = new DB_Write(sql);

        // 6. Handle result
        if (dbWrite.getErrorMessage() != null && !dbWrite.getErrorMessage().isEmpty()) 
        {
            JOptionPane.showMessageDialog(this, "Error adding new event: " + dbWrite.getErrorMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        } 
        else 
        {
            JOptionPane.showMessageDialog(this, "New event added successfully!", "ADD NEW EVENT", JOptionPane.INFORMATION_MESSAGE);

            // 7. Clear input fields
            jTextField11.setText("");
            comboBoxStatus = false;
            // 8. Refresh event list
            displayEventListing();
            displayTeamListing();
            comboBoxStatus = true;
        }
    }

    
    /***************************************************************************
    * Method:  addNewTeam() 
    * Purpose: Add a new team to the database after validating input fields.
    * Inputs:  Gets team name, contact person, phone, and email from text fields.
    * Outputs: Inserts a new team database, updates combo boxes.
    ***************************************************************************/
    private void addNewTeam()
    {   
        // Get all Data requred
        String newTeamName = jTextField4.getText().trim();
        String newContactPerson = jTextField5.getText().trim();
        String newContactPhone = jTextField6.getText().trim();
        String newContactEmail = jTextField7.getText().trim();

        //2 Check if all new team data is present and accurate 
        boolean errorStatus = false;
        String errorMessage = "ERROR(S) DETECTED:\n";

        //2.1 check if newTeamName is empty
        if (newTeamName.isEmpty()) 
        {
            errorStatus = true;
            errorMessage += "- unique team name required\n";
        } 
        else 
        {
            // else, it is not emty .. so check if newTeamName already exists
            for (int i = 0; i < teamsCSVStrArray.length; i++) 
            {
                String[] splitTeamStr = teamsCSVStrArray[i].split(",");
                if (newTeamName.equals(splitTeamStr[0])) 
                {
                    errorStatus = true;
                    errorMessage += "- team name already exists in the database (must be unique) \n";
                    break;
                }
            }
        }

        // 2.2 check if newContactPerson is empty
        if (newContactPerson.isEmpty()) 
        {
            errorStatus = true;
            errorMessage += "- Contact person name required\n";
        }
        // 2.3 check if newContactPhone is empty
        if (newContactPhone.isEmpty()) 
        {
            errorStatus = true;
            errorMessage += "- Contact phone number required\n";
        }
        // 2.4 check if newContactEmail is empty
        if (newContactEmail.isEmpty()) 
        {
            errorStatus = true;
            errorMessage += "- Contact email address required\n";
        }
        // 2.5 final error check
        if (errorStatus == true) 
        {
            // if errorStatus is true the display  error messeage in pop-up
            javax.swing.JOptionPane.showMessageDialog(null, errorMessage, "ERRORS DETECTED!", javax.swing.JOptionPane.ERROR_MESSAGE);
            // then, exit the method at this point
            return;
        }
        // 3. Confirm to proceed or not (note: No errors foun at this point)
        int yesOrNo = javax.swing.JOptionPane.showConfirmDialog(null, "You are about to save a new team for: " + newTeamName + "\nDo you wish to continue?", "ADD NEW TEAM", javax.swing.JOptionPane.YES_NO_OPTION);
        if (yesOrNo == JOptionPane.NO_OPTION) 
        {
            // exit
            System.out.println("ADD NEW TEAM: " + newTeamName + " cancelled");
        } 
        else 
        {

            sql = "INSERT INTO team (name, contact, phone, email) VALUES ('"
                   + newTeamName + "', '" + newContactPerson + "', '" + newContactPhone
                   + "', '" + newContactEmail + "')";

            dbWrite = new DB_Write(sql);

            //check for any error message 
            // if no error message INSERT sql is successful.
            if (dbWrite.getErrorMessage().equals("")) 
            {
                System.out.println("Successful write opreation to database");
                // add new team to teamStrArray String[] array 
                ArrayList<String> arrayListTeam = new ArrayList<>(Arrays.asList(teamsCSVStrArray));
                String newTeamStr = newTeamName + "," + newContactPerson + "," + newContactPhone + "," + newContactEmail;
                arrayListTeam.add(newTeamStr);
                teamsCSVStrArray = arrayListTeam.toArray(new String[arrayListTeam.size()]);
               
                comboBoxStatus = false;
                // add new team name to the 4 Jcombobox controls
                teamJComboBox2Tab1.addItem(newTeamName);
                team1jComboBox3Tab2.addItem(newTeamName);
                team2jComboBox4Tab2.addItem(newTeamName);
                teamNamejComboBox5Tab4.addItem(newTeamName);
                comboBoxStatus = true;
            
                // Clear input fields
                jTextField4.setText("");
                jTextField5.setText("");
                jTextField6.setText("");
                jTextField7.setText("");

                //display updated leader board (event or total)
                displayEventsLeaderBoard();
            } 
            else 
            {
                System.out.println(dbWrite.getErrorMessage());
            }
        }
    }
    
    /***************************************************************************
    * Method: updateTeam() 
    * Purpose: Display the contact details of the selected team in the text fields.
    * Inputs: Uses 'chosenTeam' to find the matching team in 'teamsCSVStrArray'.
    * Outputs: Sets contact name, phone, and email text fields with the team’s details.
     **************************************************************************/
    private void updateTeam() 
    {
        // Loop through the teamsCSVStrArray to find the matching team
        for (int i = 0; i < teamsCSVStrArray.length; i++) 
        {
            String[] splitTeamStr = teamsCSVStrArray[i].split(",");

            if (splitTeamStr[0].equals(chosenTeam)) 
            {
                contactNamejTextField1Tab4.setText(splitTeamStr[1]);
                phoneNumberjTextField2Tab4.setText(splitTeamStr[2]);
                emailAddressjTextField3Tab4.setText(splitTeamStr[3]);
                break; // Stop looping once we've found and updated the match
            }
        }
    }

    /***************************************************************************
    * Method: saveUpdatedTeam() 
    * Purpose: Update an existing team’s contact details in the database.
    * Inputs: Gets updated contact name, phone, and email from text fields.
    * Outputs: Updates the database record, refreshes team combo boxes, and shows messages.
    * Notes: Validates input and asks for confirmation before saving.
    ***************************************************************************/     
    private void saveUpdatedTeam() 
    {

        // Get updated data from text fields
        String updatedContact = contactNamejTextField1Tab4.getText().trim();
        String updatedPhone = phoneNumberjTextField2Tab4.getText().trim();
        String updatedEmail = emailAddressjTextField3Tab4.getText().trim();

        // Input validation (optional)
        if (updatedContact.isEmpty() || updatedPhone.isEmpty() || updatedEmail.isEmpty()) 
        {
            JOptionPane.showMessageDialog(this, "All fields must be filled out.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 4.6 Ask user to confirm
        int confirmation = JOptionPane.showConfirmDialog(this, "You are about to update the team details for: " + chosenTeam + "\n Do you wish to continue?", "UPDATE EXISTING TEAM", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirmation == JOptionPane.NO_OPTION) 
        {
            return; // Cancel the insert
        }

        // Prepare SQL update statement
        String sql = "UPDATE team SET contact = '" + updatedContact + "', phone = '" + updatedPhone + "', email = '" + updatedEmail + "' "
                   + "WHERE name = '" + chosenTeam + "'";

        // Execute using DB_Write
        DB_Write dbWrite = new DB_Write(sql);

        if (dbWrite.getErrorMessage().isEmpty()) 
        {
            JOptionPane.showMessageDialog(this, "Team details updated successfully.");
            comboBoxStatus = false;
            displayTeamListing();
            comboBoxStatus = true; // Refresh the combo boxes
        } 
        else 
        {
            JOptionPane.showMessageDialog(this, "Error updating team:\n" + dbWrite.getErrorMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /*
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        BodyjPanel = new javax.swing.JPanel();
        headerjPanel = new javax.swing.JPanel();
        headerimgjLabel = new javax.swing.JLabel();
        BodyjTabbedPane = new javax.swing.JTabbedPane();
        eventCompResultjPanel1Tab1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        eventComboBox1Tab1 = new javax.swing.JComboBox<>();
        teamJComboBox2Tab1 = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        compResultsjTable1Tab1 = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        leaderBoardjTable1Tab1 = new javax.swing.JTable();
        jLabel4 = new javax.swing.JLabel();
        dispRecordTextF = new javax.swing.JTextField();
        exportCompResultjButton1Tab1 = new javax.swing.JButton();
        exportLeaderBoardjButton2Tab1 = new javax.swing.JButton();
        addNewCompetitionResultjPanel2Tab2 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        eventjComboBox1Tab2 = new javax.swing.JComboBox<>();
        gamejComboBox2Tab2 = new javax.swing.JComboBox<>();
        team1jComboBox3Tab2 = new javax.swing.JComboBox<>();
        team2jComboBox4Tab2 = new javax.swing.JComboBox<>();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jTextField2 = new javax.swing.JTextField();
        jTextField3 = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();
        addNewTeamjPanel3Tab3 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jTextField4 = new javax.swing.JTextField();
        jTextField5 = new javax.swing.JTextField();
        jTextField6 = new javax.swing.JTextField();
        jTextField7 = new javax.swing.JTextField();
        jButton4 = new javax.swing.JButton();
        updateExistingTeamjPanel4Tab4 = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        teamNamejComboBox5Tab4 = new javax.swing.JComboBox<>();
        contactNamejTextField1Tab4 = new javax.swing.JTextField();
        phoneNumberjTextField2Tab4 = new javax.swing.JTextField();
        emailAddressjTextField3Tab4 = new javax.swing.JTextField();
        jButton5 = new javax.swing.JButton();
        addNewEventjPanel5Tab5 = new javax.swing.JPanel();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jTextField11 = new javax.swing.JTextField();
        newEventDate_JTextField = new javax.swing.JTextField();
        newEventLocation_JTextField = new javax.swing.JTextField();
        jButton6 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(800, 600));

        BodyjPanel.setPreferredSize(new java.awt.Dimension(800, 800));

        headerjPanel.setPreferredSize(new java.awt.Dimension(800, 110));

        headerimgjLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/img/goldcoast_esports_v2.jpg"))); // NOI18N

        javax.swing.GroupLayout headerjPanelLayout = new javax.swing.GroupLayout(headerjPanel);
        headerjPanel.setLayout(headerjPanelLayout);
        headerjPanelLayout.setHorizontalGroup(
            headerjPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(headerimgjLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        headerjPanelLayout.setVerticalGroup(
            headerjPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(headerimgjLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        BodyjTabbedPane.setPreferredSize(new java.awt.Dimension(800, 690));

        jLabel1.setText("Event:");

        jLabel2.setText("Team:");

        eventComboBox1Tab1.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                eventComboBox1Tab1ItemStateChanged(evt);
            }
        });
        eventComboBox1Tab1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                eventComboBox1Tab1ActionPerformed(evt);
            }
        });

        teamJComboBox2Tab1.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                teamJComboBox2Tab1ItemStateChanged(evt);
            }
        });
        teamJComboBox2Tab1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                teamJComboBox2Tab1ActionPerformed(evt);
            }
        });

        jLabel3.setText("Competition result for <code>");

        compResultsjTable1Tab1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(compResultsjTable1Tab1);

        leaderBoardjTable1Tab1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane2.setViewportView(leaderBoardjTable1Tab1);

        jLabel4.setText("Leaderboard for:");

        dispRecordTextF.setText("<code>");

        exportCompResultjButton1Tab1.setText("Export competition result as CSV file");
        exportCompResultjButton1Tab1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onClickExportResult(evt);
            }
        });

        exportLeaderBoardjButton2Tab1.setText("Export board as CSV file");
        exportLeaderBoardjButton2Tab1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onClickExportLeaderBoard(evt);
            }
        });

        javax.swing.GroupLayout eventCompResultjPanel1Tab1Layout = new javax.swing.GroupLayout(eventCompResultjPanel1Tab1);
        eventCompResultjPanel1Tab1.setLayout(eventCompResultjPanel1Tab1Layout);
        eventCompResultjPanel1Tab1Layout.setHorizontalGroup(
            eventCompResultjPanel1Tab1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(eventCompResultjPanel1Tab1Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addGroup(eventCompResultjPanel1Tab1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(eventCompResultjPanel1Tab1Layout.createSequentialGroup()
                        .addComponent(dispRecordTextF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(101, 101, 101)
                        .addComponent(exportCompResultjButton1Tab1))
                    .addComponent(jLabel3)
                    .addComponent(jLabel2)
                    .addComponent(jLabel1)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 484, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(eventCompResultjPanel1Tab1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(eventCompResultjPanel1Tab1Layout.createSequentialGroup()
                        .addGap(90, 90, 90)
                        .addComponent(exportLeaderBoardjButton2Tab1)
                        .addContainerGap(35, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, eventCompResultjPanel1Tab1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(eventCompResultjPanel1Tab1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(eventCompResultjPanel1Tab1Layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(eventCompResultjPanel1Tab1Layout.createSequentialGroup()
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                .addGap(18, 18, 18))))))
            .addGroup(eventCompResultjPanel1Tab1Layout.createSequentialGroup()
                .addGap(183, 183, 183)
                .addGroup(eventCompResultjPanel1Tab1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(eventComboBox1Tab1, 0, 400, Short.MAX_VALUE)
                    .addComponent(teamJComboBox2Tab1, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        eventCompResultjPanel1Tab1Layout.setVerticalGroup(
            eventCompResultjPanel1Tab1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(eventCompResultjPanel1Tab1Layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(eventCompResultjPanel1Tab1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(eventComboBox1Tab1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(36, 36, 36)
                .addGroup(eventCompResultjPanel1Tab1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(teamJComboBox2Tab1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(42, 42, 42)
                .addGroup(eventCompResultjPanel1Tab1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(eventCompResultjPanel1Tab1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 315, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 111, Short.MAX_VALUE)
                .addGroup(eventCompResultjPanel1Tab1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(dispRecordTextF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(exportCompResultjButton1Tab1)
                    .addComponent(exportLeaderBoardjButton2Tab1))
                .addGap(21, 21, 21))
        );

        BodyjTabbedPane.addTab("Event competition result", eventCompResultjPanel1Tab1);

        jLabel5.setText("Event:");

        jLabel6.setText("Game:");

        jLabel7.setText("Team 1:");

        jLabel8.setText("Team: 2");

        jLabel9.setText("Team 1 points:");

        jLabel10.setText("Team 2 points:");

        jTextField2.setSize(new java.awt.Dimension(64, 23));

        jTextField3.setSize(new java.awt.Dimension(64, 23));

        jButton3.setText("Add new competition result");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout addNewCompetitionResultjPanel2Tab2Layout = new javax.swing.GroupLayout(addNewCompetitionResultjPanel2Tab2);
        addNewCompetitionResultjPanel2Tab2.setLayout(addNewCompetitionResultjPanel2Tab2Layout);
        addNewCompetitionResultjPanel2Tab2Layout.setHorizontalGroup(
            addNewCompetitionResultjPanel2Tab2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addNewCompetitionResultjPanel2Tab2Layout.createSequentialGroup()
                .addGap(97, 97, 97)
                .addGroup(addNewCompetitionResultjPanel2Tab2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel8)
                    .addComponent(jLabel7)
                    .addComponent(jLabel6)
                    .addComponent(jLabel5))
                .addGap(102, 102, 102)
                .addGroup(addNewCompetitionResultjPanel2Tab2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(eventjComboBox1Tab2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(gamejComboBox2Tab2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(addNewCompetitionResultjPanel2Tab2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jButton3)
                        .addGroup(addNewCompetitionResultjPanel2Tab2Layout.createSequentialGroup()
                            .addGroup(addNewCompetitionResultjPanel2Tab2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(team1jComboBox3Tab2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(team2jComboBox4Tab2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGap(142, 142, 142)
                            .addGroup(addNewCompetitionResultjPanel2Tab2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel9)
                                .addComponent(jLabel10))
                            .addGap(54, 54, 54)
                            .addGroup(addNewCompetitionResultjPanel2Tab2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap(135, Short.MAX_VALUE))
        );
        addNewCompetitionResultjPanel2Tab2Layout.setVerticalGroup(
            addNewCompetitionResultjPanel2Tab2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addNewCompetitionResultjPanel2Tab2Layout.createSequentialGroup()
                .addGap(52, 52, 52)
                .addGroup(addNewCompetitionResultjPanel2Tab2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(eventjComboBox1Tab2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(28, 28, 28)
                .addGroup(addNewCompetitionResultjPanel2Tab2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(gamejComboBox2Tab2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(35, 35, 35)
                .addGroup(addNewCompetitionResultjPanel2Tab2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addGroup(addNewCompetitionResultjPanel2Tab2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(team1jComboBox3Tab2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel9)
                        .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(34, 34, 34)
                .addGroup(addNewCompetitionResultjPanel2Tab2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(team2jComboBox4Tab2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10)
                    .addComponent(jTextField3, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(64, 64, 64)
                .addComponent(jButton3)
                .addContainerGap(321, Short.MAX_VALUE))
        );

        BodyjTabbedPane.addTab("Add new competition result", addNewCompetitionResultjPanel2Tab2);

        jLabel11.setText("New team name:");

        jLabel12.setText("Contact name:");

        jLabel13.setText("Phone number:");

        jLabel14.setText("Email address:");

        jButton4.setText("Add new team");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout addNewTeamjPanel3Tab3Layout = new javax.swing.GroupLayout(addNewTeamjPanel3Tab3);
        addNewTeamjPanel3Tab3.setLayout(addNewTeamjPanel3Tab3Layout);
        addNewTeamjPanel3Tab3Layout.setHorizontalGroup(
            addNewTeamjPanel3Tab3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addNewTeamjPanel3Tab3Layout.createSequentialGroup()
                .addGap(109, 109, 109)
                .addGroup(addNewTeamjPanel3Tab3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel14)
                    .addComponent(jLabel13)
                    .addComponent(jLabel12)
                    .addComponent(jLabel11))
                .addGap(113, 113, 113)
                .addGroup(addNewTeamjPanel3Tab3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTextField4)
                    .addComponent(jTextField5))
                .addContainerGap(179, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, addNewTeamjPanel3Tab3Layout.createSequentialGroup()
                .addContainerGap(337, Short.MAX_VALUE)
                .addComponent(jButton4)
                .addGap(348, 348, 348))
        );
        addNewTeamjPanel3Tab3Layout.setVerticalGroup(
            addNewTeamjPanel3Tab3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addNewTeamjPanel3Tab3Layout.createSequentialGroup()
                .addGap(46, 46, 46)
                .addGroup(addNewTeamjPanel3Tab3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(jTextField4, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(27, 27, 27)
                .addGroup(addNewTeamjPanel3Tab3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(jTextField5, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(24, 24, 24)
                .addGroup(addNewTeamjPanel3Tab3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(35, 35, 35)
                .addGroup(addNewTeamjPanel3Tab3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel14)
                    .addComponent(jTextField7, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(61, 61, 61)
                .addComponent(jButton4)
                .addContainerGap(341, Short.MAX_VALUE))
        );

        BodyjTabbedPane.addTab("Add new team", addNewTeamjPanel3Tab3);

        jLabel15.setText("Team name:");

        jLabel16.setText("Contact name:");

        jLabel17.setText("Phone number:");

        jLabel18.setText("Email address:");

        teamNamejComboBox5Tab4.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                teamNamejComboBox5Tab4ItemStateChanged(evt);
            }
        });
        teamNamejComboBox5Tab4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                teamNamejComboBox5Tab4ActionPerformed(evt);
            }
        });

        jButton5.setText("Updating existing team");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout updateExistingTeamjPanel4Tab4Layout = new javax.swing.GroupLayout(updateExistingTeamjPanel4Tab4);
        updateExistingTeamjPanel4Tab4.setLayout(updateExistingTeamjPanel4Tab4Layout);
        updateExistingTeamjPanel4Tab4Layout.setHorizontalGroup(
            updateExistingTeamjPanel4Tab4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(updateExistingTeamjPanel4Tab4Layout.createSequentialGroup()
                .addGap(111, 111, 111)
                .addGroup(updateExistingTeamjPanel4Tab4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel18)
                    .addComponent(jLabel17)
                    .addComponent(jLabel16)
                    .addComponent(jLabel15))
                .addGap(107, 107, 107)
                .addGroup(updateExistingTeamjPanel4Tab4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(phoneNumberjTextField2Tab4, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                    .addComponent(contactNamejTextField1Tab4, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                    .addComponent(emailAddressjTextField3Tab4, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                    .addComponent(teamNamejComboBox5Tab4, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, updateExistingTeamjPanel4Tab4Layout.createSequentialGroup()
                .addContainerGap(341, Short.MAX_VALUE)
                .addComponent(jButton5)
                .addGap(294, 294, 294))
        );
        updateExistingTeamjPanel4Tab4Layout.setVerticalGroup(
            updateExistingTeamjPanel4Tab4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(updateExistingTeamjPanel4Tab4Layout.createSequentialGroup()
                .addGap(54, 54, 54)
                .addGroup(updateExistingTeamjPanel4Tab4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(teamNamejComboBox5Tab4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(37, 37, 37)
                .addGroup(updateExistingTeamjPanel4Tab4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(contactNamejTextField1Tab4, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(46, 46, 46)
                .addGroup(updateExistingTeamjPanel4Tab4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(phoneNumberjTextField2Tab4, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(44, 44, 44)
                .addGroup(updateExistingTeamjPanel4Tab4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(emailAddressjTextField3Tab4, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(43, 43, 43)
                .addComponent(jButton5)
                .addContainerGap(310, Short.MAX_VALUE))
        );

        BodyjTabbedPane.addTab("Update existing team", updateExistingTeamjPanel4Tab4);

        jLabel19.setText("New event name:");

        jLabel20.setText("Date:");

        jLabel21.setText("Location:");

        jButton6.setText("Add new event:");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout addNewEventjPanel5Tab5Layout = new javax.swing.GroupLayout(addNewEventjPanel5Tab5);
        addNewEventjPanel5Tab5.setLayout(addNewEventjPanel5Tab5Layout);
        addNewEventjPanel5Tab5Layout.setHorizontalGroup(
            addNewEventjPanel5Tab5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addNewEventjPanel5Tab5Layout.createSequentialGroup()
                .addGap(140, 140, 140)
                .addGroup(addNewEventjPanel5Tab5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel21)
                    .addComponent(jLabel20)
                    .addComponent(jLabel19))
                .addGap(105, 105, 105)
                .addGroup(addNewEventjPanel5Tab5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jTextField11, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(newEventDate_JTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(newEventLocation_JTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(153, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, addNewEventjPanel5Tab5Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton6)
                .addGap(292, 292, 292))
        );
        addNewEventjPanel5Tab5Layout.setVerticalGroup(
            addNewEventjPanel5Tab5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(addNewEventjPanel5Tab5Layout.createSequentialGroup()
                .addGap(51, 51, 51)
                .addGroup(addNewEventjPanel5Tab5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel19)
                    .addComponent(jTextField11, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(48, 48, 48)
                .addGroup(addNewEventjPanel5Tab5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel20)
                    .addComponent(newEventDate_JTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(39, 39, 39)
                .addGroup(addNewEventjPanel5Tab5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel21)
                    .addComponent(newEventLocation_JTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(83, 83, 83)
                .addComponent(jButton6)
                .addContainerGap(336, Short.MAX_VALUE))
        );

        BodyjTabbedPane.addTab("Add new event", addNewEventjPanel5Tab5);

        javax.swing.GroupLayout BodyjPanelLayout = new javax.swing.GroupLayout(BodyjPanel);
        BodyjPanel.setLayout(BodyjPanelLayout);
        BodyjPanelLayout.setHorizontalGroup(
            BodyjPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(headerjPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(BodyjTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        BodyjPanelLayout.setVerticalGroup(
            BodyjPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(BodyjPanelLayout.createSequentialGroup()
                .addComponent(headerjPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(BodyjTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 684, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BodyjPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(BodyjPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void onClickExportResult(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onClickExportResult
        if (comboBoxStatus)
        {   
            DefaultTableModel model = (DefaultTableModel) compResultsjTable1Tab1.getModel();
            exportCSVData(model, "Competition_Results.csv");
        }
    }//GEN-LAST:event_onClickExportResult

    private void onClickExportLeaderBoard(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onClickExportLeaderBoard
        if (comboBoxStatus) 
        {
            DefaultTableModel model = (DefaultTableModel) leaderBoardjTable1Tab1.getModel();
            exportCSVData(model, "LeaderBoard_Results.csv");
        }
    }//GEN-LAST:event_onClickExportLeaderBoard

    private void eventComboBox1Tab1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_eventComboBox1Tab1ActionPerformed
        if (comboBoxStatus)
        { 
                  displayCompResults();
        }
    }//GEN-LAST:event_eventComboBox1Tab1ActionPerformed

    private void teamJComboBox2Tab1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_teamJComboBox2Tab1ActionPerformed
        if (comboBoxStatus)
        { 
        displayCompResults();
        }
    }//GEN-LAST:event_teamJComboBox2Tab1ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        if (comboBoxStatus)
        {   
        addNewCompetitionResults();
        }
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        if (comboBoxStatus)
        {   
        addNewTeam();
        }
    }//GEN-LAST:event_jButton4ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        if (comboBoxStatus)
        {  
        addNewEvent();
        }
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        if (comboBoxStatus)
        {    
             saveUpdatedTeam();
        }
    }//GEN-LAST:event_jButton5ActionPerformed

    private void teamNamejComboBox5Tab4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_teamNamejComboBox5Tab4ActionPerformed
        if (comboBoxStatus)
        {
            chosenTeam = teamNamejComboBox5Tab4.getSelectedItem().toString();
            updateTeam();
        }
    }//GEN-LAST:event_teamNamejComboBox5Tab4ActionPerformed

    private void eventComboBox1Tab1ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_eventComboBox1Tab1ItemStateChanged
        if (comboBoxStatus)
        {  
            String selectedItem = eventComboBox1Tab1.getSelectedItem().toString();
            chosenEvent = selectedItem.split(" \\(")[0].trim();
            displayEventsLeaderBoard();
        }
    }//GEN-LAST:event_eventComboBox1Tab1ItemStateChanged

    private void teamJComboBox2Tab1ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_teamJComboBox2Tab1ItemStateChanged
          if (comboBoxStatus){
          chosenTeam = teamJComboBox2Tab1.getSelectedItem().toString();
            displayCompResults();
          }  
    }//GEN-LAST:event_teamJComboBox2Tab1ItemStateChanged

    private void teamNamejComboBox5Tab4ItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_teamNamejComboBox5Tab4ItemStateChanged
        if (comboBoxStatus)
        { 
        chosenTeam = teamNamejComboBox5Tab4.getSelectedItem().toString();
        }
    }//GEN-LAST:event_teamNamejComboBox5Tab4ItemStateChanged

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(GUI_GoldCoastESports.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GUI_GoldCoastESports.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GUI_GoldCoastESports.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GUI_GoldCoastESports.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GUI_GoldCoastESports().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel BodyjPanel;
    private javax.swing.JTabbedPane BodyjTabbedPane;
    private javax.swing.JPanel addNewCompetitionResultjPanel2Tab2;
    private javax.swing.JPanel addNewEventjPanel5Tab5;
    private javax.swing.JPanel addNewTeamjPanel3Tab3;
    private javax.swing.JTable compResultsjTable1Tab1;
    private javax.swing.JTextField contactNamejTextField1Tab4;
    private javax.swing.JTextField dispRecordTextF;
    private javax.swing.JTextField emailAddressjTextField3Tab4;
    private javax.swing.JComboBox<String> eventComboBox1Tab1;
    private javax.swing.JPanel eventCompResultjPanel1Tab1;
    private javax.swing.JComboBox<String> eventjComboBox1Tab2;
    private javax.swing.JButton exportCompResultjButton1Tab1;
    private javax.swing.JButton exportLeaderBoardjButton2Tab1;
    private javax.swing.JComboBox<String> gamejComboBox2Tab2;
    private javax.swing.JLabel headerimgjLabel;
    private javax.swing.JPanel headerjPanel;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField jTextField11;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JTextField jTextField3;
    private javax.swing.JTextField jTextField4;
    private javax.swing.JTextField jTextField5;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField jTextField7;
    private javax.swing.JTable leaderBoardjTable1Tab1;
    private javax.swing.JTextField newEventDate_JTextField;
    private javax.swing.JTextField newEventLocation_JTextField;
    private javax.swing.JTextField phoneNumberjTextField2Tab4;
    private javax.swing.JComboBox<String> team1jComboBox3Tab2;
    private javax.swing.JComboBox<String> team2jComboBox4Tab2;
    private javax.swing.JComboBox<String> teamJComboBox2Tab1;
    private javax.swing.JComboBox<String> teamNamejComboBox5Tab4;
    private javax.swing.JPanel updateExistingTeamjPanel4Tab4;
    // End of variables declaration//GEN-END:variables
}
