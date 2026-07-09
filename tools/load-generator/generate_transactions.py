#!/usr/bin/env python3

"""Generate randomized POS checkout transactions against the API."""

from __future__ import annotations

import argparse
import json
import random
import signal
import sys
import time
import urllib.error
import urllib.request
from dataclasses import dataclass
from decimal import Decimal, ROUND_HALF_UP


DEFAULT_URL = "http://localhost:8080/api/transactions"

PRODUCT_CATALOG: list[tuple[str, Decimal, Decimal]] = [
    ("Bananas", Decimal("0.45"), Decimal("0.95")),
    ("Milk 1L", Decimal("1.09"), Decimal("2.29")),
    ("Bread Loaf", Decimal("1.29"), Decimal("3.49")),
    ("Eggs 12pk", Decimal("2.39"), Decimal("4.99")),
    ("Pasta 500g", Decimal("0.89"), Decimal("2.49")),
    ("Tomato Sauce", Decimal("1.19"), Decimal("3.29")),
    ("Chicken Breast", Decimal("4.99"), Decimal("10.99")),
    ("Ground Coffee", Decimal("4.49"), Decimal("8.99")),
    ("Cheddar Cheese", Decimal("2.49"), Decimal("5.99")),
    ("Orange Juice", Decimal("1.99"), Decimal("4.29")),
    ("Potato Chips", Decimal("1.29"), Decimal("3.49")),
    ("Chocolate Bar", Decimal("0.79"), Decimal("2.49")),
    ("Sparkling Water", Decimal("0.69"), Decimal("1.99")),
    ("Yogurt Cup", Decimal("0.59"), Decimal("1.79")),
    ("Toilet Paper 8pk", Decimal("4.99"), Decimal("12.99")),
    ("Dish Soap", Decimal("1.99"), Decimal("4.99")),
]


@dataclass
class Stats:
    sent: int = 0
    created: int = 0
    failed: int = 0


class LoadGenerator:
    def __init__(
        self,
        url: str,
        min_tpm: int,
        max_tpm: int,
        timeout_seconds: float,
        duration_seconds: float | None,
        seed: int | None,
    ) -> None:
        self.url = url
        self.min_tpm = min_tpm
        self.max_tpm = max_tpm
        self.timeout_seconds = timeout_seconds
        self.duration_seconds = duration_seconds
        self.random = random.Random(seed)
        self.stats = Stats()
        self.stop_requested = False

    def request_stop(self, _signum: int, _frame: object) -> None:
        self.stop_requested = True

    def run(self) -> int:
        start = time.monotonic()
        next_rate_refresh = start
        target_tpm = self.random.randint(self.min_tpm, self.max_tpm)

        print(
            f"Starting load generator: url={self.url}, tpm-range={self.min_tpm}-{self.max_tpm}, "
            f"duration={'unlimited' if self.duration_seconds is None else f'{self.duration_seconds:.0f}s'}"
        )

        while not self.stop_requested:
            now = time.monotonic()
            if self.duration_seconds is not None and now - start >= self.duration_seconds:
                break

            # Re-sample target throughput every minute to keep load moving across the range.
            if now >= next_rate_refresh:
                target_tpm = self.random.randint(self.min_tpm, self.max_tpm)
                next_rate_refresh = now + 60.0
                print(f"[rate] target throughput now {target_tpm} tx/min")

            self._submit_once()
            delay_seconds = 60.0 / target_tpm
            jitter = delay_seconds * self.random.uniform(-0.25, 0.25)
            sleep_time = max(0.01, delay_seconds + jitter)
            time.sleep(sleep_time)

        self._print_summary()
        return 0 if self.stats.failed == 0 else 1

    def _submit_once(self) -> None:
        payload = self._build_transaction()
        body = json.dumps(payload).encode("utf-8")
        request = urllib.request.Request(
            self.url,
            data=body,
            headers={"Content-Type": "application/json"},
            method="POST",
        )

        self.stats.sent += 1
        try:
            with urllib.request.urlopen(request, timeout=self.timeout_seconds) as response:
                status_code = response.getcode()
                if status_code == 201:
                    self.stats.created += 1
                    if self.stats.sent % 25 == 0:
                        print(
                            f"[ok] sent={self.stats.sent} created={self.stats.created} "
                            f"failed={self.stats.failed}"
                        )
                else:
                    self.stats.failed += 1
                    response_body = response.read().decode("utf-8", errors="replace")
                    print(f"[warn] unexpected status={status_code}, body={response_body}")
        except urllib.error.HTTPError as error:
            self.stats.failed += 1
            error_body = error.read().decode("utf-8", errors="replace")
            print(f"[error] status={error.code}, body={error_body}")
        except urllib.error.URLError as error:
            self.stats.failed += 1
            print(f"[error] request failed: {error.reason}")

    def _build_transaction(self) -> dict[str, object]:
        cashier_id = f"cashier-{self.random.randint(1, 10):02d}"
        payment_method = self.random.choice(["CARD", "CASH"])
        item_count = self.random.randint(1, 6)
        chosen_products = self.random.sample(PRODUCT_CATALOG, k=item_count)

        items = []
        for product_name, min_price, max_price in chosen_products:
            quantity = self.random.randint(1, 5)
            unit_price = self._random_price(min_price, max_price)
            items.append(
                {
                    "productName": product_name,
                    "quantity": quantity,
                    "unitPrice": float(unit_price),
                }
            )

        return {
            "cashierId": cashier_id,
            "items": items,
            "paymentMethod": payment_method,
        }

    def _random_price(self, min_price: Decimal, max_price: Decimal) -> Decimal:
        span_cents = int((max_price - min_price) * 100)
        offset_cents = self.random.randint(0, max(0, span_cents))
        candidate = min_price + (Decimal(offset_cents) / Decimal("100"))
        return candidate.quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)

    def _print_summary(self) -> None:
        print(
            "Stopped load generator. "
            f"sent={self.stats.sent}, created={self.stats.created}, failed={self.stats.failed}"
        )


def parse_args(argv: list[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate random checkout transactions against /api/transactions"
    )
    parser.add_argument(
        "--url",
        default=DEFAULT_URL,
        help=f"Transaction API URL (default: {DEFAULT_URL})",
    )
    parser.add_argument(
        "--min-tpm",
        type=int,
        default=10,
        help="Minimum transactions per minute (default: 10)",
    )
    parser.add_argument(
        "--max-tpm",
        type=int,
        default=120,
        help="Maximum transactions per minute (default: 120)",
    )
    parser.add_argument(
        "--timeout-seconds",
        type=float,
        default=5.0,
        help="Per-request timeout in seconds (default: 5)",
    )
    parser.add_argument(
        "--duration-seconds",
        type=float,
        default=None,
        help="Optional run duration in seconds (default: run until interrupted)",
    )
    parser.add_argument(
        "--seed",
        type=int,
        default=None,
        help="Optional random seed for reproducible runs",
    )

    args = parser.parse_args(argv)
    if args.min_tpm < 1:
        parser.error("--min-tpm must be >= 1")
    if args.max_tpm < 1:
        parser.error("--max-tpm must be >= 1")
    if args.min_tpm > args.max_tpm:
        parser.error("--min-tpm must be <= --max-tpm")
    if args.timeout_seconds <= 0:
        parser.error("--timeout-seconds must be > 0")
    if args.duration_seconds is not None and args.duration_seconds <= 0:
        parser.error("--duration-seconds must be > 0 when provided")
    return args


def main(argv: list[str]) -> int:
    args = parse_args(argv)
    generator = LoadGenerator(
        url=args.url,
        min_tpm=args.min_tpm,
        max_tpm=args.max_tpm,
        timeout_seconds=args.timeout_seconds,
        duration_seconds=args.duration_seconds,
        seed=args.seed,
    )

    signal.signal(signal.SIGINT, generator.request_stop)
    signal.signal(signal.SIGTERM, generator.request_stop)

    return generator.run()


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))