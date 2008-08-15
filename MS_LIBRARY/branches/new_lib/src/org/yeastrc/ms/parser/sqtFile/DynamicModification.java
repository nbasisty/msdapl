package org.yeastrc.ms.parser.sqtFile;

import java.math.BigDecimal;

import org.yeastrc.ms.domain.search.MsSearchModification;

public class DynamicModification implements MsSearchModification {

    private char modificationChar;
    private BigDecimal modificationMass;
    private char modificationSymbol;
    
    public DynamicModification(char modChar, BigDecimal modMass, char modSymbol) {
        this.modificationChar = modChar;
        this.modificationMass = modMass;
        this.modificationSymbol = modSymbol;
    }

    /**
     * @return the modificationChar
     */
    public char getModifiedResidue() {
        return modificationChar;
    }

    /**
     * @return the modificationMass
     */
    public BigDecimal getModificationMass() {
        return modificationMass;
    }

    /**
     * @return the modificationSymbol
     */
    public char getModificationSymbol() {
        return modificationSymbol;
    }

    public ModificationType getModificationType() {
        return MsSearchModification.ModificationType.DYNAMIC;
    }
}
