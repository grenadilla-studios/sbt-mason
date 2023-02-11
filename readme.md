# sbt-mason
This sbt plugin attempts to provide an easy to use interface with the Databricks 2.0 rest API for
managing artifacts. 

## Configuration settings
* `masonClusterId` - the Databricks cluster ID of the cluster for which you are attempting to manage
libraries.
* `masonConfigFile` - if your Databricks API key and URL config is stored somewhere other than
`$HOME/.databrickscfg` you need to set this to that path.
* `masonEnvironmentName` - if you would like to use an API key or URL for an environment other than
DEFAULT defined in your config file, set that environment name here.
* `masonLibraryName` - the name of the project itself is used by default to identify the
appropriate published artifacts to use. It is recommended you _not_ set this unless you see error
messages asking you to do so.
## Currently Available Tasks
* `masonPublishLibrary` - this task looks for a published JAR matching `databricksLibraryName` and
then 1) uploads that artifact to Databricks file storage, 2) adds that uploaded library to the
cluster specified with `masonClusterId` 3) restarts that cluster if it is running to make sure the
updated library takes effect.
* `masonRemoveLibrary` - this task looks for libraries installed on the `masonClusterId` cluster and
removes them if they match `masonLibraryName`.

### Contributors
All changes merged to `main` are automatically released to Maven central as snapshot releases. In
order to make a full release, take the following actions:
* check out the latest changes on the `main` branch
* tag the branch with the next appropriate release version and push the tag
  
  ```
  git tag -a vX.Y.Z -m "vX.Y.Z"
  git push origin vX.YZ
  ```
  
* go to the releases page and publish the draft release notes
