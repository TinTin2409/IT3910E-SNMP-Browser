package frontend;

import Model.MIBTreeStructure.BuildTreeFromJson;
import Model.MIBTreeStructure.MibLoader;
import Model.MIBTreeStructure.Node;
import Model.SNMRequest.SNMPGet;
import Model.SNMRequest.SNMPGetNext;
import Model.SNMRequest.SNMPWalk;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static Model.SNMRequest.SnmpResponseFormatter.format;



public class MainController {

    // Left side of the application
    @FXML private AnchorPane MIBTreeDisplay;  //This Pane is used to display the TreeView structure of the MIBs
    @FXML private FlowPane MIBsLoaded; //This Pane is used to display the MIBs loaded by the user (when the user clicks on the Open/Load MIB
    // button or MIBs in the vendor tab)
    @FXML private Label ShowingMIBTreeName; //Label to show the name of the MIB  that are being displayed in the MIBTreeDisplay Pane
    @FXML private TextField tfOID; //Text field to display the OID of the selected node


    // Bottom right side of the application, the place where the user can see the extracted  information of the selected node
    @FXML private Label lbAccess;
    @FXML private Label lbName;
    @FXML private Label lbStatus;
    @FXML private Label lbType;
    @FXML private TextArea taDescription;
    @FXML private PasswordField tfCommunityString;
    @FXML private TextField tfTargetIP;

    // Query Table : show the result of the SNMP Get, SNMP Get Next, SNMP Walk operations
    @FXML private TableView<ARowInQueryTable> queryTable;
    @FXML private TableColumn<ARowInQueryTable, String> nameColumn;
    @FXML private TableColumn<ARowInQueryTable, String> valueColumn;
    @FXML private TableColumn<ARowInQueryTable, String> typeColumn;
    @FXML private ImageView searchImage;
    @FXML private ImageView saveImage;

    //TreeView to display the MIB tree
    TreeView<Node> treeView;

    private String getBaseDirectory() {
        String basePath = System.getProperty("user.dir");
        String path = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
        File jarFile = new File(path);
        if (path.endsWith(".jar")) { //When we run the app by using the jar file
            basePath = jarFile.getParent();
            //System.out.println("Base Path of jar: " + basePath);
        }
        else {  //When we run the app by using the source code in the IDE

            //System.out.println("Base Path of IDE: " + basePath);
        }
        return basePath;
    }
    String BASE_DIR = getBaseDirectory() + "/MIB Databases";
    //@FXML TextArea testing;





    //Some default values for attributes used across the application
    String oidValue = null;
    String ip = "127.0.0.1";
    String community = "public";
    String nodeType = null; //Node type of the selected node, help to distinguish between scalar
    // and non-scalar nodes. This is crucial to determine the OID to be used in SNMP Get
    Map<String, Object> constraints = new HashMap<>(); //Constraints of the selected node, String is the
    // constraint name, also we do not in advance what kind of constraints can have, so use Object type. This info will not be displayed to the UI


    /**
     * Load some default MIBs when the program starts
     */
    @FXML
    public void initialize() throws IOException {
        //Set the image for the search and save button
        searchImage.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/Image/search.png"))));
        saveImage.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/Image/save.png"))));

        //Build the tree view from the default MIB files
        treeView = new TreeView<>();
        returnToDefaultClicked(null);

        //Initialize the query table
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("value"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));


        // Add an event listener to the TableView. Double-lick on a row to highlight the corresponding node in the TreeView
        // From the name cell of the row, find the corresponding node in the TreeView with corresponding name and highlight/move to it.
        queryTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ARowInQueryTable selectedRow = queryTable.getSelectionModel().getSelectedItem();
                if (selectedRow != null) {

                    //If the node name contains postfix ".any_number", remove it before highlighting
                    //(e.g. "hrDeviceStatus.25", "hrDeviceStatus.10", "hrDeviceStatus.2004", etc.)
                    // Rows by SNMP walk will always have a postfix, SNMP Get rows will not if the node is scalar object
                    int lastDotPosition = selectedRow.getName().lastIndexOf(".");

                    if (lastDotPosition != -1) { //Check if the node name contains a postfix, then remove it and only search for the node name
                        String nodeName = selectedRow.getName().substring(0, lastDotPosition);
                        //System.out.println("Node name to search: " + nodeName);
                        highlightNodeInTreeView(treeView, nodeName);
                    } else { //If the node name does not contain a postfix, search for the node name as it is
                        highlightNodeInTreeView(treeView, selectedRow.getName());
                    }
                }
            }
        });

    }



    /**-------------------------------------File button in the menu bar-------------------------------------**/
    @FXML
    /**
     * Only open the MIB without saving it to the MIB Databases directory
     * Then show the MIB in the MIBsLoaded FlowPane
     */
    void openMIBClicked(ActionEvent event) throws IOException {
        //System.out.println("Open MIB Clicked");

        //Ask user to select a file
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(null);
        //System.out.println("Opening: " + file.getName() + ".");

        //Show the chosen file in loaded section
        showMIBFile(file);
    }

    /**---------------------------------------Edit button in the menu bar--------------------------------------**/
    private Scene mainScene;

    public void setMainScene(Scene mainScene) {
        this.mainScene = mainScene;
    }

    @FXML
    public void darkModeClicked() {
        mainScene.getStylesheets().clear();
        mainScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
    }

    @FXML
    public void lightModeClicked() {
        mainScene.getStylesheets().clear();
    }

    /**
     * Function to show the MIBs loaded/opened or when choose a vendor in vendor tab by the user in the MIBsLoaded FlowPane
     * @param file: the file that the user has chosen to open or the file that is in the vendor MIBs list
     */
    public void showMIBFile(File file) {
        try {
            MIBTreeDisplay.getChildren().clear();
            String path = BASE_DIR + "/" + file.getName();
            TreeView<Node> treeView = displayTreeFromFiles(List.of(path));

            // Expand to fit pane
            treeView.prefWidthProperty().bind(MIBTreeDisplay.widthProperty());
            treeView.prefHeightProperty().bind(MIBTreeDisplay.heightProperty());

            MIBTreeDisplay.getChildren().add(treeView);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    public TreeView<Node> displayTreeFromFiles(List<String> mibFilePaths) throws IOException {
        // Đường dẫn tương đối đến RFC1213-MIB.json trong resources
        String jsonPath = "src/main/resources/MIB Databases/RFC1213-MIB.json";

        // Cập nhật nhãn hiển thị tên MIB đang được load
        ShowingMIBTreeName.setText("Showing MIB Tree: RFC1213-MIB");

        // Tạo builder và load file MIB
        BuildTreeFromJson treeBuilder = new BuildTreeFromJson();
        InputStream inputStream = getClass().getResourceAsStream("/MIB Databases/RFC1213-MIB.json");
        treeBuilder.buildTreeFromJson(inputStream);


        // Tạo cây hiển thị từ gốc
        TreeItem<Node> rootItem = treeBuilder.convertNodeToTreeItem(treeBuilder.getRoot());
        treeView = new TreeView<>(rootItem);

        // Gán sự kiện khi người dùng chọn node
        treeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                Node selectedNode = newValue.getValue();
                tfOID.setText(selectedNode.oid);
                lbName.setText(selectedNode.name);
                nodeType = selectedNode.nodeType;
                constraints = (HashMap<String, Object>) selectedNode.constraints;
                lbType.setText(selectedNode.type);
                lbAccess.setText(selectedNode.access);
                lbStatus.setText(selectedNode.status);
                taDescription.setText(selectedNode.description);
                resetCurrentOid();
            }
        });

        // Gán sự kiện double click để thực hiện SNMP GET
        treeView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<Node> selectedItem = treeView.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    Node selectedNode = selectedItem.getValue();

                    if (selectedNode.type != null && !selectedNode.type.isEmpty()) {
                        tfOID.setText(selectedNode.oid);
                        lbName.setText(selectedNode.name);
                        lbType.setText(selectedNode.type);
                        nodeType = selectedNode.nodeType;
                        constraints = selectedNode.constraints;

                        try {
                            SNMPGetClicked(null); // Gọi hàm GET (truyền null vì không có MouseEvent)
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });

        return treeView;
    }



    /**
     * When click on the Return to Default MIBs button in the Standard MIBs tab, clear all and display the default MIBs as the first time the application starts
     */
    @FXML
    void returnToDefaultClicked(MouseEvent event) throws IOException {
        // Build the tree view from the default MIB files
        List<String> mibFilePaths = Arrays.asList(
                BASE_DIR + "/SNMPv2-SMI.json",
                BASE_DIR + "/RFC1213-MIB.json",
                BASE_DIR + "/HOST-RESOURCES-MIB.json",
                BASE_DIR + "/SNMPv2-MIB.json",
                BASE_DIR + "/IF-MIB.json"
        );


        treeView = displayTreeFromFiles(mibFilePaths);

        //This one is specifically for the default MIBs, it's using multiple MIBs to build the tree, reset the label
        ShowingMIBTreeName.setText("Showing MIB Tree: Default MIBs");

        MIBTreeDisplay.getChildren().clear();
        //Expand the treeview to fit the Anchor Pane width and height
        treeView.prefWidthProperty().bind(MIBTreeDisplay.widthProperty());
        treeView.prefHeightProperty().bind(MIBTreeDisplay.heightProperty());
        MIBTreeDisplay.getChildren().add(treeView);
    }


    /**---------------------------------------------------SNMP Operations------------------------------------------------------**/

    @FXML
    void SNMPGetClicked(MouseEvent event) {
        oidValue = tfOID.getText(); // Get the OID from the text field
        if (oidValue.isEmpty()) {
            return;
        }

        // Get the target IP address from the text field, change it to UdpAddress format
        // In case user let this field empty, use the default IP address
        if (!tfTargetIP.getText().isEmpty()) {
            ip = "udp:" + tfTargetIP.getText() + "/161";
        } else {
            ip = "udp:127.0.0.1/161";
        }
        Address targetAddress = GenericAddress.parse(ip);

        //System.out.println("Still Working on this part 1");

        if (targetAddress instanceof UdpAddress udpTargetAddress) {
            // Get the community string from the password field
            if (!tfCommunityString.getText().isEmpty()) {
                community = tfCommunityString.getText();
            }

            //System.out.println("Still Working on this part 2");
            //System.out.println("Node Type: " + nodeType);

            if (nodeType.equals("scalar")) {  // If the nodeType is scalar, append ".0" to the OID
                oidValue = oidValue + ".0";
                try {
                    SNMPGet snmpGet = new SNMPGet((UdpAddress) targetAddress, community, oidValue);
                    VariableBinding vb = snmpGet.getVariableBinding(); // Get the response from the SNMP request

                    if (vb != null) {
                        // Get the data type and constraints, then using SNMPResponseFormatter to format the response to human-readable format
                        String dataType = lbType.getText();
                        //constraint already defined and assign to the constraints variable when click on the tree node
                        String humanReadableValue = format(vb.getVariable(), dataType, constraints);

                        //Add the result to the query table
                        queryTable.getItems().add(new ARowInQueryTable(lbName.getText(), humanReadableValue, lbType.getText()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else { // If the nodeType is not scalar, append from ".1" to ".1000" and perform SNMP Get until a `noSuchInstance` response is received
                //System.out.println("Still Working on this part 4 (handling non-scalar nodes)");
                for (int i = 1; i <= 1000; i++) {
                    String oidToTry = oidValue + "." + i;
                    try {
                        //System.out.println("Still Working on this part 5 (handling non-scalar nodes)");
                        SNMPGet snmpGet = new SNMPGet((UdpAddress) targetAddress, community, oidToTry);
                        VariableBinding vb = snmpGet.getVariableBinding(); // Get the response from the SNMP request

                        if (vb != null) {
                            // Print raw response to console
                            String response = vb.getVariable().toString();

                            // If the response is not "noSuchInstance", print it
                            if (!response.equals("noSuchInstance")  && !response.equals("noSuchObject")) {


                                // Get the data type and constraints, then using SNMPResponseFormatter to format the response to human-readable format
                                String dataType = lbType.getText();
                                //constraint already defined and assign to the constraints variable when click on the tree node
                                String humanReadableValue = format(vb.getVariable(), dataType, constraints);


                                //Add the result to the query table
                                queryTable.getItems().add(new ARowInQueryTable(lbName.getText() + "." + i, humanReadableValue, lbType.getText()));
                                // Display name as "name.i" to indicate the instance number
                            }


                            // If the response is "noSuchInstance", break the loop
                            if (response.equals("noSuchInstance") || response.equals("noSuchObject")) {
                                if (i == 1) { // If the first response is "noSuchInstance", mean that it is not exist in the device
                                    //System.out.println("Error: Failed to get response for OID: " + oidToTry);
                                    queryTable.getItems().add(new ARowInQueryTable(lbName.getText(), "noSuchInstance", lbType.getText()));
                                    break;
                                } else { // If the response is "noSuchInstance" after some iteration, mean that it exits, we have reached the end of the table, break the loop
                                    break;
                                }
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            System.out.println("Error: Invalid target address");
        }
    }


    private String currentOid; // To track the current OID
    // Method to perform SNMP GET-NEXT request
    @FXML
    void SNMPGetNextClicked(MouseEvent event) throws IOException {
        //System.out.println("Performing SNMP Get Next.......");

        // Get the OID from the text field if it's the first click
        if (currentOid == null) {
            currentOid = tfOID.getText();
        }

        if (currentOid.isEmpty()) {
            System.out.println("Error: OID field is empty");
            return;
        }

        // Get the target IP address from the text field, change it to UdpAddress format
        // In case user let this field empty, use the default IP address
        if (!tfTargetIP.getText().isEmpty()) {
            ip = "udp:" + tfTargetIP.getText() + "/161";
        } else {
            ip = "udp:127.0.0.1/161";
        }
        Address targetAddress = GenericAddress.parse(ip);

        if (targetAddress instanceof UdpAddress udpTargetAddress) {
            // Get the community string from the password field
            if (!tfCommunityString.getText().isEmpty()) {
                community = tfCommunityString.getText();
            }

            SNMPGetNext snmpGetNext = new SNMPGetNext(udpTargetAddress, community, currentOid);
            VariableBinding vb = snmpGetNext.getVariableBinding(); // Get the response from the SNMP request

            if (vb != null) {
                //System.out.println("OID that GetNext performed: " + vb.getOid());
                //System.out.println("Return value: " + vb.getVariable());



                // Update currentOid with the OID from the response
                currentOid = vb.getOid().toString();

                // Process the retrieved OID and its value
                String oidForGetNext = vb.getOid().toString();
                int lastDotPosition = oidForGetNext.lastIndexOf('.');
                String oidWithoutLastPart = oidForGetNext.substring(0, lastDotPosition);

                // Load the MIB files and look up the node
                MibLoader mibLoader = new MibLoader();
                mibLoader.loadMibsFromFolder(BASE_DIR + "/");
                Node node = mibLoader.lookupNode(oidWithoutLastPart);

                if (node != null) {
                    // Format the return value to human-readable format
                    String dataType = node.type;
                    Map<String, Object> constraints = node.constraints;
                    String humanReadableValue = format(vb.getVariable(), dataType, constraints);
                    // Add the result to the query table
                    queryTable.getItems().add(new ARowInQueryTable(node.name + oidForGetNext.substring(lastDotPosition), humanReadableValue, dataType));
                } else {
                    // If we can't find the node in the MIB files, return the OID and raw input
                    queryTable.getItems().add(new ARowInQueryTable(oidForGetNext, vb.getVariable().toString() + " (raw value)", "None defined"));
                }
            } else {
                //System.out.println("Error: No response received.");
            }
        } else {
            //System.out.println("Error: Invalid target address.");
        }
    }

    // Method to reset the current OID when a new node is selected
    public void resetCurrentOid() {
        currentOid = null;
    }
    /**
     * Method to handle the SNMP Walk button click event. This method performs an SNMP Walk operation on from the selected OID.
     * @param event
     */
    @FXML
    void SNMPWalkClicked(MouseEvent event) {

        //System.out.println("Performing SNMP Walk.......");
        // Get the OID from the text field
        oidValue = tfOID.getText();

        // Get the target IP address from the text field, change it to UdpAddress format
        // In case user leaves this field empty, use the default IP address
        if (!tfTargetIP.getText().isEmpty()) {
            ip = "udp:" + tfTargetIP.getText() + "/161";
        } else {
            ip = "udp:127.0.0.1/161";
        }

        if (!tfCommunityString.getText().isEmpty()) {
            community = tfCommunityString.getText();
        }
        Address targetAddress = GenericAddress.parse(ip);

        if (targetAddress instanceof UdpAddress udpTargetAddress) {

            try {
                // Initialize SNMPWalk with target address, community string, and MIB folder path
                SNMPWalk snmpWalk = new SNMPWalk((UdpAddress) targetAddress, community);
                snmpWalk.start(); // Start the SNMP session
                List<VariableBinding> varBindings = snmpWalk.performSNMPWalk(oidValue);

                // Create MibLoader instance to load MIB files and resolve OIDs
                MibLoader mibLoader = new MibLoader();
                mibLoader.loadMibsFromFolder(BASE_DIR + "/");

                // Handle the results of the SNMP walk
                // For each response from SNMP Walk, extract the OID and the value
                // With OID, look up the corresponding node in the MIB files and extract the name, data type, and constraints
                // With value, format it to human-readable format based on finding data type and constraints
                for (VariableBinding varBinding : varBindings) {
                    // OID retrieved from the SNMP Walk response always contains the instance number (postfix) so we must remove from last dot till the end to get the
                    // base OID, then we can look up for this base OID in the MIB files.
                    String oid = varBinding.getOid().toString();
                    // Find the position of the last dot
                    int lastDotPosition = oid.lastIndexOf('.');
                    // Remove everything after the last dot
                    String oidWithoutLastPart = oid.substring(0, lastDotPosition);
                    // Lookup the node using the modified OID
                    Node node = mibLoader.lookupNode(oidWithoutLastPart);
                    //System.out.println("Node: " + node);

                    if (node != null) {
                        //System.out.println("Look up by OID Fucking work!");
                        String name = node.name;
                        String dataType = node.type;
                        Map<String, Object> constraint = node.constraints;

                        //System.out.println("OID: " + oid + ", Name: " + name + ", DataType: " + dataType + ", Constraints: " + constraint);
                        // Convert the variable to a human-readable format
                        String humanReadableValue = format(varBinding.getVariable(), dataType, constraints);
                        //System.out.println("Human Readable Value: " + humanReadableValue);
                        //Add the result to the query table
                        queryTable.getItems().add(new ARowInQueryTable(name + oid.substring(lastDotPosition), humanReadableValue, dataType));
                    } else { //In case we can find that node base on OID, return the OID and raw inout
                        //System.out.println("Can not find OID: " + oid + ": return raw value: " + varBinding.getVariable().toString());
                        //Add the result to the query table
                        queryTable.getItems().add(new ARowInQueryTable(oid, varBinding.getVariable().toString() + " (raw value)", "None defined"));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            //System.out.println("Error: Invalid target address");
        }
    }


    /**---------------------------------------------------Query Table------------------------------------------------------**/


    @FXML
    public void clearTableClicked(MouseEvent event) {
        // Clear the query table
        queryTable.getItems().clear();

        // Also reset the current OID of SNMP Get Next
        resetCurrentOid();
    }


    /**
     * Method to highlight a node in the TreeView, this method is called when user double-click on a row in the query table, it will
     * highlight the corresponding node in the TreeView (we map to the corresponding node by the name of the node)
     */
    // Method to highlight a node in the TreeView
    private void highlightNodeInTreeView(TreeView<Node> treeView, String nodeName) {
        TreeItem<Node> root = treeView.getRoot();
        TreeItem<Node> targetNode = findNode(root, nodeName);
        //System.out.println("Target node: " + targetNode);

        if (targetNode != null) {
            treeView.getSelectionModel().select(targetNode);
            treeView.scrollTo(treeView.getRow(targetNode));
        }
    }

    // Recursive method to find a node by name in the TreeView, the "name" has been normalized to remove the postfix ".any_number"
    // if needed by the method calling this method
    private TreeItem<Node> findNode(TreeItem<Node> currentNode, String nodeName) {
        if (currentNode.getValue().name.equals(nodeName)) {
            return currentNode;
        }
        for (TreeItem<Node> child : currentNode.getChildren()) {
            TreeItem<Node> result = findNode(child, nodeName);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Method to handle the Search button click event. This method prompts the user to enter a name to search for a
     * row with matching name in the query table. If a row with the matching name is found, the row is highlighted and moved to.
     */
    @FXML
    void searchButtonClicked(MouseEvent event) {
        // Create a TextInputDialog to prompt the user for the name to search
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Search");
        dialog.setHeaderText("Enter the name to search:");
        Optional<String> result = dialog.showAndWait();

        // If the user entered a name, search for it in the queryTable
        if (result.isPresent()) {
            String nameToSearch = result.get();

            // Iterate over the items in the queryTable
            for (ARowInQueryTable row : queryTable.getItems()) {
                // If the name of the row matches the name to search, select the row
                if (row.getName().toLowerCase().contains(nameToSearch.toLowerCase())) {
                    queryTable.getSelectionModel().select(row);
                    queryTable.scrollTo(row);
                    break;
                }
            }
        }
    }

    /**
     * Method to handle the Save button click event. This method prompts the user to select a file to save the query table to.
     * The query table is saved as a CSV file with the columns "Name", "Type", and "Value".
     */
    @FXML
    void saveButtonClicked(MouseEvent event) {
        // Create a FileChooser to prompt the user for the file to save
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Query Table");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(null);

        // If the user selected a file, save the queryTable to it
        if (file != null) {
            try {
                // Create a FileWriter to write the queryTable to the file
                FileWriter writer = new FileWriter(file);

                // Write the header to the CSV file
                writer.append("Name,Type,Value\n");

                // Iterate over the items in the queryTable
                for (ARowInQueryTable row : queryTable.getItems()) {
                    // Write each row to the CSV file
                    writer.append(row.getName());
                    writer.append(",");
                    writer.append(row.getType());
                    writer.append(",");
                    writer.append(row.getValue());
                    writer.append("\n");
                }

                // Close the FileWriter
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
