State Pension
==================================

WIP DESCRIPTION

API (Authenticated endpoints called by API)
---

| *Task*                                                                               | *Supported Methods* | *Description*                                                                                                                                       |
|--------------------------------------------------------------------------------------|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| ```/ni/:nino                ```                                                      | GET                 | Returns the State Pension Statement based on the NINO [More...](docs/get-state-pension-statement.md)                       |
| ```/pd/ni/:nino                 ```                                                  | GET                 | Returns the State Pension Dashboard data based on the NINO [More...](docs/get-dashboard-data.md)                           |
| ```/cope/:nino                                     ```                               | GET                 | Returns the Cope Case based on the NINO [More...](docs/get-cope-case.md) |

MDTP (Pertax Authenticated endpoints called by API)
---

| *Task*                                                      | *Supported Methods* | *Description*                                                                                                                                       |
|-------------------------------------------------------------|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| ```/ni/mdtp/:nino                ```                        | GET                 | Returns the State Pension Statement based on the NINO [More...](docs/get-state-pension-statement.md)                       |
| ```/cope/mdtp/:nino                                     ``` | GET                 | Returns the Cope Case based on the NINO [More...](docs/get-cope-case.md) |