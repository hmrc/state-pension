Get-cope-case
-----------------------
Returns the Cope Case based on the NINO.

* **URL**

  `/cope/:nino`

* **Method**

  `GET`
  
* **URL Parameter**

  `nino`  
  
* **Success Response:**

  * **Code:** 403 <br />

* **Example Success Response**

```json
{
  "code":"EXCLUSION_COPE_PROCESSING",
  "copeDataAvailableDate":"2022-11-21",
  "previousAvailableDate":"2022-11-21"
}
```

* **Error Response:**
                        
  * **Code:** 403 FORBIDDEN <br />
    **Content:** `{
                            "code":"EXCLUSION_COPE_PROCESSING_FAILED"
                        }`
                        
  * **Code:** 404 NOT FOUND <br />
    **Content:** `User is not a cope case`

  * **Code:** 4XX Upstream4xxResponse <br />

  OR anything else

  * **Code:** 5XX Upstream5xxResponse <br />