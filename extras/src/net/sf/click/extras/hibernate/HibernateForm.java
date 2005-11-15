/*
 * Copyright 2004-2005 Malcolm A. Edgar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.click.extras.hibernate;

import net.sf.click.control.Form;
import net.sf.click.control.HiddenField;

import org.apache.commons.lang.StringUtils;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.metadata.ClassMetadata;

/**
 * Provides Hibernate data aware Form control: &nbsp; &lt;form method='POST'&gt;.
 *
 * <table class='htmlHeader' cellspacing='10'>
 * <tr>
 * <td>
 *
 * <table class='fields'>
 * <tr>
 * <td align='left'><label>Department Name</label><span class="red">*</span></td>
 * <td align='left'><input type='text' name='name' value='' size='35' /></td>
 * </tr>
 * <tr>
 * <td align='left'><label>Description</label></td>
 * <td align='left'><textarea name='description' cols='35' rows='3'></textarea></td>
 * </tr>
 * </table>
 * <table class="buttons" align='right'>
 * <tr><td>
 * <input type='submit' name='ok' value='  OK  '/>&nbsp;<input type='submit' name='cancel' value='Cancel'/>
 * </td></tr>
 * </table>
 *
 * </td>
 * </tr>
 * </table>
 *
 * <a href="http://www.hibernate.org/">Hibernate</a> is an Object Relational
 * Mapping (ORM) framework.
 *
 * @author Malcolm Edgar
 * @version $Id$
 */
public class HibernateForm extends Form {

    private static final long serialVersionUID = -7134198516606088333L;

    /** The Hibernate session. */
    protected Session session;

    /** The Hibernate session factory. */
    protected SessionFactory sessionFactory;

    /** The value object identifier hidden field. */
    protected HiddenField oidField = new HiddenField("VOID", Long.class);

    /** The value object class name hidden field. */
    protected HiddenField classField = new HiddenField("VOCLASS", String.class);

    /**
     * The flag specifying that object validation meta data has been applied to
     * the form fields.
     */
    protected boolean metaDataApplied = false;

    // ----------------------------------------------------------- Constructors

    /**
     * Create a new HibernateForm with the given form name and value object
     * class.
     *
     * @param name the form name
     * @param valueClass the value object class
     */
    public HibernateForm(String name, Class valueClass) {
        super(name);
        add(oidField);
        add(classField);

        if (valueClass == null) {
            throw new IllegalArgumentException("Null valueClass parameter");
        }
        classField.setValue(valueClass.getName());
    }


    // --------------------------------------------------------- Public Methods

    /**
     * Return the form Hibernate <tt>Session</tt>. If form session is not
     * defined this method will obtain a session from the
     * {@link SessionContext}.
     * <p/>
     * Applications using alternative Hibernate <tt>Session</tt> sources should
     * set the form's session using the {@link #setSession(Session)} method.
     *
     * @return the form Hibernate session
     */
    public Session getSession() {
        if (session == null) {
            session = SessionContext.getSession();
        }
        return session;
    }

    /**
     * Set the user's Hibernate <tt>Session</tt>.
     *
     * @param session the user's Hibernate session
     */
    public void setSession(Session session) {
        this.session = session;
    }

    /**
     * Return the application Hibernate <tt>SessionFactory</tt>.
     * If session factory is not defined this method will obtain the session
     * factory from the {@link SessionContext}.
     * <p/>
     * Applications using an alternative Hibernate <tt>SessionFactory</tt>
     * sources should set the form's session factory using the
     * {@link #setSessionFactory(SessionFactory)} method.
     *
     * @return the user's Hibernate session
     */
    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Set the form Hibernate <tt>SessionFactory</tt>.
     *
     * @param sessionFactory the Hibernate SessionFactory
     */
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Return a Hibernate value object from the form with the form field values
     * copied into the object's properties.
     *
     * @return the Hibernate object from the form with the form field values applied
     *  to the object properties.
     */
    public Object getValueObject() {
        if (StringUtils.isNotBlank(classField.getValue())) {
            try {
                Class valueClass = Class.forName(classField.getValue());

                Long oid = (Long) oidField.getValueObject();

                Object valueObject = null;
                if (oid != null) {
                    getSession().load(valueClass, oid);
                } else {
                    valueObject = valueClass.newInstance();
                }

                copyTo(valueObject, true);

                return valueObject;

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    /**
     * Set the given Hibernate value object in the form, copying the object's
     * properties into the form field values.
     *
     * @param valueObject the Hibernate value object to set
     */
    public void setValueObject(Object valueObject) {
        if (valueObject != null) {
            // TODO: other identifier types: string
            ClassMetadata classMetadata =
                getSessionFactory().getClassMetadata(valueObject.getClass());

            // TODO
            //Long oid = (Long) classMetadata.getIdentifier(valueObject);

            //oidField.setValue(oid);

            copyFrom(valueObject, true);
        }
    }

    public boolean saveChanges() {
        Object valueObject = getValueObject();

        Transaction transaction = null;
        try {
            Session session = getSession();

            transaction = session.beginTransaction();

            session.saveOrUpdate(valueObject);

            transaction.commit();

            return true;

        } catch (HibernateException he) {
            if (transaction != null) {
                try {
                   transaction.rollback();
                } catch (HibernateException re) {
                }
            }
            throw he;
        }
    }

    /**
     * This method applies the object meta data to the form fields and then
     * invokes the <tt>super.onProcess()</tt> method.
     *
     * @see Form#onProcess()
     */
    public boolean onProcess() {
        applyMetaData();
        return super.onProcess();
    }

    /**
     * This method applies the object meta data to the form fields and then
     * invokes the <tt>super.toString()</tt> method.
     *
     * @see Form#toString()
     */
    public String toString() {
        applyMetaData();
        return super.toString();
    }

    // ------------------------------------------------------ Protected Methods

    protected void applyMetaData() {
        if (metaDataApplied) {
            return;
        }

        try {
            Class valueClass = Class.forName(classField.getValue());

            ClassMetadata classMetadata =
                getSessionFactory().getClassMetadata(valueClass);

            // TODO: apply constraints to fields.

        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }

        metaDataApplied = true;
    }

}
