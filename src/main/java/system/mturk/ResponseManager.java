package system.mturk;

import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.util.PropertiesClientConfig;
import survey.Survey;
import system.Library;
import java.util.HashMap;
import qc.QC;
import survey.SurveyException;
import survey.SurveyResponse;


public class ResponseManager {

    public static final String RESULTS = Library.OUTDIR + Library.fileSep + "results.csv";
    public static final String SUCCESS = Library.OUTDIR + Library.fileSep + "success.csv";
    private static final RequesterService service = new RequesterService(new PropertiesClientConfig(Library.CONFIG));

    public static SurveyResponse parseResponse(Assignment assignment, Survey survey) throws SurveyException {
        return new SurveyResponse(survey, assignment);
    }
    
    public static void addResponses(HashMap<String, SurveyResponse> responses, Survey survey, String hitid) throws SurveyException {
        Assignment[] assignments = service.getAllAssignmentsForHIT(hitid);
        for (Assignment a : assignments){
            SurveyResponse sr = parseResponse(a, survey);
            if (QC.isBot(sr)) 
                service.blockWorker(a.getWorkerId(), QC.BOT);
            else {
                //service.assignQualification("survey", a.getWorkerId(), 1, false);
                responses.put(a.getWorkerId(), sr);
                service.forceExpireHIT(hitid);
            }
        }
    }
    
    public static boolean hasResponse(String hittypeid, String hitid){
        for (HIT hit : service.getAllReviewableHITs(hittypeid))
            if (hit.getHITId().equals(hitid))
                return true;
        return false;
    }
    
    public static boolean hasJobs() {
        return service.searchAllHITs().length!=0;
    }
            
    public static void main(String[] args) {
        
    }

}