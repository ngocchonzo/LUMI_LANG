import * as vscode from "vscode";
import type { LumiError } from "./runner";

/** Maps the backend's 1-based error positions to VS Code's 0-based document ranges. */
export class LumiDiagnostics implements vscode.Disposable {
  private readonly collection = vscode.languages.createDiagnosticCollection("lumilang");

  update(document: vscode.TextDocument, errors: LumiError[]): void {
    if (document.languageId !== "lumilang") return;

    const diagnostics = errors.map((error) => {
      const lineNumber = Math.max(0, Math.min(error.line - 1, document.lineCount - 1));
      const line = document.lineAt(lineNumber);
      const startCharacter = Math.max(0, Math.min(error.column - 1, line.text.length));
      const endCharacter = Math.min(startCharacter + 1, line.text.length);
      const range = new vscode.Range(lineNumber, startCharacter, lineNumber, endCharacter);
      const diagnostic = new vscode.Diagnostic(
        range,
        `${error.type}: ${error.message}`,
        vscode.DiagnosticSeverity.Error
      );
      diagnostic.source = "LumiLang";
      diagnostic.code = error.type;
      return diagnostic;
    });

    this.collection.set(document.uri, diagnostics);
  }

  clear(document: vscode.TextDocument): void {
    this.collection.delete(document.uri);
  }

  dispose(): void {
    this.collection.dispose();
  }
}
