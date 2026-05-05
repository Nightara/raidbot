package de.nightara.discord.raidbot;

import de.nightara.discord.raidbot.model.tables.records.*;
import discord4j.core.*;
import discord4j.core.event.domain.lifecycle.*;
import discord4j.core.object.entity.*;
import org.jooq.*;
import org.jooq.exception.*;
import org.jooq.impl.*;
import org.jooq.types.*;
import reactor.core.publisher.*;

import java.io.*;
import java.io.IOException;
import java.math.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import static de.nightara.discord.raidbot.model.Raidbot.*;

public class Demo
{
  static void main()
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
      client.withGateway(Demo::setUpCommands)
          .block();
    }
    catch(IOException _)
    {
      System.out.println("Unable to read config file properties.xml.");
    }
    catch(DataAccessException _)
    {
      System.out.println("Unable to connect to database. Please check the database configuration in properties.xml.");
    }
  }

  private static Mono<Void> setUpCommands(GatewayDiscordClient client)
  {
    client.on(ReadyEvent.class)
        .next()
        .flatMapMany(readyEvent -> readyEvent.getClient().getGuilds())
        .map(Guild::getName)
        .subscribe(System.out::println);

    return client.logout();
  }

  private static void testDSL(DSLContext dsl) throws DataAccessException
  {
    Random random = new Random();
    LocalDate now = LocalDate.now();

    boolean runExists = dsl.selectCount()
        .from(RAIDBOT.RUN)
        .where(RAIDBOT.RUN.DATE.eq(now))
        .fetchOptional()
        .map(Record1::component1)
        .map(count -> count > 0)
        .orElse(false);

    if(!runExists)
    {
      Result<WingRecord> wings = dsl.selectFrom(RAIDBOT.WING)
          .where(DSL.rand().le(BigDecimal.valueOf(0.5)))
          .orderBy(DSL.rand().asc())
          .fetch();

      AtomicInteger ordinal = new AtomicInteger(0);
      dsl.insertInto(RAIDBOT.RUN)
          .set(wings.map(wing ->
              new RunRecord(now, UInteger.valueOf(ordinal.getAndIncrement()), wing.getId())))
          .execute();

      var signups = Util.getSignups(dsl, now, DSL.rand().le(BigDecimal.valueOf(0.2)));
      dsl.insertInto(RAIDBOT.SIGNUP)
          .set(signups.map(signup ->
              new SignupRecord(now, signup.get(RAIDBOT.ROLE.ID.as("roleId")), random.nextLong())))
          .execute();
    }

    System.out.println("Assigned Roles");
    System.out.println(Util.getSignups(dsl, now, RAIDBOT.SIGNUP.PLAYER.isNotNull()));
    System.out.println("Open Roles");
    System.out.println(Util.getSignups(dsl, now, RAIDBOT.SIGNUP.PLAYER.isNull()));
  }
}
