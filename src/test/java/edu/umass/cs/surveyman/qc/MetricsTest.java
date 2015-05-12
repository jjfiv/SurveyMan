package edu.umass.cs.surveyman.qc;

import edu.umass.cs.surveyman.TestLog;
import edu.umass.cs.surveyman.analyses.*;
import edu.umass.cs.surveyman.input.exceptions.SyntaxException;
import edu.umass.cs.surveyman.survey.*;
import edu.umass.cs.surveyman.survey.exceptions.SurveyException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.*;

@RunWith(JUnit4.class)
public class MetricsTest extends TestLog {

    static class QuestionResponse implements IQuestionResponse {

        Question q;
        List<OptTuple> opts = new ArrayList<>();

        public QuestionResponse(Question q, OptTuple... opts) {
            this.q = q;
            this.opts.addAll(Arrays.asList(opts));
        }

        public int compareTo(Object o) {
            throw new RuntimeException("Not implemented.");
        }

        public Question getQuestion() {
            return q;
        }

        public List<OptTuple> getOpts() {
            return opts;
        }

        public int getIndexSeen() {
            throw new RuntimeException("Not implemented.");
        }

        public SurveyDatum getAnswer() throws SurveyException {
            throw new RuntimeException("Not implemented.");
        }

        public List<SurveyDatum> getAnswers() throws SurveyException {
            throw new RuntimeException("Not implemented.");
        }
    };

    public static Block block1;
    public static Block block2;
    public static Block block3;
    public static Block block4;
    public static Question branchQuestion1;
    public static SurveyDatum a;
    public static SurveyDatum b;
    public static Question branchQuestion2;
    public static SurveyDatum c;
    public static SurveyDatum d;
    public static Question noBranchQuestion1;
    public static Question noBranchQuestion2;
    public static Survey survey;

    public void init() {
        block1 = new Block("1");
        block2 = new Block("2");
        block3 = new Block("3");
        block4 = new Block("4");
        branchQuestion1 = Question.makeQuestion("asdf", 1, 1);
        a = new StringDatum("a", 1, 2);
        b = new StringDatum("b", 2, 2);
        branchQuestion2 = Question.makeQuestion("fdsa", 3, 1);
        c = new StringDatum("c", 3, 1);
        d = new StringDatum("d", 4, 1);
        noBranchQuestion1 = Question.makeQuestion("foo", 5, 1);
        noBranchQuestion2 = Question.makeQuestion("bar", 6, 1);
        survey = new Survey();
        try {
            branchQuestion1.addOption(a, block2);
            branchQuestion1.addOption(b, block4);
            block1.addBranchQuestion(branchQuestion1);
            branchQuestion2.addOption(c, block3);
            branchQuestion2.addOption(d, block4);
            block2.addBranchQuestion(branchQuestion2);
            block3.addQuestion(noBranchQuestion1);
            block4.addQuestion(noBranchQuestion2);
            survey.addBlock(block1);
            survey.addBlock(block2);
            survey.addBlock(block3);
            survey.addBlock(block4);
            StaticAnalysis.wellFormednessChecks(survey);
        } catch (SurveyException e) {
            e.printStackTrace();
        }
    }

    public MetricsTest()
            throws IOException, SyntaxException {
        super.init(this.getClass());
        this.init();
    }

    @Test
    public void testGetDag() {

        init();

        List<List<Block>> answerDag = new ArrayList<List<Block>>();

        List<Block> path1 = new ArrayList<Block>();
        path1.add(block1);
        path1.add(block2);
        path1.add(block4);
        answerDag.add(path1);

        List<Block> path2 = new ArrayList<Block>();
        path2.add(block1);
        path2.add(block2);
        path2.add(block3);
        path2.add(block4);

        List<Block> path3 = new ArrayList<Block>();
        path3.add(block1);
        path3.add(block4);

        List<Block> blockList = new ArrayList<Block>();
        blockList.add(block1);
        blockList.add(block2);
        blockList.add(block3);
        blockList.add(block4);

        List<List<Block>> computedDag1 = QCMetrics.getDag(blockList);
        List<List<Block>> computedDag2 = QCMetrics.getDag(survey.topLevelBlocks);

        assert computedDag1.size() == 3 : "Expected path length of 3; got " + computedDag1.size();
        assert computedDag2.size() == 3 : "Expected path length of 3; got " + computedDag2.size();
        // TODO(etosch): show paths in dags are equivalent
    }

    @Test
    public void testGetQuestions() {
        Assert.assertEquals(survey.topLevelBlocks.size(), 4);
        int numQuestions = QCMetrics.getQuestions(survey.topLevelBlocks).size();
        Assert.assertEquals("Expected 4 questions; got "+numQuestions, 4, numQuestions);
    }

    @Test
    public void testGetPaths() {
        init();
        int numpaths = QCMetrics.getPaths(survey).size();
        Assert.assertEquals(3, numpaths);
    }

    @Test
    public void testMinPath() {
        init();
        int minPathLength = QCMetrics.minimumPathLength(survey);
        Assert.assertEquals(2, minPathLength);
        //TODO(etosch): test more survey instances
    }

    @Test
    public void testMaxPath() {
        init();
        Assert.assertEquals(4, QCMetrics.maximumPathLength(survey));
        //TODO(etosch): test more survey instances
    }

    @Test
    public void testTruncateResponses() {
        //TODO(etosch): write this
    }

    @Test
    public void testRemoveFreetext()
            throws SurveyException {
        init();
        Question freetext = new Question("asdf");
        freetext.freetext = true;
        survey.addQuestion(freetext);
        int fullSize = survey.questions.size();
        int sizeWithoutFreetext = QCMetrics.removeFreetext(survey.questions).size();
        Assert.assertEquals(5, fullSize);
        Assert.assertEquals(4, sizeWithoutFreetext);
    }

    @Test
    public void testMakeFrequenciesForPaths()
            throws SurveyException {
        init();
        List<List<Block>> paths = QCMetrics.getPaths(survey);
        Assert.assertEquals("There should be 3 paths through the survey.", 3, paths.size());
        List<SurveyResponse> responses = new ArrayList<SurveyResponse>();
        AbstractRespondent r = new RandomRespondent(survey, RandomRespondent.AdversaryType.FIRST);
        responses.add(r.getResponse());
        Map<List<Block>, List<SurveyResponse>> pathMap = QCMetrics.makeFrequenciesForPaths(paths, responses);
        Assert.assertEquals("There should be 3 unique paths key.", 3, pathMap.keySet().size());
        int totalRespondents = 0;
        for (List<SurveyResponse> sr : pathMap.values())
            totalRespondents += sr.size();
        Assert.assertEquals("Expecting 1 response total.", 1, totalRespondents);
        // add another response
        responses.add(r.getResponse());
        pathMap = QCMetrics.makeFrequenciesForPaths(paths, responses);
        Assert.assertEquals("There should be 3 unique paths key.", 3, pathMap.keySet().size());
        totalRespondents = 0;
        for (List<SurveyResponse> sr : pathMap.values())
            totalRespondents += sr.size();
        Assert.assertEquals("Expecting 2 responses total.", 2, totalRespondents);
    }

    @Test
    public void getEquivalentAnswerVariants()
            throws SurveyException {
        init();
        Block b = new Block("1");
        Question q1 = new Question("sadf");
        Question q2 = new Question("fdsa");
        SurveyDatum c1 = new StringDatum("a", 1, 2);
        q1.addOption(c1);
        q1.addOptions("b", "c");
        q2.addOptions("d", "e", "f");
        b.addQuestion(q1);
        b.addQuestion(q2);
        List<SurveyDatum> variants = QCMetrics.getEquivalentAnswerVariants(q1, c1);
        Assert.assertEquals("This variant set should be size 1.", 1, variants.size());
        b.branchParadigm = Block.BranchParadigm.ALL;
        b.propagateBranchParadigm();
        variants = QCMetrics.getEquivalentAnswerVariants(q1, c1);
        Assert.assertEquals("This variant set should be size 2.", 2, variants.size());
    }

    @Test
    public void testSurveyEntropy()
            throws SurveyException {
        init();
        Question q1 = new RadioButtonQuestion("asdf", true);
        Question q2 = new RadioButtonQuestion("fdsa", true);
        q1.randomize = false;
        q2.randomize = false;
        q1.addOption("A1");
        q1.addOption("B1");
        q2.addOption("A2");
        q2.addOption("B2");
        Survey survey1 = new Survey();
        survey1.addQuestions(q1, q2);
        // make two survey responses
        AbstractRespondent rr1 = new RandomRespondent(survey1, RandomRespondent.AdversaryType.FIRST);
        AbstractRespondent rr2 = new RandomRespondent(survey1, RandomRespondent.AdversaryType.LAST);
        List<SurveyResponse> srs = new ArrayList<SurveyResponse>();
        srs.add(rr1.getResponse());
        srs.add(rr2.getResponse());
        double expectedEntropy = 2.0;
        double observedEntropy = QCMetrics.surveyEntropy(survey1, srs);
        Assert.assertEquals(expectedEntropy, observedEntropy, 0.001);
    }

    @Test
    public void testSpearmansRank()
            throws SurveyException {
        init();
        final Question q1 = new RadioButtonQuestion("asdf", true);
        final Question q2 = new RadioButtonQuestion("fdsa", true);
        final SurveyDatum c1 = new StringDatum("a");
        final SurveyDatum c2 = new StringDatum("d");
        q1.addOption(c1);
        q1.addOptions("b", "c");
        q2.addOption(c2);
        q2.addOptions("e", "f");
        Map<String, IQuestionResponse> ansMap1 = new HashMap<String, IQuestionResponse>();
        Map<String, IQuestionResponse> ansMap2 = new HashMap<String, IQuestionResponse>();
        QuestionResponse qr1 = new QuestionResponse(q1, new OptTuple(c1, 0));
        QuestionResponse qr2 = new QuestionResponse(q2, new OptTuple(c2, 0));
        ansMap1.put("a", qr1);
        ansMap1.put("b", qr1);
        ansMap2.put("a", qr2);
        ansMap2.put("b", qr2);
        double rho = QCMetrics.spearmansRho(ansMap1, ansMap2);
        Assert.assertEquals("Rho should be 1", 1, rho, 0.001);
    }

    @Test
    public void testCramersV()
            throws SurveyException {
        init();
        final Question q1 = new RadioButtonQuestion("asdf", true);
        final Question q2 = new RadioButtonQuestion("fdsa", true);
        SurveyDatum c11 = new StringDatum("a");
        SurveyDatum c12 = new StringDatum("b");
        SurveyDatum c21 = new StringDatum("c");
        SurveyDatum c22 = new StringDatum("d");
        q1.addOption(c11);
        q1.addOption(c12);
        q2.addOption(c21);
        q2.addOption(c22);
        Map<String, IQuestionResponse> ansMap1 = new HashMap<>();
        Map<String, IQuestionResponse> ansMap2 = new HashMap<>();
        QuestionResponse qr1 = new QuestionResponse(q1, new OptTuple(c11, 0));
        QuestionResponse qr2 = new QuestionResponse(q1, new OptTuple(c12, 1));
        QuestionResponse qr3 = new QuestionResponse(q2, new OptTuple(c21, 0));
        QuestionResponse qr4 = new QuestionResponse(q2, new OptTuple(c22, 1));
        ansMap1.put("a", qr1);
        ansMap2.put("a", qr3);
        ansMap1.put("b", qr1);
        ansMap2.put("b", qr3);
        ansMap1.put("c", qr2);
        ansMap2.put("c", qr4);
        double v = QCMetrics.cramersV(ansMap1, ansMap2);
        Assert.assertEquals("V should be 1", 1, v, 0.001);
    }

    @Test
    public void testNonRandomRespondentFrequencies() {
//        AbstractRespondent profile = new NonRandomRespondent(survey);
//        List<AbstractSurveyResponse> responses = new ArrayList<AbstractSurveyResponse>();
//        for (int i = 0 ; i < 10 ; i++) {
//            responses.add(profile.getResponse());
//        }
        // none of the respondent profiles should be identical.
        init();
    }

}
