package solutions.xapient.thinkr.domain.switches

// Perfekter Spieler (Tests, Screenshots): stellt jede Weiche fuer die Perle, die als naechste dort ankommt; Perlen in wrong lenkt er absichtlich falsch
object Pilot {
    fun steer(s: SwitchesState, t: Long, wrong: Set<Int> = emptySet()): SwitchesState {
        var x = SwitchesGame.tick(s, t)
        val want = HashMap<Int, Pair<Double, Boolean>>()
        for (b in x.beads) {
            val node = x.net.nodes[x.net.edges[b.edge].to]
            if (node.kind != Kind.SWITCH) continue
            val left = x.net.edges[b.edge].track.length - x.progress(b)
            val goal = if (b.id in wrong) (b.target + 1) % x.net.gateCount else b.target
            val need = x.net.route(goal)[node.slot] ?: continue
            val cur = want[node.slot]
            if (cur == null || left < cur.first) want[node.slot] = left to need
        }
        for ((slot, w) in want) if (x.on[slot] != w.second) x = SwitchesGame.input(x, slot, t)
        return x
    }

    fun play(s: SwitchesState, until: Long, step: Long = 16, wrong: Set<Int> = emptySet()): SwitchesState {
        var x = s
        var t = x.round.now
        while (t < until) {
            t = minOf(t + step, until)
            x = steer(x, t, wrong)
        }
        return x
    }
}
