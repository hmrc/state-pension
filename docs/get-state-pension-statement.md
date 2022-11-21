Get-state-pension-statement
-----------------------
Returns the State Pension Statement for a given NINO.

* **URL**

  `/ni/:nino`

* **Method**

  `GET`
  
* **URL Parameter**

  `nino`  
  
* **Success Response:**

  * **Code:** 200 <br />

* **Example Success Response**

```json
{
  "earningsIncludedUpTo": "2015-04-05",
  "amounts": {
    "protectedPayment": false,
    "current": {
      "weeklyAmount": 133.41,
      "monthlyAmount": 580.1,
      "annualAmount": 6961.14
    },
    "forecast": {
      "yearsToWork": 3,
      "weeklyAmount": 146.76,
      "monthlyAmount": 638.14,
      "annualAmount": 7657.73
    },
    "maximum": {
      "yearsToWork": 3,
      "gapsToFill": 2,
      "weeklyAmount": 155.65,
      "monthlyAmount": 676.8,
      "annualAmount": 8121.59
    },
    "cope": {
      "weeklyAmount": 0,
      "monthlyAmount": 0,
      "annualAmount": 0
    },
    "starting": {
      "weeklyAmount": 130.05,
      "monthlyAmount": 565.49,
      "annualAmount": 6785.82
    },
    "oldRules": {
      "basicStatePension": 119.30,
      "additionalStatePension": 10.75,
      "graduatedRetirementBenefit": 0
    },
    "newRules": {
      "grossStatePension": 130.05,
      "rebateDerivedAmount": 0
    }
  },
  "pensionAge": 64,
  "pensionDate": "2018-07-06",
  "finalRelevantYear": "2017-18",
  "numberOfQualifyingYears": 30,
  "pensionSharingOrder": false,
  "currentFullWeeklyPensionAmount": 155.65,
  "reducedRateElection": false,
  "statePensionAgeUnderConsideration": false,
  "_links" : {
    "self": {
      "href": "/state-pension/ni/QQ123456A"
    }
  }
}


```

* **Error Response:**

  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `{
                     "code": "ERROR_NINO_INVALID",
                     "reason": "The provided NINO is not valid"
                  }`
                  
  * **Code:** 400 BAD_REQUEST <br />
    **Content:** `{
                     "code": "BAD_REQUEST",
                     "reason": "Bad Request"
                  }`
             
  * **Code:** 401 UNAUTHORIZED <br />
    **Content:** `{
                              "code": "UNAUTHORIZED",
                              "reason": "Bearer token is missing or not authorized"
                          }`
                  
  * **Code:** 403 FORBIDDEN <br />
    **Content:** `{
                            "code": "EXCLUSION_MANUAL_CORRESPONDENCE",
                            "reason": "The customer cannot access the service, they should contact HMRC"
                        }`
                        
  * **Code:** 403 FORBIDDEN <br />
    **Content:** `{
                            "code": "EXCLUSION_COPE_PROCESSING",
                            "reason": "Cope data has an open work item meaning that the customer cannot enter the service and must come back at a later date"
                        }`
                        
  * **Code:** 403 FORBIDDEN <br />
    **Content:** `{
                            "code": "EXCLUSION_DEAD",
                            "reason": "The customer needs to contact the National Insurance helpline"
                        }`
                       
  * **Code:** 403 FORBIDDEN <br />
    **Content:** `{
                            "code": "EXCLUSION_COPE_PROCESSING_FAILED",
                            "reason": "Cope data has a closed work item against it meaning that the customer cannot enter the service and is advised to contact DWP"
                        }`
                        
  * **Code:** 404 NOT FOUND <br />
    **Content:** `{
                            "code": "NOT_FOUND",
                            "reason": "Resource was not found"
                        }`
                                   
  * **Code:** 406 ACCEPT HEADER INVALID <br />
    **Content:** `{
                              "code": "ACCEPT_HEADER_INVALID",
                              "reason": "The accept header is missing or invalid"
                          }`
                          
  * **Code:** 500 INTERNAL SERVER ERROR <br />
    **Content:** `{
                              "code": "INTERNAL_SERVER_ERROR",
                              "reason": "Internal server error"
                          }`

  * **Code:** 4XX Upstream4xxResponse <br />

  OR anything else

  * **Code:** 5XX Upstream5xxResponse <br />