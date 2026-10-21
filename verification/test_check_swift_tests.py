import re
import unittest
from pathlib import Path

from check_swift_tests import CASES, verify


def passing_log():
    lines = ['✔ Test ' + name + '() passed after 0.001 seconds.' for name in CASES]
    lines.append('✔ Test run with 5 tests in 2 suites passed after 0.002 seconds.')
    for module, mode in [('Serde', '5'), ('SerdeCore', '5'),
                         ('KotlinRuntimeSupport', '5'), ('KotlinCoroutineSupport', '5'),
                         ('SwiftTestHarnessTests', '6')]:
        lines.append('swift-frontend -module-name ' + module + ' -swift-version ' + mode
                     + ' -warnings-as-errors -target arm64-apple-macosx15.0')
    return '\n'.join(lines)


class SwiftAcceptanceTest(unittest.TestCase):
    def test_case_roster_matches_actual_sources(self):
        folder = Path(__file__).resolve().parents[1] / 'swift-test-harness/Tests/SwiftTestHarnessTests'
        declared = set()
        for path in folder.glob('*.swift'):
            declared.update(re.findall(r'@Test(?:\("[^"]+"\))?\s+func (\w+)\(', path.read_text()))
        self.assertEqual(declared, set(CASES))

    def test_actual_count_and_modes(self):
        self.assertEqual(verify(passing_log())['actual_behavior_tests'], 5)

    def test_missing_or_duplicate_case_is_rejected(self):
        text = passing_log()
        for candidate in ['\n'.join(text.splitlines()[1:]), text + '\n' + text.splitlines()[0]]:
            with self.assertRaises(ValueError):
                verify(candidate)

    def test_zero_total_is_rejected(self):
        with self.assertRaises(ValueError):
            verify(passing_log().replace('with 5 tests', 'with 0 tests'))

    def test_failure_or_skip_is_rejected(self):
        for outcome in ['failed', 'skipped']:
            with self.assertRaises(ValueError):
                verify(passing_log() + '\nTest extra() ' + outcome)

    def test_warning_policy_and_dependency_mode_are_required(self):
        for text in [passing_log().replace('-warnings-as-errors', ''),
                     passing_log().replace('-swift-version 5', '-swift-version 6')]:
            with self.assertRaises(ValueError):
                verify(text)

    def test_final_language_mode_wins(self):
        with self.assertRaises(ValueError):
            verify(passing_log().replace('-module-name Serde -swift-version 5',
                                         '-module-name Serde -swift-version 5 -swift-version 6'))

    def test_authored_mode6_and_core_frontend_are_required(self):
        for text in [passing_log().replace('SwiftTestHarnessTests -swift-version 6',
                                           'SwiftTestHarnessTests -swift-version 5'),
                     '\n'.join(line for line in passing_log().splitlines() if '-module-name SerdeCore ' not in line)]:
            with self.assertRaises(ValueError):
                verify(text)


if __name__ == '__main__':
    unittest.main()
