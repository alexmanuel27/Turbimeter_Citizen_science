# Recorded bench sessions

Design: **Ing. Alex M. Rivera**. Updated **26 October 2026**.

JSONL files preserve serial requests/replies and host timings. Summary JSON
files document the setup, calculated results and interpretation limits.
These records contain real bench data, not app demonstration fixtures.

| Session | Setup and result | Records |
| --- | --- | --- |
| 24 October | Closed enclosure, alcohol-filled cuvette, 100 kΩ R1/R2, repaired VREF; 20 valid ratios; mean 0.646405, CV 0.0841%; previous LED pin D7 | [Frames](2026-10-24-closed.jsonl), [summary](2026-10-24-closed-summary.json) |
| 26 October, controller only | Replacement XIAO + ADS1115, no analog base; direct ground jumper; 20 valid RAW acquisitions, no errors; LED pin D10 | [Frames](2026-10-26-controller-only.jsonl), [summary](2026-10-26-controller-only-summary.json) |
| 26 October, complete instrument | Closed enclosure, **empty** cuvette; 20 valid ratios, no errors; mean 6.902305, CV 0.0760%; LED pin D10 | [Frames](2026-10-26-closed-empty.jsonl), [summary](2026-10-26-closed-empty-summary.json) |

The two optical sessions have different sample/setup conditions and are not
an accuracy comparison. An empty cuvette is not a zero-turbidity water blank.
The controller-only record establishes communication/acquisition in that setup,
not optical performance. Host timings include the bench client's polling.

Water-filled-cuvette testing was deferred by the builder. Reference calibration,
independent-sample validation, uncertainty and cross-phone performance remain
pending. No record here establishes FNU/NTU accuracy or a valid measuring range.

See the [firmware README](../README.md) for the test chronology and wiring.
