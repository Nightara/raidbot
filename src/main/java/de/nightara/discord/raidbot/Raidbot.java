package de.nightara.discord.raidbot;

import com.fasterxml.jackson.databind.*;
import de.nightara.discord.raidbot.model.tables.records.*;
import discord4j.common.*;
import discord4j.core.*;
import discord4j.core.event.domain.interaction.*;
import discord4j.core.event.domain.lifecycle.*;
import discord4j.core.object.command.*;
import discord4j.core.object.entity.*;
import discord4j.discordjson.json.*;
import discord4j.rest.*;
import discord4j.rest.service.*;
import org.jooq.*;
import org.jooq.exception.*;
import org.jooq.impl.*;
import org.jooq.types.*;
import reactor.core.publisher.*;

import java.io.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static de.nightara.discord.raidbot.model.Raidbot.*;

public class Raidbot
{
  private static DSLContext database;

  static void main()
  {
    try(InputStream is = Files.newInputStream(Path.of("properties.xml")))
    {
      Properties props =  new Properties();
      props.loadFromXML(is);

      String discordToken   = props.getProperty("discordToken");
      String dbType  = props.getProperty("dbType","mariadb");
      String dbHost  = props.getProperty("dbHost","localhost");
      String dbPort  = props.getProperty("dbPort","3306");
      String dbName  = props.getProperty("dbName");
      String dbUser  = props.getProperty("dbUser");
      String dbPass  =  props.getProperty("dbPass");

      database = DSL.using("jdbc:" + dbType + "://" + dbHost + ":" + dbPort + "/" + dbName, dbUser, dbPass);

      DiscordClient.create(discordToken).login()
          .doOnNext(client -> registerCommands(client)
              .thenMany(client.on(ChatInputInteractionEvent.class, Raidbot::handleChatCommand))
                .subscribe())
          .flatMap(GatewayDiscordClient::onDisconnect)
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

  private static List<ApplicationCommandRequest> buildCommands()
  {
    try
    {
      List<ApplicationCommandRequest> commands = new LinkedList<>();
      ObjectMapper jacksonMapper = JacksonResources.create().getObjectMapper();

      commands.add(jacksonMapper.readValue(getResourceFileAsString("commands/" + "add-wing" + ".json"),
          ApplicationCommandRequest.class));
      commands.add(jacksonMapper.readValue(getResourceFileAsString("commands/" + "show-raid" + ".json"),
          ApplicationCommandRequest.class));
      commands.add(jacksonMapper.readValue(getResourceFileAsString("commands/" + "shutdown" + ".json"),
          ApplicationCommandRequest.class));
      commands.add(jacksonMapper.readValue(getResourceFileAsString("commands/" + "sign-up" + ".json"),
          ApplicationCommandRequest.class));

      return commands;
    }
    catch(Exception _)
    {
      return List.of();
    }
  }

  private static Flux<ApplicationCommandData> registerCommands(GatewayDiscordClient client)
  {
    RestClient restClient = client.getRestClient();
    ApplicationService appService = restClient.getApplicationService();

    return client.on(ReadyEvent.class)
        .next()
        .flatMap(_ -> restClient.getApplicationId())
        .flatMapMany(appId -> Flux.fromIterable(buildCommands())
            .flatMap(command -> appService.createGlobalApplicationCommand(appId, command)));
  }

  private static Mono<Void> handleChatCommand(ChatInputInteractionEvent event)
  {
    return switch(event.getCommandName())
    {
      case "add-wing" -> handleAddWing(event);
      case "show-raid" -> handleShowRaid(event);
      case "shutdown" -> handleShutdown(event);
      case "sign-up" -> handleSignUp(event);
      default -> event.reply("Unknown command " + event.getCommandName()).withEphemeral(true);
    };
  }

  private static Mono<Void> handleAddWing(ChatInputInteractionEvent event)
  {
    return getOptionFromCommand(event,"date", LocalDate.now())
        .flatMap(date ->
            getOptionFromCommand(event,"wing","NONE").map(wing ->
                database.insertInto(RAIDBOT.RUN)
                    .values(date, DSL.selectCount().from(RAIDBOT.RUN).where(RAIDBOT.RUN.DATE.eq(date)), wing)
                    .onDuplicateKeyIgnore()
                    .returning()
                    .fetch()))
        .map(insertedRows -> insertedRows.isEmpty() ? "Failure" : insertedRows.getFirst().toString())
        .flatMap(event::reply);
  }

  //TODO: Only sign up once per boss
  private static Mono<Void> handleSignUp(ChatInputInteractionEvent event)
  {
    return getOptionFromCommand(event,"date", LocalDate.now())
        .flatMap(date ->
            getOptionFromCommand(event,"role","NONE").map(role ->
            {
              Condition condition = getOptionFromCommand(event,"wing","")
                  .filter(Predicate.not(String::isEmpty))
                  .map(RAIDBOT.WING.ID::eq)
                  .or(getOptionFromCommand(event,"boss","NONE")
                      .filter(Predicate.not(String::isEmpty))
                      .map(RAIDBOT.BOSS.ID::eq))
                  .blockOptional()
                  .orElse(DSL.trueCondition());

              Result<Record6<UInteger, String, String, UInteger, String, Long>> openRoles =
                  SqlUtil.getSignups(database, date, RAIDBOT.ROLE.NAME.eq(role)
                      .and(RAIDBOT.SIGNUP.PLAYER.isNull())
                      .and(condition));

              return database.insertInto(RAIDBOT.SIGNUP)
                  .set(openRoles.map(openRole ->
                      new SignupRecord(date, openRole.value4(), event.getUser().getId().asLong())))
                  .returning()
                  .fetch();
            }))
        .map(insertedRows -> insertedRows.isEmpty() ? "Failure" : "Signed up for " + insertedRows.size() + " bosses")
        .flatMap(event::reply);
  }

  private static Mono<Void> handleShowRaid(ChatInputInteractionEvent event)
  {
    return getOptionFromCommand(event,"date", LocalDate.now())
        .map(date -> SqlUtil.getSignups(database, date))
        .map(Result::toString)
        .flatMap(event::reply);
  }

  private static Mono<Void> handleShutdown(ChatInputInteractionEvent event)
  {
    return event.reply("Shutting down...")
        .withEphemeral(true)
        .then(event.getClient().logout());
  }

  private static <T> Mono<T> getOptionFromCommand(ChatInputInteractionEvent event, String optionName, T defaultValue)
  {
    return Mono.justOrEmpty(event.getOption(optionName)
            .flatMap(ApplicationCommandInteractionOption::getValue))
        .map(value -> parseCommandOption(value, defaultValue))
        .onErrorComplete()
        .defaultIfEmpty(defaultValue);
  }

  @SuppressWarnings("unchecked")
  private static <T> T parseCommandOption(ApplicationCommandInteractionOptionValue value, T defaultValue)
  {
    return switch(defaultValue)
    {
      case LocalDate localDate -> (T) LocalDate.parse(value.asString());
      case Long l -> (T) (Long) value.asLong();
      case Double v -> (T) (Double) value.asDouble();
      case Boolean b -> (T) (Boolean) value.asBoolean();
      case String s -> (T) value.asString();
      case Attachment attachment -> (T) value.asAttachment();
      case null, default -> defaultValue;
    };
  }

  private static String getResourceFileAsString(String fileName) throws IOException {
    ClassLoader classLoader = Raidbot.class.getClassLoader();
    try (InputStream resourceAsStream = classLoader.getResourceAsStream(fileName)) {
      if (resourceAsStream == null) return null;
      try (InputStreamReader inputStreamReader = new InputStreamReader(resourceAsStream);
           BufferedReader reader = new BufferedReader(inputStreamReader)) {
        return reader.lines().collect(Collectors.joining(System.lineSeparator()));
      }
    }
  }
}
