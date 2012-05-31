
# Test nested container operation

# SwiftScript-ish
# int A[][];
# int i1 = 31;
# int j1 = 41;
# int k1 = 51;
# A[i1][j1] = k1;
# int i2 = 32;
# int j2 = 42;
# int k2 = 52;
# A[i2][j2] = k2;
# int i3 = 32; # Note i3 == i2
# int j3 = 43;
# int k3 = 53;
# A[i3][j3] = k3;

# This could be implemented without references but is
# done with references here for testing

package require turbine 0.0.1

proc rules { } {

    turbine::allocate_container A integer

    turbine::literal i1 integer 31
    turbine::literal j1 integer 41
    turbine::literal k1 integer 51

    turbine::f_container_create_nested r1 $A $i1 integer
    turbine::f_cref_insert parent "" "$r1 $j1 $k1 $A"

    turbine::literal i2 integer 32
    turbine::literal j2 integer 42
    turbine::literal k2 integer 52

    turbine::f_container_create_nested r2 $A $i2 integer
    turbine::f_cref_insert parent "" "$r2 $j2 $k2 $A"

    turbine::literal i3 integer 31
    turbine::literal j3 integer 43
    turbine::literal k3 integer 53

    turbine::f_container_create_nested r3 $A $i3 integer
    turbine::f_cref_insert parent "" "$r3 $j3 $k3 $A"
}

turbine::defaults
turbine::init $engines $servers
turbine::start rules
turbine::finalize

puts OK

# Help Tcl free memory
proc exit args {}
