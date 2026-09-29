"""
Master script to regenerate all Word (.docx) reports for Weeks 3 to 15.
"""

import sys, os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import scripts.generate_reports_p1 as p1
import scripts.generate_reports_p2 as p2
import scripts.generate_reports_p3 as p3

def main():
    print("Regenerating all weekly DevOps reports (Weeks 3 to 15)...")
    p1.generate_week_3()
    p1.generate_week_4()
    p1.generate_week_5()
    p1.generate_week_6()
    
    p2.generate_week_7()
    p2.generate_week_8()
    p2.generate_week_9()
    p2.generate_week_10()
    
    p3.generate_week_11()
    p3.generate_week_12()
    p3.generate_week_13()
    p3.generate_week_14()
    p3.generate_week_15()
    print("All 13 reports generated successfully!")

if __name__ == '__main__':
    main()
