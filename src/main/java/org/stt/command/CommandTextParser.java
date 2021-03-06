package org.stt.command;

import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.NotNull;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.stt.g4.EnglishCommandsBaseVisitor;
import org.stt.g4.EnglishCommandsParser;
import org.stt.g4.EnglishCommandsVisitor;
import org.stt.model.TimeTrackingItem;

import static org.stt.g4.EnglishCommandsParser.CommandContext;

/**
 * Created by dante on 06.12.14.
 */
public class CommandTextParser {
    private EnglishCommandsVisitor<Object> parserVisitor = new MyEnglishCommandsBaseVisitor();

    public CommandTextItem walk(final TokenStream stream, CommandContext commandContext) {
        return (CommandTextItem) commandContext.accept(parserVisitor);
    }

    private static class MyEnglishCommandsBaseVisitor extends EnglishCommandsBaseVisitor<Object> {
        @Override
        public DateTime visitDate(@NotNull EnglishCommandsParser.DateContext ctx) {
            return new DateTime(ctx.year, ctx.month, ctx.day, 0, 0);
        }

        @Override
        public DateTime visitDateTime(@NotNull EnglishCommandsParser.DateTimeContext ctx) {
            DateTime result = ctx.date() != null ? (DateTime) visitDate(ctx.date()) : DateTime.now().withTimeAtStartOfDay();
            result = result.withHourOfDay(ctx.hour).withMinuteOfHour(ctx.minute).withSecondOfMinute(ctx.second);
            return result;
        }

        @Override
        public DateTime[] visitSinceFormat(@NotNull EnglishCommandsParser.SinceFormatContext ctx) {
            return new DateTime[]{(DateTime) visitDateTime(ctx.start), ctx.end != null ? (DateTime) visitDateTime(ctx.end) : null};
        }

        @Override
        public DateTime[] visitAgoFormat(@NotNull EnglishCommandsParser.AgoFormatContext ctx) {
            Duration duration;
            int amount = ctx.amount;
            EnglishCommandsParser.TimeUnitContext timeUnit = ctx.timeUnit();
            if (timeUnit.HOURS() != null) {
                duration = Duration.standardHours(amount);
            } else if (timeUnit.MINUTES() != null) {
                duration = Duration.standardMinutes(amount);
            } else if (timeUnit.SECONDS() != null) {
                duration = Duration.standardSeconds(amount);
            } else {
                throw new IllegalStateException("Unknown ago unit: " + ctx.getText());
            }
            return new DateTime[]{DateTime.now().minus(duration), null};
        }

        @Override
        public DateTime[] visitFromToFormat(@NotNull EnglishCommandsParser.FromToFormatContext ctx) {
            DateTime start = (DateTime) visitDateTime(ctx.start);
            DateTime end = ctx.end != null ? (DateTime) visitDateTime(ctx.end) : null;
            return new DateTime[]{start, end};
        }

        @Override
        public Object visitTimeFormat(@NotNull EnglishCommandsParser.TimeFormatContext ctx) {
            Object result = super.visitTimeFormat(ctx);
            if (result == null) {
                return new DateTime[]{DateTime.now(), null};
            }
            return result;
        }

        @Override
        public CommandTextItem visitItemWithComment(@NotNull EnglishCommandsParser.ItemWithCommentContext ctx) {
            DateTime[] period = (DateTime[]) visitTimeFormat(ctx.timeFormat());
            if (period[1] != null) {
                return new NewItemCommandTextItem(ctx.text, period[0], period[1]);
            } else {
                return new NewItemCommandTextItem(ctx.text, period[0]);
            }
        }

        @Override
        public CommandTextItem visitFinCommand(@NotNull EnglishCommandsParser.FinCommandContext ctx) {
            DateTime date = null;
        	if (ctx.at != null) {
        		date = visitDateTime(ctx.at);
            }
        	else {
        		date = DateTime.now();
        	}
        	
        	return new EndItemCommandTextItem(date, ctx.RESUME() != null);
        }

        @Override
        public CommandTextItem visitCommand(@NotNull CommandContext ctx) {
            if (ctx.finCommand() != null) {
                return visitFinCommand(ctx.finCommand());
            } else if (ctx.itemWithComment() != null) {
                return visitItemWithComment(ctx.itemWithComment());
            } else {
                throw new IllegalStateException("Unknown command " + ctx.getText());
            }
        }
    }
}
