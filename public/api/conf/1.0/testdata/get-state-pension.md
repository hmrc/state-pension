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
            <td><code>HT009413A</code></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who is predicted to receive less than the full rate</p></td>
            <td><code>BS793513C</code></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <th colspan="3">Personal maximum</th>
        </tr>
        <tr>
            <td><p>Taxpayer who is predicted to receive the full rate by paying gaps</p></td>
            <td><code>AR822514A</code></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who is predicted to receive less than the full rate even with paying gaps</p></td>
            <td><code>ER872414B</code></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who is predicted to receive more than the full rate and can fill gaps</p></td>
            <td><code>AA252813D</code></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <th colspan="3">Reached</th>
        </tr>
        <tr>
            <td><p>Taxpayer who will receive the full rate</p></td>
            <td><code>CL928713A</code></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who will receive less than the full rate</p></td>
            <td><code>BS793713C</code></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who will receive more than the full rate</p></td>
            <td><code>YN315615A</code></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <th colspan="3">Less than 10 qualifying years</th>
        </tr>
        <tr>
            <td><p>Taxpayer who has many years to contribute and will get the full rate</p></td>
            <td><code>ZT860915B</code></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has many years to contribute and will get less than the full rate</p></td>
            <td><code>AB216913B</code></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who only has gaps which may get them some pension</p></td>
            <td><code>ZX000060A</code></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who can not get any pension</p></td>
            <td><code>AA000113A</code></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <th colspan="3">More variants</th>
        </tr>
        <tr>
            <td><p>Taxpayer who has Contracted Out Pension Equivalent (COPE)</p></td>
            <td><code>ZX000059A</code></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has a pension sharing on divorce (PSOD)</p></td>
            <td><code>ZX000019A</code></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has the Married Women's Reduced Rate Election flag</p></td>
            <td><code>EA791213A</code></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <th colspan="3">State Pension age</th>
        </tr>
        <tr>
            <td><p>Taxpayer who is affected by proposed change to State Pension age</p></td>
            <td><code>ZT860915A</code></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who is not affected by proposed change to State Pension age</p></td>
            <td><code>AR822514A</code></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>                
         <tr>
             <td><p>Taxpayer who is affected by proposed change to State Pension age with COPE</p></td>
             <td><code>ZX000063A</code></td>
             <td><p>200 (OK) Payload as regular example above</p></td>
         </tr>
         <tr>
             <td><p>Taxpayer who is not affected by proposed change to State Pension age with COPE</p></td>
             <td><code>ZX000059A</code></td>
             <td><p>200 (OK) Payload as regular example above</p></td>
         </tr>  
        <tr>
            <th colspan="3">State Pension age exclusion</th>
        </tr>
        <tr>
            <td><p>Taxpayer who has amount dissonance and is affected by proposed change to State Pension age</p></td>
            <td><code>ZC974335B</code></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has amount dissonance and is not affected by proposed change to State Pension age</p></td>
            <td><code>ZC974325B</code></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>                
         <tr>
             <td><p>Taxpayer who has Isle of Man liability and is affected by proposed change to State Pension age</p></td>
             <td><code>MA000004A</code></td>
             <td><p>200 (OK) Payload as exclusion example above</p></td>
         </tr>
         <tr>
             <td><p>Taxpayer who has Isle of Man liability and is not affected by proposed change to State Pension age</p></td>
             <td><code>MA000002A</code></td>
             <td><p>200 (OK) Payload as exclusion example above</p></td>
         </tr>                          
        <tr>
            <th colspan="3">Exclusions</th>
        </tr>
        <tr>
            <td><p>Taxpayer who has amount dissonance</p></td>
            <td><code>ZC974325B</code></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who is post State Pension age</p></td>
            <td><code>MS326113B</code></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has Isle of Man liability</p></td>
            <td><code>MA000002A</code></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has the manual correspondence indicator flag</p></td>
            <td><code>ST281614D</code></td>
            <td><p>403 (Forbidden) {"code": "EXCLUSION_MANUAL_CORRESPONDENCE"}</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has a date of death</p></td>
            <td><code>ZX000056A</code></td>
            <td><p>403 (Forbidden) {"code": "EXCLUSION_DEAD"}</p></td>
        </tr>
    </tbody>
</table>
