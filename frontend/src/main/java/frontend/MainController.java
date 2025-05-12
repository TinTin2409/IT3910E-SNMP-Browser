package frontend;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

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

        if (ip.isEmpty() || oid.isEmpty() || op == null || op.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Thiếu thông tin");
            alert.setHeaderText(null);
            alert.setContentText("Vui lòng nhập đầy đủ IP, OID và chọn Operation!");
            alert.showAndWait();
            return;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream input;

            switch (op) {
                case "GET":
                    input = getClass().getResourceAsStream("/get_sample.json"); // giả lập
                    break;
                case "GETNEXT":
                    input = getClass().getResourceAsStream("/getnext_sample.json");
                    break;
                case "WALK":
                    input = getClass().getResourceAsStream("/walk_sample.json");
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported operation: " + op);
            }

            loadSnmpResultFromJson(input);

        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setContentText("Không thể thực hiện thao tác " + op);
            alert.showAndWait();
        }
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
    public void loadSnmpResultFromJson(InputStream jsonInput) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<SNMPData> dataList = mapper.readValue(jsonInput, new TypeReference<List<SNMPData>>() {});
            snmpTable.getItems().clear();
            snmpTable.getItems().addAll(dataList);
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi dữ liệu");
            alert.setContentText("Không thể tải dữ liệu SNMP từ JSON!");
            alert.showAndWait();
        }
    }




}
