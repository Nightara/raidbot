package de.nightara.discord.raidbot;

import de.nightara.discord.raidbot.model.tables.*;
import de.nightara.discord.raidbot.model.tables.records.*;
import org.jooq.*;
import org.jooq.impl.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

import static de.nightara.discord.raidbot.model.Raidbot.*;

public class Launcher
{
  static void main(String[] args)
  {
    try(InputStream is = Files.newInputStream(Path.of("properties.xml")))
    {
      Properties props =  new Properties();
      props.loadFromXML(is);

      String token   = props.getProperty("token");
      String dbHost  = props.getProperty("dbHost","localhost");
      String dbPort  = props.getProperty("dbPort","3306");
      String dbName  = props.getProperty("dbName");
      String dbUser  = props.getProperty("dbUser");
      String dbPass  =  props.getProperty("dbPass");

      DSLContext dsl = DSL.using("jdbc:mariadb://" + dbHost + ":" + dbPort + "/" + dbName, dbUser, dbPass);

      WingRecord w1 = dsl.selectFrom(RAIDBOT.WING)
          .where(RAIDBOT.WING.SHORT_NAME.eq("W1"))
          .fetchAny();

      if(w1 != null)
      {
        System.out.println(w1);
      }
      else
      {
        w1 = new WingRecord("W1","Spirit Vale");
        dsl.insertInto(RAIDBOT.WING)
            .set(w1)
            .execute();
      }

      BossRecord vg = dsl.selectFrom(RAIDBOT.BOSS)
          .where(RAIDBOT.BOSS.SHORT_NAME.eq("VG"))
          .fetchAny();

      if(vg != null)
      {
        System.out.println(vg);
      }
      else
      {
        vg = new BossRecord("VG", w1.getShortName(), "Vale Guardian",null);
        dsl.insertInto(RAIDBOT.BOSS)
            .set(vg)
            .execute();
      }

      BossRecord gorse = dsl.selectFrom(RAIDBOT.BOSS)
          .where(RAIDBOT.BOSS.SHORT_NAME.eq("Gorse"))
          .fetchAny();

      if(gorse != null)
      {
        System.out.println(gorse);
      }
      else
      {
        gorse = new BossRecord("Gorse", w1.getShortName(), "Gorseval the Multifarious", vg.getShortName());
        dsl.insertInto(RAIDBOT.BOSS)
            .set(gorse)
            .execute();
      }
    }
    catch(IOException ex)
    {
      ex.printStackTrace();
      System.out.println("Unknown error while loading properties.");
    }
  }
}
