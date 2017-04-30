package com.malikov.lowcostairline.context;

import com.malikov.lowcostairline.context.exceptions.ApplicationContextInitializationException;
import com.malikov.lowcostairline.context.exceptions.ContextException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * @author Yurii Malikov
 */
public class ApplicationContext {

    private static final String BEANS = "beans";
    private static final String BEAN = "bean";
    private static final String PROPERTY = "property";
    private static final String ID = "id";
    private static final String CLASS = "class";
    private static final String NAME = "name";
    private static final String REF = "ref";
    private static final String VALUE = "value";


    private Map<String, Object> contextMap = new HashMap<>();

    private Document[] xmlDocuments;

    private String[] xmlDocumentsPaths;

    public ApplicationContext(String... xmlDocumentsPaths) {
        this.xmlDocumentsPaths = xmlDocumentsPaths;
    }

    public void initializeXmlDocuments() {
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            URL[] xmlDocumentsUrls = new URL[xmlDocumentsPaths.length];
            for (int i = 0; i < xmlDocuments.length; i++) {
                xmlDocumentsUrls[0] = getClass().getClassLoader().getResource(xmlDocumentsPaths[i]);
                if (xmlDocumentsUrls[0] == null) {
                    throw new FileNotFoundException("XML document has not been found.");
                }
                xmlDocuments[i] = documentBuilder.parse(xmlDocumentsUrls[i].getPath());
            }
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new ApplicationContextInitializationException(e);
        }
        parseXmlDocuments();
    }

    private void parseXmlDocuments() {
        for (Document document : xmlDocuments) {
            NodeList beanDeclarationNodes = document.getElementsByTagName(BEANS);
            Node xmlBeansTagNode = beanDeclarationNodes.item(0);
            if (xmlBeansTagNode == null) {
                throw new ContextException("There is no <beans> tag in " + document.getDocumentURI() + " file to parse");
            }
            Queue<XmlBeanProperty> commonXmlBeanPropertiesQueue = new LinkedList<>();
            NodeList xmlBeans = xmlBeansTagNode.getChildNodes();
            for (int i = 0; i < xmlBeans.getLength(); i++) {
                Node xmlBean = xmlBeans.item(i);
                if (BEAN.equals(xmlBean.getNodeName())) {
                    String beanName = createBean(xmlBean);

                    NodeList xmlProperties = xmlBean.getChildNodes();
                    for (int j = 0; j < xmlProperties.getLength(); j++) {
                        if (PROPERTY.equals(xmlProperties.item(i).getNodeName())) {
                            commonXmlBeanPropertiesQueue.offer(new XmlBeanProperty(beanName, xmlProperties.item(i)));
                        }
                    }
                }
            }
            while (!commonXmlBeanPropertiesQueue.isEmpty()) {
                instantiateXmlBeanProperties(commonXmlBeanPropertiesQueue.poll());
            }
        }

    }

    private void instantiateXmlBeanProperties(XmlBeanProperty xmlBeanProperty) {

        Node xmlBeanPropertyNode = xmlBeanProperty.getBeanProperty();

        NamedNodeMap xmlBeanPropertyAttributes = xmlBeanPropertyNode.getAttributes();
        Node propertyNameNode = xmlBeanPropertyAttributes.getNamedItem(NAME);
        Node propertyRefNode = xmlBeanPropertyAttributes.getNamedItem(REF);
        Node propertyValueNode = xmlBeanPropertyAttributes.getNamedItem(VALUE);

        if (propertyNameNode == null || propertyRefNode == null ^ propertyValueNode == null) {
            throw new ContextException(String.format("Bean property initialization failure. Bad syntax. "
                    + "Tag \"%s\" and one of \"%s\" or \"%s\" should be declared.", NAME, REF, VALUE));
        }

        Object bean = contextMap.get(xmlBeanProperty.getBeanName());
        String propertyNameValue = propertyNameNode.getNodeValue();
        Class<?> beanClass = bean.getClass();

        Field beanField = findClassField(beanClass, propertyNameValue);

        if (beanField == null) {
            throw new ContextException(String.format("Bean property instantiation failure." +
                            " Property %s has not been found in class %s", propertyNameValue, beanClass));
        }

        beanField.setAccessible(true);

        Object beanFieldValue;
        if (propertyRefNode != null) {
            beanFieldValue = contextMap.get(propertyRefNode.getNodeValue());
        } else {
            if (propertyValueNode.getNodeValue() == null) {
                throw new NullPointerException(String.format("Property value for bean \"%s\" not provided",  bean));
            }
            beanFieldValue = getFieldValue(beanField, propertyValueNode.getNodeValue());
        }

    }

    private Object getFieldValue(Field beanField, String stringValue) {
        if (beanField.getType() == String.class) {
            return stringValue;
        }
        try {
            return beanField.getType().getConstructor(String.class).newInstance(stringValue);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new ContextException(String.format("Field %s instantiatiation by value %s failed", beanField, stringValue));
        }
    }

    /**
     * Recursive search for field in class and its superclasses
     */
    private static Field findClassField(Class clazz, String fieldName) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (fieldName.equals(field.getName())) {
                return field;
            }
        }
        Class superClass = clazz.getSuperclass();
        if (superClass != null) {
            findClassField(superClass, fieldName);
        }
        return null;
    }

    private String createBean(Node bean) {
        NamedNodeMap xmlBeanAttributes = bean.getAttributes();
        String xmlBeanId = xmlBeanAttributes.getNamedItem(ID).getNodeValue();
        String xmlBeanClass = xmlBeanAttributes.getNamedItem(CLASS).getNodeValue();
        contextMap.put(xmlBeanId, instantiateXmlBean(xmlBeanClass));
        return xmlBeanId;
    }

    private Object instantiateXmlBean(String xmlBeanClass) {
        try {
            Class<?> cls = Class.forName(xmlBeanClass);
            return cls.newInstance();
        } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
            throw new ContextException("Bean instantiation failure.", e);
        }
    }


    private class XmlBeanProperty {

        String beanName;

        Node beanProperty;

        public XmlBeanProperty(String beanName, Node beanProperty) {
            this.beanName = beanName;
            this.beanProperty = beanProperty;
        }

        public String getBeanName() {
            return beanName;
        }

        public Node getBeanProperty() {
            return beanProperty;
        }
    }



}
