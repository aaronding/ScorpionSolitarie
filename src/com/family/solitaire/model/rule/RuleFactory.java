/*
 * RuleFactory.java
 *
 * Created on December 8, 2006, 4:16 PM
 *
 */

package com.family.solitaire.model.rule;

/**
 *
 * @author Aaron Ding
 */
public class RuleFactory {
    
    private RuleFactory() {
    }
    
    private static RuleFactory instance = new RuleFactory();
    
    public static RuleFactory instance() { return instance; }
    
    public Rule createRule(String level) {
        if (level.equalsIgnoreCase("easy"))
            return new EasyRule();
        
        if (level.equalsIgnoreCase("medium"))
            return new MediumRule();
        
        if (level.equalsIgnoreCase("difficult"))
            return new DifficultRule();
        
        throw new IllegalArgumentException("invalid level:" + level);
    }
}
