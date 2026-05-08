/**
 * LICENSE
 * SPDX-License-Identifier: EUPL-1.2 OR LicenseRef-commercial
 *
 * mgm A12 Platform – https://www.mgm-tp.com
 * Copyright (c) 2012–2026 mgm technology partners GmbH
 *
 * This software is developed by mgm technology partners GmbH and
 * is available under a choice of two different licenses:
 *
 * 1. Open-Source License – EUPL v1.2
 *    You may redistribute and/or modify this software under the
 *    terms of the European Union Public Licence, version 1.2.
 *    The license text is available at https://eupl.eu/
 *    and included in this distribution as:
 *        licenses/EUPL-1.2.txt
 *
 * 2. Commercial License
 *    Alternatively, you may obtain a commercial license from
 *    mgm technology partners GmbH, that permits use of this
 *    software under different terms (including support and
 *    maintenance options). For commercial licensing, please contact:
 *        a12-license@mgm-tp.com
 *
 * You must select and comply with exactly one of the above
 * licensing options.
 *
 * Third-Party Components
 * ----------------------
 * This software includes third-party components.
 * Their licenses are provided in:
 *    THIRD_PARTY_NOTICES
 *    licenses/   (folder with full license texts)
 *
 * Trademarks
 * ----------
 * “mgm A12” is a registered trademark of mgm technology partners GmbH.
 * The A12 logo is protected by copyright.
 *
 * Warranty Disclaimer (applies to either option)
 * ----------------------------------------------
 * THIS SOFTWARE IS PROVIDED “AS IS” AND WITHOUT WARRANTY OF ANY KIND,
 * WHETHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NON-INFRINGEMENT,
 * EXCEPT WHERE SUCH DISCLAIMERS ARE NOT PERMITTED BY APPLICABLE LAW.
 * SEE THE RESPECTIVE LICENSE TEXT FOR DETAILS.
 *
 * End of LICENSE.
 */

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
