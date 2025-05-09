import javafx.fxml.FXML;
import javafx.scene.control.*;

public class MainController {

    @FXML private TextField ipField;
    @FXML private ComboBox<String> operationBox;
    @FXML private TextField oidField;

    @FXML private TreeView<String> mibTree;

    @FXML private TableView<SNMPData> snmpTable;
    @FXML private TableColumn<SNMPData, String> oidCol;
    @FXML private TableColumn<SNMPData, String> valueCol;
    @FXML private TableColumn<SNMPData, String> typeCol;
    @FXML private TableColumn<SNMPData, String> ipPortCol;

    @FXML
    public void onGoClicked() {
        String ip = ipField.getText().trim();
        String oid = oidField.getText().trim();
        String op = operationBox.getValue();

        // 🔴 Kiểm tra đầu vào trống
        if (ip.isEmpty() || oid.isEmpty() || op == null || op.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Thiếu thông tin");
            alert.setHeaderText(null);
            alert.setContentText("Vui lòng nhập đầy đủ IP, OID và chọn Operation!");
            alert.showAndWait();
            return;
        }

        System.out.println("Đã nhấn nút Go!");
        System.out.println("IP: " + ip + ", OID: " + oid + ", Operation: " + op);

        // Dữ liệu test
        SNMPData testData = new SNMPData(
                oid, "Sample Value", "OctetString", ip + ":161"
        );
        snmpTable.getItems().add(testData);
    }

    @FXML
    public void initialize() {
        oidCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getOid()));
        valueCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getValue()));
        typeCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getType()));
        ipPortCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().getIpPort()));
        snmpTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // ⚙️ Gán lựa chọn cho ComboBox
        operationBox.getItems().addAll("GET", "GETNEXT", "WALK");

    }



}
