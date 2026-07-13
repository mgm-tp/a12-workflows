
package com.mgmtp.a12.model.workflows.migration.config.types;

import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.mgmtp.a12.model.workflows.migration.config.types package. 
 * <p>An ObjectFactory allows you to programmatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {


    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.mgmtp.a12.model.workflows.migration.config.types
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link MigrationConfig }
     * 
     * @return
     *     the new instance of {@link MigrationConfig }
     */
    public MigrationConfig createMigrationConfig() {
        return new MigrationConfig();
    }

    /**
     * Create an instance of {@link MigrationStepConfigType }
     * 
     * @return
     *     the new instance of {@link MigrationStepConfigType }
     */
    public MigrationStepConfigType createMigrationStepConfigType() {
        return new MigrationStepConfigType();
    }

    /**
     * Create an instance of {@link BeanType }
     * 
     * @return
     *     the new instance of {@link BeanType }
     */
    public BeanType createBeanType() {
        return new BeanType();
    }

}
