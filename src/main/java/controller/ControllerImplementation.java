
package controller;

import model.entity.Person;
import model.entity.PersonException;
import model.dao.DAOArrayList;
import model.dao.DAOFile;
import model.dao.DAOFileSerializable;
import model.dao.DAOHashMap;
import model.dao.DAOJPA;
import model.dao.DAOSQL;
import model.dao.IDAO;
import start.Routes;
import view.DataStorageSelection;
import view.Delete;
import view.Insert;
import view.Menu;
import view.Read;
import view.ReadAll;
import view.Update;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.persistence.*;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import org.jdatepicker.DateModel;

import utils.Constants;
import view.Count;

public class ControllerImplementation implements IController, ActionListener {

    private final DataStorageSelection dSS;
    private IDAO dao;
    private Menu menu;
    private Insert insert;
    private Read read;
    private Delete delete;
    private Update update;
    private ReadAll readAll;
    private Count count;

    public ControllerImplementation(DataStorageSelection dSS) {
        this.dSS = dSS;
        ((JButton) (dSS.getAccept()[0])).addActionListener(this);
    }

    @Override
    public void start() {
        dSS.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == dSS.getAccept()[0]) {
            handleDataStorageSelection();
        } else if (e.getSource() == menu.getInsert()) {
            handleInsertAction();
        } else if (insert != null && e.getSource() == insert.getInsert()) {
            handleInsertPerson();
        } else if (e.getSource() == menu.getRead()) {
            handleReadAction();
        } else if (read != null && e.getSource() == read.getRead()) {
            handleReadPerson();
        } else if (e.getSource() == menu.getDelete()) {
            handleDeleteAction();
        } else if (delete != null && e.getSource() == delete.getDelete()) {
            handleDeletePerson();
        } else if (e.getSource() == menu.getUpdate()) {
            handleUpdateAction();
        } else if (update != null && e.getSource() == update.getRead()) {
            handleReadForUpdate();
        } else if (update != null && e.getSource() == update.getUpdate()) {
            handleUpdatePerson();
        } else if (e.getSource() == menu.getReadAll()) {
            handleReadAll();
        } else if (e.getSource() == menu.getDeleteAll()) {
            handleDeleteAll();
        } else if (e.getSource() == menu.getCount()) {
            handleCount();
        }
    }

    private void handleDataStorageSelection() {
        String daoSelected = ((javax.swing.JCheckBox) (dSS.getAccept()[1])).getText();
        dSS.dispose();
        switch (daoSelected) {
            case Constants.ARRAY_LIST -> dao = new DAOArrayList();
            case Constants.HASH_MAP -> dao = new DAOHashMap();
            case Constants.FILE -> setupFileStorage();
            case Constants.FILE_SERIALIZATION -> setupFileSerialization();
            case Constants.SQL_DATABASE -> setupSQLDatabase();
            case Constants.JPA_DATABASE -> setupJPADatabase();
        }
        setupMenu();
    }

    private void setupFileStorage() {
        File folderPath = new File(Routes.FILE.getFolderPath());
        File folderPhotos = new File(Routes.FILE.getFolderPhotos());
        File dataFile = new File(Routes.FILE.getDataFile());
        folderPath.mkdir();
        folderPhotos.mkdir();
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(dSS, "File structure not created. Closing application.", "File - People v1.1.0", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
        dao = new DAOFile();
    }

    private void setupFileSerialization() {
        File folderPath = new File(Routes.FILES.getFolderPath());
        File dataFile = new File(Routes.FILES.getDataFile());
        folderPath.mkdir();
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(dSS, "File structure not created. Closing application.", "FileSer - People v1.1.0", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
        dao = new DAOFileSerializable();
    }

    private void setupSQLDatabase() {
        try {
            Connection conn = DriverManager.getConnection(Routes.DB.getDbServerAddress() + Routes.DB.getDbServerComOpt(),
                    Routes.DB.getDbServerUser(), Routes.DB.getDbServerPassword());
            if (conn != null) {
                Statement stmt = conn.createStatement();
                stmt.executeUpdate("create database if not exists " + Routes.DB.getDbServerDB() + ";");
                stmt.executeUpdate("create table if not exists " + Routes.DB.getDbServerDB() + "." + Routes.DB.getDbServerTABLE() + "("
                        + "nif varchar(9) primary key not null, "
                        + "name varchar(50), "
                        + "dateOfBirth DATE, "
                        + "photo varchar(200), "
                        + "phoneNumber varchar(20));");
                stmt.close();
                conn.close();
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(dSS, "SQL-DDBB structure not created. Closing application.", "SQL_DDBB - People v1.1.0", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        dao = new DAOSQL();
    }

    private void setupJPADatabase() {
        try {
            EntityManagerFactory emf = Persistence.createEntityManagerFactory(Routes.DBO.getDbServerAddress());
            EntityManager em = emf.createEntityManager();
            em.close();
            emf.close();
        } catch (PersistenceException ex) {
            JOptionPane.showMessageDialog(dSS, "JPA_DDBB not created. Closing application.", "JPA_DDBB - People v1.1.0", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
        dao = new DAOJPA();
    }

    private void setupMenu() {
        menu = new Menu();
        menu.setVisible(true);
        menu.getInsert().addActionListener(this);
        menu.getRead().addActionListener(this);
        menu.getUpdate().addActionListener(this);
        menu.getDelete().addActionListener(this);
        menu.getReadAll().addActionListener(this);
        menu.getDeleteAll().addActionListener(this);
        menu.getCount().addActionListener(this);
    }

    private void handleInsertAction() {
        insert = new Insert(menu, true);
        insert.getInsert().addActionListener(this);
        insert.setVisible(true);
    }

    private void handleInsertPerson() {
        Person p = new Person(insert.getNam().getText(), insert.getNif().getText());
        if (insert.getPhoneNumber().getText() != null) {
            p.setPhoneNumber(insert.getPhoneNumber().getText());
        }
        if (insert.getDateOfBirth().getModel().getValue() != null) {
            p.setDateOfBirth(((GregorianCalendar) insert.getDateOfBirth().getModel().getValue()).getTime());
        }
        if (insert.getPhoto().getIcon() != null) {
            p.setPhoto((ImageIcon) insert.getPhoto().getIcon());
        }
        insert(p);
        insert.getReset().doClick();
    }

    private void handleReadAction() {
        read = new Read(menu, true);
        read.getRead().addActionListener(this);
        read.setVisible(true);
    }

    private void handleReadPerson() {
        Person p = new Person(read.getNif().getText());
        Person pNew = read(p);
        if (pNew != null) {
            read.getNam().setText(pNew.getName());
            read.getPhoneNumber().setText(pNew.getPhoneNumber());
            if (pNew.getDateOfBirth() != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(pNew.getDateOfBirth());
                DateModel<Calendar> dateModel = (DateModel<Calendar>) read.getDateOfBirth().getModel();
                dateModel.setValue(calendar);
            }
            if (pNew.getPhoto() != null) {
                pNew.getPhoto().getImage().flush();
                read.getPhoto().setIcon(pNew.getPhoto());
            }
        } else {
            JOptionPane.showMessageDialog(read, p.getNif() + " doesn't exist.", read.getTitle(), JOptionPane.WARNING_MESSAGE);
            read.getReset().doClick();
        }
    }

    private void handleDeleteAction() {
        delete = new Delete(menu, true);
        delete.getDelete().addActionListener(this);
        delete.setVisible(true);
    }

    private void handleDeletePerson() {
        Object[] options = {"Yes", "No"};
        int answer = JOptionPane.showOptionDialog(menu, "Are you sure you want to delete this person?",
                "Delete All - People v1.1.0", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[1]);
        if (answer == 0) {
            if (delete != null) {
                Person p = new Person(delete.getNif().getText());
                delete(p);
                delete.getReset().doClick();
            }
        }
    }

    private void handleUpdateAction() {
        update = new Update(menu, true);
        update.getUpdate().addActionListener(this);
        update.getRead().addActionListener(this);
        update.setVisible(true);
    }

    private void handleReadForUpdate() {
        if (update != null) {
            Person p = new Person(update.getNif().getText());
            Person pNew = read(p);
            if (pNew != null) {
                update.getNam().setEnabled(true);
                update.getDateOfBirth().setEnabled(true);
                update.getPhoto().setEnabled(true);
                update.getPhoneNumber().setEnabled(true);
                update.getUpdate().setEnabled(true);

                update.getNam().setText(pNew.getName());
                update.getPhoneNumber().setText(pNew.getPhoneNumber());
                if (pNew.getDateOfBirth() != null) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(pNew.getDateOfBirth());
                    DateModel<Calendar> dateModel = (DateModel<Calendar>) update.getDateOfBirth().getModel();
                    dateModel.setValue(calendar);
                }
                if (pNew.getPhoto() != null) {
                    pNew.getPhoto().getImage().flush();
                    update.getPhoto().setIcon(pNew.getPhoto());
                }
            } else {
                JOptionPane.showMessageDialog(update, p.getNif() + " doesn't exist.", update.getTitle(), JOptionPane.WARNING_MESSAGE);
                update.getReset().doClick();
            }
        }
    }

    private void handleUpdatePerson() {
        if (update != null) {
            String telefono = update.getPhoneNumber().getText();
            if (telefono == null || telefono.isBlank() || !telefono.matches("^\\+?[0-9 .()\\-]{7,20}$"))
            {
                JOptionPane.showMessageDialog(update,
                        "Only digits, spaces, '+', '-', '.', and parentheses are allowed.\nExample: +34 912-34-56-78",
                        update.getTitle(), JOptionPane.ERROR_MESSAGE);
                return;
            }

            Person p = new Person(update.getNam().getText(), update.getNif().getText());
            p.setPhoneNumber(telefono);

            if (update.getDateOfBirth().getModel().getValue() != null) {
                p.setDateOfBirth(((GregorianCalendar) update.getDateOfBirth().getModel().getValue()).getTime());
            }
            if (update.getPhoto().getIcon() != null) {
                p.setPhoto((ImageIcon) update.getPhoto().getIcon());
            }

            update(p);
            JOptionPane.showMessageDialog(null, "Person updated successfully!", "Person Updated", JOptionPane.INFORMATION_MESSAGE);
            update.getReset().doClick();
        }
    }

    private void handleReadAll() {
        ArrayList<Person> s = readAll();
        if (s.isEmpty()) {
            JOptionPane.showMessageDialog(menu, "There are not people registered yet.", "Read All - People v1.1.0", JOptionPane.WARNING_MESSAGE);
        } else {
            readAll = new ReadAll(menu, true);
            DefaultTableModel model = (DefaultTableModel) readAll.getTable().getModel();
            for (int i = 0; i < s.size(); i++) {
                model.addRow(new Object[i]);
                model.setValueAt(s.get(i).getNif(), i, 0);
                model.setValueAt(s.get(i).getName(), i, 1);
                model.setValueAt(s.get(i).getPhoneNumber(), i, 2);
                if (s.get(i).getDateOfBirth() != null) {
                    model.setValueAt(s.get(i).getDateOfBirth().toString(), i, 3);
                } else {
                    model.setValueAt("", i, 3);
                }
                if (s.get(i).getPhoto() != null) {
                    model.setValueAt("yes", i, 4);
                } else {
                    model.setValueAt("no", i, 4);
                }
            }
            readAll.setVisible(true);
        }
    }

    private void handleDeleteAll() {
        Object[] options = {"Yes", "No"};
        int answer = JOptionPane.showOptionDialog(menu, "Are you sure you want to delete all registered people?",
                "Delete All - People v1.1.0", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[1]);
        if (answer == 0) {
            deleteAll();
            JOptionPane.showMessageDialog(null, "All persons have been deleted successfully!", "Person Deleted All", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void handleCount() {
        int c = count();
        count = new Count(menu, true);
        JLabel label = (JLabel) count.getLabel();
        label.setText(String.valueOf(c));
        count.setVisible(true);
    }

    @Override
    public void insert(Person p) {
        try {
            if (dao.read(p) == null) {
                dao.insert(p);
            } else {
                throw new PersonException(p.getNif() + " is registered and can not be INSERTED.");
            }
        } catch (Exception ex) {
            handleException(ex, insert);
        }
    }

    @Override
    public void update(Person p) {
        try {
            dao.update(p);
        } catch (Exception ex) {
            handleException(ex, update);
        }
    }

    @Override
    public void delete(Person p) {
        try {
            if (dao.read(p) != null) {
                dao.delete(p);
                JOptionPane.showMessageDialog(null, "Person deleted successfully!", "Person Deleted", JOptionPane.INFORMATION_MESSAGE);
            } else {
                throw new PersonException(p.getNif() + " is not registered and can not be DELETED");
            }
        } catch (Exception ex) {
            handleException(ex, read);
        }
    }

    @Override
    public Person read(Person p) {
        try {
            return dao.read(p);
        } catch (Exception ex) {
            handleException(ex, read);
            return null;
        }
    }

    @Override
    public ArrayList<Person> readAll() {
        try {
            return dao.readAll();
        } catch (Exception ex) {
            handleException(ex, readAll);
            return new ArrayList<>();
        }
    }

    @Override
    public void deleteAll() {
        try {
            dao.deleteAll();
        } catch (Exception ex) {
            handleException(ex, menu);
        }
    }

    public int count() {
        return readAll().size();
    }

    private void handleException(Exception ex, Object parent) {
        if (ex instanceof FileNotFoundException || ex instanceof IOException ||
            ex instanceof ParseException || ex instanceof ClassNotFoundException ||
            ex instanceof SQLException || ex instanceof PersistenceException) {
            JOptionPane.showMessageDialog(null, ex.getMessage() + " Closing application.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        } else if (ex instanceof PersonException) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }
}
