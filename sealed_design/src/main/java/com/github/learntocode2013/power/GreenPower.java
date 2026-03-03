package com.github.learntocode2013.power;

public sealed interface GreenPower extends PowerSource permits
    WindPower,
    SolarPower{

}
