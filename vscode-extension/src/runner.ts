import { spawn } from "node:child_process";
import * as path from "node:path";

export interface LumiError {
  type: string;
  line: number;
  column: number;
  message: string;
}

export interface ExecutionStep {
  step: number;
  line: number;
  statement: string;
  variables: Record<string, number | string | boolean>;
  output: string[];
}

export interface ExecutionResult {
  success: boolean;
  output: string[];
  errors: LumiError[];
  trace: ExecutionStep[];
  variables: Record<string, number | string | boolean>;
}

/** Runs the shaded Java backend. Source is passed on stdin; stdout contains JSON only. */
export function runSource(extensionPath: string, source: string): Promise<ExecutionResult> {
  const jarPath = path.resolve(extensionPath, "..", "backend", "target", "lumilang-backend.jar");

  return new Promise((resolve, reject) => {
    const child = spawn("java", ["-jar", jarPath], { stdio: ["pipe", "pipe", "pipe"] });
    let stdout = "";
    let stderr = "";
    let settled = false;
    const timeout = setTimeout(() => {
      child.kill();
      finish(new Error("LumiLang backend timed out after 10 seconds."));
    }, 10_000);

    const finish = (error?: Error, result?: ExecutionResult) => {
      if (settled) return;
      settled = true;
      clearTimeout(timeout);
      if (error) reject(error);
      else resolve(result!);
    };

    child.stdout.setEncoding("utf8");
    child.stderr.setEncoding("utf8");
    child.stdout.on("data", (chunk: string) => { stdout += chunk; });
    child.stderr.on("data", (chunk: string) => { stderr += chunk; });
    child.on("error", (error) => finish(new Error(`Could not start Java: ${error.message}`)));
    child.on("close", (code) => {
      if (code !== 0) {
        finish(new Error(stderr.trim() || `Backend exited with code ${code}.`));
        return;
      }
      try {
        finish(undefined, JSON.parse(stdout) as ExecutionResult);
      } catch {
        finish(new Error(`Backend returned invalid JSON. ${stderr.trim()}`));
      }
    });

    child.stdin.end(source, "utf8");
  });
}

export function showBackendError(error: unknown): string {
  const message = error instanceof Error ? error.message : String(error);
  if (message.includes("ENOENT") || message.includes("Could not start Java")) {
    return "Java was not found. Install Java 17 or newer, then restart VS Code.";
  }
  if (message.includes("lumilang-backend.jar")) {
    return "Backend JAR is missing. From the project root, run: cd backend && mvn package.";
  }
  return message;
}

