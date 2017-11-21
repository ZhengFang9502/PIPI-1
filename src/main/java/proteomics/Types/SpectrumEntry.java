package proteomics.Types;

import java.util.*;

public class SpectrumEntry {

    public final int scanNum;
    public final String scanId;
    public final float precursorMz;
    public final float precursorMass;
    public final int precursorCharge;
    public final String mgfTitle;
    public final int isotopeCorrectionNum;
    public final double ms1PearsonCorrelationCoefficient;
    public TreeMap<Integer, TreeSet<DevEntry>> chargeDevEntryMap;

    public SpectrumEntry(int scanNum, String scanId, float precursorMz, float precursorMass, int precursorCharge, String mgfTitle, int isotopeCorrectionNum, double ms1PearsonCorrelationCoefficient) {
        this.scanNum = scanNum;
        this.scanId = scanId;
        this.precursorMz = precursorMz;
        this.precursorMass = precursorMass;
        this.precursorCharge = precursorCharge;
        this.mgfTitle = mgfTitle;
        this.isotopeCorrectionNum = isotopeCorrectionNum;
        this.ms1PearsonCorrelationCoefficient = ms1PearsonCorrelationCoefficient;
    }

    public static class DevEntry implements Comparable<DevEntry> {

        public final int isotopeCorrectionNum;
        public final double pearsonCorrelationCoefficient;
        public final double[][] expMatrix;
        public final double[][] theoMatrix;
        private final String toString;
        private final int hashCode;

        public DevEntry(int isotopeCorrectionNum, double pearsonCorrelationCoefficient, double[][] expMatrix, double[][] theoMatrix) {
            this.isotopeCorrectionNum = isotopeCorrectionNum;
            this.pearsonCorrelationCoefficient = pearsonCorrelationCoefficient;
            this.expMatrix = expMatrix;
            this.theoMatrix = theoMatrix;
            toString = isotopeCorrectionNum + "-" + pearsonCorrelationCoefficient;
            hashCode = toString.hashCode();
        }

        public String toString() {
            return toString;
        }

        public int hashCode() {
            return hashCode;
        }

        public boolean equals(Object other) {
            if (other instanceof DevEntry) {
                DevEntry temp = (DevEntry) other;
                return isotopeCorrectionNum == temp.isotopeCorrectionNum && pearsonCorrelationCoefficient == temp.pearsonCorrelationCoefficient;
            } else {
                return false;
            }
        }

        public int compareTo(DevEntry other) {
            if (isotopeCorrectionNum > other.isotopeCorrectionNum) {
                return 1;
            } else if (isotopeCorrectionNum < other.isotopeCorrectionNum) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}