package de.nightara.discord.raidbot;

import de.nightara.discord.raidbot.model.*;
import de.nightara.discord.raidbot.model.tables.records.*;
import discord4j.core.*;
import discord4j.core.event.domain.lifecycle.*;
import discord4j.core.object.entity.*;
import org.jooq.*;
import org.jooq.Record;
import org.jooq.impl.*;
import org.jooq.types.*;
import reactor.core.publisher.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
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

      String discordToken   = props.getProperty("discordToken");
      String dbHost  = props.getProperty("dbHost","localhost");
      String dbPort  = props.getProperty("dbPort","3306");
      String dbName  = props.getProperty("dbName");
      String dbUser  = props.getProperty("dbUser");
      String dbPass  =  props.getProperty("dbPass");

      DSLContext dsl = DSL.using("jdbc:mariadb://" + dbHost + ":" + dbPort + "/" + dbName, dbUser, dbPass);
      testDSL(dsl);

      DiscordClient client = DiscordClient.create(discordToken);
      client.withGateway(Launcher::testDiscordClient)
          .block();
    }
    catch(IOException _)
    {
      System.out.println("Unknown error while loading properties.");
    }
  }

  private static void testDSL(DSLContext dsl)
  {
    WingRecord w1 = dsl.selectFrom(RAIDBOT.WING)
        .where(RAIDBOT.WING.ID.eq("W1"))
        .fetchAny();
    if(w1 == null)
    {
      w1 = new WingRecord("W1","Spirit Vale");
      dsl.insertInto(RAIDBOT.WING)
          .set(w1)
          .execute();
    }

    BossRecord vg = dsl.selectFrom(RAIDBOT.BOSS)
        .where(RAIDBOT.BOSS.ID.eq("VG"))
        .fetchAny();
    if(vg == null)
    {
      vg = new BossRecord("VG","Vale Guardian", w1.getId(),null);
      dsl.insertInto(RAIDBOT.BOSS)
          .set(vg)
          .execute();
    }

    int vgRoles = dsl.fetchCount(
      dsl.selectFrom(RAIDBOT.ROLE)
          .where(RAIDBOT.ROLE.BOSS.eq(vg.getId())));
    if(vgRoles == 0)
    {
      dsl.insertInto(RAIDBOT.ROLE)
          .columns(RAIDBOT.ROLE.BOSS, RAIDBOT.ROLE.NAME)
          .values(vg.getId(), "Heal")
          .values(vg.getId(), "Heal")
          .values(vg.getId(), "Alac")
          .values(vg.getId(), "Alac")
          .values(vg.getId(), "Quick")
          .values(vg.getId(), "Quick")
          .values(vg.getId(), "Tank")
          .values(vg.getId(), "Condi")
          .values(vg.getId(), "Condi")
          .values(vg.getId(), "Condi")
          .values(vg.getId(), "Boonstrip")
          .execute();
    }

    BossRecord gorse = dsl.selectFrom(RAIDBOT.BOSS)
        .where(RAIDBOT.BOSS.ID.eq("Gorse"))
        .fetchAny();
    if(gorse == null)
    {
      gorse = new BossRecord("Gorse","Gorseval the Multifarious", w1.getId(), vg.getId());
      dsl.insertInto(RAIDBOT.BOSS)
          .set(gorse)
          .execute();
    }

    int gorseRoles = dsl.fetchCount(
        dsl.selectFrom(RAIDBOT.ROLE)
            .where(RAIDBOT.ROLE.BOSS.eq(gorse.getId())));
    if(gorseRoles == 0)
    {
      dsl.insertInto(RAIDBOT.ROLE)
          .columns(RAIDBOT.ROLE.BOSS, RAIDBOT.ROLE.NAME)
          .values(gorse.getId(), "Heal")
          .values(gorse.getId(), "Heal")
          .values(gorse.getId(), "Alac")
          .values(gorse.getId(), "Alac")
          .values(gorse.getId(), "Quick")
          .values(gorse.getId(), "Quick")
          .values(gorse.getId(), "Tank")
          .execute();
    }

    LocalDate now = LocalDate.now();
    int runs = dsl.fetchCount(
        dsl.selectFrom(RAIDBOT.RUN)
            .where(RAIDBOT.RUN.DATE.eq(now)));
    if(runs == 0)
    {
      dsl.insertInto(RAIDBOT.RUN)
          .set(new RunRecord(now, UInteger.valueOf(0), w1.getId()))
          .execute();
    }

    var vgSignups = dsl.select(RAIDBOT.ROLE.ID, RAIDBOT.ROLE.NAME, RAIDBOT.SIGNUP.PLAYER)
        .from(RAIDBOT.RUN)
        .join(RAIDBOT.WING).onKey()
        .join(RAIDBOT.BOSS).onKey()
        .join(RAIDBOT.ROLE).onKey()
        .leftJoin(RAIDBOT.SIGNUP).onKey(Keys.SIGNUP_ROLE_ID_FK)
        .where(RAIDBOT.RUN.DATE.eq(now)
            .and(RAIDBOT.SIGNUP.DATE.isNull().or(RAIDBOT.SIGNUP.DATE.eq(now)))
            .and(RAIDBOT.BOSS.ID.eq(vg.getId())))
        .fetch();

    if(vgSignups.stream().noneMatch(signup -> signup.get(RAIDBOT.SIGNUP.PLAYER) != null))
    {
      Random random = new Random();
      for(Record signup : vgSignups)
      {
        if(random.nextBoolean())
        {
          dsl.insertInto(RAIDBOT.SIGNUP)
              .set(new SignupRecord(now, signup.get(RAIDBOT.ROLE.ID), random.nextLong()))
              .execute();
        }
      }
    }

    System.out.println("Assigned Roles");
    System.out.println(dsl.select(
            RAIDBOT.WING.ID.as("wing"),
            RAIDBOT.BOSS.ID.as("boss"),
            RAIDBOT.ROLE.NAME.as("role"),
            RAIDBOT.SIGNUP.PLAYER)
        .from(RAIDBOT.RUN)
        .join(RAIDBOT.WING).onKey()
        .join(RAIDBOT.BOSS).onKey()
        .join(RAIDBOT.ROLE).onKey()
        .join(RAIDBOT.SIGNUP).onKey(Keys.SIGNUP_ROLE_ID_FK)
        .where(RAIDBOT.RUN.DATE.eq(now)
            .and(RAIDBOT.SIGNUP.DATE.isNull().or(RAIDBOT.SIGNUP.DATE.eq(now))))
        .orderBy(RAIDBOT.RUN.ORDINAL.asc(), RAIDBOT.BOSS.NAME.asc(), RAIDBOT.ROLE.ID.asc())
        .fetch());

    System.out.println("Open Roles");
    System.out.println(dsl.select(
            RAIDBOT.WING.ID.as("wing"),
            RAIDBOT.BOSS.ID.as("boss"),
            RAIDBOT.ROLE.NAME.as("role"),
            RAIDBOT.SIGNUP.PLAYER)
        .from(RAIDBOT.RUN)
        .join(RAIDBOT.WING).onKey()
        .join(RAIDBOT.BOSS).onKey()
        .join(RAIDBOT.ROLE).onKey()
        .leftJoin(RAIDBOT.SIGNUP).onKey(Keys.SIGNUP_ROLE_ID_FK)
        .where(RAIDBOT.RUN.DATE.eq(now)
            .and(RAIDBOT.SIGNUP.ROLE.isNull()))
        .orderBy(RAIDBOT.RUN.ORDINAL.asc(), RAIDBOT.BOSS.NAME.asc(), RAIDBOT.ROLE.ID.asc())
        .fetch());
  }

  private static Mono<Void> testDiscordClient(GatewayDiscordClient client)
  {
    client.on(ReadyEvent.class)
        .next()
        .flatMapMany(readyEvent -> readyEvent.getClient().getGuilds())
        .map(Guild::getName)
        .subscribe(System.out::println);

    return client.logout();
  }
}
