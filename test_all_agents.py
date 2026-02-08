"""
🧪 KAIRONEX BRAIN - MASTER TEST SUITE
======================================
Runs ALL agent brain tests in sequence.
Designed for hackathon judges to verify system integrity.

Usage:
    python test_all_agents.py

Tests ALL 5 agent brains:
1. 🌍 Radius Brain   - Cultural Survival Engine (12 handlers)
2. 🧬 Vitality Brain - Life Logistics Engine (20+ handlers)
3. 🎓 Study Brain    - Cognitive Supply Chain (3 branches)
4. ⚔️ Campaign Brain - Career Strategist (8 event types)
5. 🛡️ Survival Protocol - Domain Logic (existing tests)

User: demo_user_001
All tests run OFFLINE with no external API calls.
"""

import sys
import os
import time
from datetime import datetime

sys.path.insert(0, os.path.dirname(__file__))
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))


def separator():
    print("\n" + "=" * 70)


def run_suite(name, module_name):
    """Import and run a test module's run_all_tests()."""
    print(f"\n{'=' * 70}")
    print(f"  RUNNING: {name}")
    print(f"{'=' * 70}")

    start = time.time()
    try:
        mod = __import__(module_name)
        passed = mod.run_all_tests()
        elapsed = time.time() - start
        return passed, elapsed, mod.PASS_COUNT, mod.FAIL_COUNT
    except Exception as e:
        elapsed = time.time() - start
        print(f"\n  ❌ SUITE CRASHED: {e}")
        import traceback
        traceback.print_exc()
        return False, elapsed, 0, 1


def main():
    print("=" * 70)
    print("  🧠 KAIRONEX BRAIN — COMPREHENSIVE TEST SUITE")
    print(f"  📅 {datetime.now().isoformat()}")
    print(f"  👤 User: demo_user_001")
    print(f"  🖥️ Platform: {sys.platform}")
    print(f"  🐍 Python: {sys.version.split()[0]}")
    print("=" * 70)

    total_start = time.time()

    suites = [
        ("🌍 Radius Brain  — Cultural Survival", "test_radius_brain"),
        ("🧬 Vitality Brain — Life Logistics", "test_vitality_brain"),
        ("🎓 Study Brain    — Cognitive System", "test_study_brain"),
        ("⚔️ Campaign Brain — Career Strategist", "test_campaign_brain"),
    ]

    results = []
    total_pass = 0
    total_fail = 0

    for name, module_name in suites:
        passed, elapsed, p, f = run_suite(name, module_name)
        results.append((name, passed, elapsed, p, f))
        total_pass += p
        total_fail += f

    total_elapsed = time.time() - total_start

    # === FINAL REPORT ===
    print("\n\n" + "=" * 70)
    print("  📊 FINAL TEST REPORT")
    print("=" * 70)

    for name, passed, elapsed, p, f in results:
        status = "✅ PASS" if passed else "❌ FAIL"
        print(f"  {status}  {name:48s}  {p:3d}✅  {f:2d}❌  ({elapsed:.1f}s)")

    print("-" * 70)
    print(f"  TOTAL: {total_pass} passed, {total_fail} failed ({total_elapsed:.1f}s)")
    print("=" * 70)

    if total_fail == 0:
        print("\n  🎉🎉🎉  ALL TESTS PASSED!  🎉🎉🎉")
        print("  Kaironex Brain is ready for production.")
    else:
        print(f"\n  ⚠️ {total_fail} test(s) failed. Review output above.")

    print()
    return total_fail == 0


if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
