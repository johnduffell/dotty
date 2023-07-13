/* sbt -- Simple Build Tool
 * Copyright 2008, 2009 Mark Harrah
 */
package dotty.tools.xsbt;

import java.util.List;

import scala.Tuple2;
import scala.collection.mutable.HashMap;
import scala.jdk.javaapi.CollectionConverters;

import dotty.tools.dotc.core.Contexts.Context;
import dotty.tools.dotc.reporting.AbstractReporter;
import dotty.tools.dotc.reporting.CodeAction;
import dotty.tools.dotc.reporting.Diagnostic;
import dotty.tools.dotc.reporting.Message;
import dotty.tools.dotc.util.SourceFile;
import dotty.tools.dotc.util.SourcePosition;
import xsbti.Position;
import xsbti.Severity;

import java.util.Collections;
import java.util.function.*;

final public class DelegatingReporter extends AbstractReporter {
  private xsbti.Reporter delegate;
  private final BiFunction<DelegatingReporter, SourceFile, String> baseLookup;
  private final Function<SourceFile, String> lookup;

  public DelegatingReporter(xsbti.Reporter delegate, BiFunction<DelegatingReporter, SourceFile, String> baseLookup) {
    super();
    this.delegate = delegate;
    this.baseLookup = baseLookup;
    this.lookup = sourceFile -> baseLookup.apply(this, sourceFile);
  }

  public void dropDelegate() {
    delegate = null;
  }

  @Override
  public void printSummary(Context ctx) {
    delegate.printSummary();
  }

  public void doReport(Diagnostic dia, Context ctx) {
    Severity severity = severityOf(dia.level());
    Position position = positionOf(dia.pos().nonInlined());

    StringBuilder rendered = new StringBuilder();
    rendered.append(messageAndPos(dia, ctx));
    Message message = dia.msg();
    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append(message.message());
    String diagnosticCode = String.valueOf(message.errorId().errorNumber());
    boolean shouldExplain = Diagnostic.shouldExplain(dia, ctx);
    List<CodeAction> actions = CollectionConverters.asJava(message.actions(ctx));
    if (shouldExplain && !message.explanation().isEmpty()) {
      rendered.append(explanation(message, ctx));
      messageBuilder.append(System.lineSeparator()).append(explanation(message, ctx));
    }

    delegate.log(new Problem(position, messageBuilder.toString(), severity, rendered.toString(), diagnosticCode, actions, lookup));
  }

  public void reportBasicWarning(String message) {
    Position position = PositionBridge.noPosition;
    Severity severity = Severity.Warn;
    String diagnosticCode = "-1"; // no error code
    List<CodeAction> actions = Collections.emptyList();
    delegate.log(new Problem(position, message, severity, message, diagnosticCode, actions, lookup));
  }

  private static Severity severityOf(int level) {
    Severity severity;
    switch (level) {
      case dotty.tools.dotc.interfaces.Diagnostic.ERROR: severity = Severity.Error; break;
      case dotty.tools.dotc.interfaces.Diagnostic.WARNING: severity = Severity.Warn; break;
      case dotty.tools.dotc.interfaces.Diagnostic.INFO: severity = Severity.Info; break;
      default:
        throw new IllegalArgumentException(String.format("Bad diagnostic level: %s", level));
    }
    return severity;
  }

  private Position positionOf(SourcePosition pos) {
    if (pos.exists()) {
      return new PositionBridge(pos, lookup.apply(pos.source()));
    } else {
      return PositionBridge.noPosition;
    }
  }

  @SuppressWarnings("unchecked")
  // [warn] sbt-bridge/src/dotty/tools/xsbt/DelegatingReporter.java:18:1: dotty$tools$dotc$reporting$UniqueMessagePositions$$positions() in dotty.tools.dotc.reporting.AbstractReporter implements dotty$tools$dotc$reporting$UniqueMessagePositions$$positions() in dotty.tools.dotc.reporting.UniqueMessagePositions
  // [warn]   return type requires unchecked conversion from scala.collection.mutable.HashMap to scala.collection.mutable.HashMap<scala.Tuple2<dotty.tools.dotc.util.SourceFile,java.lang.Integer>,dotty.tools.dotc.reporting.Diagnostic>
  public HashMap<Tuple2<SourceFile, Integer>, Diagnostic> dotty$tools$dotc$reporting$UniqueMessagePositions$$positions() {
    return (HashMap<Tuple2<SourceFile, Integer>, Diagnostic>) super.dotty$tools$dotc$reporting$UniqueMessagePositions$$positions();
  }
}
