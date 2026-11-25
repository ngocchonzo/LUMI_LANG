import * as vscode from "vscode";
import { runSource, showBackendError } from "./runner";
import { LumiDiagnostics } from "./diagnostics";

export function activate(context: vscode.ExtensionContext): void {
  const output = vscode.window.createOutputChannel("LumiLang");
  const diagnostics = new LumiDiagnostics();
  context.subscriptions.push(output, diagnostics);

  const runCommand = vscode.commands.registerCommand("lumilang.run", async () => {
    const editor = vscode.window.activeTextEditor;
    if (!editor) {
      vscode.window.showInformationMessage("Open a .lumi file before running LumiLang.");
      return;
    }

    output.clear();
    diagnostics.clear(editor.document);
    output.appendLine(`[LumiLang] Running ${editor.document.fileName}...`);
    output.show(true);

    try {
      const result = await runSource(context.extensionPath, editor.document.getText());
      diagnostics.update(editor.document, result.errors);
      if (result.output.length > 0) {
        output.appendLine("");
        output.appendLine("Output");
        result.output.forEach((line) => output.appendLine(line));
      }
      if (result.errors.length > 0) {
        output.appendLine("");
        result.errors.forEach((error) => {
          output.appendLine(`${error.type} ERROR at ${error.line}:${error.column}: ${error.message}`);
        });
        output.appendLine("Execution aborted.");
      }
      output.appendLine("");
      output.appendLine(result.success ? "[LumiLang] Finished successfully." : "[LumiLang] Finished with errors.");

      // TODO (Visualization owner): expose result.trace and result.variables in a step viewer.
    } catch (error) {
      const message = showBackendError(error);
      output.appendLine(`[LumiLang] ${message}`);
      vscode.window.showErrorMessage(message);
    }
  });

  context.subscriptions.push(runCommand);
}

export function deactivate(): void { }
