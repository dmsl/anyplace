### Logcat filter
Put the following component in (.idea/workspace.xml)[.idea/workspace.xml] to filter logs
```xml
  <component name="AndroidConfiguredLogFilters">
    <filters>
      <filter>
        <option name="logLevel" value="verbose" />
        <option name="logMessagePattern" value="" />
        <option name="logTagPattern" value="^(?!.*(Adreno|artvv|libEGL))" />
        <option name="name" value="ap-filter" />
        <option name="packageNamePattern" value="cy.ac.ucy.cs.anyplace" />
      </filter>
    </filters>
  </component>
```
