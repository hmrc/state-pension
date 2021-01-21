<table>
    <thead>
        <tr>
            <th>Scenario</th>
            <th>NINO</th>
            <th>Response</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <th colspan="3">More than 10 qualifying years</th>
        </tr>
        <tr>
            <th colspan="3">Forecast</th>
        </tr>
        <tr>
            <td><p>Taxpayer who is predicted to receive the full rate</p></td>
            <td><p><code>LG000001A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who is predicted to receive less than the full rate</p></td>
            <td><p><code>LG001101A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <th colspan="3">Personal maximum</th>
        </tr>
        <tr>
            <td><p>Taxpayer who is predicted to receive the full rate by paying gaps</p></td>
            <td><p><code>LG000601A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who is predicted to receive less than the full rate even with paying gaps</p></td>
            <td><p><code>ER872414B</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who is predicted to receive more than the full rate and can fill gaps</p></td>
            <td><p><code>LG002301A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <th colspan="3">Reached</th>
        </tr>
        <tr>
            <td><p>Taxpayer who will receive the full rate</p></td>
            <td><p><code>CL928713A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who will receive less than the full rate</p></td>
            <td><p><code>LG007401A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who will receive more than the full rate</p></td>
            <td><p><code>AA002813A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <th colspan="3">Less than 10 qualifying years</th>
        </tr>
        <tr>
            <td><p>Taxpayer who has many years to contribute and will get the full rate</p></td>
            <td><p><code>LG000401A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has many years to contribute and will get less than the full rate</p></td>
            <td><p><code>LG001301A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who only has gaps which may get them some pension</p></td>
            <td><p><code>ZX000060A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who can not get any pension</p></td>
            <td><p><code>LG002801</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <th colspan="3">More variants</th>
        </tr>
        <tr>
            <td><p>Taxpayer who has Contracted Out Pension Equivalent (COPE)</p></td>
            <td><p><code>ZX000059A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has a pension sharing on divorce (PSOD)</p></td>
            <td><p><code>ZX000019A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has the Married Women's Reduced Rate Election flag</p></td>
            <td><p><code>LG007201A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <th colspan="3">State Pension age</th>
        </tr>
        <tr>
            <td><p>Taxpayer who is affected by proposed change to State Pension age</p></td>
            <td><p><code>ZT860915A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who is not affected by proposed change to State Pension age</p></td>
            <td><p><code>LG002201A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>                
         <tr>
             <td><p>Taxpayer who is affected by proposed change to State Pension age with COPE</p></td>
             <td><p><code>ZX000063A</code></p></td>
             <td><p>200 (OK) Payload as regular example above</p></td>
         </tr>
         <tr>
             <td><p>Taxpayer who is not affected by proposed change to State Pension age with COPE</p></td>
             <td><p><code>ZX000059A</code></p></td>
             <td><p>200 (OK) Payload as regular example above</p></td>
         </tr>  
        <tr>
            <th colspan="3">State Pension age exclusion</th>
        </tr>
        <tr>
            <td><p>Taxpayer who has amount dissonance and is affected by proposed change to State Pension age</p></td>
            <td><p><code>ZC974335B</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has amount dissonance and is not affected by proposed change to State Pension age</p></td>
            <td><p><code>ZC974325B</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>                
         <tr>
             <td><p>Taxpayer who has Isle of Man liability and is affected by proposed change to State Pension age</p></td>
             <td><p><code>MA000004A</code></p></td>
             <td><p>200 (OK) Payload as exclusion example above</p></td>
         </tr>
         <tr>
             <td><p>Taxpayer who has Isle of Man liability and is not affected by proposed change to State Pension age</p></td>
             <td><p><code>LG005101A</code></p></td>
             <td><p>200 (OK) Payload as exclusion example above</p></td>
         </tr>                          
        <tr>
            <th colspan="3">Exclusions</th>
        </tr>
        <tr>
            <td><p>Taxpayer who has amount dissonance</p></td>
            <td><p><code>ZC974325B</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who is post State Pension age</p></td>
            <td><p><code>LG005901A</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has Isle of Man liability</p></td>
            <td><p><code>LG005101A</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has the manual correspondence indicator flag</p></td>
            <td><p><code>LG007101A</code></p></td>
            <td><p>403 (Forbidden) {"code": "EXCLUSION_MANUAL_CORRESPONDENCE"}</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has a date of death</p></td>
            <td><p><code>LG005801A</code></p></td>
            <td><p>403 (Forbidden) {"code": "EXCLUSION_DEAD"}</p></td>
        </tr>
    </trs>
</table>
