<table>
    <thead>
        <tr>
            <th>Scenario</th>
            <th>NINO Prefix</th>
            <th>Response</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td><p>Regular taxpayer</p></td>
            <td><p>(Any not prefixed as below)</p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer with the PostStatePensionAge exclusion</p></td>
            <td><p><code>MS326113B</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer with the AmountDissonance exclusion</p></td>
            <td><p><code>ZC974325B</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer with the Abroad exclusion</p></td>
            <td><p><code>RB313715C</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer with the IsleOfMan exclusion</p></td>
            <td><p><code>MA000002A</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer with the MarriedWomenReducedRateElection exclusion</p></td>
            <td><p><code>EA791213A</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer that is excluded for multiple reasons</p></td>
            <td><p><code>AR237613D</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer with COPE</p></td>
            <td><p><code>ZX000059A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer with a protected payment</p></td>
            <td><p><code>AA055121B</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer below the minimum qualifying period who cannot get any State Pension even by working until State Pension age and filling payable gaps</p></td>
            <td><p><code>AA000113A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer below the minimum qualifying period who can get some State Pension by working until State Pension age and also filling payable gaps</p></td>
            <td><p><code>ZX000060A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer below the minimum qualifying period that can get some State Pension by just working unitl State Pension age, but can get more by filling payable gaps as well</p></td>
            <td><p><code>AB216913B</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who will not reach the full rate by working until State Pension age, but can make up the difference by filling a gap</p></td>
            <td><p><code>ZT860915B</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who will reach the maximum by continuing to work, they will reach the maximum before their State Pension age</p></td>
            <td><p><code>AR822514A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has already reached the full State Pension rate</p></td>
            <td><p><code>CL928713A</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer with a date of death</p></td>
            <td><p><code>ZX000056A</code></p></td>
            <td><p>403 (Forbidden) {"code": "EXCLUSION_DEAD"}</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has manual correspondence only and cannot use the service</p></td>
            <td><p><code>ST281614D</code></p></td>
            <td><p>403 (Forbidden) {"code": "EXCLUSION_MANUAL_CORRESPONDENCE"}</p></td>
        </tr>
    </trs>
</table>
