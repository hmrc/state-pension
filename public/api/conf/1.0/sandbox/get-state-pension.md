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
            <td><p><code>HT009413A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who is predicted to receive less than the full rate</p></td>
            <td><p><code>BS793513C</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <th colspan="3">Personal maximum</th>
        </tr>
        <tr>
            <td><p>Taxpayer who is predicted to receive the full rate by paying gaps</p></td>
            <td><p><code>AR822514A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who is predicted to receive less than the full rate even with paying gaps</p></td>
            <td><p><code>ER872414B</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who is predicted to receive more than the full rate and can fill gaps</p></td>
            <td><p><code>AA252813D</code></p></td>
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
            <td><p><code>BS793713C</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who will receive more than the full rate</p></td>
            <td><p><code>YN315615A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <th colspan="3">Less than 10 qualifying years</th>
        </tr>
        <tr>
            <td><p>Taxpayer who has many years to contribute and will get the full rate</p></td>
            <td><p><code>ZT860915B</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has many years to contribute and will get less than the full rate</p></td>
            <td><p><code>AB216913B</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who only has gaps which may get them some pension</p></td>
            <td><p><code>ZX000060A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who can not get any pension</p></td>
            <td><p><code>AA000113A</code></p></td>
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
            <th colspan="3">Exclusions</th>
        </tr>
        <tr>
            <td><p>Taxpayer who has amount dissonance</p></td>
            <td><p><code>ZC974325B</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who is post State Pension age</p></td>
            <td><p><code>MS326113B</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has overseas male auto credits (also known as "abroad" user)</p></td>
            <td><p><code>RB313715C</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has Isle of Man liability</p></td>
            <td><p><code>MA000002A</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has the Married Women's Reduced Rate Election flag</p></td>
            <td><p><code>EA791213A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has the manual correspondence indicator flag</p></td>
            <td><p><code>ST281614D</code></p></td>
            <td><p>403 (Forbidden) {"code": "EXCLUSION_MANUAL_CORRESPONDENCE"}</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has a date of death</p></td>
            <td><p><code>ZX000056A</code></p></td>
            <td><p>403 (Forbidden) {"code": "EXCLUSION_DEAD"}</p></td>
        </tr>
    </trs>
</table>
