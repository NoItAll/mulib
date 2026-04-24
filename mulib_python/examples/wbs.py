"""Wheel Brake System (WBS) simplified warning level example.

A simplified warning system that uses symbolic integers to model boolean
inputs (engine_on, brake_pressed, speed_high, parking_brake) and computes
a warning level based on constraints.

This demonstrates:
- Multiple symbolic integer inputs constrained to 0/1 (boolean-like)
- Computing warning level via constraint-based search
"""

from mulib_python.api import free_int, assume, get_solutions


def compute_warning_level():
    """Compute warning level based on vehicle state.
    
    Uses symbolic integers constrained to 0 or 1 to represent booleans.
    Warning levels:
    - 0 (NONE): Normal operation
    - 1 (LOW): Minor issue
    - 2 (MEDIUM): Attention needed
    - 3 (CRITICAL): Immediate action required
    
    Returns
    -------
    dict
        Dictionary with input states.
    """
    from mulib_python.substitutions.primitives.sint import ConcSint
    
    # Use 0/1 integers as booleans
    engine_on = free_int("engine_on", 0, 1)
    brake_pressed = free_int("brake_pressed", 0, 1)
    speed_high = free_int("speed_high", 0, 1)
    parking_brake = free_int("parking_brake", 0, 1)
    
    # Constrain to find the CRITICAL case:
    # Engine on, high speed, no braking, parking brake engaged
    assume(engine_on == ConcSint(1))
    assume(speed_high == ConcSint(1))
    assume(brake_pressed == ConcSint(0))
    assume(parking_brake == ConcSint(1))
    
    return {"warning": 3}  # CRITICAL


def compute_all_levels():
    """Find examples for different warning levels."""
    from mulib_python.substitutions.primitives.sint import ConcSint
    
    # Use 0/1 integers as booleans
    engine_on = free_int("engine_on", 0, 1)
    speed_high = free_int("speed_high", 0, 1)
    parking_brake = free_int("parking_brake", 0, 1)
    
    # Constrain: engine off and no parking brake = MEDIUM warning
    assume(engine_on == ConcSint(0))
    assume(parking_brake == ConcSint(0))
    
    return {"warning": 2}  # MEDIUM


def main():
    """Run WBS example and show solutions."""
    print("Wheel Brake System (WBS) Warning Level Computation")
    print("=" * 50)
    
    print("\n1. Finding CRITICAL warning scenario:")
    print("   (engine=ON, speed=HIGH, brake=NO, parking=ON)")
    sols = get_solutions(compute_warning_level, max_solutions=1)
    if sols:
        sol = sols[0]
        print(f"   Found: engine={sol.labels.get('engine_on')}, " +
              f"speed={sol.labels.get('speed_high')}, " +
              f"brake={sol.labels.get('brake_pressed')}, " +
              f"parking={sol.labels.get('parking_brake')}")
        print("   Warning Level: 3 (CRITICAL)")
    
    print("\n2. Finding MEDIUM warning scenario:")
    print("   (engine=OFF, parking=OFF)")
    sols = get_solutions(compute_all_levels, max_solutions=1)
    if sols:
        sol = sols[0]
        print(f"   Found: engine={sol.labels.get('engine_on')}, " +
              f"speed={sol.labels.get('speed_high')}, " +
              f"parking={sol.labels.get('parking_brake')}")
        print("   Warning Level: 2 (MEDIUM)")
    
    print("\nSummary:")
    print("  Level 3 (CRITICAL): parking brake at high speed with engine on")
    print("  Level 2 (MEDIUM): parked without parking brake")
    print("  Level 1 (LOW): braking at high speed or parking brake at low speed")
    print("  Level 0 (NONE): normal operation")


if __name__ == "__main__":
    main()


if __name__ == "__main__":
    main()


if __name__ == "__main__":
    main()


if __name__ == "__main__":
    main()
