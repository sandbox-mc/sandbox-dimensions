package io.sandboxmc.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.Collection;

public class AuthCodeArgumentType implements ArgumentType<String> {
  private static final Collection<String> EXAMPLES = Arrays.asList("000000", "123456", "999999");
  private static final String MATCH_REGEX = "^\\d{1,6}$";
  public static final DynamicCommandExceptionType INVALID_AUTH_CODE = new DynamicCommandExceptionType(o -> Text.literal("Invalid auth code (must be a 6-digit code): " + o));

  public static AuthCodeArgumentType authCode() {
    return new AuthCodeArgumentType();
  }

  public static String getAuthCode(final CommandContext<?> context, final String name) {
    return context.getArgument(name, String.class);
  }

  @Override
  public String parse(final StringReader reader) throws CommandSyntaxException {
    String result = reader.readUnquotedString();
    if (!result.matches(MATCH_REGEX)) { // let them type up to 6 digits so it doesn't look bad while entering it
      reader.setCursor(reader.getCursor());
      throw INVALID_AUTH_CODE.createWithContext(reader, result);
    }
    return result;
  }

  @Override
  public String toString() {
    return "AuthCodeArgumentType";
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}