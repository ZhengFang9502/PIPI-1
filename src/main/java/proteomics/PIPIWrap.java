package proteomics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proteomics.Index.BuildIndex;
import proteomics.PTM.FindPTM;
import proteomics.PTM.GeneratePtmCandidatesCalculateXcorr;
import proteomics.Search.CalSubscores;
import proteomics.Search.CalXcorr;
import proteomics.Search.Search;
import proteomics.Segment.InferenceSegment;
import proteomics.Spectrum.PreSpectrum;
import proteomics.TheoSeq.MassTool;
import proteomics.Types.*;

import java.util.*;
import java.util.concurrent.Callable;

public class PIPIWrap implements Callable<FinalResultEntry> {

    private static final Logger logger = LoggerFactory.getLogger(PIPIWrap.class);

    private final BuildIndex buildIndexObj;
    private final MassTool massToolObj;
    private final InferenceSegment inference3SegmentObj;
    private final SpectrumEntry spectrumEntry;
    private String sqlPath;
    private final float ms1Tolerance;
    private final int ms1ToleranceUnit;
    private final float ms2Tolerance;
    private final float minPtmMass;
    private final float maxPtmMass;
    private final int maxMs2Charge;
    private final boolean sqlInMemory;
    private final int fetchSize;

    public PIPIWrap(BuildIndex buildIndexObj, MassTool massToolObj, SpectrumEntry spectrumEntry, String sqlPath, float ms1Tolerance, int ms1ToleranceUnit, float ms2Tolerance, float minPtmMass, float maxPtmMass, int maxMs2Charge, boolean sqlInMemory, int fetchSize) {
        this.buildIndexObj = buildIndexObj;
        this.massToolObj = massToolObj;
        this.spectrumEntry = spectrumEntry;
        this.sqlPath = sqlPath;
        this.ms1Tolerance = ms1Tolerance;
        this.ms1ToleranceUnit = ms1ToleranceUnit;
        this.ms2Tolerance = ms2Tolerance;
        this.minPtmMass = minPtmMass;
        this.maxPtmMass = maxPtmMass;
        this.maxMs2Charge = maxMs2Charge;
        inference3SegmentObj = buildIndexObj.getInference3SegmentObj();
        this.sqlInMemory = sqlInMemory;
        this.fetchSize = fetchSize;
    }

    @Override
    public FinalResultEntry call() {
        // Coding
        List<ThreeExpAA> expAaLists = inference3SegmentObj.inferSegmentLocationFromSpectrum(spectrumEntry);
        if (!expAaLists.isEmpty()) {
            SparseVector scanCode = inference3SegmentObj.generateSegmentIntensityVector(expAaLists);

            // Begin search.
            Search searchObj = new Search(buildIndexObj, spectrumEntry, scanCode, sqlPath, massToolObj, ms1Tolerance, ms1ToleranceUnit, minPtmMass, maxPtmMass, maxMs2Charge, sqlInMemory, fetchSize);

            // Infer PTMs based on DP
            FindPTM findPtmObj = new FindPTM(searchObj.getPTMOnlyResult(), spectrumEntry, expAaLists, inference3SegmentObj.getModifiedAAMassMap(), inference3SegmentObj.getPepNTermPossibleMod(), inference3SegmentObj.getPepCTermPossibleMod(), inference3SegmentObj.getProNTermPossibleMod(), inference3SegmentObj.getProCTermPossibleMod(), minPtmMass, maxPtmMass, ms1Tolerance, ms1ToleranceUnit, ms2Tolerance);

            // prepare the XCorr spectrum
            PreSpectrum preSpectrumObj = new PreSpectrum(massToolObj);
            SparseVector expXcorrPl = preSpectrumObj.prepareXcorr(spectrumEntry.unprocessedPlMap);

            GeneratePtmCandidatesCalculateXcorr generatePtmCandidatesCalculateXcorr = new GeneratePtmCandidatesCalculateXcorr(spectrumEntry, massToolObj, inference3SegmentObj.getVarModParamSet(), buildIndexObj.returnFixModMap(), ms2Tolerance, maxMs2Charge, expXcorrPl, spectrumEntry.scanNum, spectrumEntry.precursorCharge, spectrumEntry.precursorMz);

            // Generate all candidates based on inferred PTMs and known PTMs. Calculate XCorr
            FinalResultEntry psm = generatePtmCandidatesCalculateXcorr.generateAllPtmCandidatesCalculateXcorr(generatePtmCandidatesCalculateXcorr.eliminateMissedCleavageCausedPtms(searchObj.getPTMFreeResult(), findPtmObj.getPeptidesWithPTMs()));

            // Calculate XCorr for PTM free peptide
            for (Peptide peptide : searchObj.getPTMFreeResult()) {
                CalXcorr.calXcorr(peptide, expXcorrPl, psm, massToolObj, null);
            }

            if (psm.getPeptide() != null) {
                new CalSubscores(psm, spectrumEntry, ms2Tolerance);
                if (!psm.getPeptide().hasVarPTM()) {
                    // The final peptides doesn't have PTM. Recalculate the PTM delta score.
                    psm.setPtmDeltasScore(psm.getScore());
                    psm.setSecondBestPtmPattern("-");
                }
                return psm;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
