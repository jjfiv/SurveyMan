(ns testPipeline
  (:import Report
    (survey Survey)
           (system Runner)
           (interstitial AbstractResponseManager Record)
           (system.localhost LocalResponseManager LocalLibrary LocalSurveyPoster Server)
           (java.util Properties))
  (:use testLog)
  (:use clojure.test)
  )

(defn get-survey-by-name [name]
  (loop [surveys (keys @response-lookup)]
    (cond (nil? surveys) (throw (Exception. (str "Survey " name " not found")))
          (= name (.source (first surveys))) (first surveys)
          :else (recur (rest surveys)))
    )
  )

(deftest testRunnerDefaults
  (println 'testRunnerDefaults)
  (let [^Survey survey (->> (filter #(= "," (% 1)) tests)
                            (filter #(= true (read-string (% 2))))
                            (shuffle)
                            (ffirst)
                            (get-survey-by-name))
        args (into-array String [(.source survey)])
        runner (agent (Thread. (fn [] (Runner/main args))))]
    (send runner #(.start %))
    (while (not (Runner/library)) (Thread/sleep 2000))
    (is (type Runner/responseManager) LocalResponseManager)
    (is (type Runner/surveyPoster) LocalSurveyPoster)
    (is (type Runner/library) LocalLibrary)
    (try (Server/endServe))
    (shutdown-agents)
    )
  )

(deftest testRunnerWithArgs
  (println 'testRunnerWithArgs)
  (let [^Survey survey (->> (filter #(= "\\t" (% 1)) tests)
                            (filter #(= true (read-string (% 2))))
                            (shuffle)
                            (ffirst)
                            (get-survey-by-name))
        args (into-array String ["--backend=LOCALHOST" "--separator=\\t" "--properties=foo.properties" (.source survey)])
        runner (agent (Thread. (fn [] (Runner/main args))))]
    (spit "foo.properties"
          "numparticipants=2
reward=0.02
assignmentduration=1200
keywords=phonology, survey, research
hitlifetime=86400
autoapprovaldelay=0
title=Your Sample Survey
splashpage=<p><b>Please read the following instructions before beginning.</b></p> <p>This is a sample consent form that you can use as the front matter on an AMT HIT. It might say something like, &quot;If you choose to accept this HIT, you will be participating in a phonological (&quot;fun&quot;-ological) study. We estimate that this takes about 20 minutes to complete. The data collected from this study is for phonological research. There are no known risks or benefits to participation.</p> <p>Since the quality of responses is important, we have permitted you to submit early. At the end of the study, we will disburse bonuses to workers who answered more. This study will appear multiple times on Mechanical Turk.</p> <p><b> Please only answer this survey once, and return the HIT if you have completed this one before.</b></p><p>If you have any questions or concerns, please contact the requester.&quot;</p>
sandbox=true
description=Your description here.")
    (printf "Testing %s..." (.source survey))
    (send runner #(.start %))
    (while (not (AbstractResponseManager/getRecord survey)) (Thread/sleep 2000))
    (is (type Runner/responseManager) LocalResponseManager)
    (is (type Runner/surveyPoster) LocalSurveyPoster)
    (is (type Runner/library) LocalLibrary)
    (let [recordLib (.library (AbstractResponseManager/getRecord survey))]
      (is recordLib Runner/library)
      (is (Integer/parseInt (.getProperty ^Properties (.props recordLib) "numparticipants")) 2)
      (is (Double/parseDouble (.getProperty ^Properties (.props recordLib) "reward")) 0.02)
      )
    )
  (try (Server/endServe))
  (shutdown-agents)
  (clojure.java.io/delete-file "foo.properties")
  )

(deftest testStaticReport
  (println 'testStaticReport)
  (doseq [csv '("data/samples/wage_survey.csv" "data/samples/prototypicality.csv" "data/samples/phonology.csv")]
    (let [args (into-array String ["--report=static" "--separator=," csv])
          reporter (Thread. (fn [] (Report/main args)))]
      (.start reporter)
      (.join reporter)
      )
    )
  )