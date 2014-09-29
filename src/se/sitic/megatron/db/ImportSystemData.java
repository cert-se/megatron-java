package se.sitic.megatron.db;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import se.sitic.megatron.core.AppProperties;
import se.sitic.megatron.core.MegatronException;
import se.sitic.megatron.core.TypedProperties;
import se.sitic.megatron.entity.Organization;
import se.sitic.megatron.util.Constants;
import se.sitic.megatron.util.IpAddressUtil;



public class ImportSystemData {

	private static final Logger log = Logger.getLogger(ImportSystemData.class);
	private TypedProperties props;
    private Matcher commentMatcher;
    private Matcher classDefMatcher;
    private Matcher dataMatcher;
    private String currentClassName = null;  
    private Object parentObject = null;
    private Map<String, String[]> relatedObjectsToAdd = null; 
    private Map<String, Integer> objectKeyIndexes = null; 
    private Map<String, String[]> objectParameterNames = null;
    private static final String ENITY_PACKAGE_PREFIX = "se.sitic.megatron.entity.";
    
    private static final String OBJECT_KEY_MARKER = "*";
    private static final String INTEGER_CLASS_NAME = "java.lang.Integer";
    private static final String STRING_CLASS_NAME = "java.lang.String";
    private static final String LONG_CLASS_NAME = "java.lang.Long";
    private static final String PRIORITY_CLASS_NAME = "Priority";
    private static final String ENTRYTYPE_CLASS_NAME = "EntryType";
    private static final String IPRANGE_CLASS_NAME = "IpRange";
    private static final String ORGANIZATION_CLASS_NAME = "Organization";
    private static final String SET_EMAILS_METHOD_NAME = "setEmailAddresses";
    private static final String GET_ORGANIZATION_ID = "getOrganizationId";
    private static final String SET_ORGANIZATION_ID = "setOrganizationId";

    
    private static final String COMMENT_REG_EXP = "^#";
    private static final String CLASSDEF_REG_EXP = "^!(.+);(.*)$";
    private static final String DATA_REG_EXP = "(^.+(;.)*)$";
    private static final String SET_CLASS_NAME_REG_EXP = "^java\\.util\\.Set<se\\.sitic\\.megatron\\.entity\\.(.+)>";
    
    private DbManager dbm = null;
    private int processedLinesC = 0;
    private int readLinesC = 0;
    
	
	public ImportSystemData(TypedProperties props) throws MegatronException {
        this.props = props;
        this.commentMatcher = Pattern.compile(COMMENT_REG_EXP).matcher("");
        
        this.classDefMatcher = Pattern.compile(CLASSDEF_REG_EXP).matcher("");
                
        this.dataMatcher = Pattern.compile(DATA_REG_EXP).matcher("");
        
        objectParameterNames = new HashMap<String,String[]>();
        objectKeyIndexes = new HashMap<String, Integer>();
        
        try {
			dbm = DbManager.createDbManager(props);
		} catch (DbException e) {
		    throw handleException("ImportSystemData", e);			
		}                
        
    }
	
	
	public void importFile() throws MegatronException {
	    
	   importFile(null); 
	    
	}
	
	public void importFile(String fileName) throws MegatronException {
	    
	    if (fileName == null || fileName.equals("")) {	
	        fileName = props.getString(AppProperties.CONTACTS_IMPORT_FILE_KEY, "conf/dev/systemdata.txt");
	        log.error("No import filename provided");
	        return;
	    }	    
	    
        File file = new File(fileName);
        log.info("Reading system data from file: " + file.getAbsolutePath());               
        BufferedReader in = null;
        try {                       
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file), Constants.UTF8));
            String line = null;
            while ((line = in.readLine()) != null) {
                ++readLinesC;
                if (processLine(line)) {
                    ++processedLinesC;
                } 
            }
            log.info("Import finished, number of processed lines: " + processedLinesC);
        } catch (IOException e) {            
            throw handleException("importFile", e);
        } finally {
            try { if (in != null) in.close(); } catch (Exception ignored) {}            
        }
    }
	
	
	private boolean processLine(String line) throws MegatronException {
        boolean result = false;
        
        log.debug("Processing line: " + line);
        
        if (isComment(line)){
        	log.debug("Line is comment");        	
        }
        else if (line.trim().equals("") == false) {
            result = true;
        	if (processClassDef(line) == false) {
        		processDataLine(line);
        	}        		
        }        
        return result;
	}
	
	
	private boolean isComment(String line) {
				
		this.commentMatcher.reset(line);
		
		return this.commentMatcher.find();		
		
	}
	
	
	private boolean processClassDef(String line) {
	
		boolean result = false;
		
		
		this.classDefMatcher.reset(line);
		
		if (this.classDefMatcher.matches()) {
			
			String className = this.classDefMatcher.group(1);
			log.debug("Class= " + className + ", attributeNames = " + this.classDefMatcher.group(2));
			String[] attributeNames = this.classDefMatcher.group(2).split(",");
			
			for (int i=0; i<attributeNames.length; i++) {
				if (attributeNames[i].startsWith(OBJECT_KEY_MARKER)) {
					log.debug("Object key index for " + attributeNames[i] + " is " + i);
					objectKeyIndexes.put(className, i);
					attributeNames[i] = attributeNames[i].substring(1);
				}				
			}
						
			this.objectParameterNames.put(className, attributeNames);
			
			this.currentClassName = className;
			result = true;
		}		
		return result;
					
	}
	
	private boolean processDataLine(String line) 
		throws MegatronException {

		log.debug("Processing data line" + line);
		boolean result = false;

		this.dataMatcher.reset(line);

		if (this.dataMatcher.matches()) {
		    
			String[] attributeValues = this.dataMatcher.group(1).split(";", this.objectParameterNames.get(this.currentClassName).length);		    
		    
			this.relatedObjectsToAdd = new HashMap<String, String[]>();
			this.parentObject = createOrLoadObject(this.currentClassName, attributeValues);
			if (this.parentObject != null) {
			    setObjectAttributes(this.parentObject, attributeValues, true);
			}
			else {
			    log.error("Could not load or create parent object " + this.currentClassName);
			}
			result = true;
		}
		else {
			log.error("Data line did not match data format");
			throw new MegatronException("Data format error in line: " + line);
		}

		return result;
	}

	private Type getMethodParamType(String className, String methodName) 
		throws MegatronException {
		
		Type type = null;
		
		
			try {
				for (Method method : Class.forName(ENITY_PACKAGE_PREFIX + className).getMethods()) {
					
					if (method.getName().equals(methodName)) {					    
					    Type[] types = method.getGenericParameterTypes();
					    if (types != null && types.length > 0) {
						  type = types[0]; 
					    }
					}			
				}
			} catch (Exception e) {				
				throw handleException("getMethodParamType", e);
			}
			
		return type;
	}
	

	private Object createOrLoadObject(String className, String[] attributeValues) 
		throws MegatronException {
				
		
		Object obj = null;
		
		try {
			log.debug("Trying to load " + className);
			Integer objectKeyIndex = this.objectKeyIndexes.get(className);
			if (objectKeyIndex != null){
				// load object

				String keyValue = attributeValues[objectKeyIndex];
				// Check if value is empty
				if (keyValue == null || keyValue.equals("")) {
				    // Empty value field bail out...				    
				    return null;				    
				}
				
				String keyName = this.objectParameterNames.get(className)[objectKeyIndex];				
				Type paramType = getMethodParamType(className, "set" + keyName);				
				
				Object keyObject = setMethodParameter(keyValue, paramType);
								
				obj = dbm.genericLoadObject(className, keyName, keyObject);
				
			}
			if (obj == null){ 		
				obj = Class.forName(ENITY_PACKAGE_PREFIX + className).newInstance();
				initNewObject(obj);
			}
		} catch (Exception e) {
			throw handleException("createOrLoadObject", e);
		} 
		
		return obj;
	}

	
	private void initNewObject(Object obj) {
	    if (obj instanceof Organization) {
	        Organization organization = (Organization)obj;
	        organization.setEnabled(true);
	        organization.setCountryCode("SE");
	        organization.setLanguageCode("sv");
            organization.setAutoUpdateMatchFields(true);
	    }
	}
	

	private void setObjectAttributes(Object object, String[] attributeValues, boolean isParentObject) 
		throws MegatronException {
	    
	    String className =  object.getClass().getSimpleName();
        log.debug("Decorating object of class " + className);
        
        if (attributeValues.length < 1) {
	        log.info("Empty attribute values for class " + className + ", exiting");
	        return;
	    }
	    
		try {						
			String[] attributeNames = this.objectParameterNames.get(className);
			
			if(attributeNames == null) {
			    throw new MegatronException("Emtpy attributeNames for object " + className);
			}
			
			Method[] methods = object.getClass().getMethods();
			for (int a=0; a<attributeNames.length; a++) {
								
				String methodName = "set" + attributeNames[a];				
				//log.debug("Method is : " + methodName);
				
				
				for (Method method : methods) {
					//log.debug("Method is " + method.getName());
					// Check if method name matches and if it is allowed to be automaticly edited
					if (method.getName().equals(methodName) && canAutoEditAttribute(methodName, object)) {
					        						
						Class<?>[] paramTypes = method.getParameterTypes();
						for (Class<?> paramClass: paramTypes) {
							log.debug("Class found " + paramClass.getName());
							String paramTypeName = method.getGenericParameterTypes()[0].toString();
							Matcher classNameMatcher = Pattern.compile(SET_CLASS_NAME_REG_EXP).matcher(paramTypeName);							
							if (classNameMatcher.matches() && canAutoEditSet(object)) {
								// Set
								log.debug("Found Set " + paramTypeName);
								
								String[] setValues = attributeValues[a].split(",");
								relatedObjectsToAdd.put(classNameMatcher.group(1), setValues);
								
							}
							else {		
								
								Object valueObject = null;
								valueObject = setMethodParameter(attributeValues[a], paramClass);
								
	 							if (valueObject != null) {
	 								log.debug("Invoking method: " + method.getName() + " on " + object.getClass().getSimpleName() + " whith " +  valueObject.toString());
	 								log.debug("ParamClass = " + paramClass.getSimpleName());
	 								method.invoke(object, valueObject);
	 							}
							}
						}
						
					}				
				}
			}
			if (isParentObject){
				Method dbMethod = dbm.getClass().getMethod("add" + className , this.parentObject.getClass());
				// Save the object
				log.debug("Saving object " + dbMethod.getName());
				dbMethod.invoke(dbm, this.parentObject);			
				if (relatedObjectsToAdd.isEmpty() ==  false && addRelatedObjects()) {
					//save again
					dbMethod.invoke(dbm, this.parentObject);					
				}
			}
		} catch (Exception e) {			
			throw handleException("setObjectAttributes", e);			
		}
	}



	private Object setMethodParameter(String attributeValue, Type type) 
		throws DbException {
		
		Object valueObject = null;
		
		log.debug("Type is " + type.toString());		
		log.debug("Value is: " + attributeValue);
		
		
		if ((type.equals(Integer.TYPE) || type.toString().endsWith(INTEGER_CLASS_NAME)) && attributeValue.equals("") == false ) {
			valueObject = new Integer(attributeValue);
		} 
		else if ((type.equals(Long.TYPE) || type.toString().endsWith(LONG_CLASS_NAME)) && attributeValue.equals("") == false ) {
			valueObject = new Long(attributeValue);								
		}
		else if (type.equals(Boolean.TYPE) && attributeValue.equals("") == false) {
			valueObject = new Boolean(attributeValue);									 
		}									
		else if (type.toString().endsWith(STRING_CLASS_NAME)) {
		    if (attributeValue.trim().equals("") == false) { 
		        valueObject = attributeValue;					
		    }
		}
		else if (type.toString().endsWith(PRIORITY_CLASS_NAME)) {																		
			log.debug("Object is prio, value is " + attributeValue);
			valueObject = dbm.getPriority(Integer.parseInt(attributeValue));										
		}
		else if (type.toString().endsWith(ENTRYTYPE_CLASS_NAME)) {																		
			log.debug("Object is EntryType, value is " + attributeValue);
			valueObject = dbm.genericLoadObject(ENTRYTYPE_CLASS_NAME, "Name", attributeValue);												
		}
		else {
			log.error("Unknown type " + type.toString());
		}
		return valueObject;
	}
	
	
	private boolean addRelatedObjects() throws MegatronException {
		
		boolean result = false;
		
		
		
		Set<String> objectNames = relatedObjectsToAdd.keySet();
	
		while (objectNames.iterator().hasNext()) {
			try {
				String relatedClassName = objectNames.iterator().next();
				Object obj;

				

				String[] valueList = relatedObjectsToAdd.get(relatedClassName);
				for (String objectValues : valueList) {
					log.debug("Object values: " + objectValues);
				}
								
				relatedObjectsToAdd.remove(relatedClassName);
				for (String objectValues : valueList) {
					String[] values = objectValues.split("-");
															
					// Dirty solution to convert ip-address for IpRange object from dotted format to numeric format.
					if (relatedClassName.endsWith(IPRANGE_CLASS_NAME)) {
						for (int i=0;i<values.length;i++) {
						    if (values[i]!= null && values[i].equals("") == false) {
						        values[i] = Long.toString(IpAddressUtil.convertIpAddress(values[i]));
						    }
						}						
					}
					obj = createOrLoadObject(relatedClassName, values);
					if (obj != null) {
					    // Check that parent is same
					    Type paramType = getMethodParamType(obj.getClass().getSimpleName(), SET_ORGANIZATION_ID);					    
					    if (paramType != null) {									        
					        Object ownerId = obj.getClass().getMethod(GET_ORGANIZATION_ID).invoke(obj);
					        Object parentId = this.parentObject.getClass().getMethod("getId").invoke(this.parentObject);					        
					        if (ownerId != null && parentId != null && ownerId.equals(parentId) == false) {
					            // Object owned by another parent.
					            throw handleException ("", new MegatronException("Conflict! " + obj.getClass().getSimpleName() + " is allready owned by "  
					                    + this.parentObject.getClass().getSimpleName() + " with id " + parentId));
					        }
					    }					    
					    
					    setObjectAttributes(obj, values, false);

					    Method addToListMethod = parentObject.getClass().getMethod("addTo" + relatedClassName + "s", obj.getClass());
					    addToListMethod.invoke(this.parentObject, obj);
					}
				}
				
				log.debug("Empty " + relatedObjectsToAdd.isEmpty()); 
				result = true;				

			} catch (Exception e) {
				throw handleException("addRelatedObjects", e);
			}

		}
	
		return result;
	
    }

	private boolean canAutoEditSet(Object obj) {
	    
	    boolean result = true; 
	    
	    //Check if class is Organization
	    if (obj.getClass().getSimpleName() == ORGANIZATION_CLASS_NAME) {
	        // Check if automatic update is allowed.
	        result = ((Organization)obj).isAutoUpdateMatchFields();
	    }	   
	    return result;
	}
	
	private boolean canAutoEditAttribute(String methodName, Object obj) {
        
        boolean result = true; 
        
        //Check if class is Organization
        if (methodName.equals(SET_EMAILS_METHOD_NAME) && obj.getClass().getSimpleName() == ORGANIZATION_CLASS_NAME) {
            // Check if automatic update is allowed.         
        	;
        }      
        return result;
    }
	
	private MegatronException handleException(String methodName, Exception e) {
		
		MegatronException result = null;
		
		String msg = e.getClass().getSimpleName() + " in " + methodName + "; (import file line no: " + readLinesC + ") : " + e.getMessage();
		log.error(msg, e);
		if (e.getClass().getSimpleName().equals("MegatronException")){
			result = (MegatronException)e;
		}
		else {			
			e.printStackTrace();
		    result = new MegatronException(msg);			
		}
		return result;
	}
	
}
