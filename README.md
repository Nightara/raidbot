# RaidBot
This project is a Java-based Discord bot based on [Discord4J](https://discord4j.com/) that allows admins to configure
raid parties and open up signups other users can sign in to.

## System Requirements
- Java 25
- Maven 3+
- A database compatible with [jOOQ](https://www.jooq.org/)

## Installation
### Clone the Repository
```bash
git clone https://github.com/Nightara/raidbot
cd raidbot
```
### Set Up the Database
The repository contains an SQL file `setup.sql` that can be executed to set up the database.
Depending on the chosen database, the process to execute the SQL file might look slightly different, but this example
will be assuming a MariaDB database named `raidbot`.

If you are using a different database, make sure to alter the SQL file accordingly.
```bash
mariadb < setup.sql
```
### Configure Database Access
The bot attempts to read the bot and database credentials from the file `properties.xml` in the root directory.
For security reasons, this file is not included in the repository and has to be created manually.
It uses the Java Properties XML format as defined at http://java.sun.com/dtd/properties.dtd.

If the properties `dbType`, `dbHost`, and `dbPort` are not defined, they will default to `mariadb`, `localhost`, and
`3306`, respectively.
```XML
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties>
  <entry key="discordToken"><!-- DISCORD BOT TOKEN --></entry>
  <entry key="dbType"><!-- DATABASE TYPE AS USED IN JDBC CONNECTION STRING --></entry>
  <entry key="dbHost"><!-- DATABASE HOST URL --></entry>
  <entry key="dbPort"><!-- DATABASE PORT --></entry>
  <entry key="dbUser"><!-- DATABASE USER --></entry>
  <entry key="dbPass"><!-- DATABASE PASSWORD --></entry>
  <entry key="dbName"><!-- DATABASE NAME --></entry>
</properties>
```
To enable jOOQ's automatic code generation, jOOQ has to be configured separately using the file `jooqConfig.xml` in the
root directory.
Similar to the property file, this file has been excluded from the repository for security reasons, but a minimal
configuration would look like this (For more information, please refer to the
[jOOQ Code Generator Guide](https://www.jooq.org/doc/latest/manual/code-generation/codegen-execution/codegen-maven/)
and the [JDBC Tutorial](https://docs.oracle.com/javase/tutorial/jdbc/basics/connecting.html)):
```XML
<configuration>
  <jdbc>
    <driver><!-- JDBC DRIVER --></driver>
    <url><!-- JDBC CONNECTION STRING --></url>
    <user><!-- DATABASE USER --></user>
    <password><!-- DATABSE PASSWORD --> </password>
  </jdbc>

  <generator>
    <database>
      <name><!-- JOOQ DATABASE TYPE IDENTIFIER --></name>
      <inputSchema><!-- DATABASE NAME --></inputSchema>
    </database>
    <target>
      <packageName>de.nightara.discord.raidbot.model</packageName>
      <directory>src/main/java</directory>
    </target>
  </generator>
</configuration>
```
### jOOQ Codegen
During the `generate-sources` step (Which is usually performed by Maven as part of the `compile` command), jOOQ will
connect to the supplied database and generate its database model based on the layout of the database.
In this process, the entire `de.nightara.discord.raidbot.model` package will be wiped, including any external edits to
this code.
Due to this, it is highly recommended to not place any database utility code inside the `model` directory.

As of now, jOOQ codegen is entirely optional and can be removed from the Maven configuration if preferred, since the
entire database model has been committed to the repository, but this might change in a future update.

## Running the Application
The preferred method of running the application is by executing the Maven goal `exec:java`, but in a future update, the
project will be able to be run as a standalone JAR file.