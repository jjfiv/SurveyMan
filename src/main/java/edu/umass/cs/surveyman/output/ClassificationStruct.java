package edu.umass.cs.surveyman.output;

import edu.umass.cs.surveyman.analyses.KnownValidityStatus;
import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.classifiers.AbstractClassifier;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

public class ClassificationStruct {

    public final SurveyResponse surveyResponse;
    public final AbstractClassifier classifier;
    public final int numanswered;
    public final double score;
    public final double threshold;
    private final boolean valid;
    protected final String RESPONSEID = "responseid";
    protected final String CLASSIFIER = "classifier";
    protected final String NUMANSWERED = "numanswered";
    protected final String SCORE = "score";
    protected final String THRESHOLD = "threshold";
    protected final String VALID = "valid";

    public ClassificationStruct(SurveyResponse surveyResponse, AbstractClassifier classifier) throws SurveyException {
        this.surveyResponse = surveyResponse;
        this.classifier = classifier;
        this.numanswered = surveyResponse.getNonCustomResponses().size();
        this.score = surveyResponse.getScore();
        this.threshold = surveyResponse.getThreshold();
        this.valid = classifier.classifyResponse(surveyResponse);
    }

    public boolean isValid() {
        return this.valid;
    }

    @Override
    public String toString()
    {
        return String.format("%s, %s, %d, %f, %f, %b",
                this.surveyResponse.getSrid(),
                this.classifier.getClass().getName(),
                this.numanswered,
                this.score,
                this.threshold,
                this.valid);
    }

    public String jsonize()
    {
        return String.format(
                "{\"%s\" : \"%s\", " +
                        "\"%s\" : \"%s\", " +
                        "\"%s\" : %d," +
                        "\"%s\" : %f, " +
                        "\"%s\" : %f, " +
                        "\"%s\" : %b}",
                this.RESPONSEID, this.surveyResponse.getSrid(),
                this.CLASSIFIER, this.classifier.getClass().getName(),
                this.NUMANSWERED, this.numanswered,
                this.SCORE, this.score,
                this.THRESHOLD, this.threshold,
                this.VALID, this.valid);
    }
}
