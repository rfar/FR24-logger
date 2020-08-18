# fr24-logger

Data logger for FlightRadar24 website.

## Usage

- Run using `lein run`. Logs will be saved under `logs/` folder.
- Call `(stop)` to terminate logging.

## Logs

- For each run a subfolder within `logs/` will be created with start `yyyy-mm-dd_hh-mm-ss` stamp.
- Each aircraft’s log will be saved in a CSV file, named the aircraft’s ICAO flight number, under the created log folder.


