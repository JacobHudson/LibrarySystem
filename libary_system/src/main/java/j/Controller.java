package j;

import java.sql.SQLException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Optional;

public class Controller {
    @FXML
    private TableView<Book> tableView;
    @FXML
    private TableColumn<Book, Integer> idColumn;
    @FXML
    private TableColumn<Book, String> titleColumn;
    @FXML
    private TableColumn<Book, String> authorColumn;
    @FXML
    private TableColumn<Book, String> isbnColumn;
    @FXML
    private TableColumn<Book, String> availableColumn;

    @FXML
    private TextField titleField;
    @FXML
    private TextField authorField;
    @FXML
    private TextField isbnField;
    @FXML
    private Button addButton;
    @FXML
    private Button checkOutButton;
    @FXML
    private Button checkInButton;
    @FXML
    private Button removeButton;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> searchCriteriaBox;

    private ObservableList<Book> bookList;

    @FXML
    public void initialize() {
        // Initialize table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorColumn.setCellValueFactory(new PropertyValueFactory<>("author"));
        isbnColumn.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        availableColumn.setCellValueFactory(new PropertyValueFactory<>("available"));

        // Initialize search criteria ComboBox
        searchCriteriaBox.setItems(FXCollections.observableArrayList("Title", "Author"));
        searchCriteriaBox.setValue("Title"); // Set default search criteria

        // Load books from database
        try {
            bookList = FXCollections.observableArrayList(BookDAO.getAllBooks());
            // Wrap the ObservableList in a FilteredList (initially display all data)
            FilteredList<Book> filteredData = new FilteredList<>(bookList, b -> true);

            // Set the filter Predicate whenever the filter changes
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filteredData.setPredicate(book -> {
                    // If filter text is empty, display all books
                    if (newValue == null || newValue.isEmpty()) {
                        return true;
                    }

                    // Get search criteria
                    String searchCriteria = searchCriteriaBox.getValue();
                    String lowerCaseFilter = newValue.toLowerCase();

                    if (searchCriteria.equals("Title")) {
                        return book.getTitle().toLowerCase().contains(lowerCaseFilter);
                    } else if (searchCriteria.equals("Author")) {
                        return book.getAuthor().toLowerCase().contains(lowerCaseFilter);
                    }
                    return false; // Default
                });
            });

            // Wrap the FilteredList in a SortedList
            SortedList<Book> sortedData = new SortedList<>(filteredData);

            // Bind the SortedList comparator to the TableView comparator
            sortedData.comparatorProperty().bind(tableView.comparatorProperty());

            // Add sorted (and filtered) data to the table
            tableView.setItems(sortedData);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not load books.");
            e.printStackTrace();
        }

        // Disable remove button when no selection
        removeButton.setDisable(true);
        tableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            removeButton.setDisable(newValue == null);
        });
    }

    @FXML
    public void addBook(ActionEvent event) {
        String title = titleField.getText().trim();
        String author = authorField.getText().trim();
        String isbn = isbnField.getText().trim();

        if (title.isEmpty() || author.isEmpty() || isbn.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Input Error", "Please fill all fields.");
            return;
        }

        try {
            BookDAO.addBook(title, author, isbn);
            bookList.setAll(BookDAO.getAllBooks()); // Refresh the book list
            titleField.clear();
            authorField.clear();
            isbnField.clear();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Book added successfully.");
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                showAlert(Alert.AlertType.ERROR, "Error", "ISBN must be unique.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Could not add book.");
                e.printStackTrace();
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not add book.");
            e.printStackTrace();
        }
    }

    @FXML
    public void checkOutBook(ActionEvent event) {
        Book selectedBook = tableView.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a book to check out.");
            return;
        }

        if (selectedBook.getAvailable().equals("No")) {
            showAlert(Alert.AlertType.WARNING, "Unavailable", "Book is already checked out.");
            return;
        }

        try {
            BookDAO.updateBookAvailability(selectedBook.getId(), false);
            BookDAO.addTransaction(selectedBook.getId(), "checkout");
            bookList.setAll(BookDAO.getAllBooks()); // Refresh the book list
            showAlert(Alert.AlertType.INFORMATION, "Success", "Book checked out successfully.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not check out book.");
            e.printStackTrace();
        }
    }

    @FXML
    public void checkInBook(ActionEvent event) {
        Book selectedBook = tableView.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a book to check in.");
            return;
        }

        if (selectedBook.getAvailable().equals("Yes")) {
            showAlert(Alert.AlertType.WARNING, "Already Available", "Book is already available.");
            return;
        }

        try {
            BookDAO.updateBookAvailability(selectedBook.getId(), true);
            BookDAO.addTransaction(selectedBook.getId(), "checkin");
            bookList.setAll(BookDAO.getAllBooks()); // Refresh the book list
            showAlert(Alert.AlertType.INFORMATION, "Success", "Book checked in successfully.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Error", "Could not check in book.");
            e.printStackTrace();
        }
    }

    @FXML
    public void removeBook(ActionEvent event) {
        Book selectedBook = tableView.getSelectionModel().getSelectedItem();
        if (selectedBook == null) {
            showAlert(Alert.AlertType.WARNING, "No Selection", "Please select a book to remove.");
            return;
        }

        // Confirm deletion
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Deletion");
        confirmationAlert.setHeaderText(null);
        confirmationAlert.setContentText("Are you sure you want to remove the selected book?");
        confirmationAlert.initOwner(tableView.getScene().getWindow());

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                BookDAO.deleteBook(selectedBook.getId());
                bookList.setAll(BookDAO.getAllBooks()); // Refresh the book list
                showAlert(Alert.AlertType.INFORMATION, "Success", "Book removed successfully.");
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Could not remove book. It might be referenced elsewhere.");
                e.printStackTrace();
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Error", "Could not remove book.");
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void handleSearch(ActionEvent event) {
        // Optional: Additional actions can be implemented here
        // Since filtering is already handled by the listener, this method can be left empty
    }

    @FXML
    public void handleClearSearch(ActionEvent event) {
        searchField.clear();
        // The FilteredList listener will automatically reset the table view
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.initOwner(tableView.getScene().getWindow());
        alert.showAndWait();
    }
}
