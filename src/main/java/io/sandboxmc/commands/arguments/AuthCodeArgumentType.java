package io.sandboxmc.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.Arrays;
import java.util.Collection;

public class AuthCodeArgumentType implements ArgumentType<String> {
  private static final Collection<String> EXAMPLES = Arrays.asList("000000", "123456", "999999");

  public static AuthCodeArgumentType authCode() {
    return new AuthCodeArgumentType();
  }

  public static String getAuthCode(final CommandContext<?> context, final String name) {
    return context.getArgument(name, String.class);
  }

  @Override
  public String parse(final StringReader reader) throws CommandSyntaxException {
    final int start = reader.getCursor();
    final String result = reader.getString();
    if (!result.matches("\\d{6}")) {
      reader.setCursor(start);
      throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect().createWithContext(reader, result);
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