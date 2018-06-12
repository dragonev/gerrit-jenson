package com.dragon.gerrit.jenson.data;

import static com.dragon.gerrit.jenson.util.Localization.getLocalized;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.dragon.gerrit.jenson.data.entity.SonarRule;
import com.dragon.gerrit.jenson.util.Logger;

public class ParseSonarRule {
	
	private SonarRule sonarRules;
	
	public SonarRule getSonarRules(InputStream inputStream, Logger logger) {
		sonarRules = new SonarRule();
		SAXReader reader = new SAXReader();
		Document document = null;
		try {
			document = reader.read(inputStream);
		} catch (DocumentException e) {
			logger.e(getLocalized("jenkins.plugin.error.sonar.rule.invalid"));
			return null;
		}
		Element root = document.getRootElement();
		parseNodes(root);
		return sonarRules;
	}
	
	private void parseNodes(Element node) {
    	if (node.getName().compareTo("rules") == 0) {
            List<Attribute> list = node.attributes();  
            for(Attribute attribute : list){  
            	sonarRules.setVersion(attribute.getValue());
            } 
    	} else if (node.getName().compareTo("rule") == 0) {
            String sonarKey = null;
            String sonarLevel = null;
            List<Attribute> list = node.attributes();
            for (Attribute attribute : list){  
                if (attribute.getName().compareTo("key") == 0) {
                	sonarKey = attribute.getValue();
                } else if (attribute.getName().compareTo("level") == 0) {
                	sonarLevel = attribute.getValue();
                }
            }
            if ((sonarKey != null) && (sonarLevel != null)) {
            	sonarRules.getRules().put(sonarKey, sonarLevel);
            }
    	}
    	
        Iterator<Element> iterator = node.elementIterator();  
        while(iterator.hasNext()){  
            Element e = iterator.next();  
            parseNodes(e);  
        } 
    } 
}
