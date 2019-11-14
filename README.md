# XL CI Migration Tool #

## CI status ##

[![Build Status][xl-ci-tool-travis-image] ][xl-ci-tool-travis-url]
[![Codacy Badge][xl-ci-tool-codacy-image] ][xl-ci-tool-codacy-url]
[![Code Climate][xl-ci-tool-code-climate-image] ][xl-ci-tool-code-climate-url]
[![License: MIT][xl-ci-tool-license-image] ][xl-ci-tool-license-url]
[![Github All Releases][xl-ci-tool-downloads-image] ]()



## Overview ##

XL Release and XL Deploy define configuration items.  These items represent object types implemented by the plugin.  As these object types are used, the type name is stored in the repository along with the data appropriate to the type.  This couples repository items to plugins.  If you remove the a plugin or replace it with a similar plugin that has different type names, the applications will complain.  While every attempt is made to not change configuration item type names, sometimes it cannot be helped.  Configuration items may be deprecated or renamed to avoid namespace collisions with new features, etc.  This is where the CI Migration Tool is useful.  It scans the XL Release or XL Deploy repository for CI names and takes some action according to a configuration file.

NOTE: Because the tool relies on user created configuration files, it is essential to thoroughly test migration on a test system before migrating production systems. Always backup the databases.


---

## Requirements ##

* **XL Release 7.5+** or **XL Deploy 7.5+**
* Note: only tested with internal (Derby), MySql and Postgres repositories

---

## Usage Scenarios ##

The CI Migration Tool was developed to handle two types of usage scenarios.

_1. CI Type Names changes (additions, updates, removal) between plugin versions_
In this case, a new version of a plugin will replace an old version of a plugin. The migration tool updates existing entries in the application database and the report database to work with the new plugin.

See: examples/xld_mapping_sample.json, examples/xlr_mapping_sample.json

_2. Customized plugin to coexist with new plugin_

It is not uncommon for customers to modify plugins to perform some specialized action.  Over time these specialized actions may no longer be necessary but the plugin cannot be simply removed or upgraded to a later version due to legacy applications or environments.  In this scenario the migration tool can be used to move the ci type names of existing database entries to a new namespace (along with corresponding changes to the synthetic.xml of the plugin) so the new plugin can be used in parallel.

See: examples/xld_namespace_sample.json

---

## Usage ##

The CI Migration Tool is a standalone java application that performs actions directly upon the repository database and is driven by an external JSON mapping file.  You invoke the tool from the command-line as follows:

```
java -jar xl_ci_tool_exec-1.1.jar -f "/Users/tester/XLR-ALL.json" -i "/xl-release-8.1.0-server/"
```

### Command-line Arguments ###

| Argument  | Value | Required? | Note |
| --------- | ----- | --------- | ---- |
| -f        | path/filename | yes | the full path and file name for the mapping file |
| -i        | path          | yes | The full path to the XL Deploy or XL Release installation directory*
| -pw       | <none>        | no  | if this flag is set, user will be prompted for the database password
| -reportpw | <none>        | no  | if this flag is set, user will be prompted for the report/archive database password
| -preview  | <none>        | no  | if this flag is set, the application will only preview the mapping actions. The database will not be changed.

\* NOTE: The migration tool will load database driver jars dynamically. The tool uses the installation directory to find the lib directory within the XL installation and loads all jars from there. The tool also uses the installation directory to find embedded databases in the case where external database have not been configured. If external databases have been configured, the tool will inspect the XLR or XLD configuration file (xl-deploy.conf or xl-release.conf) to find the external databases.

\* NOTE: If databases have been configured with passwords, the user of the migration tool can be prompted to supply database passwords with the -pw (main repository) and -reportpw (archive repository) flags. The command line entry of database passwords is masked. The tool does not use the password information stored in the xl-deploy.conf or xl-release.conf files.

### Steps ###

1. Create a mapping file that accomplishes the CI Type name changes you need for the target plugin.
2. Shut down the target system (either XL Release or XL Deploy).
3. Run the migration tool in test mode:
    java -jar xl_ci_tool_exec-<version>.jar -f </path/mapping_file.json> -i </path/target_home> -preview

Verify the results are as you expect.  Adjust the mappign file as needed.  When you are ready to make the migration do the following:

1. Make a backup of the repository database.
2. Run the migration tool:
    java -jar xl_ci_tool_exec-<version>.jar -f </path/mapping_file.json> -i </path/target_home>
3. Delete the old plugin
4. Copy in the new plugin
5. Start the target system
6. Navigate to items that represent the mapped types to verify no errors.



---

## Mapping File ##

The migration mapping file is essentially a list of actions to be carried out on the repository.  These actions internally map to sql statements or code methods that carry out the process.  A sample configuration file is shown here:

```
{
    "systemType": "xld",
    "description": "example migration script for customized OpenShift plugin to avoid collision with latest official openshift plugin",
    "actions": [
        {
            "order": 1,
            "action": "update",
            "type": "ci",
            "description": "migrate existing type to new type to avoid collision with v8",
            "properties": {
                "oldValue": "openshift.Server",
                "newValue": "openshift6.Server"
            }
        }
    ]
}
```

### Mapping Properties ###

**Top-Level Properties**

| Property | Value | Note |
| -------- | ----- | ---- |
| systemType | xld or xlr | required |
| description |  describe the purpose of the mapping configuration | optional |
| actions | list of json objects that represent actions to carry out for the migration | required |

**Action Object**

| Property | Value | Note |
| -------- | ----- | ---- |
| order | number | order in which the actions are to be carried out, required |
| type | ci \| ci_property \| task | the things the action applies to |
| action | varies per type as below | the action to carry out, required |
|        | _for type = ci_ |
|        | copy   | copy a ci and its properties to a new ci |
|        | update | update an existing ci name |
|        | delete | remove a ci and its properties |
|        | _for type = ci_property_ |
|        | create | create a new property for a ci |
|        | update | update the name of an existing ci property |
|        | delete | remove a ci property |
|        | _for type = task_ | XL Release only |
|        | update | update the name of an existing ci property |
| description | describe the action | optional |
| properties  | json object of action specific properties | required |

**Action Properties Object**

| Type        | Action   | Property      | Value   | Note |
| ----------- | -------- | ------------- | ------- | ---- |
| ci          | update   | oldValue      | string  | name of ci you want to update |
|             |          | newValue      | string  | new name of ci |
|             | copy     | newNameSuffix | string  | name namespace for the ci (left of last period)|
|             |          | oldValue      | string  | name of ci to copy |
|             |          | newValue      | string  | new name of ci (may be same as old name |
|             | delete   | ciName        | string  | name of ci to delete |
| ci_property | update   | ciName        | string  | name of ci this property belongs to |
|             |          | oldValue      | string  | name of the property to update |
|             |          | newValue      | string  | new name of property |
|             | create   | ciName        | string  | name of ci that gets new property |
|             |          | propertyName  | string  | name of new property |
|             |          | _one of the following_  |
|             |          | string_value  | string  | property value as string |
|             |          | boolean_value | boolean | e.g. true or false |
|             |          | integer_value | integer | integer number |
|             |          | date_value    | date    | ISO date value |
|             | delete   | ciName        | string  | name of ci that has property |
|             |          | propertyName  | string  | name of property to delete |
| task        | update   | taskParentType | string | generally 'pythonScript' |
|             |          | oldValue       | string |  |
|             |          | newValue       | string |  |

The most common operation will be 'update' on ci types.  This changes the name of a ci from 'oldValue' to 'newValue'.  Less common is where properties change names.

---

## Development ##

Build and package the code with:

```
.\gradlew fatJar
```

To run tests:

```
.\gradlew test
```

[xl-ci-tool-travis-image]: https://travis-ci.org/xebialabs-community/xl-ci-tool.svg?branch=master
[xl-ci-tool-travis-url]: https://travis-ci.org/xebialabs-community/xl-ci-tool

[xl-ci-tool-codacy-image]: https://api.codacy.com/project/badge/Grade/d85d740ba9124d1e8fba24b2df376d13
[xl-ci-tool-codacy-url]: https://www.codacy.com/app/ladamato/xl-ci-tool

[xl-ci-tool-code-climate-image]: https://codeclimate.com/github/xebialabs-community/xl-ci-tool/badges/gpa.svg
[xl-ci-tool-code-climate-url]: https://codeclimate.com/github/xebialabs-community/xl-ci-tool/maintainability

[xl-ci-tool-license-image]: https://img.shields.io/badge/License-MIT-yellow.svg
[xl-ci-tool-license-url]: https://opensource.org/licenses/MIT

[xl-ci-tool-downloads-image]: https://img.shields.io/github/downloads/xebialabs-community/xl-ci-tool/total.svg
