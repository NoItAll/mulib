package de.wwu.mulib.search.examples;

import de.wwu.mulib.MulibConfig;
import de.wwu.mulib.TestUtility;
import de.wwu.mulib.search.executors.SymbolicExecution;
import de.wwu.mulib.search.trees.ExceptionPathSolution;
import de.wwu.mulib.search.trees.PathSolution;
import de.wwu.mulib.substitutions.primitives.Sbool;
import de.wwu.mulib.substitutions.primitives.Sint;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
// Taken from https://github.com/SymbolicPathFinder/jpf-symbc/blob/master/src/examples/WBS.java and https://github.com/sosy-lab/sv-benchmarks/blob/master/java/java-ranger-regression/WBS/impl/WBS.java

/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * Symbolic Pathfinder (jpf-symbc) is licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// HW: The class has been renamed and altered to fit Mulib.
public class WBS {

    //Internal state
    private Sint WBS_Node_WBS_BSCU_SystemModeSelCmd_rlt_PRE;
    private Sint WBS_Node_WBS_BSCU_rlt_PRE1;
    private Sint WBS_Node_WBS_rlt_PRE2;

    //Outputs
    private Sint Nor_Pressure;
    private Sint Alt_Pressure;
    private Sint Sys_Mode;

    public WBS() {
        WBS_Node_WBS_BSCU_SystemModeSelCmd_rlt_PRE = Sint.concSint(0);
        WBS_Node_WBS_BSCU_rlt_PRE1 = Sint.concSint(0);
        WBS_Node_WBS_rlt_PRE2 = Sint.concSint(100);
        Nor_Pressure = Sint.concSint(0);
        Alt_Pressure = Sint.concSint(0);
        Sys_Mode = Sint.concSint(0);
    }

    public void update(Sint PedalPos, Sbool AutoBrake,
                       Sbool Skid) {
        SymbolicExecution se = SymbolicExecution.get();
        Sint WBS_Node_WBS_AS_MeterValve_Switch;
        Sint WBS_Node_WBS_AccumulatorValve_Switch;
        Sint WBS_Node_WBS_BSCU_Command_AntiSkidCommand_Normal_Switch;
        Sbool WBS_Node_WBS_BSCU_Command_Is_Normal_Relational_Operator;
        Sint WBS_Node_WBS_BSCU_Command_PedalCommand_Switch1;
        Sint WBS_Node_WBS_BSCU_Command_Switch;
        Sbool WBS_Node_WBS_BSCU_SystemModeSelCmd_Logical_Operator6;
        Sint WBS_Node_WBS_BSCU_SystemModeSelCmd_Unit_Delay;
        Sint WBS_Node_WBS_BSCU_Switch2;
        Sint WBS_Node_WBS_BSCU_Switch3;
        Sint WBS_Node_WBS_BSCU_Unit_Delay1;
        Sint WBS_Node_WBS_Green_Pump_IsolationValve_Switch;
        Sint WBS_Node_WBS_SelectorValve_Switch;
        Sint WBS_Node_WBS_SelectorValve_Switch1;
        Sint WBS_Node_WBS_Unit_Delay2;

        WBS_Node_WBS_Unit_Delay2 = WBS_Node_WBS_rlt_PRE2;
        WBS_Node_WBS_BSCU_Unit_Delay1 = WBS_Node_WBS_BSCU_rlt_PRE1;
        WBS_Node_WBS_BSCU_SystemModeSelCmd_Unit_Delay = WBS_Node_WBS_BSCU_SystemModeSelCmd_rlt_PRE;

        WBS_Node_WBS_BSCU_Command_Is_Normal_Relational_Operator = se.eq(WBS_Node_WBS_BSCU_SystemModeSelCmd_Unit_Delay, se.concSint(0));

        if (se.boolChoice(se.eq(PedalPos, se.concSint(0)))) {
            WBS_Node_WBS_BSCU_Command_PedalCommand_Switch1 = se.concSint(0);
        } else {
            if (se.boolChoice(se.eq(PedalPos, se.concSint(1)))) {
                WBS_Node_WBS_BSCU_Command_PedalCommand_Switch1 = se.concSint(1);
            }  else {
                if (se.boolChoice(se.eq(PedalPos, se.concSint(2)))) {
                    WBS_Node_WBS_BSCU_Command_PedalCommand_Switch1 = se.concSint(2);
                } else {
                    if (se.boolChoice(se.eq(PedalPos, se.concSint(3)))) {
                        WBS_Node_WBS_BSCU_Command_PedalCommand_Switch1 = se.concSint(3);
                    } else {
                        if (se.boolChoice(se.eq(PedalPos, se.concSint(4)))) {
                            WBS_Node_WBS_BSCU_Command_PedalCommand_Switch1 = se.concSint(4);
                        }  else {
                            WBS_Node_WBS_BSCU_Command_PedalCommand_Switch1 = se.concSint(0);
                        }
                    }
                }
            }
        }

        if (se.boolChoice(se.and(AutoBrake, WBS_Node_WBS_BSCU_Command_Is_Normal_Relational_Operator))) {
            WBS_Node_WBS_BSCU_Command_Switch = se.concSint(1);
        }  else {
            WBS_Node_WBS_BSCU_Command_Switch = se.concSint(0);
        }

        WBS_Node_WBS_BSCU_SystemModeSelCmd_Logical_Operator6 =
                se.or(
                    se.and(
                            se.and(
                                se.not(se.eq(WBS_Node_WBS_BSCU_Unit_Delay1, se.concSint(0))),
                                se.lte(WBS_Node_WBS_Unit_Delay2, se.concSint(0))
                             ),
                            WBS_Node_WBS_BSCU_Command_Is_Normal_Relational_Operator
                    ),
                    se.not(WBS_Node_WBS_BSCU_Command_Is_Normal_Relational_Operator)
                );

        if (se.boolChoice(WBS_Node_WBS_BSCU_SystemModeSelCmd_Logical_Operator6)) {
            if (se.boolChoice(Skid))
                WBS_Node_WBS_BSCU_Switch3 = se.concSint(0);
            else
                WBS_Node_WBS_BSCU_Switch3 = se.concSint(4);
        }
        else {
            WBS_Node_WBS_BSCU_Switch3 = se.concSint(4);
        }

        if (se.boolChoice(WBS_Node_WBS_BSCU_SystemModeSelCmd_Logical_Operator6)) {
            WBS_Node_WBS_Green_Pump_IsolationValve_Switch = se.concSint(0);
        }  else {
            WBS_Node_WBS_Green_Pump_IsolationValve_Switch = se.concSint(5);
        }

        if (se.gteChoice(WBS_Node_WBS_Green_Pump_IsolationValve_Switch, se.concSint(1))) {
            WBS_Node_WBS_SelectorValve_Switch1 = se.concSint(0);
        }
        else {
            WBS_Node_WBS_SelectorValve_Switch1 = se.concSint(5);
        }

        if (se.boolChoice(se.not(WBS_Node_WBS_BSCU_SystemModeSelCmd_Logical_Operator6))) {
            WBS_Node_WBS_AccumulatorValve_Switch = se.concSint(0);
        }  else {
            if (se.gteChoice(WBS_Node_WBS_SelectorValve_Switch1, se.concSint(1))) {
                WBS_Node_WBS_AccumulatorValve_Switch = WBS_Node_WBS_SelectorValve_Switch1;
            }
            else {
                WBS_Node_WBS_AccumulatorValve_Switch = se.concSint(5);
            }
        }

        if (se.eqChoice(WBS_Node_WBS_BSCU_Switch3, se.concSint(0))) {
            WBS_Node_WBS_AS_MeterValve_Switch = se.concSint(0);
        }  else {
            if (se.eqChoice(WBS_Node_WBS_BSCU_Switch3, se.concSint(1)))  {
                WBS_Node_WBS_AS_MeterValve_Switch = WBS_Node_WBS_AccumulatorValve_Switch.div(se.concSint(4), se);
            }  else {
                if (se.eqChoice(WBS_Node_WBS_BSCU_Switch3, se.concSint(2)))  {
                    WBS_Node_WBS_AS_MeterValve_Switch = WBS_Node_WBS_AccumulatorValve_Switch.div(se.concSint(2), se);
                }  else {
                    if (se.eqChoice(WBS_Node_WBS_BSCU_Switch3, se.concSint(3))) {
                        WBS_Node_WBS_AS_MeterValve_Switch = WBS_Node_WBS_AccumulatorValve_Switch.div(se.concSint(4), se).mul(se.concSint(3), se);
                    }  else {
                        if (se.eqChoice(WBS_Node_WBS_BSCU_Switch3, se.concSint(4))) {
                            WBS_Node_WBS_AS_MeterValve_Switch = WBS_Node_WBS_AccumulatorValve_Switch;
                        }  else {
                            WBS_Node_WBS_AS_MeterValve_Switch = se.concSint(0);
                        }
                    }
                }
            }
        }

        if (se.boolChoice(Skid)) {
            WBS_Node_WBS_BSCU_Command_AntiSkidCommand_Normal_Switch = se.concSint(0);
        }  else {
            WBS_Node_WBS_BSCU_Command_AntiSkidCommand_Normal_Switch = WBS_Node_WBS_BSCU_Command_Switch.add(WBS_Node_WBS_BSCU_Command_PedalCommand_Switch1, se);
        }

        if (se.boolChoice(WBS_Node_WBS_BSCU_SystemModeSelCmd_Logical_Operator6)) {
            Sys_Mode = se.concSint(1);
        }  else {
            Sys_Mode = se.concSint(0);
        }

        if (se.boolChoice(WBS_Node_WBS_BSCU_SystemModeSelCmd_Logical_Operator6)) {
            WBS_Node_WBS_BSCU_Switch2 = se.concSint(0);
        }  else {
            if (se.boolChoice(se.and(se.gte(WBS_Node_WBS_BSCU_Command_AntiSkidCommand_Normal_Switch, se.concSint(0)),
                    se.lt(WBS_Node_WBS_BSCU_Command_AntiSkidCommand_Normal_Switch, se.concSint(1))))) {
                WBS_Node_WBS_BSCU_Switch2 = se.concSint(0);
            } else {
                if (se.boolChoice(se.and(se.gte(WBS_Node_WBS_BSCU_Command_AntiSkidCommand_Normal_Switch, se.concSint(1)),
                        se.lt(WBS_Node_WBS_BSCU_Command_AntiSkidCommand_Normal_Switch, se.concSint(2)))))  {
                    WBS_Node_WBS_BSCU_Switch2 = se.concSint(1);
                }  else {
                    if (se.boolChoice(se.and(se.gte(WBS_Node_WBS_BSCU_Command_AntiSkidCommand_Normal_Switch, se.concSint(2)),
                            se.lt(WBS_Node_WBS_BSCU_Command_AntiSkidCommand_Normal_Switch, se.concSint(3))))) {
                        WBS_Node_WBS_BSCU_Switch2 = se.concSint(2);
                    } else {
                        if (se.boolChoice(se.and(se.gte(WBS_Node_WBS_BSCU_Command_AntiSkidCommand_Normal_Switch, se.concSint(3)),
                                se.lt(WBS_Node_WBS_BSCU_Command_AntiSkidCommand_Normal_Switch, se.concSint(4)))))  {
                            WBS_Node_WBS_BSCU_Switch2 = se.concSint(3);
                        } else {
                            WBS_Node_WBS_BSCU_Switch2 = se.concSint(4);
                        }
                    }
                }
            }
        }

        if (se.gteChoice(WBS_Node_WBS_Green_Pump_IsolationValve_Switch, se.concSint(1)))  {
            WBS_Node_WBS_SelectorValve_Switch = WBS_Node_WBS_Green_Pump_IsolationValve_Switch;
        }  else {
            WBS_Node_WBS_SelectorValve_Switch = se.concSint(0);
        }

        if (se.eqChoice(WBS_Node_WBS_BSCU_Switch2, se.concSint(0))) {
            Nor_Pressure = se.concSint(0);
        }  else {
            if (se.eqChoice(WBS_Node_WBS_BSCU_Switch2, se.concSint(1))) {
                Nor_Pressure = WBS_Node_WBS_SelectorValve_Switch.div(se.concSint(4), se);
            }  else {
                if (se.eqChoice(WBS_Node_WBS_BSCU_Switch2, se.concSint(2))) {
                    Nor_Pressure = WBS_Node_WBS_SelectorValve_Switch.div(se.concSint(2), se);
                }  else {
                    if (se.eqChoice(WBS_Node_WBS_BSCU_Switch2, se.concSint(3))) {
                        Nor_Pressure = WBS_Node_WBS_SelectorValve_Switch.div(se.concSint(4), se).mul(se.concSint(3), se);
                    } else {
                        if (se.eqChoice(WBS_Node_WBS_BSCU_Switch2, se.concSint(4))) {
                            Nor_Pressure = WBS_Node_WBS_SelectorValve_Switch;
                        } else {
                            Nor_Pressure = se.concSint(0);
                        }
                    }
                }
            }
        }

        if (se.eqChoice(WBS_Node_WBS_BSCU_Command_PedalCommand_Switch1, se.concSint(0))) {
            Alt_Pressure = se.concSint(0);
        }  else {
            if (se.eqChoice(WBS_Node_WBS_BSCU_Command_PedalCommand_Switch1, se.concSint(1))) {
                Alt_Pressure = WBS_Node_WBS_AS_MeterValve_Switch.div(se.concSint(4), se);
            }  else {
                if (se.eqChoice(WBS_Node_WBS_BSCU_Command_PedalCommand_Switch1, se.concSint(2))) {
                    Alt_Pressure = WBS_Node_WBS_AS_MeterValve_Switch.div(se.concSint(2), se);
                } else {
                    if (se.eqChoice(WBS_Node_WBS_BSCU_Command_PedalCommand_Switch1, se.concSint(3))) {
                        Alt_Pressure = WBS_Node_WBS_AS_MeterValve_Switch.div(se.concSint(4), se).mul(se.concSint(3), se);
                    } else {
                        if (se.eqChoice(WBS_Node_WBS_BSCU_Command_PedalCommand_Switch1, se.concSint(4))) {
                            Alt_Pressure = WBS_Node_WBS_AS_MeterValve_Switch;
                        } else {
                            Alt_Pressure = se.concSint(0);
                        }
                    }
                }
            }
        }

        WBS_Node_WBS_rlt_PRE2 = Nor_Pressure;

        WBS_Node_WBS_BSCU_rlt_PRE1 = WBS_Node_WBS_BSCU_Switch2;

        WBS_Node_WBS_BSCU_SystemModeSelCmd_rlt_PRE = Sys_Mode;

    }

    public static void launch() {
        SymbolicExecution se = SymbolicExecution.get();
        Sint pedal1 = se.namedSymSint("pedal1");
        Sbool auto1 = se.namedSymSbool("auto1");
        Sbool skid1 = se.namedSymSbool("skid1");
        Sint pedal2 = se.namedSymSint("pedal2");
        Sbool auto2 = se.namedSymSbool("auto2");
        Sbool skid2 = se.namedSymSbool("skid2");
        Sint pedal3 = se.namedSymSint("pedal3");
        Sbool auto3 = se.namedSymSbool("auto3");
        Sbool skid3 = se.namedSymSbool("skid3");
        WBS wbs = new WBS();
        wbs.update(pedal1, auto1, skid1);
        wbs.update(pedal2, auto2, skid2);
        wbs.update(pedal3, auto3, skid3);
    }

    @Test @Disabled
    public void testLaunch() {
        TestUtility.getAllSolutions(this::_checkLaunch, "launch");
    }

    private List<PathSolution> _checkLaunch(MulibConfig.MulibConfigBuilder mb) {
        List<PathSolution> result = TestUtility.executeMulib(
                "launch",
                WBS.class,
                1,
                mb,
                false
        );
        assertEquals(13824, result.size());
        assertTrue(result.stream().noneMatch(ps -> ps instanceof ExceptionPathSolution));
        return result;
    }
}