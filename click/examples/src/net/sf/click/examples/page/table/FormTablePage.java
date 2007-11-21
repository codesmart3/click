package net.sf.click.examples.page.table;

import java.util.List;

import net.sf.click.control.Checkbox;
import net.sf.click.control.Column;
import net.sf.click.control.Form;
import net.sf.click.control.Submit;
import net.sf.click.control.Table;
import net.sf.click.control.TextField;
import net.sf.click.examples.control.InvestmentSelect;
import net.sf.click.examples.page.BorderPage;
import net.sf.click.extras.control.DateField;
import net.sf.click.extras.control.EmailField;
import net.sf.click.extras.control.FieldColumn;
import net.sf.click.extras.control.FormTable;
import net.sf.click.extras.control.NumberField;

/**
 * Provides an demonstration of Table control paging.
 *
 * @author Malcolm Edgar
 */
public class FormTablePage extends BorderPage {

    private static final int NUM_ROWS = 20;

    public FormTable table = new FormTable();

    // ------------------------------------------------------------ Constructor

    public FormTablePage() {
        // Setup customers table
        table.setClass(Table.CLASS_SIMPLE);
        table.setWidth("700px");
        table.getForm().setButtonAlign(Form.ALIGN_RIGHT);
        table.setPageSize(10);
        table.setShowBanner(true);

        table.addColumn(new Column("id"));

        FieldColumn column = new FieldColumn("name", new TextField());
        column.getField().setRequired(true);
        column.setVerticalAlign("baseline");
        table.addColumn(column);

        column = new FieldColumn("email", new EmailField());
        column.getField().setRequired(true);
        table.addColumn(column);

        column = new FieldColumn("investments", new InvestmentSelect());
        column.getField().setRequired(true);
        table.addColumn(column);

        NumberField numberField = new NumberField();
        numberField.setSize(10);
        column = new FieldColumn("holdings", numberField);
        column.setTextAlign("right");
        table.addColumn(column);

        column = new FieldColumn("dateJoined", new DateField());
        column.setDataStyle("white-space", "nowrap");
        table.addColumn(column);

        column = new FieldColumn("active", new Checkbox());
        column.setTextAlign("center");
        table.addColumn(column);

        table.getForm().add(new Submit("ok", "  OK  ", this, "onOkClick"));
        table.getForm().add(new Submit("cancel", this, "onCancelClick"));
    }

    // --------------------------------------------------------- Event Handlers

    /**
     * @see net.sf.click.Page#onInit()
     */
    public void onInit() {
        List customers = getCustomerService().getCustomersSortedByName(NUM_ROWS);
        table.setRowList(customers);
    }

    public boolean onOkClick() {
        if (table.getForm().isValid()) {
            // Please note with Cayenne ORM this will persist any changes
            // to data objects submitted by the form.
            getDataContext().commitChanges();

            // With other ORM frameworks like Hibernate you would retrieve
            // rows for the table as persist those objects. For example:
            /*
            List rowList = table.getRowList();
            for (Iterator i = rowList.iterator(); i.hasNext();) {
                Object row = (Object) i.next();
                getSession().save(row);
            }
            */
        }
        return true;
    }

    public boolean onCancelClick() {
        // Rollback any changes made to the customers, which are stored in
        // the data context
        getDataContext().rollbackChanges();

        List customers = getCustomerService().getCustomersSortedByName(NUM_ROWS);

        table.setRowList(customers);
        table.setRenderSubmittedValues(false);

        return true;
    }

}
