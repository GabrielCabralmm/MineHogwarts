package com.gabriel.hogwartsmod.client.trace;

import com.gabriel.hogwartsmod.network.StudentsPacket;
import com.gabriel.hogwartsmod.teach.TeachRules;

import java.util.List;

public final class ClientTeachState {
    public static TeachRules.Limits limits = new TeachRules.Limits(10, 10);
    public static int range = 8;
    public static String spell = "";
    public static List<StudentsPacket.Entry> students = List.of();

    private ClientTeachState() {
    }
}
