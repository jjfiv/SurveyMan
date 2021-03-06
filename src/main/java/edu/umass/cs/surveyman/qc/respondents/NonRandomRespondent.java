package edu.umass.cs.surveyman.qc.respondents;

import edu.umass.cs.surveyman.analyses.SurveyResponse;
import edu.umass.cs.surveyman.qc.Interpreter;
import edu.umass.cs.surveyman.survey.Question;
import edu.umass.cs.surveyman.survey.Survey;
import edu.umass.cs.surveyman.survey.SurveyDatum;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simulates a cluster of responses.
 */
public class NonRandomRespondent extends AbstractRespondent {

    private Survey survey;
    protected Map<Question, SurveyDatum> answers = new HashMap<>();
    protected Map<SurveyDatum, Double> strength = new HashMap<>();

    public NonRandomRespondent(Survey survey)  {
        this.survey = survey;
        for (Question q : survey.questions) {
            if (!q.freetext && !q.options.isEmpty()) {
                List<SurveyDatum> possibleAnswers = new ArrayList<SurveyDatum>(q.options.values());
                int index = rng.nextInt(possibleAnswers.size());
                SurveyDatum answer = possibleAnswers.get(index);
                this.answers.put(q, answer);
                double uni = 1.0 / possibleAnswers.size();
                double pref = rng.nextDouble() * (1.0 - uni);
                assert pref < (1 - uni);
                this.strength.put(answer, uni + pref);
            }
        }
        assert answers.size() > 0 : "Answer set for survey " + survey.sourceName + " (" + survey.sid + ")\nhas size 0.";
        assert strength.size() > 0;
        assert answers.size() == strength.size();
    }

    private NonRandomRespondent(NonRandomRespondent nonRandomRespondent) {
        this.survey = nonRandomRespondent.survey;
        this.answers = new HashMap<>(nonRandomRespondent.answers);
        this.strength = new HashMap<>(nonRandomRespondent.strength);
    }

    @Override
    public SurveyResponse getResponse() throws SurveyException {
        Interpreter interpreter = new Interpreter(survey);
        do {
            Question q = interpreter.getNextQuestion();
            SurveyDatum c = answers.get(q);
            List<SurveyDatum> ans = new ArrayList<>();
            // calculate our answer
            if (!q.freetext && q.options.size() > 0 ) {
                double prob = rng.nextDouble();
                double threshold = strength.get(answers.get(q));
                if (prob > threshold) {
                    // uniformly select from the other options
                    List<SurveyDatum> otherAns = new ArrayList<>();
                    for (SurveyDatum cc : q.options.values()) {
                        if (!c.equals(cc))
                            otherAns.add(cc);
                    }
                    ans.add(otherAns.get(rng.nextInt(otherAns.size())));
                } else {
                    ans.add(c);
                }
                interpreter.answer(q, ans);
            }
        } while (!interpreter.terminated());
        return interpreter.getResponse();
    }

    @Override
    public AbstractRespondent copy()
    {
        return new NonRandomRespondent(this);
    }
}
