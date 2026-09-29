#!/usr/bin/env python3
"""
Automated Health Check & Reliability Probe for Podcast Episode Release System
Used for post-deployment validation, CI/CD health gates, and rollback verification.
"""

import sys
import time
import argparse
import urllib.request
import urllib.error
import json

def perform_health_check(url: str, expected_code: int = 200, timeout: int = 5, retries: int = 6, delay: int = 5) -> bool:
    print("=" * 70)
    print(" PODCAST EPISODE RELEASE SYSTEM - HEALTH CHECK PROBE")
    print("=" * 70)
    print(f"Target URL       : {url}")
    print(f"Expected Status  : {expected_code}")
    print(f"Connection Limits: timeout={timeout}s, max_retries={retries}, delay={delay}s")
    print("-" * 70)

    for attempt in range(1, retries + 1):
        start_time = time.time()
        try:
            req = urllib.request.Request(
                url,
                headers={"User-Agent": "PodcastReleaseHealthCheck/1.0"}
            )
            with urllib.request.urlopen(req, timeout=timeout) as response:
                latency_ms = round((time.time() - start_time) * 1000, 2)
                status_code = response.getcode()
                content = response.read().decode('utf-8', errors='ignore')

                if status_code == expected_code:
                    has_brand = "podcast" in content.lower()
                    print(f"[SUCCESS] Attempt {attempt}/{retries}: Received HTTP {status_code} in {latency_ms}ms (Brand matched: {has_brand})")
                    print("-" * 70)
                    print(json.dumps({
                        "status": "HEALTHY",
                        "target": url,
                        "http_code": status_code,
                        "latency_ms": latency_ms,
                        "attempt": attempt,
                        "brand_verified": has_brand
                    }, indent=2))
                    print("=" * 70)
                    return True
                else:
                    print(f"[WARNING] Attempt {attempt}/{retries}: Unexpected HTTP status {status_code}")

        except urllib.error.HTTPError as he:
            latency_ms = round((time.time() - start_time) * 1000, 2)
            print(f"[FAIL] Attempt {attempt}/{retries}: HTTP Error {he.code} in {latency_ms}ms")
        except urllib.error.URLError as ue:
            print(f"[FAIL] Attempt {attempt}/{retries}: Connection failed: {ue.reason}")
        except Exception as ex:
            print(f"[FAIL] Attempt {attempt}/{retries}: Exception occurred: {str(ex)}")

        if attempt < retries:
            print(f"Retrying in {delay} seconds...")
            time.sleep(delay)

    print("-" * 70)
    print(f"[FATAL] Health check probe failed after {retries} attempts.")
    print("=" * 70)
    return False

def main():
    parser = argparse.ArgumentParser(description="Reliability and Health Check Script")
    parser.add_argument("--url", default="http://localhost:8005/login", help="Target URL to check")
    parser.add_argument("--code", type=int, default=200, help="Expected HTTP status code")
    parser.add_argument("--timeout", type=int, default=5, help="HTTP timeout in seconds")
    parser.add_argument("--retries", type=int, default=6, help="Maximum retry attempts")
    parser.add_argument("--delay", type=int, default=3, help="Delay between retries in seconds")

    args = parser.parse_args()
    success = perform_health_check(
        url=args.url,
        expected_code=args.code,
        timeout=args.timeout,
        retries=args.retries,
        delay=args.delay
    )

    sys.exit(0 if success else 1)

if __name__ == "__main__":
    main()
