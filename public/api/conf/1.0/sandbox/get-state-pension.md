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
            <td><p><code>PS</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer with the AmountDissonance exclusion</p></td>
            <td><p><code>AM</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer with the Abroad exclusion</p></td>
            <td><p><code>AB</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer with the IsleOfMan exclusion</p></td>
            <td><p><code>MA</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer with the MarriedWomenReducedRateElection exclusion</p></td>
            <td><p><code>MW</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer that is excluded for multiple reasons</p></td>
            <td><p><code>MX</code></p></td>
            <td><p>200 (OK) Payload as exclusion example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer with COPE</p></td>
            <td><p><code>CR</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer with a protected payment</p></td>
            <td><p><code>PP</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer below the minimum qualifying period who cannot get any State Pension even by working until State Pension age and filling payable gaps</p></td>
            <td><p><code>CA</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer below the minimum qualifying period who can get some State Pension by working until State Pension age and also filling payable gaps</p></td>
            <td><p><code>AL</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer below the minimum qualifying period that can get some State Pension by just working unitl State Pension age, but can get more by filling payable gaps as well</p></td>
            <td><p><code>AE</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who will not reach the full rate by working until State Pension age, but can make up the difference by filling a gap</p></td>
            <td><p><code>AH</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who will reach the maximum by continuing to work, they will reach the maximum before their State Pension age</p></td>
            <td><p><code>AK</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has already reached the full State Pension rate</p></td>
            <td><p><code>AP</code></p></td>
            <td><p>200 (OK) Payload as regular example above</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer with a date of death</p></td>
            <td><p><code>EZ</code></p></td>
            <td><p>403 (Forbidden) {"code": "EXCLUSION_DEAD"}</p></td>
        </tr>
        <tr>
            <td><p>Taxpayer who has manual correspondence only and cannot use the service</p></td>
            <td><p><code>PG</code></p></td>
            <td><p>403 (Forbidden) {"code": "EXCLUSION_MANUAL_CORRESPONDENCE"}</p></td>
        </tr>
    </trs>
</table>