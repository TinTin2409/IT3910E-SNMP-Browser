package frontend;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

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

    @FXML private Label nameLabel;
    @FXML private Label oidInfoLabel;
    @FXML private Label mibLabel;
    @FXML private Label syntaxLabel;
    @FXML private Label accessLabel;
    @FXML private Label statusLabel;


    @FXML private TreeView<Node> mibTreeView;
    @FXML private TextArea descriptionArea;



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
        // Gán cột TableView (phần bạn đã có)
        oidCol.setCellValueFactory(new PropertyValueFactory<>("oid"));
        valueCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        ipPortCol.setCellValueFactory(new PropertyValueFactory<>("ipPort"));

        snmpTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        operationBox.getItems().addAll("GET", "GETNEXT", "WALK");


        // 🌳 Khởi tạo mock cây MIB
        TreeItem<Node> root = buildMockTree();
        mibTreeView.setRoot(root);
        mibTreeView.setShowRoot(true);

        // Gán sự kiện click để hiển thị chi tiết
        mibTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                Node selected = newVal.getValue();

                oidField.setText(selected.oid);
                if (nameLabel != null) nameLabel.setText(selected.name);
                if (oidInfoLabel != null) oidInfoLabel.setText(selected.oid);
                if (mibLabel != null) mibLabel.setText("MockMIB"); // bạn có thể sửa nếu có field cụ thể
                if (syntaxLabel != null) syntaxLabel.setText(selected.type);
                if (accessLabel != null) accessLabel.setText("read-only"); // nếu có thật thì sửa lại
                if (statusLabel != null) statusLabel.setText("current"); // giả định
                if (descriptionArea != null) descriptionArea.setText(selected.description);
            }
        });

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

    private TreeItem<Node> buildMockTree() {
        Node rootNode = new Node("1.3.6.1.2.1", "mib-2", "Group", "Root of MIB-2 tree");
        TreeItem<Node> root = new TreeItem<>(rootNode);

        TreeItem<Node> system = new TreeItem<>(new Node("1.3.6.1.2.1.1", "system", "Group", "System Information"));
        system.getChildren().addAll(
                new TreeItem<>(new Node("1.3.6.1.2.1.1.1.0", "sysDescr", "OctetString", "Device description")),
                new TreeItem<>(new Node("1.3.6.1.2.1.1.2.0", "sysObjectID", "ObjectIdentifier", "Object ID"))
        );

        TreeItem<Node> interfaces = new TreeItem<>(new Node("1.3.6.1.2.1.2", "interfaces", "Group", "Interface group"));

        root.getChildren().addAll(system, interfaces);
        root.setExpanded(true);
        return root;
    }





}
