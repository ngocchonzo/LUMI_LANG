# Backend and extension JSON contract

The Java CLI reads the complete `.lumi` source from standard input and writes exactly one JSON object to standard output. Technical logs belong on standard error.

```json
{
  "success": true,
  "output": ["Final score:", "85"],
  "errors": [],
  "trace": [
    {
      "step": 1,
      "line": 1,
      "statement": "let score = 70;",
      "variables": { "score": 70 },
      "output": []
    }
  ],
  "variables": { "score": 85, "bonus": 15 }
}
```

Rules agreed for the MVP:

- `line` and `column` are 1-based. The extension converts them to 0-based ranges for VS Code diagnostics.
- `success` is false if parsing or execution reports an error.
- `errors[].type` is `SYNTAX`, `SEMANTIC`, `RUNTIME`, or `INTERNAL`.
- `output` is an array of display-ready strings.
- `trace` contains execution snapshots for the later Step-by-Step/Variables Viewer work.
- The interpreter has one global scope and a 1,000 executed-statement safety limit.
