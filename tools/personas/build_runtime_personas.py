#!/usr/bin/env python3
"""Build the runtime persona registry from the audited master CSV."""

from __future__ import annotations

import argparse
import csv
import json
from collections import Counter
from pathlib import Path


VALID_FACTIONS = {
    "HISPANIC_CREATORS",
    "INTERNATIONAL_CREATORS",
    "MINECRAFT_LEGENDS",
}
EXPECTED_FACTIONS = {
    "HISPANIC_CREATORS": 308,
    "INTERNATIONAL_CREATORS": 397,
    "MINECRAFT_LEGENDS": 26,
}
RUNTIME_FIELDS = {
    "id": "persona_id",
    "displayName": "display_name",
    "minecraftUsername": "minecraft_username",
    "minecraftUsernameStatus": "minecraft_username_status",
    "uuid": "uuid",
    "skinUrl": "skin_url",
    "skinStrategy": "skin_strategy",
    "faction": "faction",
    "legendKind": "legend_kind",
}


def parse_bool(value: str, row_number: int) -> bool:
    normalized = value.strip().lower()
    if normalized == "true":
        return True
    if normalized == "false":
        return False
    raise ValueError(
        f"row {row_number}: unique_while_alive must be True or False, got {value!r}"
    )


def build(source: Path, destination: Path) -> None:
    with source.open("r", encoding="utf-8-sig", newline="") as csv_file:
        reader = csv.DictReader(csv_file)
        required = set(RUNTIME_FIELDS.values()) | {"tier", "unique_while_alive"}
        missing = required.difference(reader.fieldnames or ())
        if missing:
            raise ValueError(f"missing required CSV columns: {', '.join(sorted(missing))}")

        personas: list[dict[str, object]] = []
        seen_ids: set[str] = set()
        faction_counts: Counter[str] = Counter()
        tier_counts: Counter[int] = Counter()

        for row_number, row in enumerate(reader, start=2):
            persona_id = row["persona_id"].strip()
            display_name = row["display_name"].strip()
            faction = row["faction"].strip()

            if not persona_id:
                raise ValueError(f"row {row_number}: persona_id must not be empty")
            if persona_id in seen_ids:
                raise ValueError(f"row {row_number}: duplicate persona_id {persona_id!r}")
            if not display_name:
                raise ValueError(f"row {row_number}: display_name must not be empty")
            if faction not in VALID_FACTIONS:
                raise ValueError(f"row {row_number}: unknown faction {faction!r}")

            try:
                tier = int(row["tier"])
            except ValueError as error:
                raise ValueError(f"row {row_number}: tier must be an integer") from error
            if tier not in range(1, 6):
                raise ValueError(f"row {row_number}: tier must be between 1 and 5")

            persona: dict[str, object] = {
                runtime_name: row[csv_name].strip()
                for runtime_name, csv_name in RUNTIME_FIELDS.items()
            }
            persona["tier"] = tier
            persona["uniqueWhileAlive"] = parse_bool(
                row["unique_while_alive"], row_number
            )
            personas.append(persona)
            seen_ids.add(persona_id)
            faction_counts[faction] += 1
            tier_counts[tier] += 1

    if len(personas) != 731:
        raise ValueError(f"expected 731 personas, got {len(personas)}")
    if dict(faction_counts) != EXPECTED_FACTIONS:
        raise ValueError(
            f"unexpected faction counts: {dict(sorted(faction_counts.items()))}"
        )

    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(
        json.dumps(personas, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print(f"TOTAL = {len(personas)}")
    for faction in sorted(VALID_FACTIONS):
        print(f"{faction} = {faction_counts[faction]}")
    for tier in range(1, 6):
        print(f"T{tier} = {tier_counts[tier]}")


def main() -> None:
    script_dir = Path(__file__).resolve().parent
    repository_root = script_dir.parents[1]
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--source",
        type=Path,
        default=script_dir / "hostile-humans-final-persona-dataset.csv",
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=repository_root
        / "src/main/resources/data/hostile_humans/personas/personas.json",
    )
    arguments = parser.parse_args()
    build(arguments.source, arguments.output)


if __name__ == "__main__":
    main()
