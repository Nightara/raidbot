package de.nightara.discord.raidbot;

import lombok.*;
import org.jooq.*;
import org.jooq.impl.*;
import org.jooq.types.*;

import java.time.*;

import static de.nightara.discord.raidbot.model.Raidbot.*;

@NoArgsConstructor(access=AccessLevel.NONE)
public abstract class Util
{
  public static Result<Record6<UInteger, String, String, UInteger, String, Long>> getSignups(DSLContext dsl, LocalDate date)
  {
    return getSignups(dsl, date, DSL.trueCondition());
  }

  public static Result<Record6<UInteger, String, String, UInteger, String, Long>> getSignups(DSLContext dsl, LocalDate date, Condition condition)
  {
    return dsl.select(RAIDBOT.RUN.ORDINAL,
            RAIDBOT.WING.ID.as("wing"),
            RAIDBOT.BOSS.ID.as("boss"),
            RAIDBOT.ROLE.ID.as("roleId"),
            RAIDBOT.ROLE.NAME.as("role"),
            RAIDBOT.SIGNUP.PLAYER)
        .from(RAIDBOT.RUN)
        .join(RAIDBOT.WING).onKey()
        .join(RAIDBOT.BOSS).onKey()
        .join(RAIDBOT.ROLE).onKey()
        .leftJoin(RAIDBOT.SIGNUP).on(RAIDBOT.SIGNUP.ROLE.eq(RAIDBOT.ROLE.ID).and(RAIDBOT.SIGNUP.DATE.eq(RAIDBOT.RUN.DATE)))
        .where(RAIDBOT.RUN.DATE.eq(date)
            .and(condition))
        .fetch();
  }
}
