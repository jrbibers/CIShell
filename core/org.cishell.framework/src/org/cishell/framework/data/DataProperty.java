/* **************************************************************************** 
 * CIShell: Cyberinfrastructure Shell, An Algorithm Integration Framework.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Apache License v2.0 which accompanies
 * this distribution, and is available at:
 * http://www.apache.org/licenses/LICENSE-2.0.html
 * 
 * Created on Jun 15, 2006 at Indiana University.
 * 
 * Contributors:
 *     Indiana University - Initial API
 * ***************************************************************************/
package org.cishell.framework.data;


/**
 * Standard property keys to use when creating meta-data for a 
 * {@link Data} object.
 * 
 * @author Bruce Herr (bh2@bh2.net)
 */
public interface DataProperty {
    /** 
     * The label to give the Data object if displayed. The type associated with
     * this property is of type {@link String}.
     */
    public static final String LABEL = "Label";
    
    /** 
     * The parent Data object of the Data object. This is used when a Data object
     * is derived from another Data object to show the hierarchical relationship
     * between them.  This property can be null, signifying that the Data object
     * was not derived from any other Data object, such as when loading a new Data
     * object from a file. The type associated with this property is of type 
     * {@link Data} 
     */
    public static final String PARENT = "Parent";    
    
    //TODO: should we consider removing this/changing it?
    /**
     * The general type of the Data object. Various standard types are created as 
     * constants with name *_TYPE from this class. These can be used, or new
     * types can be introduced as needed. The type associated with this 
     * property is of type {@link String}.
     */
    public static final String TYPE = "Type";
    
    /**
     * Flag to determine if the Data object has been modified and not saved since
     * the modification. This is used to do things like notify the user before 
     * they exit that a modified Data object exists and see if they want to save 
     * it. The type associated with this property is of type {@link Boolean}.
     */
    public static final String MODIFIED = "Modified";
    
    /** Says this data model is abstractly a matrix */
    public static String MATRIX_TYPE = "Matrix";
    
    /** Says this data model is abstractly a network */
    public static String NETWORK_TYPE = "Network";
    
    /** Says this data model is abstractly a tree */
    public static String TREE_TYPE = "Tree";
    
    /** Says this data model is abstractly an unknown type */
    public static String OTHER_TYPE = "Unknown";     
}